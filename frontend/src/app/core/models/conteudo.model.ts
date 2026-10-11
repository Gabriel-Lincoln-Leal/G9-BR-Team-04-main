export interface Conteudo {
  id: number;
  titulo: string;
  texto: string;
  categoria: string;
  probabilidade: number;
  palavrasChave: string[];
  criadoEm: string;
}

// Resposta paginada padrão do Spring
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface LoteResponse {
  totalLinhas: number;
  sucessos: number;
  falhas: number;
  idsProcessados: number[];
  erros: { linha: number; titulo: string; mensagem: string }[];
}
