import { api } from "./api.js";
import { el } from "./dom.js";
import { formatarData, formatarDecimal, formatarNumero } from "./formato.js";

const SEM_FILTRO = { uf: "", classificacao: "", dataInicio: "", dataFim: "" };

async function carregarDestaques(): Promise<void> {
  try {
    const r = await api.resumo(SEM_FILTRO);
    el("destaque-total").textContent = formatarNumero(r.total);
    el("destaque-mortos").textContent = formatarNumero(r.mortos);
    el("destaque-rodovia").textContent = r.rodoviaMaisCritica ?? "—";
    if (r.rodoviaMaisCritica && r.total) {
      const participacao = formatarDecimal((r.ocorrenciasRodovia / r.total) * 100);
      el("destaque-rodovia-texto").textContent = `rodovia com mais acidentes · ${participacao}% do total`;
    }
    el("destaques-periodo").textContent = r.dataInicial && r.dataFinal
      ? `Registros de ${formatarData(r.dataInicial)} a ${formatarData(r.dataFinal)}, importados da base oficial da PRF.`
      : "A base ainda não tem registros importados.";
  } catch {
    el("destaques-periodo").textContent = "Não foi possível carregar os números da base agora.";
  }
}

void carregarDestaques();
