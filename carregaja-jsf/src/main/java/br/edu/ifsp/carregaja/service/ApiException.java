package br.edu.ifsp.carregaja.service;

/** Falha ao falar com a API, com a mensagem que o usuário vê na tela. */
public class ApiException extends Exception {
  private static final long serialVersionUID = 1L;

  public ApiException(String mensagem) {
    super(mensagem);
  }

  public ApiException(String mensagem, Throwable causa) {
    super(mensagem, causa);
  }
}
