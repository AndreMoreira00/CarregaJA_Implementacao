package br.edu.ifsp.carregaja.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.repository.UsuarioRepository;
import br.edu.ifsp.carregaja.view.AutenticacaoResponse;
import br.edu.ifsp.carregaja.view.LoginRequest;
import br.edu.ifsp.carregaja.view.TrocarSenhaRequest;

@Service
public class AutenticacaoService {

  // Mesma mensagem para e-mail e senha, para não revelar qual dos dois está errado (HU-01, teste 02)
  private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha incorretos.";

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;

  public AutenticacaoService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
      TokenService tokenService) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
  }

  public AutenticacaoResponse autenticar(LoginRequest requisicao) {
    Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(requisicao.getEmail()))
        .filter(encontrado -> passwordEncoder.matches(requisicao.getSenha(), encontrado.getSenha()))
        .orElseThrow(() -> new RegraNegocioException(HttpStatus.UNAUTHORIZED, CREDENCIAIS_INVALIDAS));
    if (!usuario.isAtivo()) {
      throw new RegraNegocioException(HttpStatus.FORBIDDEN, "Seu acesso está desativado. Procure o síndico.");
    }
    return new AutenticacaoResponse(tokenService.gerar(usuario), usuario);
  }

  public void trocarSenha(Usuario usuario, TrocarSenhaRequest requisicao) {
    if (!passwordEncoder.matches(requisicao.getSenhaAtual(), usuario.getSenha())) {
      throw new RegraNegocioException(HttpStatus.UNPROCESSABLE_ENTITY, "A senha atual está incorreta.");
    }
    if (requisicao.getSenhaNova().equals(requisicao.getSenhaAtual())) {
      throw new RegraNegocioException(HttpStatus.UNPROCESSABLE_ENTITY, "A nova senha deve ser diferente da atual.");
    }
    usuario.setSenha(passwordEncoder.encode(requisicao.getSenhaNova()));
    usuario.setTrocarSenha(false);
    usuarioRepository.save(usuario);
  }

  public static String normalizarEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
