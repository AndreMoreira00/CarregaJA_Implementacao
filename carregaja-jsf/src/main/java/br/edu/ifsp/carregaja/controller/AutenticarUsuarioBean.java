package br.edu.ifsp.carregaja.controller;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import br.edu.ifsp.carregaja.model.LoginRequest;
import br.edu.ifsp.carregaja.service.ApiException;
import br.edu.ifsp.carregaja.service.AutenticacaoRestClient;
import br.edu.ifsp.carregaja.service.UsuarioLogado;

import lombok.Getter;
import lombok.Setter;

@Named
@ViewScoped
public class AutenticarUsuarioBean implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final String INICIO = "/index.xhtml?faces-redirect=true";
  private static final String LOGIN = "/autenticarusuario/AutenticarUsuarioView.xhtml?faces-redirect=true";

  @Inject
  private AutenticacaoRestClient autenticacaoRestClient;
  @Inject
  private UsuarioLogado usuarioLogado;
  @Getter
  @Setter
  private String email;
  @Getter
  @Setter
  private String senha;

  public void inicializar() throws IOException {
    if (usuarioLogado.isLogado()) {
      ExternalContext contexto = FacesContext.getCurrentInstance().getExternalContext();
      contexto.redirect(contexto.getRequestContextPath() + "/index.xhtml");
    }
  }

  public String entrar() {
    try {
      usuarioLogado.entrar(autenticacaoRestClient.autenticar(new LoginRequest(email, senha)));
      trocarIdDaSessao();
      return INICIO;
    } catch (ApiException e) {
      System.out.println("AutenticarUsuario: " + e.getMessage());
      FacesContext.getCurrentInstance().addMessage(null,
          new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
      return null;
    }
  }

  public String sair() {
    FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
    return LOGIN;
  }

  // Troca o id da sessão no login para evitar session fixation
  private void trocarIdDaSessao() {
    ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).changeSessionId();
  }
}
