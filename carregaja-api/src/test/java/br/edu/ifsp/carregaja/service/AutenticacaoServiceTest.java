package br.edu.ifsp.carregaja.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.repository.UsuarioRepository;
import br.edu.ifsp.carregaja.view.AutenticacaoResponse;
import br.edu.ifsp.carregaja.view.LoginRequest;
import br.edu.ifsp.carregaja.view.TrocarSenhaRequest;

class AutenticacaoServiceTest {

  private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

  private UsuarioRepository usuarioRepository;
  private TokenService tokenService;
  private AutenticacaoService service;
  private Usuario usuario;

  @BeforeEach
  void preparar() {
    usuarioRepository = mock(UsuarioRepository.class);
    tokenService = new TokenService("segredo-usado-somente-nos-testes-de-unidade", 12);
    service = new AutenticacaoService(usuarioRepository, ENCODER, tokenService);

    usuario = new Usuario();
    usuario.setId(42L);
    usuario.setEmail("apto42@carregaja.test");
    usuario.setSenha(ENCODER.encode("senha-valida"));
    usuario.getPerfis().add(Perfil.MORADOR);
    when(usuarioRepository.findByEmail("apto42@carregaja.test")).thenReturn(Optional.of(usuario));
  }

  private static LoginRequest login(String email, String senha) {
    LoginRequest requisicao = new LoginRequest();
    requisicao.setEmail(email);
    requisicao.setSenha(senha);
    return requisicao;
  }

  private static TrocarSenhaRequest troca(String atual, String nova) {
    TrocarSenhaRequest requisicao = new TrocarSenhaRequest();
    requisicao.setSenhaAtual(atual);
    requisicao.setSenhaNova(nova);
    return requisicao;
  }

  // HU-01, teste 01
  @Test
  void credenciaisValidasGeramTokenDoUsuario() {
    AutenticacaoResponse resposta = service.autenticar(login("apto42@carregaja.test", "senha-valida"));

    assertEquals(usuario, resposta.getUsuario());
    assertEquals(Optional.of(42L), tokenService.lerUsuarioId(resposta.getToken()));
  }

  @Test
  void emailNaoDiferenciaMaiusculasNemEspacos() {
    assertEquals(usuario, service.autenticar(login("  APTO42@carregaja.test ", "senha-valida")).getUsuario());
  }

  // HU-01, teste 02: senha errada e e-mail desconhecido dão a mesma resposta
  @Test
  void senhaIncorretaEEmailDesconhecidoTemAMesmaRecusa() {
    when(usuarioRepository.findByEmail("outro@carregaja.test")).thenReturn(Optional.empty());

    RegraNegocioException senhaErrada = assertThrows(RegraNegocioException.class,
        () -> service.autenticar(login("apto42@carregaja.test", "senha-errada")));
    RegraNegocioException emailErrado = assertThrows(RegraNegocioException.class,
        () -> service.autenticar(login("outro@carregaja.test", "senha-valida")));

    assertEquals(HttpStatus.UNAUTHORIZED, senhaErrada.getStatus());
    assertEquals(senhaErrada.getStatus(), emailErrado.getStatus());
    assertEquals(senhaErrada.getMessage(), emailErrado.getMessage());
  }

  // HU-01, teste 03
  @Test
  void moradorDesativadoERecusadoComOrientacao() {
    usuario.setAtivo(false);

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> service.autenticar(login("apto42@carregaja.test", "senha-valida")));

    assertEquals(HttpStatus.FORBIDDEN, recusa.getStatus());
    assertTrue(recusa.getMessage().contains("síndico"));
  }

  @Test
  void desativadoComSenhaErradaNaoRevelaQueEstaDesativado() {
    usuario.setAtivo(false);

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> service.autenticar(login("apto42@carregaja.test", "senha-errada")));

    assertEquals(HttpStatus.UNAUTHORIZED, recusa.getStatus());
  }

  // HU-01, teste 06
  @Test
  void trocarSenhaGravaANovaELiberaOAcesso() {
    assertTrue(usuario.isTrocarSenha());

    service.trocarSenha(usuario, troca("senha-valida", "senha-nova-1"));

    assertFalse(usuario.isTrocarSenha());
    assertTrue(ENCODER.matches("senha-nova-1", usuario.getSenha()));
    verify(usuarioRepository).save(usuario);
  }

  // HU-01, teste 07
  @Test
  void senhaAtualIncorretaNaoTroca() {
    assertThrows(RegraNegocioException.class, () -> service.trocarSenha(usuario, troca("errada", "senha-nova-1")));

    assertTrue(usuario.isTrocarSenha());
    verify(usuarioRepository, never()).save(any());
  }

  @Test
  void novaSenhaIgualAAtualERecusada() {
    assertThrows(RegraNegocioException.class, () -> service.trocarSenha(usuario, troca("senha-valida", "senha-valida")));

    verify(usuarioRepository, never()).save(any());
  }
}
