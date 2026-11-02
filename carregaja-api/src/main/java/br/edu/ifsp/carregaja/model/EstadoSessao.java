package br.edu.ifsp.carregaja.model;

/** O que a sessão aberta significa agora. Não é gravado: deriva do horário, da previsão e da duração máxima. */
public enum EstadoSessao {
  EM_ANDAMENTO,
  CONCLUIDA_AINDA_OCUPADA,
  EXCEDIDA,
  ENCERRADA
}
