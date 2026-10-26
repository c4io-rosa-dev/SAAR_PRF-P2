import { el, elemento } from "./dom.js";
import { formatarData, formatarNumero } from "./formato.js";
let registroAtual = null;
export function registroEmDetalhe() {
    return registroAtual;
}
export function fecharDetalhe() {
    registroAtual = null;
    el("dialogo-detalhe").close();
}
export function mostrarDetalhe(a) {
    registroAtual = a;
    const itens = [
        ["ID", String(a.id)],
        ["ID PRF", a.idPrf ? String(a.idPrf) : "cadastro manual"],
        ["Data e hora", `${formatarData(a.dataOcorrencia)} ${a.horario.slice(0, 5)}`],
        ["Local", `${a.municipio} / ${a.uf}`],
        ["Rodovia", `${a.rodovia}, km ${formatarNumero(a.quilometro)}`],
        ["Causa", a.causaAcidente],
        ["Tipo", a.tipoAcidente],
        ["Classificação", a.classificacao],
        ["Vítimas", `${a.mortos} ${a.mortos === 1 ? "morto" : "mortos"}, ${a.feridos} ${a.feridos === 1 ? "ferido" : "feridos"}`],
        ["Coordenadas", `${a.latitude}, ${a.longitude}`],
    ];
    const lista = elemento("dl", undefined, "detalhe-lista");
    for (const [rotulo, valor] of itens)
        lista.append(elemento("dt", rotulo), elemento("dd", valor));
    el("titulo-detalhe").textContent = `Registro #${a.id}`;
    el("detalhe-registro").replaceChildren(lista);
    el("dialogo-detalhe").showModal();
}
