package br.edu.ifsp.carregaja.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TrocarSenhaRequest {
  private final String senhaAtual;
  private final String senhaNova;
}
