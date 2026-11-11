package br.edu.ifsp.carregaja.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginRequest {
  private final String email;
  private final String senha;
}
