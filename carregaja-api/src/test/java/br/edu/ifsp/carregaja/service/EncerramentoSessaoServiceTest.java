package br.edu.ifsp.carregaja.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import br.edu.ifsp.carregaja.model.Apartamento;
import br.edu.ifsp.carregaja.model.EstadoSessao;
import br.edu.ifsp.carregaja.model.Perfil;
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
import br.edu.ifsp.carregaja.view.SessaoAbertaResponse;
import br.edu.ifsp.carregaja.view.SessaoResponse;

/** UC03 (encerramento) e UC10, com os números das histórias HU-03 e HU-10. */
class EncerramentoSessaoServiceTest {

  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
  private static final LocalDateTime AS_18H10 = LocalDateTime.of(2026, 9, 8, 18, 10);
  private static final Long MORADOR = 42L;
  private static final Long SESSAO = 7L;

  private SessaoRecargaRepository sessaoRepository;
  private TarifaService tarifaService;
  private Usuario morador;
  private Usuario sindico;

  @BeforeEach
  void preparar() {
    sessaoRepository = mock(SessaoRecargaRepository.class);
    tarifaService = mock(TarifaService.class);
    when(sessaoRepository.save(any(SessaoRecarga.class))).thenAnswer(chamada -> chamada.getArgument(0));

    Apartamento apartamento = new Apartamento();
    apartamento.setNumero("17");
    morador = new Usuario();
    morador.setId(MORADOR);
    morador.setNome("Morador do 17");
    morador.setApartamento(apartamento);
    sindico = new Usuario();
    sindico.setId(1L);
    sindico.setNome("Síndica");
    sindico.getPerfis().add(Perfil.SINDICO);
  }

  private SessaoRecargaService servicoAs(LocalDateTime momento) {
    Clock relogio = Clock.fixed(momento.atZone(FUSO).toInstant(), FUSO);
    return new SessaoRecargaService(sessaoRepository, mock(RecusaVagaRepository.class), mock(UsuarioRepository.class),
        mock(VagaRepository.class), mock(VeiculoRepository.class), tarifaService, new ApuracaoEnergiaService(),
        relogio, 6);
  }

  // A sessão como o início a deixou: potência, energia a repor, previsão e tarifa já fixadas (RN-04)
  private SessaoRecarga aberta(LocalDateTime inicio, double potencia, double energiaARepor, LocalDateTime previsao) {
    Vaga vaga = new Vaga();
    vaga.setIdentificacao("G-02");
    Veiculo veiculo = new Veiculo();
    veiculo.setModelo("Nissan Leaf");
    Tarifa tarifa = new Tarifa();
    tarifa.setValorKwh(new BigDecimal("0.92"));

    SessaoRecarga sessao = new SessaoRecarga();
    sessao.setId(SESSAO);
    sessao.setUsuario(morador);
    sessao.setVaga(vaga);
    sessao.setVeiculo(veiculo);
    sessao.setTarifa(tarifa);
    sessao.setInicio(inicio);
    sessao.setNivelBateriaInicial(30);
    sessao.setPotenciaEfetiva(potencia);
    sessao.setEnergiaARepor(energiaARepor);
    sessao.setPrevisaoConclusao(previsao);
    return sessao;
  }

  private SessaoRecarga doLeaf() {
    return aberta(AS_18H10, 6.6, 28.0, LocalDateTime.of(2026, 9, 8, 22, 25));
  }

  private void moradorTem(SessaoRecarga sessao) {
    when(sessaoRepository.findComTravaByUsuarioIdAndSituacao(MORADOR, SituacaoSessao.EM_ANDAMENTO))
        .thenReturn(Optional.of(sessao));
  }

