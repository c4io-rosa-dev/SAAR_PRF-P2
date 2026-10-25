export function el(id) {
    const elementoEncontrado = document.getElementById(id);
    if (!elementoEncontrado) {
        throw new Error(`Elemento #${id} não encontrado`);
    }
    return elementoEncontrado;
}
export function elemento(tag, texto, classe) {
    const novo = document.createElement(tag);
    if (texto !== undefined)
        novo.textContent = texto;
    if (classe)
        novo.className = classe;
    return novo;
}
export function preencherOpcoes(alvo, valores) {
    for (const valor of valores) {
        const opcao = document.createElement("option");
        opcao.value = valor;
        opcao.textContent = valor;
        alvo.append(opcao);
    }
}
