package br.edu.ifsp.carregaja.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.edu.ifsp.carregaja.model.SituacaoSessao;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConsumoResponse {
  private final String apartamento;
  private final int ano;
  private final int mes;
  private final boolean semRegistros;
  private final String mensagem;
  private final double energiaKwh;
  private final BigDecimal valor;
  private final List<SessaoDoMes> sessoes;

  @Getter
  @AllArgsConstructor
  public static class SessaoDoMes {
    private final String vaga;
    private final LocalDateTime inicio;
    private final LocalDateTime fim;
    private final double energiaKwh;
    private final BigDecimal valor;
    private final SituacaoSessao situacao;
  }
}
