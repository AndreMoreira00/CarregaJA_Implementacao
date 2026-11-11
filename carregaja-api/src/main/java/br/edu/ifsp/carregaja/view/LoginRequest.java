package br.edu.ifsp.carregaja.view;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
  @NotBlank(message = "Informe o e-mail.")
  private String email;

  @NotBlank(message = "Informe a senha.")
  private String senha;
}
