package br.edu.ifsp.carregaja.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutenticacaoResponse {
  private String token;
  private Usuario usuario;
}
