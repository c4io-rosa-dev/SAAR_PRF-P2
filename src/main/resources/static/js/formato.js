export function formatarData(iso) {
    const [ano, mes, dia] = iso.split("-");
    return `${dia}/${mes}/${ano}`;
}
export function formatarNumero(valor) {
    return valor.toLocaleString("pt-BR");
}
