import type {
  Acidente, AcidenteEntrada, ContagemCausa, ContagemMensal, ErroResposta, Filtro, PaginaResposta,
  ResultadoImportacao, ResumoAcidentes,
} from "./tipos.js";

const BASE = "/api/acidentes";

export class ApiErro extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly erros: Record<string, string> = {},
  ) {
    super(message);
  }
}

async function requisitar<T>(url: string, opcoes: RequestInit = {}): Promise<T> {
  const resposta = await fetch(url, opcoes);
  if (!resposta.ok) {
    const corpo = (await resposta.json().catch(() => null)) as ErroResposta | null;
    const padrao = resposta.status === 404 ? "Registro não encontrado (pode ter sido excluído)." : `Erro HTTP ${resposta.status}`;
    throw new ApiErro(corpo?.mensagem ?? padrao, resposta.status, corpo?.erros ?? {});
  }
  if (resposta.status === 204) {
    return undefined as T;
  }
  return (await resposta.json()) as T;
}

function json(method: string, corpo: unknown): RequestInit {
  return { method, headers: { "Content-Type": "application/json" }, body: JSON.stringify(corpo) };
}

function parametros(filtro: Filtro, extras: Record<string, number> = {}): string {
  const busca = new URLSearchParams();
  for (const [chave, valor] of Object.entries({ ...filtro, ...extras })) {
    if (valor !== "") busca.set(chave, String(valor));
  }
  return busca.toString();
}

export const api = {
  listar: (pagina: number, tamanho: number) =>
    requisitar<PaginaResposta<Acidente>>(`${BASE}?page=${pagina}&size=${tamanho}`),

  filtrar: (filtro: Filtro, pagina: number, tamanho: number) =>
    requisitar<PaginaResposta<Acidente>>(`${BASE}/filtro?${parametros(filtro, { page: pagina, size: tamanho })}`),

  async buscar(id: number): Promise<Acidente | null> {
    try {
      return await requisitar<Acidente>(`${BASE}/${id}`);
    } catch (erro) {
      if (erro instanceof ApiErro && erro.status === 404) return null;
      throw erro;
    }
  },

  criar: (dados: AcidenteEntrada) => requisitar<Acidente>(BASE, json("POST", dados)),

  atualizar: (id: number, dados: AcidenteEntrada) => requisitar<Acidente>(`${BASE}/${id}`, json("PUT", dados)),

  excluir: (id: number) => requisitar<void>(`${BASE}/${id}`, { method: "DELETE" }),

  importar(arquivo: File): Promise<ResultadoImportacao> {
    const dados = new FormData();
    dados.append("arquivo", arquivo);
    return requisitar<ResultadoImportacao>(`${BASE}/importar`, { method: "POST", body: dados });
  },

  topCausas: (filtro: Filtro) => requisitar<ContagemCausa[]>(`${BASE}/estatisticas/causas?${parametros(filtro)}`),

  mensal: (filtro: Filtro) => requisitar<ContagemMensal[]>(`${BASE}/estatisticas/mensal?${parametros(filtro)}`),

  resumo: (filtro: Filtro) => requisitar<ResumoAcidentes>(`${BASE}/estatisticas/resumo?${parametros(filtro)}`),
};
