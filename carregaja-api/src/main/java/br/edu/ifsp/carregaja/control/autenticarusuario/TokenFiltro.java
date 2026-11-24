package br.edu.ifsp.carregaja.control.autenticarusuario;

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

import br.edu.ifsp.carregaja.model.compartilhado.Usuario;
import br.edu.ifsp.carregaja.repository.compartilhado.UsuarioRepository;
import br.edu.ifsp.carregaja.service.autenticarusuario.TokenService;

/**
 * Busca o usuário no banco a cada requisição, assim quem é desativado perde o acesso na hora (HU-09, teste 04), e
 * o token emitido antes de uma troca de senha deixa de valer (Roteiro de Teste, UC01 cenário 08).
 */
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
      String token = cabecalho.substring(PREFIXO.length());
      tokenService.lerUsuarioId(token)
          .flatMap(usuarioRepository::findById)
          .filter(Usuario::isAtivo)
          .filter(usuario -> tokenService.emitidoDepoisDaTrocaDeSenha(token, usuario))
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
