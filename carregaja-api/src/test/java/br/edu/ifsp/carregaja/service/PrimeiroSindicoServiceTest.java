package br.edu.ifsp.carregaja.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.edu.ifsp.carregaja.model.Apartamento;
import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.repository.ApartamentoRepository;
import br.edu.ifsp.carregaja.repository.UsuarioRepository;

class PrimeiroSindicoServiceTest {

  private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

  private UsuarioRepository usuarioRepository;
  private ApartamentoRepository apartamentoRepository;

  @BeforeEach
  void preparar() {
    usuarioRepository = mock(UsuarioRepository.class);
    apartamentoRepository = mock(ApartamentoRepository.class);
    when(apartamentoRepository.findByNumero(any())).thenReturn(Optional.empty());
    when(apartamentoRepository.save(any(Apartamento.class))).thenAnswer(chamada -> chamada.getArgument(0));
  }

  private PrimeiroSindicoService servico(String email, String senha, String apartamento) {
    return new PrimeiroSindicoService(usuarioRepository, apartamentoRepository, ENCODER, "Síndica", email, senha, apartamento);
  }

  private Usuario criado() {
    ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
    verify(usuarioRepository).save(usuario.capture());
    return usuario.getValue();
  }

  @Test
  void semApartamentoNasceSoSindico() {
    servico("sindica@carregaja.test", "provisoria1", "").run(null);

    Usuario sindica = criado();
    assertEquals(Set.of(Perfil.SINDICO), sindica.getPerfis());
    assertNull(sindica.getApartamento());
    assertTrue(sindica.isTrocarSenha());
    assertTrue(ENCODER.matches("provisoria1", sindica.getSenha()));
  }

  // HU-01, teste 04
  @Test
  void comApartamentoTemOsDoisPerfis() {
    servico("sindica@carregaja.test", "provisoria1", "42").run(null);

    Usuario sindica = criado();
    assertEquals(Set.of(Perfil.SINDICO, Perfil.MORADOR), sindica.getPerfis());
    assertEquals("42", sindica.getApartamento().getNumero());
  }

  @Test
  void naoCriaSeJaHaSindico() {
    when(usuarioRepository.existsByPerfisContaining(Perfil.SINDICO)).thenReturn(true);

    servico("sindica@carregaja.test", "provisoria1", "42").run(null);

    verify(usuarioRepository, never()).save(any());
  }

  @Test
  void naoCriaSemEmailOuSenha() {
    servico("", "", "42").run(null);

    verify(usuarioRepository, never()).save(any());
  }
}
