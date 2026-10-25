import type { Chart as GraficoChart } from "chart.js";
import { MESES } from "./constantes.js";
import { el } from "./dom.js";
import type { ContagemCausa, ContagemMensal } from "./tipos.js";

// Chart.js é carregado por uma cópia local no index.html; o pacote npm serve só para os tipos
declare const Chart: typeof import("chart.js").Chart;

const CORES = ["#12355b", "#c0504d", "#4f8a3c", "#8064a2"];

let graficoCausas: GraficoChart<"bar", number[], string> | null = null;
let graficoMensal: GraficoChart<"line", (number | null)[], string> | null = null;

export function desenharCausas(dados: ContagemCausa[]): void {
  graficoCausas?.destroy();
  graficoCausas = new Chart(el<HTMLCanvasElement>("grafico-causas"), {
    type: "bar",
    data: {
      labels: dados.map((d) => d.causa),
      datasets: [{ label: "Acidentes", data: dados.map((d) => d.total), backgroundColor: CORES[0] }],
    },
    options: {
      indexAxis: "y",
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
    },
  });
}

// uma linha por ano, com os 12 meses no eixo x, para comparar 2025 e 2026 no mesmo período
export function desenharMensal(dados: ContagemMensal[]): void {
  graficoMensal?.destroy();
  const anos = [...new Set(dados.map((d) => d.ano))].sort((a, b) => a - b);
  graficoMensal = new Chart(el<HTMLCanvasElement>("grafico-mensal"), {
    type: "line",
    data: {
      labels: [...MESES],
      datasets: anos.map((ano, i) => ({
        label: String(ano),
        data: MESES.map((_, mes) => dados.find((d) => d.ano === ano && d.mes === mes + 1)?.total ?? null),
        borderColor: CORES[i % CORES.length],
        backgroundColor: CORES[i % CORES.length],
        tension: 0.25,
      })),
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: { y: { beginAtZero: true } },
    },
  });
}
