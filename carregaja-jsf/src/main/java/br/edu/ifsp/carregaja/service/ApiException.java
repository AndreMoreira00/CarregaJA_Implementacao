package br.edu.ifsp.carregaja.service;

/** Erro ao chamar a API. A mensagem é mostrada ao usuário. */
public class ApiException extends Exception {
  private static final long serialVersionUID = 1L;

  public ApiException(String mensagem) {
    super(mensagem);
  }

  public ApiException(String mensagem, Throwable causa) {
    super(mensagem, causa);
  }
}
