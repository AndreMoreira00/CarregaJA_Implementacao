package br.edu.ifsp.carregaja.manterveiculo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.ifsp.carregaja.manterveiculo.model.Veiculo;
import br.edu.ifsp.carregaja.manterveiculo.repository.VeiculoRepository;
import br.edu.ifsp.carregaja.manterveiculo.view.VeiculoRequest;

class VeiculoServiceTest {

  private VeiculoRepository veiculoRepository;
  private VeiculoService service;

  @BeforeEach
  void preparar() {
    veiculoRepository = mock(VeiculoRepository.class);
    when(veiculoRepository.save(any(Veiculo.class))).thenAnswer(chamada -> chamada.getArgument(0));
    service = new VeiculoService(veiculoRepository);
  }

  private static VeiculoRequest requisicao(String modelo, Double capacidade, Double potencia) {
    VeiculoRequest requisicao = new VeiculoRequest();
    requisicao.setModelo(modelo);
    requisicao.setCapacidadeBateria(capacidade);
    requisicao.setPotenciaMaxima(potencia);
    return requisicao;
  }

  @Test
  void criarGravaOsDadosInformados() {
    Veiculo veiculo = service.criar(requisicao("Nissan Leaf", 40.0, 6.6));

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
    when(veiculoRepository.findById(1L)).thenReturn(Optional.of(existente));

    Veiculo alterado = service.atualizar(1L, requisicao("BYD Dolphin", 44.9, 11.0)).orElseThrow();

    assertEquals(1L, alterado.getId());
    assertEquals("BYD Dolphin", alterado.getModelo());
    assertEquals(44.9, alterado.getCapacidadeBateria());
    assertEquals(11.0, alterado.getPotenciaMaxima());
  }

  @Test
  void atualizarVeiculoInexistenteNaoGravaNada() {
    when(veiculoRepository.findById(99L)).thenReturn(Optional.empty());

    assertTrue(service.atualizar(99L, requisicao("Nissan Leaf", 40.0, 6.6)).isEmpty());
    verify(veiculoRepository, never()).save(any());
  }
}
