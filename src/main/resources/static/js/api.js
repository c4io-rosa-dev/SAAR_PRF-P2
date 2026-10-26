const BASE = "/api/acidentes";
export class ApiErro extends Error {
    status;
    erros;
    constructor(message, status, erros = {}) {
        super(message);
        this.status = status;
        this.erros = erros;
    }
}
async function requisitar(url, opcoes = {}) {
    const resposta = await fetch(url, opcoes);
    if (!resposta.ok) {
        const corpo = (await resposta.json().catch(() => null));
        const padrao = resposta.status === 404 ? "Registro não encontrado (pode ter sido excluído)." : `Erro HTTP ${resposta.status}`;
        throw new ApiErro(corpo?.mensagem ?? padrao, resposta.status, corpo?.erros ?? {});
    }
    if (resposta.status === 204) {
        return undefined;
    }
    return (await resposta.json());
}
function json(method, corpo) {
    return { method, headers: { "Content-Type": "application/json" }, body: JSON.stringify(corpo) };
}
function parametros(filtro, extras = {}) {
    const busca = new URLSearchParams();
    for (const [chave, valor] of Object.entries({ ...filtro, ...extras })) {
        if (valor !== "")
            busca.set(chave, String(valor));
    }
    return busca.toString();
}
export const api = {
    listar: (pagina, tamanho) => requisitar(`${BASE}?page=${pagina}&size=${tamanho}`),
    filtrar: (filtro, pagina, tamanho) => requisitar(`${BASE}/filtro?${parametros(filtro, { page: pagina, size: tamanho })}`),
    async buscar(id) {
        try {
            return await requisitar(`${BASE}/${id}`);
        }
        catch (erro) {
            if (erro instanceof ApiErro && erro.status === 404)
                return null;
            throw erro;
        }
    },
    criar: (dados) => requisitar(BASE, json("POST", dados)),
    atualizar: (id, dados) => requisitar(`${BASE}/${id}`, json("PUT", dados)),
    excluir: (id) => requisitar(`${BASE}/${id}`, { method: "DELETE" }),
    importar(arquivo) {
        const dados = new FormData();
        dados.append("arquivo", arquivo);
        return requisitar(`${BASE}/importar`, { method: "POST", body: dados });
    },
    topCausas: (filtro) => requisitar(`${BASE}/estatisticas/causas?${parametros(filtro)}`),
    mensal: (filtro) => requisitar(`${BASE}/estatisticas/mensal?${parametros(filtro)}`),
    resumo: (filtro) => requisitar(`${BASE}/estatisticas/resumo?${parametros(filtro)}`),
};
