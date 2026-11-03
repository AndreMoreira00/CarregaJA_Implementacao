package br.edu.ifsp.carregaja.service;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import br.edu.ifsp.carregaja.model.AutenticacaoResponse;
import br.edu.ifsp.carregaja.model.Usuario;

import lombok.Getter;

/** Guarda na sessão o usuário logado e o token usado nas chamadas à API. */
@Named
@SessionScoped
public class UsuarioLogado implements Serializable {
  private static final long serialVersionUID = 1L;

  @Getter
  private String token;
  @Getter
  private Usuario usuario;

  public void entrar(AutenticacaoResponse autenticacao) {
    token = autenticacao.getToken();
    usuario = autenticacao.getUsuario();
  }

  public void senhaTrocada() {
    usuario.setTrocarSenha(false);
  }

  public boolean isLogado() {
    return usuario != null;
  }

  public boolean isTrocarSenha() {
    return isLogado() && usuario.isTrocarSenha();
  }

  public boolean isMorador() {
    return isLogado() && usuario.getPerfis().contains("MORADOR");
  }
}
