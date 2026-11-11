package br.edu.ifsp.carregaja.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.model.Veiculo;
import br.edu.ifsp.carregaja.repository.VeiculoRepository;
import br.edu.ifsp.carregaja.view.VeiculoRequest;

class VeiculoServiceTest {

  private VeiculoRepository veiculoRepository;
  private VeiculoService service;
  private final Usuario morador = usuario(42L, Perfil.MORADOR);
  private final Usuario vizinho = usuario(17L, Perfil.MORADOR);
  private final Usuario sindico = usuario(1L, Perfil.SINDICO);

  @BeforeEach
  void preparar() {
    veiculoRepository = mock(VeiculoRepository.class);
    when(veiculoRepository.save(any(Veiculo.class))).thenAnswer(chamada -> chamada.getArgument(0));
    service = new VeiculoService(veiculoRepository);
  }

  private static Usuario usuario(Long id, Perfil perfil) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.getPerfis().add(perfil);
    return usuario;
  }

  private static VeiculoRequest requisicao(String modelo, Double capacidade, Double potencia) {
    VeiculoRequest requisicao = new VeiculoRequest();
    requisicao.setModelo(modelo);
    requisicao.setCapacidadeBateria(capacidade);
    requisicao.setPotenciaMaxima(potencia);
    return requisicao;
  }

  // HU-05, teste 01
  @Test
  void criarGravaOsDadosInformadosELigaAoMorador() {
    Veiculo veiculo = service.criar(morador, requisicao("Nissan Leaf", 40.0, 6.6));

    assertEquals(morador, veiculo.getUsuario());

    assertEquals("Nissan Leaf", veiculo.getModelo());
    assertEquals(40.0, veiculo.getCapacidadeBateria());
    assertEquals(6.6, veiculo.getPotenciaMaxima());
    verify(veiculoRepository).save(veiculo);
  }

  @Test
  void atualizarTrocaOsDadosDoVeiculoExistente() {
    Veiculo existente = new Veiculo();
    existente.setId(1L);
    existente.setModelo("Nissan Leaf");
    existente.setCapacidadeBateria(40.0);
    existente.setPotenciaMaxima(6.6);
    existente.setUsuario(morador);
    when(veiculoRepository.findById(1L)).thenReturn(Optional.of(existente));

    Veiculo alterado = service.atualizar(morador, 1L, requisicao("BYD Dolphin", 44.9, 11.0)).orElseThrow();

    assertEquals(1L, alterado.getId());
    assertEquals("BYD Dolphin", alterado.getModelo());
    assertEquals(44.9, alterado.getCapacidadeBateria());
    assertEquals(11.0, alterado.getPotenciaMaxima());
  }

  @Test
  void atualizarVeiculoInexistenteNaoGravaNada() {
    when(veiculoRepository.findById(99L)).thenReturn(Optional.empty());

    assertTrue(service.atualizar(morador, 99L, requisicao("Nissan Leaf", 40.0, 6.6)).isEmpty());
    verify(veiculoRepository, never()).save(any());
  }

  @Test
  void segundoVeiculoDoMesmoMoradorERecusado() {
    when(veiculoRepository.existsByUsuarioId(42L)).thenReturn(true);

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> service.criar(morador, requisicao("BYD Dolphin", 60.0, 11.0)));

    assertEquals(HttpStatus.CONFLICT, recusa.getStatus());
    verify(veiculoRepository, never()).save(any());
  }

  @Test
  void moradorNaoAlteraVeiculoDeOutro() {
    Veiculo doVizinho = new Veiculo();
    doVizinho.setUsuario(vizinho);
    when(veiculoRepository.findById(5L)).thenReturn(Optional.of(doVizinho));

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> service.atualizar(morador, 5L, requisicao("Nissan Leaf", 40.0, 6.6)));

    assertEquals(HttpStatus.FORBIDDEN, recusa.getStatus());
    verify(veiculoRepository, never()).save(any());
  }

  @Test
  void moradorNaoConsultaVeiculoDeOutroMasOSindicoSim() {
    Veiculo doVizinho = new Veiculo();
    doVizinho.setUsuario(vizinho);
    when(veiculoRepository.findById(5L)).thenReturn(Optional.of(doVizinho));

    assertThrows(RegraNegocioException.class, () -> service.buscarPorId(morador, 5L));
    assertEquals(doVizinho, service.buscarPorId(sindico, 5L).orElseThrow());
  }
}
