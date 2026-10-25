import { el, elemento } from "./dom.js";
import { formatarData, formatarNumero } from "./formato.js";
export function renderizarDetalhe(a, aoEditar) {
    const alvo = el("detalhe-registro");
    if (!a) {
        alvo.hidden = true;
        alvo.replaceChildren();
        return;
    }
    const itens = [
        ["ID", String(a.id)],
        ["ID PRF", a.idPrf ? String(a.idPrf) : "cadastro manual"],
        ["Data e hora", `${formatarData(a.dataOcorrencia)} ${a.horario.slice(0, 5)}`],
        ["Local", `${a.municipio} / ${a.uf}`],
        ["Rodovia", `${a.rodovia}, km ${formatarNumero(a.quilometro)}`],
        ["Causa", a.causaAcidente],
        ["Tipo", a.tipoAcidente],
        ["Classificação", a.classificacao],
        ["Vítimas", `${a.mortos} mortos, ${a.feridos} feridos`],
        ["Coordenadas", `${a.latitude}, ${a.longitude}`],
    ];
    const lista = elemento("dl");
    for (const [rotulo, valor] of itens)
        lista.append(elemento("dt", rotulo), elemento("dd", valor));
    const editar = elemento("button", "Editar este registro", "secundario");
    editar.type = "button";
    editar.addEventListener("click", () => aoEditar(a));
    alvo.replaceChildren(elemento("h3", `Registro #${a.id}`), lista, editar);
    alvo.hidden = false;
}
