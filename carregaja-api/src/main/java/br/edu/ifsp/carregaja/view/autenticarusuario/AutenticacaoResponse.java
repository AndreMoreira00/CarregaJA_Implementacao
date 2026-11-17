package br.edu.ifsp.carregaja.view.autenticarusuario;

import lombok.AllArgsConstructor;
import lombok.Getter;

import br.edu.ifsp.carregaja.model.compartilhado.Usuario;

@Getter
@AllArgsConstructor
public class AutenticacaoResponse {
  private final String token;
  private final Usuario usuario;
}
