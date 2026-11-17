package br.edu.ifsp.carregaja.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import br.edu.ifsp.carregaja.model.EstadoSessao;
import br.edu.ifsp.carregaja.model.RecusaVaga;
import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.SituacaoSessao;
import br.edu.ifsp.carregaja.model.Tarifa;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.model.Vaga;
import br.edu.ifsp.carregaja.model.Veiculo;
import br.edu.ifsp.carregaja.repository.RecusaVagaRepository;
import br.edu.ifsp.carregaja.repository.SessaoRecargaRepository;
import br.edu.ifsp.carregaja.repository.UsuarioRepository;
import br.edu.ifsp.carregaja.repository.VagaRepository;
import br.edu.ifsp.carregaja.repository.VeiculoRepository;
import br.edu.ifsp.carregaja.view.SessaoRequest;
import br.edu.ifsp.carregaja.view.SessaoResponse;

class SessaoRecargaServiceTest {

  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
  private static final LocalDateTime AS_18H10 = LocalDateTime.of(2026, 9, 8, 18, 10);
  private static final Long MORADOR = 42L;
  private static final Long G02 = 2L;

  private SessaoRecargaRepository sessaoRepository;
  private RecusaVagaRepository recusaRepository;
  private VeiculoRepository veiculoRepository;
  private TarifaService tarifaService;
  private SessaoRecargaService service;
  private Usuario morador;
  private Vaga vaga;

  private SessaoRecargaService servicoAs(LocalDateTime momento) {
    UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    VagaRepository vagaRepository = mock(VagaRepository.class);
    when(usuarioRepository.findComTravaById(MORADOR)).thenReturn(Optional.of(morador));
    when(vagaRepository.findComTravaById(G02)).thenReturn(Optional.of(vaga));
    Clock relogio = Clock.fixed(momento.atZone(FUSO).toInstant(), FUSO);
    return new SessaoRecargaService(sessaoRepository, recusaRepository, usuarioRepository, vagaRepository,
        veiculoRepository, tarifaService, new ApuracaoEnergiaService(), relogio, 6);
  }

  @BeforeEach
  void preparar() {
    sessaoRepository = mock(SessaoRecargaRepository.class);
    recusaRepository = mock(RecusaVagaRepository.class);
    veiculoRepository = mock(VeiculoRepository.class);
    tarifaService = mock(TarifaService.class);
    when(sessaoRepository.save(any(SessaoRecarga.class))).thenAnswer(chamada -> chamada.getArgument(0));
    when(sessaoRepository.findByUsuarioIdAndSituacao(any(), any())).thenReturn(Optional.empty());
    when(sessaoRepository.findByVagaIdAndSituacao(any(), any())).thenReturn(Optional.empty());

    morador = new Usuario();
    morador.setId(MORADOR);
    vaga = new Vaga();
    vaga.setId(G02);
    vaga.setIdentificacao("G-02");
    vaga.setPotenciaCarregador(7.4);

    Veiculo leaf = new Veiculo();
    leaf.setModelo("Nissan Leaf");
    leaf.setCapacidadeBateria(40.0);
    leaf.setPotenciaMaxima(6.6);
    when(veiculoRepository.findByUsuarioId(MORADOR)).thenReturn(Optional.of(leaf));

    Tarifa tarifa = new Tarifa();
    tarifa.setValorKwh(new BigDecimal("0.92"));
    when(tarifaService.buscarVigenteEm(AS_18H10)).thenReturn(Optional.of(tarifa));

    service = servicoAs(AS_18H10);
  }

  private static SessaoRequest requisicao(int nivel) {
    SessaoRequest requisicao = new SessaoRequest();
    requisicao.setVagaId(G02);
    requisicao.setNivelBateria(nivel);
    return requisicao;
  }

  private RegraNegocioException recusaAoIniciar(int nivel) {
    return assertThrows(RegraNegocioException.class, () -> service.iniciar(morador, requisicao(nivel)));
  }

  private SessaoRecarga sessaoAberta(LocalDateTime previsao) {
    SessaoRecarga sessao = new SessaoRecarga();
    sessao.setUsuario(morador);
    sessao.setVaga(vaga);
    sessao.setVeiculo(new Veiculo());
    sessao.setTarifa(new Tarifa());
    sessao.setInicio(AS_18H10);
    sessao.setPrevisaoConclusao(previsao);
    return sessao;
  }

  // HU-03, teste 01; HU-05, teste 04: pede só a vaga e o nível
  @Test
  void inicioEmVagaLivreAbreASessaoComAPrevisao() {
    SessaoResponse sessao = service.iniciar(morador, requisicao(30));

    assertEquals(6.6, sessao.getPotenciaEfetiva());
    assertEquals(28.0, sessao.getEnergiaARepor());
    assertEquals(LocalDateTime.of(2026, 9, 8, 22, 25), sessao.getPrevisaoConclusao());
    assertEquals(EstadoSessao.EM_ANDAMENTO, sessao.getEstado());
    assertEquals("Nissan Leaf", sessao.getVeiculo());
  }

