import { MESES } from "./constantes.js";
import { el, elemento } from "./dom.js";
import { formatarDecimal, formatarNumero } from "./formato.js";
const CIANO = "#38bdf8";
const CINZA = "#64748b";
const LARANJA = "#ff8340";
let graficoMensal = null;
// ranking em barras horizontais: o comprimento é relativo à causa mais frequente e o percentual é sobre o recorte
export function desenharCausas(dados, totalRecorte) {
    const lista = el("grafico-causas");
    lista.replaceChildren();
    if (dados.length === 0) {
        lista.append(elemento("li", "Nenhum acidente no recorte selecionado.", "causas-vazio"));
        return;
    }
    const maior = dados[0].total;
    dados.forEach((d, i) => {
        const item = elemento("li", undefined, `causa-${i + 1}`);
        const percentual = totalRecorte ? (d.total / totalRecorte) * 100 : 0;
        const linha = elemento("div", undefined, "causa-linha");
        const nome = elemento("span", d.causa, "causa-nome");
        nome.title = d.causa;
        linha.append(nome, elemento("span", `${formatarDecimal(percentual)}% (${formatarNumero(d.total)})`, "causa-numero"));
        const barra = elemento("div", undefined, "causa-barra");
        barra.style.width = `${(d.total / maior) * 100}%`;
        const trilho = elemento("div", undefined, "causa-trilho");
        trilho.append(barra);
        item.append(linha, trilho);
        lista.append(item);
    });
}
// uma linha por ano, com os 12 meses no eixo x; o ano mais recente fica em destaque
export function desenharMensal(dados) {
    graficoMensal?.destroy();
    const anos = [...new Set(dados.map((d) => d.ano))].sort((a, b) => a - b);
    const ultimoAno = anos[anos.length - 1];
    const legenda = el("legenda-mensal");
    legenda.replaceChildren();
    for (const ano of anos) {
        const item = elemento("span", String(ano), "legenda");
        item.style.setProperty("--cor-legenda", ano === ultimoAno ? CIANO : CINZA);
        legenda.append(item);
    }
    const totalGeral = dados.reduce((soma, d) => soma + d.total, 0);
    el("media-mensal").textContent = dados.length ? `${formatarNumero(Math.round(totalGeral / dados.length))}/mês` : "—";
    const canvas = el("grafico-mensal");
    const preenchimento = canvas.getContext("2d").createLinearGradient(0, 0, 0, 320);
    preenchimento.addColorStop(0, "rgba(56, 189, 248, 0.28)");
    preenchimento.addColorStop(1, "rgba(56, 189, 248, 0)");
    graficoMensal = new Chart(canvas, {
        type: "line",
        data: {
            labels: MESES.map((m) => m.toUpperCase()),
            datasets: anos.map((ano) => {
                const destaque = ano === ultimoAno;
                return {
                    label: String(ano),
                    data: MESES.map((_, mes) => dados.find((d) => d.ano === ano && d.mes === mes + 1)?.total ?? null),
                    borderColor: destaque ? CIANO : CINZA,
                    backgroundColor: destaque ? preenchimento : "transparent",
                    borderDash: destaque ? [] : [5, 4],
                    borderWidth: destaque ? 2.5 : 1.5,
                    pointBackgroundColor: destaque ? CIANO : CINZA,
                    pointHoverBackgroundColor: LARANJA,
                    pointRadius: destaque ? 3 : 0,
                    fill: destaque,
                    tension: 0.3,
                };
            }),
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: "index", intersect: false },
            plugins: { legend: { display: false } },
            scales: {
                x: { grid: { color: "rgba(255, 255, 255, 0.05)" }, ticks: { color: "#94a3b8" } },
                y: { beginAtZero: true, grid: { color: "rgba(255, 255, 255, 0.06)" }, ticks: { color: "#94a3b8" } },
            },
        },
    });
}
export function configurarGraficos() {
    Chart.defaults.font.family = "'Public Sans', system-ui, sans-serif";
    Chart.defaults.color = "#94a3b8";
}