  // HU-03, teste 13; HU-04, teste 02: 6,6 kW por 4h30 dariam 29,7 kWh, mas só faltavam 28,0
  @Test
  void encerramentoComTetoDeEnergiaAcionado() {
    SessaoRecarga sessao = doLeaf();
    moradorTem(sessao);

    SessaoResponse encerrada = servicoAs(LocalDateTime.of(2026, 9, 8, 22, 40)).encerrar(morador);

    assertEquals(28.0, encerrada.getEnergiaEstimada());
    assertEquals(new BigDecimal("25.76"), encerrada.getValor());
    assertEquals(SituacaoSessao.ENCERRADA, sessao.getSituacao());
    assertEquals(EstadoSessao.ENCERRADA, encerrada.getEstado());
    verify(sessaoRepository).save(sessao);
  }

  // HU-03, teste 14; HU-04, teste 01
  @Test
  void encerramentoAntesDeCompletarACarga() {
    moradorTem(doLeaf());

    SessaoResponse encerrada = servicoAs(LocalDateTime.of(2026, 9, 8, 20, 10)).encerrar(morador);

    assertEquals(13.2, encerrada.getEnergiaEstimada());
    assertEquals(new BigDecimal("12.14"), encerrada.getValor());
  }

  // HU-03, teste 15: a resposta já traz o que a tela mostra ao morador
  @Test
  void encerramentoDevolveDuracaoEnergiaEValor() {
    moradorTem(doLeaf());

    SessaoResponse encerrada = servicoAs(LocalDateTime.of(2026, 9, 8, 22, 40)).encerrar(morador);

    assertEquals(LocalDateTime.of(2026, 9, 8, 22, 40), encerrada.getFim());
    assertEquals("4h30", encerrada.getDuracao());
    assertEquals(28.0, encerrada.getEnergiaEstimada());
    assertEquals(new BigDecimal("25.76"), encerrada.getValor());
    assertNull(encerrada.getEncerradaPor());
  }

  // HU-03, teste 16. Também é o que o morador recebe se o síndico encerrou a sessão um instante antes
  @Test
  void semSessaoAbertaInformaQueNaoHaOQueEncerrar() {
    when(sessaoRepository.findComTravaByUsuarioIdAndSituacao(MORADOR, SituacaoSessao.EM_ANDAMENTO))
        .thenReturn(Optional.empty());

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> servicoAs(AS_18H10).encerrar(morador));

