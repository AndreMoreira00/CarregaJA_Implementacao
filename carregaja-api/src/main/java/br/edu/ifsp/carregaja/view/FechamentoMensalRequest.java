package br.edu.ifsp.carregaja.view;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FechamentoMensalRequest {
  @NotNull(message = "Informe o ano.")
  private Integer ano;

  @NotNull(message = "Informe o mês.")
  @Min(value = 1, message = "O mês deve estar entre 1 e 12.")
  @Max(value = 12, message = "O mês deve estar entre 1 e 12.")
  private Integer mes;
}
