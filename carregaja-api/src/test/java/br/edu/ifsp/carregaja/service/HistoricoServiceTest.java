package br.edu.ifsp.carregaja.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import br.edu.ifsp.carregaja.model.Apartamento;
import br.edu.ifsp.carregaja.model.RecusaVaga;
import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.SituacaoSessao;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.model.Vaga;
import br.edu.ifsp.carregaja.repository.RecusaVagaRepository;
import br.edu.ifsp.carregaja.repository.SessaoRecargaRepository;
import br.edu.ifsp.carregaja.repository.VagaRepository;
import br.edu.ifsp.carregaja.view.HistoricoResponse;

class HistoricoServiceTest {

  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
  private static final LocalDate SETEMBRO_DE = LocalDate.of(2026, 9, 1);
  private static final LocalDate SETEMBRO_ATE = LocalDate.of(2026, 9, 30);
  private static final double HORAS_DE_SETEMBRO = 30 * 24;

  private SessaoRecargaRepository sessaoRepository;
  private RecusaVagaRepository recusaRepository;
  private VagaRepository vagaRepository;
  private Vaga g01;
  private Vaga g02;
  private Usuario apto17;
  private Usuario apto42;
  private List<SessaoRecarga> sessoes;
  private List<RecusaVaga> recusas;

  private HistoricoService servicoEm(LocalDateTime agora) {
    Clock relogio = Clock.fixed(agora.atZone(FUSO).toInstant(), FUSO);
    return new HistoricoService(sessaoRepository, recusaRepository, vagaRepository, relogio);
  }

  private HistoricoService servico() {
    return servicoEm(LocalDateTime.of(2026, 10, 5, 12, 0));
  }

  @BeforeEach
  void preparar() {
    sessaoRepository = mock(SessaoRecargaRepository.class);
    recusaRepository = mock(RecusaVagaRepository.class);
    vagaRepository = mock(VagaRepository.class);

    g01 = vaga(1L, "G-01", true);
    g02 = vaga(2L, "G-02", true);
    apto17 = morador(17L, "17", 170L);
    apto42 = morador(42L, "42", 420L);

    // Dados de HU-12: o apto 17 usa a G-01 e o apto 42 a G-02, no horário de pico das 19h
    sessoes = new ArrayList<>();
    sessoes.add(encerrada(g01, apto17, "2026-09-08T18:00", "2026-09-08T21:00", 50.0, "46.00"));
    sessoes.add(encerrada(g01, apto17, "2026-09-09T19:00", "2026-09-09T20:30", 31.0, "28.52"));
    sessoes.add(encerrada(g02, apto42, "2026-09-08T18:30", "2026-09-08T22:30", 40.0, "36.80"));
    sessoes.add(encerrada(g02, apto42, "2026-09-10T09:00", "2026-09-10T11:00", 36.0, "33.12"));

    recusas = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      recusas.add(recusa(g01, apto42, "2026-09-0" + (i + 1) + "T19:10"));
    }
    recusas.add(recusa(g02, apto17, "2026-09-04T19:40"));
    recusas.add(recusa(g01, apto42, "2026-09-05T20:05"));
    recusas.add(recusa(g01, apto42, "2026-09-06T20:30"));
    recusas.add(recusa(g02, apto17, "2026-09-07T08:15"));

