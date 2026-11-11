package br.edu.ifsp.carregaja.view;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrocarSenhaRequest {
  @NotBlank(message = "Informe a senha atual.")
  private String senhaAtual;

  @NotBlank(message = "Informe a nova senha.")
  @Size(min = 8, max = 72, message = "A nova senha deve ter de 8 a 72 caracteres.")
  private String senhaNova;
}
