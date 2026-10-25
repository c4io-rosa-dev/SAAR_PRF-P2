export interface AcidenteEntrada {
  dataOcorrencia: string | null;
  horario: string | null;
  uf: string;
  municipio: string;
  rodovia: string;
  quilometro: number | null;
  causaAcidente: string;
  tipoAcidente: string;
  classificacao: string;
  mortos: number | null;
  feridos: number | null;
  latitude: number | null;
  longitude: number | null;
}

export interface Acidente extends AcidenteEntrada {
  id: number;
  idPrf: number | null;
  dataOcorrencia: string;
  horario: string;
  quilometro: number;
  mortos: number;
  feridos: number;
  latitude: number;
  longitude: number;
}

export interface PaginaResposta<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface RejeicaoLinha {
  linha: number;
  motivo: string;
}

export interface ResultadoImportacao {
  arquivo: string;
  linhasLidas: number;
  importadas: number;
  rejeitadas: number;
  duplicadas: number;
  motivosRejeicao: Record<string, number>;
  exemplosRejeicao: RejeicaoLinha[];
  tempoMs: number;
  mensagem: string;
}

export interface ContagemCausa {
  causa: string;
  total: number;
}

export interface ContagemMensal {
  ano: number;
  mes: number;
  total: number;
}

export interface ErroResposta {
  mensagem: string;
  erros: Record<string, string>;
}

export interface Filtro {
  uf: string;
  classificacao: string;
  dataInicio: string;
  dataFim: string;
}
