package br.edu.ifsp.carregaja.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.ifsp.carregaja.model.Tarifa;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.repository.TarifaRepository;
import br.edu.ifsp.carregaja.view.TarifaRequest;

class TarifaServiceTest {

  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
  private static final LocalDateTime PRIMEIRO_DE_SETEMBRO = LocalDateTime.of(2026, 9, 1, 8, 0);

  private TarifaRepository tarifaRepository;
  private TarifaService service;

  @BeforeEach
  void preparar() {
    tarifaRepository = mock(TarifaRepository.class);
    when(tarifaRepository.save(any(Tarifa.class))).thenAnswer(chamada -> chamada.getArgument(0));
    Clock relogio = Clock.fixed(PRIMEIRO_DE_SETEMBRO.atZone(FUSO).toInstant(), FUSO);
    service = new TarifaService(tarifaRepository, relogio);
  }

  // HU-08, teste 01
  @Test
  void aVigenciaComecaNoMomentoDoCadastroComOResponsavel() {
    TarifaRequest requisicao = new TarifaRequest();
    requisicao.setValorKwh(new BigDecimal("0.92"));
    Usuario sindico = new Usuario();

    Tarifa tarifa = service.criar(sindico, requisicao);

    assertEquals(new BigDecimal("0.92"), tarifa.getValorKwh());
    assertEquals(PRIMEIRO_DE_SETEMBRO, tarifa.getInicioVigencia());
    assertEquals(sindico, tarifa.getResponsavel());
  }

  // HU-08, teste 03: vigente é a última cadastrada até aquele momento
  @Test
  void vigenteEAUltimaCadastradaAteOMomento() {
    Tarifa vigente = new Tarifa();
    when(tarifaRepository.findFirstByInicioVigenciaLessThanEqualOrderByInicioVigenciaDescIdDesc(PRIMEIRO_DE_SETEMBRO))
        .thenReturn(Optional.of(vigente));

    assertEquals(vigente, service.buscarVigente().orElseThrow());
    assertEquals(vigente, service.buscarVigenteEm(PRIMEIRO_DE_SETEMBRO).orElseThrow());
  }
}
