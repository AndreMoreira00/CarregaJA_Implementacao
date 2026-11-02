package br.edu.ifsp.carregaja.view;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/** Só a vaga e o nível da bateria: o veículo já está cadastrado (HU-05, teste 04). */
@Getter
@Setter
public class SessaoRequest {
  @NotNull(message = "Informe a vaga.")
  private Long vagaId;

  @NotNull(message = "Informe o nível da bateria.")
  @Min(value = 0, message = "O nível da bateria deve estar entre 0 e 100%.")
  @Max(value = 100, message = "O nível da bateria deve estar entre 0 e 100%.")
  private Integer nivelBateria;
}
