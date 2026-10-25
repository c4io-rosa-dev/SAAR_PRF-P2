export function formatarData(iso: string): string {
  const [ano, mes, dia] = iso.split("-");
  return `${dia}/${mes}/${ano}`;
}

export function formatarNumero(valor: number): string {
  return valor.toLocaleString("pt-BR");
}
