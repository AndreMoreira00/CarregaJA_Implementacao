package br.edu.ifsp.carregaja.controller;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.edu.ifsp.carregaja.service.UsuarioLogado;

/** Só orienta a navegação: quem garante as permissões é a API. */
@WebFilter(urlPatterns = {"/restricted/*", "/index.xhtml"}, dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD})
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
    String pagina = requisicao.getServletPath();

    String destino = destinoObrigatorio(pagina);
    if (destino != null) {
      redirecionar(requisicao, resposta, destino);
      return;
    }
    // Depois de sair, o botão Voltar do navegador não mostra a página guardada
    resposta.setHeader("Cache-Control", "no-store");
    cadeia.doFilter(request, response);
  }

  private String destinoObrigatorio(String pagina) {
    if (!usuarioLogado.isLogado()) {
      return LOGIN;
    }
    if (usuarioLogado.isTrocarSenha() && !pagina.equals(TROCAR_SENHA)) {
      return TROCAR_SENHA;
    }
    return null;
  }

  // Uma requisição AJAX do JSF não segue o 302: precisa da resposta que o JavaScript dele entende
  private void redirecionar(HttpServletRequest requisicao, HttpServletResponse resposta, String destino)
      throws IOException {
    String url = requisicao.getContextPath() + destino;
    if ("partial/ajax".equals(requisicao.getHeader("Faces-Request"))) {
      resposta.setContentType("text/xml");
      resposta.setCharacterEncoding("UTF-8");
      resposta.getWriter().write(
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?><partial-response><redirect url=\"" + url + "\"/></partial-response>");
      return;
    }
    resposta.sendRedirect(url);
  }
}
