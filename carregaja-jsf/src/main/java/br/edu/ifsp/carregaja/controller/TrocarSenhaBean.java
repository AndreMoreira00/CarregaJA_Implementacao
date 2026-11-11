package br.edu.ifsp.carregaja.controller;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import br.edu.ifsp.carregaja.model.TrocarSenhaRequest;
import br.edu.ifsp.carregaja.service.ApiException;
import br.edu.ifsp.carregaja.service.AutenticacaoRestClient;
import br.edu.ifsp.carregaja.service.UsuarioLogado;

import lombok.Getter;
import lombok.Setter;

@Named
@ViewScoped
public class TrocarSenhaBean implements Serializable {

  private static final long serialVersionUID = 1L;

  @Inject
  private AutenticacaoRestClient autenticacaoRestClient;
  @Inject
  private UsuarioLogado usuarioLogado;
  @Getter
  @Setter
  private String senhaAtual;
  @Getter
  @Setter
  private String senhaNova;
  @Getter
  @Setter
  private String confirmacao;

  public String trocar() {
    if (!senhaNova.equals(confirmacao)) {
      avisar("A confirmação não é igual à nova senha.");
      return null;
    }
    try {
      autenticacaoRestClient.trocarSenha(new TrocarSenhaRequest(senhaAtual, senhaNova));
      usuarioLogado.senhaTrocada();
      return "/index.xhtml?faces-redirect=true";
    } catch (ApiException e) {
      avisar(e.getMessage());
      return null;
    }
  }

  private void avisar(String mensagem) {
    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, mensagem, null));
  }
}