    when(sessaoRepository.buscarQueTocamOPeriodo(any(), any())).thenAnswer(chamada -> sessoes);
    when(recusaRepository.buscarDoPeriodo(any(), any())).thenAnswer(chamada -> recusas);
    when(vagaRepository.findAll()).thenReturn(List.of(g01, g02));
  }

  private static Vaga vaga(Long id, String identificacao, boolean ativa) {
    Vaga vaga = new Vaga();
    vaga.setId(id);
    vaga.setIdentificacao(identificacao);
    vaga.setAtiva(ativa);
    return vaga;
  }

  private static Usuario morador(Long id, String numero, Long apartamentoId) {
    Apartamento apartamento = new Apartamento();
    apartamento.setId(apartamentoId);
    apartamento.setNumero(numero);
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setApartamento(apartamento);
    return usuario;
  }

  private static SessaoRecarga encerrada(Vaga vaga, Usuario usuario, String inicio, String fim, double energia,
      String valor) {
    SessaoRecarga sessao = new SessaoRecarga();
    sessao.setVaga(vaga);
    sessao.setUsuario(usuario);
    sessao.setInicio(LocalDateTime.parse(inicio));
    sessao.setFim(LocalDateTime.parse(fim));
    sessao.setSituacao(SituacaoSessao.ENCERRADA);
    sessao.setEnergiaEstimada(energia);
    sessao.setValor(new BigDecimal(valor));
    return sessao;
  }

  private static SessaoRecarga emAndamento(Vaga vaga, Usuario usuario, String inicio) {
    SessaoRecarga sessao = new SessaoRecarga();
    sessao.setVaga(vaga);
    sessao.setUsuario(usuario);
    sessao.setInicio(LocalDateTime.parse(inicio));
    sessao.setSituacao(SituacaoSessao.EM_ANDAMENTO);
    return sessao;
  }

  private static RecusaVaga recusa(Vaga vaga, Usuario usuario, String momento) {
    RecusaVaga recusa = new RecusaVaga();
    recusa.setVaga(vaga);
    recusa.setUsuario(usuario);
    recusa.setMomento(LocalDateTime.parse(momento));
    return recusa;
  }

  private HistoricoResponse setembro() {
    return servico().consultar(SETEMBRO_DE, SETEMBRO_ATE, null, null);
  }

  // HU-12, teste 01
  @Test
  void ocupacaoPorVagaDaAsHorasEOPercentualDoPeriodo() {
    HistoricoResponse historico = setembro();

    assertFalse(historico.isSemRegistros());
    assertEquals(2, historico.getOcupacaoPorVaga().size());
    assertEquals("G-01", historico.getOcupacaoPorVaga().get(0).getVaga());
    assertEquals(4.5, historico.getOcupacaoPorVaga().get(0).getHorasOcupadas());
    assertEquals(Math.round(4.5 * 1000 / HORAS_DE_SETEMBRO) / 10.0, historico.getOcupacaoPorVaga().get(0).getPercentualPeriodo());
    assertEquals("G-02", historico.getOcupacaoPorVaga().get(1).getVaga());
    assertEquals(6.0, historico.getOcupacaoPorVaga().get(1).getHorasOcupadas());
    assertEquals(0.8, historico.getOcupacaoPorVaga().get(1).getPercentualPeriodo());
  }

  // HU-12, teste 02: três sessões estão ocupando vagas às 19h; nenhuma outra hora tem tanto movimento
  @Test
  void faixaDePicoEAHoraComMaisOcupacao() {
    HistoricoResponse historico = setembro();

    assertEquals(19, historico.getFaixaPico());
    assertEquals(24, historico.getFaixasHorarias().size());
    assertEquals(180, historico.getFaixasHorarias().get(19).getMinutosOcupados());
    assertEquals(150, historico.getFaixasHorarias().get(20).getMinutosOcupados());
    assertEquals(90, historico.getFaixasHorarias().get(18).getMinutosOcupados());
    assertEquals(0, historico.getFaixasHorarias().get(3).getMinutosOcupados());
  }

  // HU-12, teste 03: 81,0 e 76,0 kWh
  @Test
  void consumoPorApartamentoSomaAEnergiaApurada() {
    HistoricoResponse historico = setembro();

    assertEquals(2, historico.getConsumoPorApartamento().size());
    assertEquals("17", historico.getConsumoPorApartamento().get(0).getApartamento());
    assertEquals(81.0, historico.getConsumoPorApartamento().get(0).getEnergiaKwh());
    assertEquals(new BigDecimal("74.52"), historico.getConsumoPorApartamento().get(0).getValor());
    assertEquals(2, historico.getConsumoPorApartamento().get(0).getSessoes());
    assertEquals("42", historico.getConsumoPorApartamento().get(1).getApartamento());
    assertEquals(76.0, historico.getConsumoPorApartamento().get(1).getEnergiaKwh());
  }

  // HU-12, teste 04: sete recusas por vaga ocupada
  @Test
  void demandaReprimidaContaAsRecusasPorVaga() {
    HistoricoResponse historico = setembro();

    assertEquals(7, historico.getTotalRecusas());
    assertEquals(2, historico.getRecusasPorVaga().size());
    assertEquals("G-01", historico.getRecusasPorVaga().get(0).getVaga());
    assertEquals(5, historico.getRecusasPorVaga().get(0).getRecusas());
    assertEquals("G-02", historico.getRecusasPorVaga().get(1).getVaga());
    assertEquals(2, historico.getRecusasPorVaga().get(1).getRecusas());
  }

  // HU-12, teste 05
  @Test
  void recusasSaoDistribuidasPorFaixaHoraria() {
    HistoricoResponse historico = setembro();

    assertEquals(4, historico.getFaixasHorarias().get(19).getRecusas());
    assertEquals(2, historico.getFaixasHorarias().get(20).getRecusas());
    assertEquals(1, historico.getFaixasHorarias().get(8).getRecusas());
    assertEquals(0, historico.getFaixasHorarias().get(12).getRecusas());
    assertEquals(7, historico.getFaixasHorarias().stream().mapToLong(faixa -> faixa.getRecusas()).sum());
  }

  // HU-12, teste 06
  @Test
  void periodoSemUtilizacaoInformaAusenciaDeRegistros() {
    sessoes.clear();
    recusas.clear();

    HistoricoResponse historico = servico().consultar(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, null);

    assertTrue(historico.isSemRegistros());
    assertEquals("Não há registros de utilização no período.", historico.getMensagem());
    assertNull(historico.getFaixaPico());
    assertEquals(0, historico.getTotalRecusas());
    assertTrue(historico.getOcupacaoPorVaga().isEmpty());
    assertTrue(historico.getConsumoPorApartamento().isEmpty());
    assertTrue(historico.getFaixasHorarias().isEmpty());
  }

  @Test
  void soRecusasNoPeriodoJaSaoRegistros() {
    sessoes.clear();

    HistoricoResponse historico = setembro();

    assertFalse(historico.isSemRegistros());
    assertEquals(7, historico.getTotalRecusas());
    assertNull(historico.getFaixaPico());
    assertTrue(historico.getConsumoPorApartamento().isEmpty());
    assertEquals(0.0, historico.getOcupacaoPorVaga().get(0).getHorasOcupadas());
  }

  // A sessão aberta ocupa a vaga até agora, mas ainda não consumiu nada apurado
  @Test
  void sessaoAbertaOcupaAteAgoraEFicaForaDoConsumo() {
    sessoes.clear();
    sessoes.add(emAndamento(g01, apto17, "2026-10-05T10:00"));

    HistoricoResponse historico = servico().consultar(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), null, null);

    assertEquals(2.0, historico.getOcupacaoPorVaga().get(0).getHorasOcupadas());
    assertTrue(historico.getConsumoPorApartamento().isEmpty());
    assertEquals(60, historico.getFaixasHorarias().get(10).getMinutosOcupados());
    assertEquals(60, historico.getFaixasHorarias().get(11).getMinutosOcupados());
  }

  // O percentual não é diluído pelos dias que ainda não aconteceram: o período para às 12h do dia 5
  @Test
  void periodoNaoPassaDeAgora() {
    sessoes.clear();
    sessoes.add(encerrada(g01, apto17, "2026-10-04T00:00", "2026-10-04T12:00", 10.0, "9.20"));

    HistoricoResponse historico = servico().consultar(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null);

    // 12 h em 4,5 dias (108 h)
    assertEquals(11.1, historico.getOcupacaoPorVaga().get(0).getPercentualPeriodo());
  }

  @Test
  void periodoTodoNoFuturoNaoTemRegistros() {
    HistoricoResponse historico = servico().consultar(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, null);

    assertTrue(historico.isSemRegistros());
    verify(sessaoRepository, never()).buscarQueTocamOPeriodo(any(), any());
  }

  // A sessão que atravessa o início do período conta só a parte de dentro; o consumo é da sessão iniciada no período
  @Test
  void sessaoQueComecouAntesContaNaOcupacaoMasNaoNoConsumo() {
    sessoes.clear();
    sessoes.add(encerrada(g01, apto17, "2026-08-31T22:00", "2026-09-01T02:00", 20.0, "18.40"));

    HistoricoResponse historico = setembro();

    assertEquals(2.0, historico.getOcupacaoPorVaga().get(0).getHorasOcupadas());
    assertEquals(120, historico.getFaixasHorarias().stream().mapToLong(faixa -> faixa.getMinutosOcupados()).sum());
    assertEquals(0, historico.getFaixasHorarias().get(23).getMinutosOcupados());
    assertTrue(historico.getConsumoPorApartamento().isEmpty());
  }

  @Test
  void filtraPorVaga() {
    HistoricoResponse historico = servico().consultar(SETEMBRO_DE, SETEMBRO_ATE, 2L, null);

    assertEquals(1, historico.getOcupacaoPorVaga().size());
    assertEquals("G-02", historico.getOcupacaoPorVaga().get(0).getVaga());
    assertEquals(2, historico.getTotalRecusas());
    assertEquals(1, historico.getConsumoPorApartamento().size());
    assertEquals("42", historico.getConsumoPorApartamento().get(0).getApartamento());
  }

  @Test
  void filtraPorApartamento() {
    HistoricoResponse historico = servico().consultar(SETEMBRO_DE, SETEMBRO_ATE, null, 170L);

    assertEquals(1, historico.getConsumoPorApartamento().size());
    assertEquals(81.0, historico.getConsumoPorApartamento().get(0).getEnergiaKwh());
    assertEquals(4.5, historico.getOcupacaoPorVaga().get(0).getHorasOcupadas());
    assertEquals(0.0, historico.getOcupacaoPorVaga().get(1).getHorasOcupadas());
    // as duas recusas do apto 17 foram na G-02
    assertEquals(2, historico.getTotalRecusas());
  }

  @Test
  void vagaDesativadaSoApareceSeFoiUsadaNoPeriodo() {
    Vaga g03 = vaga(3L, "G-03", false);
    when(vagaRepository.findAll()).thenReturn(List.of(g01, g02, g03));
    assertEquals(2, setembro().getOcupacaoPorVaga().size());

    sessoes.add(encerrada(g03, apto17, "2026-09-12T10:00", "2026-09-12T11:00", 5.0, "4.60"));
    assertEquals(3, setembro().getOcupacaoPorVaga().size());
  }

  @Test
  void periodoSemDatasOuInvertidoEhRecusado() {
    RegraNegocioException semDatas = assertThrows(RegraNegocioException.class,
        () -> servico().consultar(null, SETEMBRO_ATE, null, null));
    assertEquals(HttpStatus.BAD_REQUEST, semDatas.getStatus());

    RegraNegocioException invertido = assertThrows(RegraNegocioException.class,
        () -> servico().consultar(SETEMBRO_ATE, SETEMBRO_DE, null, null));
    assertEquals(HttpStatus.BAD_REQUEST, invertido.getStatus());
    assertEquals("A data inicial não pode ser depois da final.", invertido.getMessage());
  }
}
