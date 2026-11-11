package br.edu.ifsp.carregaja.service;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** Lançada quando uma regra de negócio é violada. A mensagem é mostrada ao usuário. */
@Getter
public class RegraNegocioException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final HttpStatus status;

  public RegraNegocioException(HttpStatus status, String mensagem) {
    super(mensagem);
    this.status = status;
  }
}