    assertEquals(HttpStatus.NOT_FOUND, recusa.getStatus());
    assertTrue(recusa.getMessage().contains("não tem sessão de recarga aberta"));
    verify(sessaoRepository, never()).save(any());
  }

  // HU-03, teste 17; HU-04, teste 04: a tarifa é a gravada no início, nem se consulta a vigente
  @Test
  void tarifaAlteradaDuranteASessaoNaoMudaOValor() {
    moradorTem(doLeaf());

    SessaoResponse encerrada = servicoAs(LocalDateTime.of(2026, 9, 8, 20, 10)).encerrar(morador);

    assertEquals(new BigDecimal("0.92"), encerrada.getValorKwh());
    assertEquals(new BigDecimal("12.14"), encerrada.getValor());
    verifyNoInteractions(tarifaService);
  }

  // HU-10: sessão de 14h00 a 21h00, 11 kW, 48 kWh a repor, prevista para 18h22
  private SessaoRecarga orfa() {
    SessaoRecarga sessao = aberta(LocalDateTime.of(2026, 9, 8, 14, 0), 11.0, 48.0,
        LocalDateTime.of(2026, 9, 8, 18, 22));
    when(sessaoRepository.findComTravaById(SESSAO)).thenReturn(Optional.of(sessao));
    return sessao;
  }

  // HU-10, testes 01 e 03; HU-04, teste 03: 11 kW por 7 h dariam 77,0 kWh, mas só faltavam 48,0
  @Test
  void encerramentoAdministrativoApuraComOTeto() {
    orfa();

    SessaoResponse encerrada = servicoAs(LocalDateTime.of(2026, 9, 8, 21, 0))
        .encerrarAdministrativamente(SESSAO, sindico);

    assertEquals(48.0, encerrada.getEnergiaEstimada());
    assertEquals(new BigDecimal("44.16"), encerrada.getValor());
    assertEquals(EstadoSessao.ENCERRADA, encerrada.getEstado());
  }

  // HU-10, teste 02 (RF-15): fica marcada como administrativa, com o síndico e o horário
  @Test
  void encerramentoAdministrativoRegistraOSindicoEOHorario() {
    SessaoRecarga sessao = orfa();

    SessaoResponse encerrada = servicoAs(LocalDateTime.of(2026, 9, 8, 21, 0))
        .encerrarAdministrativamente(SESSAO, sindico);

    assertEquals(SituacaoSessao.ENCERRADA_ADMINISTRATIVAMENTE, sessao.getSituacao());
    assertEquals(sindico, sessao.getEncerradaPor());
    assertEquals(LocalDateTime.of(2026, 9, 8, 21, 0), sessao.getFim());
    assertEquals(SituacaoSessao.ENCERRADA_ADMINISTRATIVAMENTE, encerrada.getSituacao());
    assertEquals("Síndica", encerrada.getEncerradaPor());
    verify(sessaoRepository).save(sessao);
  }

  // RN-07: antes da previsão a recarga pode estar acontecendo, então só o morador encerra
  @Test
  void sindicoNaoEncerraSessaoDentroDaPrevisao() {
    orfa();

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> servicoAs(LocalDateTime.of(2026, 9, 8, 18, 0)).encerrarAdministrativamente(SESSAO, sindico));

    assertEquals(HttpStatus.CONFLICT, recusa.getStatus());
    assertTrue(recusa.getMessage().contains("18:22"));
    verify(sessaoRepository, never()).save(any());
  }

  // Segunda barreira: a rota já é do síndico, mas o encerramento grava o responsável (RF-15)
  @Test
  void quemNaoESindicoNaoEncerraAdministrativamente() {
    orfa();

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> servicoAs(LocalDateTime.of(2026, 9, 8, 21, 0)).encerrarAdministrativamente(SESSAO, morador));

    assertEquals(HttpStatus.FORBIDDEN, recusa.getStatus());
    verify(sessaoRepository, never()).save(any());
  }

  @Test
  void sindicoNaoEncerraSessaoJaEncerrada() {
    orfa().setSituacao(SituacaoSessao.ENCERRADA);

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> servicoAs(LocalDateTime.of(2026, 9, 8, 21, 0)).encerrarAdministrativamente(SESSAO, sindico));

    assertEquals(HttpStatus.CONFLICT, recusa.getStatus());
    verify(sessaoRepository, never()).save(any());
  }

  @Test
  void sessaoInexistenteDevolve404() {
    when(sessaoRepository.findComTravaById(99L)).thenReturn(Optional.empty());

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> servicoAs(AS_18H10).encerrarAdministrativamente(99L, sindico));

    assertEquals(HttpStatus.NOT_FOUND, recusa.getStatus());
  }

  // UC10: o síndico vê as abertas, as mais antigas primeiro, com quem ocupa e o estado de cada uma
  @Test
  void listaAsAbertasDaMaisAntigaParaAMaisNova() {
    SessaoRecarga recente = doLeaf();
    SessaoRecarga antiga = aberta(LocalDateTime.of(2026, 9, 8, 14, 0), 11.0, 48.0,
        LocalDateTime.of(2026, 9, 8, 18, 22));
    antiga.setId(8L);
    when(sessaoRepository.findBySituacao(SituacaoSessao.EM_ANDAMENTO)).thenReturn(List.of(recente, antiga));

    List<SessaoAbertaResponse> abertas = servicoAs(LocalDateTime.of(2026, 9, 8, 21, 0)).listarAbertas();

    assertEquals(2, abertas.size());
    assertEquals(8L, abertas.get(0).getId());
    assertEquals(EstadoSessao.EXCEDIDA, abertas.get(0).getEstado());
    assertEquals("17", abertas.get(0).getApartamento());
    assertEquals("Morador do 17", abertas.get(0).getMorador());
    assertEquals(EstadoSessao.EM_ANDAMENTO, abertas.get(1).getEstado());
  }
}
