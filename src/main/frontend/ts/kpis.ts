import { el, elemento } from "./dom.js";
import { formatarData, formatarDecimal, formatarNumero } from "./formato.js";
import type { ResumoAcidentes } from "./tipos.js";

function rodape(id: string, ...partes: (string | Node)[]): void {
  el(id).replaceChildren(...partes);
}

export function renderizarKpis(r: ResumoAcidentes): void {
  el("kpi-total-valor").textContent = formatarNumero(r.total);
  el("kpi-mortos-valor").textContent = formatarNumero(r.mortos);
  el("kpi-feridos-valor").textContent = formatarNumero(r.feridos);

  if (r.dataInicial && r.dataFinal) {
    rodape("kpi-total-rodape", "Período: ", elemento("strong", `${formatarData(r.dataInicial)} a ${formatarData(r.dataFinal)}`));
  } else {
    rodape("kpi-total-rodape", "Sem registros no recorte");
  }

  // letalidade = óbitos a cada mil acidentes do recorte
  const letalidade = r.total ? (r.mortos / r.total) * 1000 : 0;
  rodape("kpi-mortos-rodape", "Letalidade: ", elemento("strong", formatarDecimal(letalidade)), " óbitos por mil acidentes");

  const mediaFeridos = r.total ? r.feridos / r.total : 0;
  rodape("kpi-feridos-rodape", "Média: ", elemento("strong", formatarDecimal(mediaFeridos)), " feridos por acidente");

  el("kpi-rodovia-valor").textContent = r.rodoviaMaisCritica ?? "—";
  const participacao = r.total ? (r.ocorrenciasRodovia / r.total) * 100 : 0;
  el("kpi-rodovia-pct").textContent = r.rodoviaMaisCritica ? `${formatarDecimal(participacao)}%` : "";
  rodape("kpi-rodovia-rodape", elemento("strong", formatarNumero(r.ocorrenciasRodovia)), " ocorrências no recorte");
}
