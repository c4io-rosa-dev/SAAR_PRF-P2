import { el } from "./dom.js";
const CAMPOS = [
    "dataOcorrencia", "horario", "uf", "municipio", "rodovia", "quilometro", "causaAcidente",
    "tipoAcidente", "classificacao", "mortos", "feridos", "latitude", "longitude",
];
function campo(nome) {
    return el(`campo-${nome}`);
}
function textoOuNulo(nome) {
    const valor = campo(nome).value.trim();
    return valor === "" ? null : valor;
}
function numeroOuNulo(nome) {
    const valor = campo(nome).value.trim();
    return valor === "" ? null : Number(valor);
}
function limparErros() {
    for (const nome of CAMPOS) {
        const c = campo(nome);
        c.classList.remove("invalido");
        c.title = "";
    }
}
export function preencherFormulario(a) {
    limparErros();
    el("campo-id").value = String(a.id);
    campo("dataOcorrencia").value = a.dataOcorrencia;
    campo("horario").value = a.horario.slice(0, 5);
    campo("uf").value = a.uf;
    campo("municipio").value = a.municipio;
    campo("rodovia").value = String(Number(a.rodovia.replace("BR-", "")));
    campo("quilometro").value = String(a.quilometro);
    campo("causaAcidente").value = a.causaAcidente;
    campo("tipoAcidente").value = a.tipoAcidente;
    campo("classificacao").value = a.classificacao;
    campo("mortos").value = String(a.mortos);
    campo("feridos").value = String(a.feridos);
    campo("latitude").value = String(a.latitude);
    campo("longitude").value = String(a.longitude);
    el("titulo-form").textContent = `Editar registro #${a.id}`;
    el("secao-formulario").scrollIntoView({ behavior: "smooth" });
}
export function lerFormulario() {
    const id = el("campo-id").value;
    const rodovia = campo("rodovia").value.trim();
    return {
        id: id ? Number(id) : null,
        dados: {
            dataOcorrencia: textoOuNulo("dataOcorrencia"),
            horario: textoOuNulo("horario"),
            uf: campo("uf").value,
            municipio: campo("municipio").value.trim(),
            rodovia: rodovia ? `BR-${rodovia.padStart(3, "0")}` : "",
            quilometro: numeroOuNulo("quilometro"),
            causaAcidente: campo("causaAcidente").value.trim(),
            tipoAcidente: campo("tipoAcidente").value.trim(),
            classificacao: campo("classificacao").value,
            mortos: numeroOuNulo("mortos"),
            feridos: numeroOuNulo("feridos"),
            latitude: numeroOuNulo("latitude"),
            longitude: numeroOuNulo("longitude"),
        },
    };
}
export function limparFormulario() {
    el("form-acidente").reset();
    el("campo-id").value = "";
    el("titulo-form").textContent = "Cadastrar acidente";
    limparErros();
}
export function marcarErros(erros) {
    limparErros();
    for (const [nome, mensagem] of Object.entries(erros)) {
        if (!CAMPOS.includes(nome))
            continue;
        const c = campo(nome);
        c.classList.add("invalido");
        c.title = mensagem;
    }
}
