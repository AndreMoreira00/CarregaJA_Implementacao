package br.edu.ifsp.carregaja.view;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VeiculoRequest {
  @NotBlank(message = "modelo é obrigatório")
  @Size(max = 100, message = "modelo deve ter no máximo 100 caracteres")
  private String modelo;

  @NotNull(message = "capacidadeBateria é obrigatória")
  @Positive(message = "capacidadeBateria deve ser maior que zero")
  private Double capacidadeBateria;

  @NotNull(message = "potenciaMaxima é obrigatória")
  @Positive(message = "potenciaMaxima deve ser maior que zero")
  private Double potenciaMaxima;
}
