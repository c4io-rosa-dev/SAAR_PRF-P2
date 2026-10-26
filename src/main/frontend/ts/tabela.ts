import { el, elemento } from "./dom.js";
import { formatarData, formatarNumero } from "./formato.js";
import type { Acidente, PaginaResposta } from "./tipos.js";

export interface AcoesTabela {
  detalhar(acidente: Acidente): void;
  editar(acidente: Acidente): void;
  excluir(acidente: Acidente): void;
}

const TOTAL_COLUNAS = 9;

function botaoAcao(icone: string, rotulo: string, acao: () => void, classe = ""): HTMLButtonElement {
  const b = elemento("button", undefined, `acao ${classe}`);
  b.type = "button";
  b.title = rotulo;
  b.setAttribute("aria-label", rotulo);
  b.append(elemento("span", icone, "icone"));
  b.addEventListener("click", acao);
  return b;
}

// a etiqueta resume a gravidade com as vítimas do registro, como no boletim da PRF
function etiquetaGravidade(a: Acidente): HTMLSpanElement {
  if (a.classificacao === "Com Vítimas Fatais") {
    const feridos = a.feridos ? ` / ${a.feridos} ${a.feridos === 1 ? "ferido" : "feridos"}` : "";
    return elemento("span", `${a.mortos} ${a.mortos === 1 ? "fatal" : "fatais"}${feridos}`, "etiqueta etiqueta-fatal");
  }
  if (a.classificacao === "Com Vítimas Feridas") {
    return elemento("span", `${a.feridos} ${a.feridos === 1 ? "ferido" : "feridos"}`, "etiqueta etiqueta-ferido");
  }
  return elemento("span", a.classificacao, "etiqueta etiqueta-neutra");
}

export function renderizarTabela(pagina: PaginaResposta<Acidente>, acoes: AcoesTabela): void {
  const corpo = el<HTMLTableSectionElement>("corpo-tabela");
  corpo.replaceChildren();

  if (pagina.conteudo.length === 0) {
    const celula = corpo.insertRow().insertCell();
    celula.colSpan = TOTAL_COLUNAS;
    celula.className = "vazio";
    celula.textContent = "Nenhum registro encontrado.";
  }

  for (const a of pagina.conteudo) {
    const linha = corpo.insertRow();

    const id = elemento("button", String(a.id), "link-id");
    id.type = "button";
    id.addEventListener("click", () => acoes.detalhar(a));
    linha.insertCell().append(id);

    linha.insertCell().textContent = `${formatarData(a.dataOcorrencia)} ${a.horario.slice(0, 5)}`;
    const uf = linha.insertCell();
    uf.className = "uf";
    uf.textContent = a.uf;
    linha.insertCell().append(elemento("span", `${a.rodovia} km ${formatarNumero(a.quilometro)}`, "chip-rodovia"));
    linha.insertCell().textContent = a.municipio;
    linha.insertCell().textContent = a.tipoAcidente;
    const causa = linha.insertCell();
    causa.className = "causa";
    causa.textContent = a.causaAcidente;
    causa.title = a.causaAcidente;
    linha.insertCell().append(etiquetaGravidade(a));

    const celulaAcoes = linha.insertCell();
    celulaAcoes.className = "acoes";
    celulaAcoes.append(
      botaoAcao("edit", `Editar registro ${a.id}`, () => acoes.editar(a)),
      botaoAcao("delete", `Excluir registro ${a.id}`, () => acoes.excluir(a), "acao-excluir"),
    );
  }

  const inicio = pagina.totalElementos === 0 ? 0 : pagina.pagina * pagina.tamanho + 1;
  const fim = pagina.pagina * pagina.tamanho + pagina.conteudo.length;
  const resumo = el("pag-resumo");
  resumo.replaceChildren(
    "Exibindo ",
    elemento("strong", `${formatarNumero(inicio)}–${formatarNumero(fim)}`),
    " de ",
    elemento("strong", formatarNumero(pagina.totalElementos)),
    " ocorrências",
  );
  el("pag-info").textContent = pagina.totalPaginas === 0
    ? "Sem páginas"
    : `Página ${formatarNumero(pagina.pagina + 1)} de ${formatarNumero(pagina.totalPaginas)}`;
  el<HTMLButtonElement>("pag-anterior").disabled = pagina.pagina === 0;
  el<HTMLButtonElement>("pag-proxima").disabled = pagina.pagina + 1 >= pagina.totalPaginas;
}
