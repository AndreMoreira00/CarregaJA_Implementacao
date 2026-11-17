package br.edu.ifsp.carregaja.view;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VagaRequest {
  @NotBlank(message = "Informe a identificação da vaga.")
  @Size(max = 20, message = "A identificação deve ter no máximo 20 caracteres.")
  private String identificacao;

  @NotNull(message = "Informe a potência do carregador.")
  @Positive(message = "A potência do carregador deve ser maior que zero.")
  private Double potenciaCarregador;
}
