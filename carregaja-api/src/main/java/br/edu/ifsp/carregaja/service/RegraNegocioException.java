package br.edu.ifsp.carregaja.service;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** Recusa de uma regra de negócio, com a mensagem que o usuário vê na tela. */
@Getter
public class RegraNegocioException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final HttpStatus status;

  public RegraNegocioException(HttpStatus status, String mensagem) {
    super(mensagem);
    this.status = status;
  }
}
