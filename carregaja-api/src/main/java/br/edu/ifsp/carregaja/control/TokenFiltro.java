package br.edu.ifsp.carregaja.control;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.repository.UsuarioRepository;
import br.edu.ifsp.carregaja.service.TokenService;

/** Busca o usuário no banco a cada requisição, assim quem é desativado perde o acesso na hora (HU-09, teste 04). */
public class TokenFiltro extends OncePerRequestFilter {

  private static final String PREFIXO = "Bearer ";

  private final TokenService tokenService;
  private final UsuarioRepository usuarioRepository;

  public TokenFiltro(TokenService tokenService, UsuarioRepository usuarioRepository) {
    this.tokenService = tokenService;
    this.usuarioRepository = usuarioRepository;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
      throws ServletException, IOException {
    String cabecalho = requisicao.getHeader(HttpHeaders.AUTHORIZATION);
    if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
      tokenService.lerUsuarioId(cabecalho.substring(PREFIXO.length()))
          .flatMap(usuarioRepository::findById)
          .filter(Usuario::isAtivo)
          .ifPresent(this::autenticar);
    }
    cadeia.doFilter(requisicao, resposta);
  }

  private void autenticar(Usuario usuario) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(usuario, null, permissoes(usuario)));
  }

  // Com senha provisória o usuário fica sem perfil e só consegue acessar a rota de troca de senha
  private List<SimpleGrantedAuthority> permissoes(Usuario usuario) {
    if (usuario.isTrocarSenha()) {
      return List.of(new SimpleGrantedAuthority("ROLE_TROCA_SENHA"));
    }
    return usuario.getPerfis().stream()
        .map(perfil -> new SimpleGrantedAuthority("ROLE_" + perfil.name()))
        .collect(Collectors.toList());
  }
}
