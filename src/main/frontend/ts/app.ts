import { api, ApiErro } from "./api.js";
import { CLASSIFICACOES, TIPOS_ACIDENTE, UFS } from "./constantes.js";
import { fecharDetalhe, mostrarDetalhe, registroEmDetalhe } from "./detalhe.js";
import { el, preencherOpcoes } from "./dom.js";
import { formatarData, formatarNumero } from "./formato.js";
import {
  abrirNovoRegistro, fecharFormulario, lerFormulario, limparFormulario, marcarErros, preencherFormulario,
} from "./formulario.js";
import { configurarGraficos, desenharCausas, desenharMensal } from "./graficos.js";
import { mostrarErroImportacao, renderizarResultado } from "./importacao.js";
import { renderizarKpis } from "./kpis.js";
import { mensagemDeErro, mostrarMensagem } from "./mensagens.js";
import { renderizarTabela } from "./tabela.js";
import type { Acidente, Filtro } from "./tipos.js";

const TAMANHO_PAGINA = 50;
const FILTRO_VAZIO: Filtro = { uf: "", classificacao: "", dataInicio: "", dataFim: "" };

let pagina = 0;
let filtro: Filtro = { ...FILTRO_VAZIO };

const filtroAtivo = (): boolean => Object.values(filtro).some((valor) => valor !== "");

function descreverFiltro(): string {
  const partes: string[] = [];
  if (filtro.uf) partes.push(`UF ${filtro.uf}`);
  if (filtro.classificacao) partes.push(filtro.classificacao);
  if (filtro.dataInicio || filtro.dataFim) {
    const inicio = filtro.dataInicio ? formatarData(filtro.dataInicio) : "início";
    const fim = filtro.dataFim ? formatarData(filtro.dataFim) : "hoje";
    partes.push(`${inicio} a ${fim}`);
  }
  return partes.length ? partes.join(" · ") : "todos os registros";
}

async function carregarTabela(): Promise<void> {
  try {
    const resultado = filtroAtivo()
      ? await api.filtrar(filtro, pagina, TAMANHO_PAGINA)
      : await api.listar(pagina, TAMANHO_PAGINA);
    if (pagina > 0 && resultado.conteudo.length === 0 && resultado.totalPaginas > 0) {
      pagina = resultado.totalPaginas - 1;
      return await carregarTabela();
    }
    renderizarTabela(resultado, { detalhar: mostrarDetalhe, editar: preencherFormulario, excluir });
    const rotulo = filtroAtivo() ? " no filtro" : "";
    el("contador").textContent = `${formatarNumero(resultado.totalElementos)}${rotulo}`;
  } catch (erro) {
    mostrarMensagem(mensagemDeErro(erro), "erro");
  }
}

async function carregarIndicadores(): Promise<void> {
  try {
    const [resumo, causas, mensal] = await Promise.all([api.resumo(filtro), api.topCausas(filtro), api.mensal(filtro)]);
    renderizarKpis(resumo);
    desenharCausas(causas, resumo.total);
    desenharMensal(mensal);
    if (!filtroAtivo()) {
      const sugestoes = el<HTMLDataListElement>("lista-causas");
      sugestoes.replaceChildren();
      preencherOpcoes(sugestoes, causas.map((c) => c.causa));
    }
  } catch (erro) {
    mostrarMensagem(mensagemDeErro(erro), "erro");
  }
}

function atualizarTudo(): Promise<unknown> {
  el("criterios-ativos").textContent = descreverFiltro();
  return Promise.all([carregarTabela(), carregarIndicadores()]);
}

async function salvar(evento: SubmitEvent): Promise<void> {
  evento.preventDefault();
  const botaoSalvar = el<HTMLButtonElement>("btn-salvar");
  botaoSalvar.disabled = true;
  const { id, dados } = lerFormulario();
  try {
    if (id) {
      await api.atualizar(id, dados);
      mostrarMensagem(`Registro #${id} atualizado.`);
    } else {
      const criado = await api.criar(dados);
      mostrarMensagem(`Registro #${criado.id} cadastrado.`);
    }
    fecharFormulario();
    await atualizarTudo();
  } catch (erro) {
    if (erro instanceof ApiErro) marcarErros(erro.erros);
    mostrarMensagem(mensagemDeErro(erro), "erro");
  } finally {
    botaoSalvar.disabled = false;
  }
}

async function excluir(a: Acidente): Promise<void> {
  if (!confirm(`Excluir o registro #${a.id} (${a.municipio}/${a.uf})?`)) return;
  try {
    await api.excluir(a.id);
    mostrarMensagem(`Registro #${a.id} excluído.`);
    if (el<HTMLInputElement>("campo-id").value === String(a.id)) limparFormulario();
    if (registroEmDetalhe()?.id === a.id) fecharDetalhe();
    await atualizarTudo();
  } catch (erro) {
    mostrarMensagem(mensagemDeErro(erro), "erro");
  }
}

