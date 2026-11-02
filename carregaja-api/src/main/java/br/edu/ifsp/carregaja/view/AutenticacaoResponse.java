package br.edu.ifsp.carregaja.view;

import br.edu.ifsp.carregaja.model.Usuario;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AutenticacaoResponse {
  private final String token;
  private final Usuario usuario;
}
