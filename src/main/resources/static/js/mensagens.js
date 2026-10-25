import { ApiErro } from "./api.js";
import { el, elemento } from "./dom.js";
export function mostrarMensagem(texto, tipo = "sucesso") {
    const toast = elemento("div", texto, `toast toast-${tipo}`);
    el("toasts").append(toast);
    setTimeout(() => toast.remove(), 5000);
}
export function mensagemDeErro(erro) {
    if (erro instanceof ApiErro) {
        const detalhes = Object.values(erro.erros);
        return detalhes.length ? `${erro.message}: ${detalhes.join("; ")}` : erro.message;
    }
    console.error(erro);
    return "Falha de comunicação com o servidor.";
}
