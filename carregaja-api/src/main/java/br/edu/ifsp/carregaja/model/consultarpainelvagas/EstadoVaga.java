package br.edu.ifsp.carregaja.model.consultarpainelvagas;

/** Situação da vaga no painel (RF-02). Não é gravada: deriva da sessão aberta na vaga e do horário da consulta. */
public enum EstadoVaga {
  LIVRE,
  OCUPADA,
  CONCLUIDA_AINDA_OCUPADA,
  EXCEDIDA
}
