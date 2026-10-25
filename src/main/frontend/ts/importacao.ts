import { el, elemento } from "./dom.js";
import { formatarNumero } from "./formato.js";
import type { ResultadoImportacao } from "./tipos.js";

export function renderizarResultado(r: ResultadoImportacao): void {
  const alvo = el("resultado-importacao");
  const numeros = elemento("dl");
  const contagens: [string, number][] = [
    ["Linhas lidas", r.linhasLidas],
    ["Importadas", r.importadas],
    ["Rejeitadas", r.rejeitadas],
    ["Duplicadas", r.duplicadas],
  ];
  for (const [rotulo, valor] of contagens) numeros.append(elemento("dt", rotulo), elemento("dd", formatarNumero(valor)));

  alvo.replaceChildren(
    elemento("p", r.mensagem),
    numeros,
    elemento("p", `Arquivo ${r.arquivo} processado em ${(r.tempoMs / 1000).toFixed(1)} s.`),
  );

  const motivos = Object.entries(r.motivosRejeicao);
  if (motivos.length) {
    const lista = elemento("ul");
    for (const [motivo, quantidade] of motivos) lista.append(elemento("li", `${motivo}: ${quantidade}`));
    alvo.append(elemento("p", "Motivos de rejeição:"), lista);
  }

  if (r.exemplosRejeicao.length) {
    const lista = elemento("ul");
    for (const e of r.exemplosRejeicao) lista.append(elemento("li", `Linha ${e.linha}: ${e.motivo}`));
    alvo.append(elemento("p", "Linhas rejeitadas (primeiras ocorrências):"), lista);
  }
}

export function mostrarErroImportacao(mensagem: string): void {
  el("resultado-importacao").replaceChildren(elemento("p", mensagem, "resultado-erro"));
}