  // HU-03, teste 01; RN-04: potência, energia e tarifa ficam gravadas na sessão
  @Test
  void aSessaoGravaOQueFoiFixadoNoInicio() {
    service.iniciar(morador, requisicao(30));

    ArgumentCaptor<SessaoRecarga> gravada = ArgumentCaptor.forClass(SessaoRecarga.class);
    verify(sessaoRepository).save(gravada.capture());
    assertEquals(6.6, gravada.getValue().getPotenciaEfetiva());
    assertEquals(28.0, gravada.getValue().getEnergiaARepor());
    assertEquals(new BigDecimal("0.92"), gravada.getValue().getTarifa().getValorKwh());
    assertEquals(SituacaoSessao.EM_ANDAMENTO, gravada.getValue().getSituacao());
    assertEquals(morador, gravada.getValue().getUsuario());
  }

  // HU-03, teste 02: recusa com a previsão, contabilizada para o UC12
  @Test
  void vagaOcupadaERecusadaComAPrevisaoEARecusaFicaRegistrada() {
    when(sessaoRepository.findByVagaIdAndSituacao(G02, SituacaoSessao.EM_ANDAMENTO))
        .thenReturn(Optional.of(sessaoAberta(LocalDateTime.of(2026, 9, 8, 19, 30))));

    RegraNegocioException recusa = recusaAoIniciar(30);

    assertEquals(HttpStatus.CONFLICT, recusa.getStatus());
    assertTrue(recusa.getMessage().contains("19:30"));
    ArgumentCaptor<RecusaVaga> registrada = ArgumentCaptor.forClass(RecusaVaga.class);
    verify(recusaRepository).save(registrada.capture());
    assertEquals(vaga, registrada.getValue().getVaga());
    assertEquals(morador, registrada.getValue().getUsuario());
    assertEquals(AS_18H10, registrada.getValue().getMomento());
    verify(sessaoRepository, never()).save(any());
  }

  // HU-03, teste 04
  @Test
  void vagaDesativadaERecusadaSemContarComoDemanda() {
    vaga.setAtiva(false);

    assertEquals(HttpStatus.CONFLICT, recusaAoIniciar(30).getStatus());
    verify(recusaRepository, never()).save(any());
    verify(sessaoRepository, never()).save(any());
  }

  // HU-03, teste 05
  @Test
  void moradorSemVeiculoEEncaminhadoAoCadastro() {
    when(veiculoRepository.findByUsuarioId(MORADOR)).thenReturn(Optional.empty());

    RegraNegocioException recusa = recusaAoIniciar(30);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, recusa.getStatus());
    assertTrue(recusa.getMessage().contains("Cadastre seu veículo"));
  }

  // HU-03, teste 06
  @Test
  void moradorComSessaoAbertaERecusado() {
    when(sessaoRepository.findByUsuarioIdAndSituacao(MORADOR, SituacaoSessao.EM_ANDAMENTO))
        .thenReturn(Optional.of(sessaoAberta(AS_18H10.plusHours(4))));

    assertEquals(HttpStatus.CONFLICT, recusaAoIniciar(30).getStatus());
    verify(sessaoRepository, never()).save(any());
  }

  // HU-03, teste 07
  @Test
  void sessaoAbertaAlemDaDuracaoMaximaApareceExcedida() {
    SessaoRecarga aberta = sessaoAberta(LocalDateTime.of(2026, 9, 8, 22, 25));
    aberta.getVaga().setIdentificacao("G-02");
    when(sessaoRepository.findByUsuarioIdAndSituacao(MORADOR, SituacaoSessao.EM_ANDAMENTO))
        .thenReturn(Optional.of(aberta));

    SessaoRecargaService seisHorasDepois = servicoAs(AS_18H10.plusHours(6));

    assertEquals(EstadoSessao.EXCEDIDA, seisHorasDepois.buscarAberta(morador).orElseThrow().getEstado());
  }

  // HU-03, teste 12
  @Test
  void bateriaCheiaInformaQueNaoHaRecargaAFazer() {
    RegraNegocioException recusa = recusaAoIniciar(100);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, recusa.getStatus());
    assertTrue(recusa.getMessage().contains("não há recarga a fazer"));
    verify(sessaoRepository, never()).save(any());
  }

  @Test
  void semTarifaCadastradaNaoAbreSessao() {
    when(tarifaService.buscarVigenteEm(AS_18H10)).thenReturn(Optional.empty());

    assertEquals(HttpStatus.CONFLICT, recusaAoIniciar(30).getStatus());
    verify(sessaoRepository, never()).save(any());
  }

  @Test
  void vagaInexistenteDevolve404() {
    SessaoRequest requisicao = requisicao(30);
    requisicao.setVagaId(99L);

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class, () -> service.iniciar(morador, requisicao));

    assertEquals(HttpStatus.NOT_FOUND, recusa.getStatus());
  }
}
