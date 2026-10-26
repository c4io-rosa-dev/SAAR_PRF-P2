import { el, elemento } from "./dom.js";
import { formatarNumero } from "./formato.js";
export function renderizarResultado(r) {
    const alvo = el("resultado-importacao");
    const numeros = elemento("dl", undefined, "resultado-numeros");
    const contagens = [
        ["Linhas lidas", r.linhasLidas, ""],
        ["Importadas", r.importadas, "ok"],
        ["Rejeitadas", r.rejeitadas, "rejeitado"],
        ["Duplicadas", r.duplicadas, "duplicado"],
    ];
    for (const [rotulo, valor, classe] of contagens) {
        const bloco = elemento("div", undefined, classe);
        bloco.append(elemento("dt", rotulo), elemento("dd", formatarNumero(valor)));
        numeros.append(bloco);
    }
    alvo.replaceChildren(elemento("p", r.mensagem, "resultado-titulo"), numeros, elemento("p", `Arquivo ${r.arquivo} processado em ${(r.tempoMs / 1000).toFixed(1)} s.`, "resultado-meta"));
    const motivos = Object.entries(r.motivosRejeicao);
    if (motivos.length) {
        const lista = elemento("ul");
        for (const [motivo, quantidade] of motivos)
            lista.append(elemento("li", `${motivo}: ${quantidade}`));
        alvo.append(elemento("h4", "Motivos de rejeição"), lista);
    }
    if (r.exemplosRejeicao.length) {
        const lista = elemento("ul");
        for (const e of r.exemplosRejeicao)
            lista.append(elemento("li", `Linha ${e.linha}: ${e.motivo}`));
        alvo.append(elemento("h4", "Linhas rejeitadas (primeiras ocorrências)"), lista);
    }
}
export function mostrarErroImportacao(mensagem) {
    el("resultado-importacao").replaceChildren(elemento("p", mensagem, "resultado-erro"));
}
