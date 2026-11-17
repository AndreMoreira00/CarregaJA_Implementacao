package br.edu.ifsp.carregaja.view.fecharmes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FechamentoMensalResponse {
  private final Long id;
  private final int ano;
  private final int mes;
  private final LocalDateTime momento;
  private final String nomeResponsavel;
  private final double energiaTotal;
  private final BigDecimal valorTotal;
  private final List<Item> itens;

  @Getter
  @AllArgsConstructor
  public static class Item {
    private final String apartamento;
    private final double energia;
    private final BigDecimal valor;
  }
}
