package br.edu.ifsp.carregaja.view.consultarpainelvagas;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

/** O painel na hora da consulta. A mensagem e a liberação mais próxima só vão quando não há vaga livre. */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PainelVagasResponse {
  private final LocalDateTime consultadoEm;
  private final long vagasLivres;
  private final String mensagem;
  private final VagaPainelResponse proximaLiberacao;
  private final List<VagaPainelResponse> vagas;

  public PainelVagasResponse(LocalDateTime consultadoEm, long vagasLivres, String mensagem,
      VagaPainelResponse proximaLiberacao, List<VagaPainelResponse> vagas) {
    this.consultadoEm = consultadoEm;
    this.vagasLivres = vagasLivres;
    this.mensagem = mensagem;
    this.proximaLiberacao = proximaLiberacao;
    this.vagas = vagas;
  }
}
