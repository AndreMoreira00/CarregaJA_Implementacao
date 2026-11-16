package br.edu.ifsp.carregaja.view;

import java.math.BigDecimal;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

/** Só o valor: a vigência começa no cadastro, sem data informada pelo síndico (RN-10). */
@Getter
@Setter
public class TarifaRequest {
  @NotNull(message = "Informe o valor do kWh.")
  @Positive(message = "O valor do kWh deve ser maior que zero.")
  @Digits(integer = 6, fraction = 4, message = "O valor do kWh aceita até 4 casas decimais.")
  private BigDecimal valorKwh;
}
