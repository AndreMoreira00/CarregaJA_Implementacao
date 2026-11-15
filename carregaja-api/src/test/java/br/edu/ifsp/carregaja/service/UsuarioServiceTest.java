package br.edu.ifsp.carregaja.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.edu.ifsp.carregaja.model.Apartamento;
import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.model.Veiculo;
import br.edu.ifsp.carregaja.repository.ApartamentoRepository;
import br.edu.ifsp.carregaja.repository.RecusaVagaRepository;
import br.edu.ifsp.carregaja.repository.SessaoRecargaRepository;
import br.edu.ifsp.carregaja.repository.UsuarioRepository;
import br.edu.ifsp.carregaja.repository.VeiculoRepository;
import br.edu.ifsp.carregaja.view.UsuarioRequest;

class UsuarioServiceTest {

  private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

  private UsuarioRepository usuarioRepository;
  private ApartamentoRepository apartamentoRepository;
  private VeiculoRepository veiculoRepository;
  private SessaoRecargaRepository sessaoRepository;
  private UsuarioService service;

  @BeforeEach
  void preparar() {
    usuarioRepository = mock(UsuarioRepository.class);
    apartamentoRepository = mock(ApartamentoRepository.class);
    veiculoRepository = mock(VeiculoRepository.class);
    when(usuarioRepository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));
    when(apartamentoRepository.save(any(Apartamento.class))).thenAnswer(chamada -> chamada.getArgument(0));
    when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());
    when(apartamentoRepository.findByNumero(any())).thenReturn(Optional.empty());
    sessaoRepository = mock(SessaoRecargaRepository.class);
    service = new UsuarioService(usuarioRepository, apartamentoRepository, veiculoRepository, sessaoRepository,
        mock(RecusaVagaRepository.class), ENCODER);
  }

  private static UsuarioRequest requisicao(String apartamento, boolean sindico, String senha) {
    UsuarioRequest requisicao = new UsuarioRequest();
    requisicao.setNome("Ana");
    requisicao.setEmail("ana@carregaja.test");
    requisicao.setApartamento(apartamento);
    requisicao.setSindico(sindico);
    requisicao.setSenhaProvisoria(senha);
    return requisicao;
  }

  private Usuario existente(Long id, Perfil... perfis) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setSenha("hash");
    usuario.setTrocarSenha(false);
    usuario.getPerfis().addAll(Set.of(perfis));
    when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
    return usuario;
  }

  // HU-09, teste 01
  @Test
  void cadastroValidoCriaMoradorAtivoComSenhaProvisoria() {
    Usuario morador = service.criar(requisicao("42", false, "provisoria1"));

    assertTrue(morador.isAtivo());
    assertTrue(morador.isTrocarSenha());
    assertEquals("42", morador.getApartamento().getNumero());
    assertEquals(Set.of(Perfil.MORADOR), morador.getPerfis());
    assertTrue(ENCODER.matches("provisoria1", morador.getSenha()));
  }

  @Test
  void cadastroSemSenhaProvisoriaERecusado() {
    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> service.criar(requisicao("42", false, null)));

    assertEquals(HttpStatus.BAD_REQUEST, recusa.getStatus());
    verify(usuarioRepository, never()).save(any());
  }

  // HU-09, teste 02
  @Test
  void emailDuplicadoERecusado() {
    Usuario outro = new Usuario();
    outro.setId(7L);
    when(usuarioRepository.findByEmail("ana@carregaja.test")).thenReturn(Optional.of(outro));

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> service.criar(requisicao("42", false, "provisoria1")));

    assertEquals(HttpStatus.CONFLICT, recusa.getStatus());
    verify(usuarioRepository, never()).save(any());
  }

  // HU-09, teste 03
  @Test
  void doisMoradoresNoMesmoApartamentoCompartilhamOApartamento() {
    Apartamento apto42 = new Apartamento();
    apto42.setNumero("42");
    when(apartamentoRepository.findByNumero("42")).thenReturn(Optional.of(apto42));

    Usuario segundo = service.criar(requisicao("42", false, "provisoria1"));

    assertSame(apto42, segundo.getApartamento());
    verify(apartamentoRepository, never()).save(any());
  }

  // HU-01, teste 04
  @Test
  void sindicaQueTambemMoraFicaComOsDoisPerfis() {
    Usuario sindica = service.criar(requisicao("42", true, "provisoria1"));

    assertEquals(Set.of(Perfil.MORADOR, Perfil.SINDICO), sindica.getPerfis());
  }

  @Test
  void prepostoDaAdministradoraESindicoSemApartamento() {
    Usuario preposto = service.criar(requisicao("", true, "provisoria1"));

    assertEquals(Set.of(Perfil.SINDICO), preposto.getPerfis());
    assertEquals(null, preposto.getApartamento());
  }

  @Test
  void moradorSemApartamentoERecusado() {
    assertThrows(RegraNegocioException.class, () -> service.criar(requisicao(" ", false, "provisoria1")));

    verify(usuarioRepository, never()).save(any());
  }

  @Test
  void alterarMantemASenhaQuandoNaoVemOutra() {
    Usuario usuario = existente(42L, Perfil.MORADOR);

    service.atualizar(42L, requisicao("17", false, null));

    assertEquals("hash", usuario.getSenha());
    assertFalse(usuario.isTrocarSenha());
    assertEquals("17", usuario.getApartamento().getNumero());
  }

  @Test
  void redefinirASenhaVoltaAExigirATroca() {
    Usuario usuario = existente(42L, Perfil.MORADOR);

    service.atualizar(42L, requisicao("42", false, "provisoria2"));

    assertTrue(usuario.isTrocarSenha());
    assertTrue(ENCODER.matches("provisoria2", usuario.getSenha()));
  }

  // HU-09, teste 04
  @Test
  void desativarMantemOCadastro() {
    Usuario usuario = existente(17L, Perfil.MORADOR);

    assertFalse(service.desativar(17L).orElseThrow().isAtivo());
    verify(usuarioRepository, never()).delete(usuario);
  }

  @Test
  void reativarDevolveOAcesso() {
    Usuario usuario = existente(17L, Perfil.MORADOR);
    usuario.setAtivo(false);

    assertTrue(service.reativar(17L).orElseThrow().isAtivo());
  }

  @Test
  void naoDesativaOUnicoSindico() {
    Usuario sindico = existente(1L, Perfil.SINDICO);
    when(usuarioRepository.countByPerfisContainingAndAtivoTrue(Perfil.SINDICO)).thenReturn(1L);

    assertThrows(RegraNegocioException.class, () -> service.desativar(1L));

    assertTrue(sindico.isAtivo());
  }

  @Test
  void naoTiraOPerfilDoUnicoSindico() {
    existente(1L, Perfil.SINDICO);
    when(usuarioRepository.countByPerfisContainingAndAtivoTrue(Perfil.SINDICO)).thenReturn(1L);

    assertThrows(RegraNegocioException.class, () -> service.atualizar(1L, requisicao("42", false, null)));

    verify(usuarioRepository, never()).save(any());
  }

  @Test
  void removerSemHistoricoApagaOVeiculoEOMorador() {
    Usuario usuario = existente(42L, Perfil.MORADOR);
    Veiculo veiculo = new Veiculo();
    when(veiculoRepository.findByUsuarioId(42L)).thenReturn(Optional.of(veiculo));

    assertTrue(service.remover(42L));

    verify(veiculoRepository).delete(veiculo);
    verify(usuarioRepository).delete(usuario);
  }

  // HU-09, teste 05
  @Test
  void moradorComSessoesNaoERemovido() {
    Usuario usuario = existente(42L, Perfil.MORADOR);
    when(sessaoRepository.existsByUsuarioId(42L)).thenReturn(true);

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class, () -> service.remover(42L));

    assertEquals(HttpStatus.CONFLICT, recusa.getStatus());
    assertTrue(recusa.getMessage().contains("desative"));
    verify(usuarioRepository, never()).delete(usuario);
  }

  @Test
  void removerInexistenteDevolveFalso() {
    when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

    assertFalse(service.remover(99L));
  }
}
