package br.edu.ifsp.carregaja.service.consultarconsumo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.ifsp.carregaja.model.compartilhado.Apartamento;
import br.edu.ifsp.carregaja.model.compartilhado.SessaoRecarga;
import br.edu.ifsp.carregaja.model.compartilhado.SituacaoSessao;
import br.edu.ifsp.carregaja.model.compartilhado.Usuario;
import br.edu.ifsp.carregaja.model.compartilhado.Vaga;
import br.edu.ifsp.carregaja.repository.compartilhado.SessaoRecargaRepository;
import br.edu.ifsp.carregaja.service.fecharmes.RateioService;
import br.edu.ifsp.carregaja.view.consultarconsumo.ConsumoResponse;

class ConsumoServiceTest {

  private static final Long APARTAMENTO = 1L;
  private static final LocalDateTime SETEMBRO = LocalDateTime.of(2026, 9, 1, 0, 0);
  private static final LocalDateTime OUTUBRO = LocalDateTime.of(2026, 10, 1, 0, 0);

  private SessaoRecargaRepository sessaoRepository;
  private ConsumoService service;
  private Usuario morador;

  @BeforeEach
  void preparar() {
    sessaoRepository = mock(SessaoRecargaRepository.class);
    service = new ConsumoService(sessaoRepository, new RateioService());

    Apartamento apartamento = new Apartamento();
    apartamento.setId(APARTAMENTO);
    apartamento.setNumero("42");
    morador = new Usuario();
    morador.setId(42L);
    morador.setApartamento(apartamento);
  }

  private SessaoRecarga sessao(int dia, double energia, String valor, SituacaoSessao situacao) {
    Vaga vaga = new Vaga();
    vaga.setIdentificacao("G-02");
    SessaoRecarga sessao = new SessaoRecarga();
    sessao.setUsuario(morador);
    sessao.setVaga(vaga);
    sessao.setInicio(LocalDateTime.of(2026, 9, dia, 18, 10));
    sessao.setFim(LocalDateTime.of(2026, 9, dia, 23, 0));
    sessao.setEnergiaEstimada(energia);
    sessao.setValor(new BigDecimal(valor));
    sessao.setSituacao(situacao);
    return sessao;
  }

  private void sessoesDoMes(List<SessaoRecarga> sessoes) {
    when(sessaoRepository.findByUsuarioApartamentoIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(APARTAMENTO,
        SETEMBRO, OUTUBRO)).thenReturn(sessoes);
  }

  // HU-06, teste 01
  @Test
  void consumoDoMesTrazSessoesETotal() {
    List<SessaoRecarga> sessoes = new ArrayList<>();
    sessoes.add(sessao(3, 28.0, "25.76", SituacaoSessao.ENCERRADA));
    sessoes.add(sessao(10, 24.5, "22.54", SituacaoSessao.ENCERRADA));
    sessoes.add(sessao(17, 23.5, "21.62", SituacaoSessao.ENCERRADA));
    sessoesDoMes(sessoes);

    ConsumoResponse resposta = service.consultarDoMes(morador, 2026, 9);

    assertEquals("42", resposta.getApartamento());
    assertEquals(3, resposta.getSessoes().size());
    assertEquals(76.0, resposta.getEnergiaKwh());
    assertEquals(new BigDecimal("69.92"), resposta.getValor());
  }

  // HU-06, teste 02
  @Test
  void mesSemSessoesInformaAusencia() {
    sessoesDoMes(new ArrayList<>());

    ConsumoResponse resposta = service.consultarDoMes(morador, 2026, 9);

    assertTrue(resposta.isSemRegistros());
    assertEquals("Não há consumo registrado no mês.", resposta.getMensagem());
    assertTrue(resposta.getSessoes().isEmpty());
  }

  // HU-06, teste 03
  @Test
  void sessaoEncerradaPeloSindicoVemMarcada() {
    List<SessaoRecarga> sessoes = new ArrayList<>();
    sessoes.add(sessao(3, 28.0, "25.76", SituacaoSessao.ENCERRADA));
    sessoes.add(sessao(10, 48.0, "44.16", SituacaoSessao.ENCERRADA_ADMINISTRATIVAMENTE));
    sessoesDoMes(sessoes);

    ConsumoResponse resposta = service.consultarDoMes(morador, 2026, 9);

    assertEquals(SituacaoSessao.ENCERRADA, resposta.getSessoes().get(0).getSituacao());
    assertEquals(SituacaoSessao.ENCERRADA_ADMINISTRATIVAMENTE, resposta.getSessoes().get(1).getSituacao());
  }

  // HU-06, teste 04
  @Test
  void consultaUsaOApartamentoDoMoradorLogado() {
    sessoesDoMes(new ArrayList<>());

    service.consultarDoMes(morador, 2026, 9);

    verify(sessaoRepository).findByUsuarioApartamentoIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(
        APARTAMENTO, SETEMBRO, OUTUBRO);
  }
}
