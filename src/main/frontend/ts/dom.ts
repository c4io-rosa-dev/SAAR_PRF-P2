export function el<T extends HTMLElement = HTMLElement>(id: string): T {
  const elementoEncontrado = document.getElementById(id);
  if (!elementoEncontrado) {
    throw new Error(`Elemento #${id} não encontrado`);
  }
  return elementoEncontrado as T;
}

export function elemento<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  texto?: string,
  classe?: string,
): HTMLElementTagNameMap[K] {
  const novo = document.createElement(tag);
  if (texto !== undefined) novo.textContent = texto;
  if (classe) novo.className = classe;
  return novo;
}

export function preencherOpcoes(alvo: HTMLSelectElement | HTMLDataListElement, valores: readonly string[]): void {
  for (const valor of valores) {
    const opcao = document.createElement("option");
    opcao.value = valor;
    opcao.textContent = valor;
    alvo.append(opcao);
  }
}
