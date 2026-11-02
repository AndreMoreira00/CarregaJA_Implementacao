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

import br.edu.ifsp.carregaja.model.Vaga;
import br.edu.ifsp.carregaja.repository.VagaRepository;
import br.edu.ifsp.carregaja.view.VagaRequest;

class VagaServiceTest {

  private VagaRepository vagaRepository;
  private VagaService service;

  @BeforeEach
  void preparar() {
    vagaRepository = mock(VagaRepository.class);
    when(vagaRepository.save(any(Vaga.class))).thenAnswer(chamada -> chamada.getArgument(0));
    when(vagaRepository.findByIdentificacao(any())).thenReturn(Optional.empty());
    service = new VagaService(vagaRepository);
  }

  private static VagaRequest requisicao(String identificacao, Double potencia) {
    VagaRequest requisicao = new VagaRequest();
    requisicao.setIdentificacao(identificacao);
    requisicao.setPotenciaCarregador(potencia);
    return requisicao;
  }

  private Vaga existente(Long id, String identificacao) {
    Vaga vaga = new Vaga();
    vaga.setId(id);
    vaga.setIdentificacao(identificacao);
    when(vagaRepository.findById(id)).thenReturn(Optional.of(vaga));
    return vaga;
  }

  // HU-07, teste 01
  @Test
  void cadastroValidoNasceAtivo() {
    Vaga vaga = service.criar(requisicao("G-01", 11.0));

    assertEquals("G-01", vaga.getIdentificacao());
    assertEquals(11.0, vaga.getPotenciaCarregador());
    assertTrue(vaga.isAtiva());
  }

  // HU-07, teste 02: "g-01 " é a mesma vaga que "G-01"
  @Test
  void codigoDuplicadoERecusadoMesmoEmMinusculas() {
    Vaga outra = new Vaga();
    outra.setId(1L);
    when(vagaRepository.findByIdentificacao("G-01")).thenReturn(Optional.of(outra));

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> service.criar(requisicao(" g-01 ", 7.4)));

    assertEquals(HttpStatus.CONFLICT, recusa.getStatus());
    verify(vagaRepository, never()).save(any());
  }

  @Test
  void alterarAPotenciaMantendoOCodigoNaoEDuplicidade() {
    Vaga g01 = existente(1L, "G-01");
    when(vagaRepository.findByIdentificacao("G-01")).thenReturn(Optional.of(g01));

    assertEquals(7.4, service.atualizar(1L, requisicao("G-01", 7.4)).orElseThrow().getPotenciaCarregador());
  }

  // HU-07, teste 04
  @Test
  void desativarMantemAVagaGravada() {
    Vaga g02 = existente(2L, "G-02");

    assertFalse(service.desativar(2L).orElseThrow().isAtiva());
    verify(vagaRepository, never()).delete(g02);
  }

  @Test
  void reativarVoltaAAceitarSessoes() {
    existente(2L, "G-02").setAtiva(false);

    assertTrue(service.reativar(2L).orElseThrow().isAtiva());
  }

  @Test
  void removerApagaAVagaSemHistorico() {
    Vaga g03 = existente(3L, "G-03");

    assertTrue(service.remover(3L));
    verify(vagaRepository).delete(g03);
  }

  @Test
  void removerInexistenteDevolveFalso() {
    when(vagaRepository.findById(99L)).thenReturn(Optional.empty());

    assertFalse(service.remover(99L));
  }
}
