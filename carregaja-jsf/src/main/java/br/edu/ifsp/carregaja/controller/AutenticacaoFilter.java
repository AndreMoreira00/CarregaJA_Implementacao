package br.edu.ifsp.carregaja.controller;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.edu.ifsp.carregaja.service.UsuarioLogado;

/** Manda para o login quem não entrou e para a troca de senha quem ainda tem senha provisória. */
@WebFilter(urlPatterns = {"/restricted/*", "/index.xhtml"})
public class AutenticacaoFilter implements Filter {

  private static final String LOGIN = "/autenticarusuario/AutenticarUsuarioView.xhtml";
  private static final String TROCAR_SENHA = "/restricted/autenticarusuario/TrocarSenhaView.xhtml";

  @Inject
  private UsuarioLogado usuarioLogado;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain cadeia)
      throws IOException, ServletException {
    HttpServletRequest requisicao = (HttpServletRequest) request;
    HttpServletResponse resposta = (HttpServletResponse) response;

    if (!usuarioLogado.isLogado()) {
      resposta.sendRedirect(requisicao.getContextPath() + LOGIN);
    } else if (usuarioLogado.isTrocarSenha() && !requisicao.getServletPath().equals(TROCAR_SENHA)) {
      resposta.sendRedirect(requisicao.getContextPath() + TROCAR_SENHA);
    } else {
      cadeia.doFilter(request, response);
    }
  }
}
