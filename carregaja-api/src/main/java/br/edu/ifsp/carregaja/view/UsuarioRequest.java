package br.edu.ifsp.carregaja.view;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRequest {
  @NotBlank(message = "Informe o nome.")
  @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
  private String nome;

  @NotBlank(message = "Informe o e-mail.")
  @Email(message = "Informe um e-mail válido.")
  @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres.")
  private String email;

  @Size(max = 10, message = "O apartamento deve ter no máximo 10 caracteres.")
  private String apartamento;

  private boolean sindico;

  // Obrigatória no cadastro. Na alteração, só vem quando o síndico redefine a senha esquecida
  @Size(min = 8, max = 72, message = "A senha provisória deve ter de 8 a 72 caracteres.")
  private String senhaProvisoria;
}
