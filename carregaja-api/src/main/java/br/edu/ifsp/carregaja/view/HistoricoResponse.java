package br.edu.ifsp.carregaja.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** O histórico de utilização do período (HU-12). Sem registros, as listas vêm vazias e semRegistros é verdadeiro. */
@Getter
@AllArgsConstructor
public class HistoricoResponse {
  private final LocalDate de;
  private final LocalDate ate;
  private final boolean semRegistros;
  private final String mensagem;
  // Hora do dia (0 a 23) em que as vagas ficaram mais ocupadas; nulo se não houve ocupação
  private final Integer faixaPico;
  private final long totalRecusas;
  private final List<OcupacaoVaga> ocupacaoPorVaga;
  private final List<FaixaHoraria> faixasHorarias;
  private final List<ConsumoApartamento> consumoPorApartamento;
  private final List<RecusaPorVaga> recusasPorVaga;

  @Getter
  @AllArgsConstructor
  public static class OcupacaoVaga {
    private final String vaga;
    private final double horasOcupadas;
    private final double percentualPeriodo;
  }

  /** A faixa da hora cheia: {@code hora = 18} é das 18h às 19h, somando todos os dias do período. */
  @Getter
  @AllArgsConstructor
  public static class FaixaHoraria {
    private final int hora;
    private final long minutosOcupados;
    private final long recusas;
  }

  @Getter
  @AllArgsConstructor
  public static class ConsumoApartamento {
    private final String apartamento;
    private final double energiaKwh;
    private final BigDecimal valor;
    private final long sessoes;
  }

  @Getter
  @AllArgsConstructor
  public static class RecusaPorVaga {
    private final String vaga;
    private final long recusas;
  }
}