async function buscarPorId(): Promise<void> {
  const id = Number(el<HTMLInputElement>("busca-id").value);
  if (!Number.isInteger(id) || id <= 0) {
    mostrarMensagem("Informe um ID válido.", "erro");
    return;
  }
  try {
    const acidente = await api.buscar(id);
    if (acidente) mostrarDetalhe(acidente);
    else mostrarMensagem(`Registro ${id} não encontrado (HTTP 404).`, "erro");
  } catch (erro) {
    mostrarMensagem(mensagemDeErro(erro), "erro");
  }
}

async function importar(): Promise<void> {
  const arquivo = el<HTMLInputElement>("arquivo").files?.[0];
  if (!arquivo) {
    mostrarMensagem("Selecione um arquivo CSV.", "erro");
    return;
  }
  const botao = el<HTMLButtonElement>("btn-importar");
  const rotulo = botao.lastChild!;
  botao.disabled = true;
  rotulo.textContent = "Importando...";
  try {
    const resultado = await api.importar(arquivo);
    renderizarResultado(resultado);
    mostrarMensagem(resultado.mensagem);
    pagina = 0;
    await atualizarTudo();
  } catch (erro) {
    const mensagem = mensagemDeErro(erro);
    mostrarErroImportacao(mensagem);
    mostrarMensagem(mensagem, "erro");
  } finally {
    botao.disabled = false;
    rotulo.textContent = "Importar";
  }
}

function aplicarFiltros(): void {
  filtro = {
    uf: el<HTMLSelectElement>("filtro-uf").value,
    classificacao: el<HTMLSelectElement>("filtro-classificacao").value,
    dataInicio: el<HTMLInputElement>("filtro-inicio").value,
    dataFim: el<HTMLInputElement>("filtro-fim").value,
  };
  pagina = 0;
  void atualizarTudo();
}

function limparFiltros(): void {
  for (const id of ["filtro-uf", "filtro-classificacao", "filtro-inicio", "filtro-fim"]) {
    el<HTMLInputElement | HTMLSelectElement>(id).value = "";
  }
  filtro = { ...FILTRO_VAZIO };
  pagina = 0;
  void atualizarTudo();
}

function abrirImportacao(): void {
  el<HTMLInputElement>("arquivo").value = "";
  el("resultado-importacao").replaceChildren();
  el<HTMLDialogElement>("dialogo-importacao").showModal();
}

function iniciar(): void {
  configurarGraficos();
  preencherOpcoes(el<HTMLSelectElement>("filtro-uf"), UFS);
  preencherOpcoes(el<HTMLSelectElement>("campo-uf"), UFS);
  preencherOpcoes(el<HTMLSelectElement>("filtro-classificacao"), CLASSIFICACOES);
  preencherOpcoes(el<HTMLSelectElement>("campo-classificacao"), CLASSIFICACOES);
  preencherOpcoes(el<HTMLDataListElement>("lista-tipos"), TIPOS_ACIDENTE);

  for (const botao of document.querySelectorAll<HTMLButtonElement>("[data-fechar]")) {
    botao.addEventListener("click", () => botao.closest("dialog")?.close());
  }
  el<HTMLDialogElement>("dialogo-formulario").addEventListener("close", limparFormulario);

  el("btn-novo").addEventListener("click", abrirNovoRegistro);
  el("btn-abrir-importacao").addEventListener("click", abrirImportacao);
  el<HTMLFormElement>("form-acidente").addEventListener("submit", (e) => void salvar(e));
  el("btn-cancelar").addEventListener("click", fecharFormulario);
  el("btn-editar-detalhe").addEventListener("click", () => {
    const registro = registroEmDetalhe();
    fecharDetalhe();
    if (registro) preencherFormulario(registro);
  });
  el("btn-filtrar").addEventListener("click", aplicarFiltros);
  el("btn-limpar-filtros").addEventListener("click", limparFiltros);
  el("btn-buscar").addEventListener("click", () => void buscarPorId());
  el("busca-id").addEventListener("keydown", (e) => {
    if (e.key === "Enter") void buscarPorId();
  });
  el("btn-importar").addEventListener("click", () => void importar());
  el("pag-anterior").addEventListener("click", () => {
    pagina--;
    void carregarTabela();
  });
  el("pag-proxima").addEventListener("click", () => {
    pagina++;
    void carregarTabela();
  });

  void atualizarTudo();
}

iniciar();
