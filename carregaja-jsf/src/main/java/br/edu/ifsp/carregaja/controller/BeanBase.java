package br.edu.ifsp.carregaja.controller;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import br.edu.ifsp.carregaja.service.ApiException;

/** As mensagens que todo bean mostra na tela. */
public abstract class BeanBase implements Serializable {
  private static final long serialVersionUID = 1L;

  protected void informar(String mensagem) {
    adicionar(FacesMessage.SEVERITY_INFO, mensagem);
  }

  protected void avisar(String mensagem) {
    adicionar(FacesMessage.SEVERITY_ERROR, mensagem);
  }

  protected void reclamar(ApiException e) {
    System.out.println(getClass().getSimpleName() + ": " + e.getMessage());
    avisar(e.getMessage());
  }

  private void adicionar(FacesMessage.Severity severidade, String mensagem) {
    FacesContext contexto = FacesContext.getCurrentInstance();
    if (contexto != null) {
      contexto.addMessage(null, new FacesMessage(severidade, mensagem, null));
    }
  }
}
