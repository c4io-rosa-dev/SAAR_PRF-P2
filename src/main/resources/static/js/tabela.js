import { el, elemento } from "./dom.js";
import { formatarData, formatarNumero } from "./formato.js";
const TOTAL_COLUNAS = 10;
function botao(texto, acao, classe) {
    const b = elemento("button", texto, `pequeno ${classe}`);
    b.type = "button";
    b.addEventListener("click", acao);
    return b;
}
export function renderizarTabela(pagina, acoes) {
    const corpo = el("corpo-tabela");
    corpo.replaceChildren();
    if (pagina.conteudo.length === 0) {
        const celula = corpo.insertRow().insertCell();
        celula.colSpan = TOTAL_COLUNAS;
        celula.className = "vazio";
        celula.textContent = "Nenhum registro encontrado.";
    }
    for (const a of pagina.conteudo) {
        const linha = corpo.insertRow();
        const valores = [
            String(a.id),
            formatarData(a.dataOcorrencia),
            a.horario.slice(0, 5),
            a.uf,
            a.municipio,
            `${a.rodovia} km ${formatarNumero(a.quilometro)}`,
            a.causaAcidente,
            a.classificacao,
            `${a.mortos} / ${a.feridos}`,
        ];
        for (const valor of valores)
            linha.insertCell().textContent = valor;
        linha.insertCell().append(botao("Editar", () => acoes.editar(a), "secundario"), botao("Excluir", () => acoes.excluir(a), "perigo"));
    }
    el("pag-info").textContent = pagina.totalPaginas === 0
        ? "Sem páginas"
        : `Página ${pagina.pagina + 1} de ${formatarNumero(pagina.totalPaginas)}`;
    el("pag-anterior").disabled = pagina.pagina === 0;
    el("pag-proxima").disabled = pagina.pagina + 1 >= pagina.totalPaginas;
}
