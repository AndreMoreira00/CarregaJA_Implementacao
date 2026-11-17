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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import br.edu.ifsp.carregaja.model.Apartamento;
import br.edu.ifsp.carregaja.model.FechamentoMensal;
import br.edu.ifsp.carregaja.model.ItemRateio;
import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.SituacaoSessao;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.model.Vaga;
import br.edu.ifsp.carregaja.repository.FechamentoMensalRepository;
import br.edu.ifsp.carregaja.repository.ItemRateioRepository;
import br.edu.ifsp.carregaja.repository.SessaoRecargaRepository;
import br.edu.ifsp.carregaja.view.FechamentoMensalRequest;
import br.edu.ifsp.carregaja.view.FechamentoMensalResponse;
import br.edu.ifsp.carregaja.view.FechamentoMensalResponse.Item;

class FechamentoMensalServiceTest {

  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
  private static final LocalDateTime AS_10H = LocalDateTime.of(2026, 10, 5, 10, 0);
  private static final LocalDateTime SETEMBRO = LocalDateTime.of(2026, 9, 1, 0, 0);
  private static final LocalDateTime OUTUBRO = LocalDateTime.of(2026, 10, 1, 0, 0);

  private FechamentoMensalRepository fechamentoRepository;
  private ItemRateioRepository itemRepository;
  private SessaoRecargaRepository sessaoRepository;
  private FechamentoMensalService service;
  private final Usuario sindico = usuario(1L, "Síndico");
  private final Usuario moradorDo42 = morador(42L, "42");
  private final Usuario moradorDo17 = morador(17L, "17");
  private final List<SessaoRecarga> sessoesDeSetembro = new ArrayList<>();

  @BeforeEach
  void preparar() {
    fechamentoRepository = mock(FechamentoMensalRepository.class);
    itemRepository = mock(ItemRateioRepository.class);
    sessaoRepository = mock(SessaoRecargaRepository.class);
    when(fechamentoRepository.save(any(FechamentoMensal.class))).thenAnswer(chamada -> chamada.getArgument(0));
    when(itemRepository.save(any(ItemRateio.class))).thenAnswer(chamada -> chamada.getArgument(0));
    when(sessaoRepository.findByInicioGreaterThanEqualAndInicioLessThan(SETEMBRO, OUTUBRO))
        .thenReturn(sessoesDeSetembro);

    sessoesDeSetembro.add(encerrada(moradorDo42, 3, 28.0, "25.76"));
    sessoesDeSetembro.add(encerrada(moradorDo17, 5, 48.0, "44.16"));
    sessoesDeSetembro.add(encerrada(moradorDo42, 12, 24.5, "22.54"));
    sessoesDeSetembro.add(encerrada(moradorDo17, 20, 33.0, "30.36"));
    sessoesDeSetembro.add(encerrada(moradorDo42, 28, 23.5, "21.62"));

    Clock relogio = Clock.fixed(AS_10H.atZone(FUSO).toInstant(), FUSO);
    service = new FechamentoMensalService(fechamentoRepository, itemRepository, sessaoRepository, new RateioService(),
        relogio);
  }

  private static Usuario usuario(Long id, String nome) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setNome(nome);
    usuario.getPerfis().add(Perfil.SINDICO);
    return usuario;
  }

  private static Usuario morador(Long id, String numero) {
    Apartamento apartamento = new Apartamento();
    apartamento.setNumero(numero);
    Usuario morador = new Usuario();
    morador.setId(id);
    morador.setNome("Morador " + numero);
    morador.setApartamento(apartamento);
    morador.getPerfis().add(Perfil.MORADOR);
    return morador;
  }

  private static SessaoRecarga encerrada(Usuario morador, int dia, double energia, String valor) {
    SessaoRecarga sessao = new SessaoRecarga();
    sessao.setUsuario(morador);
    sessao.setVaga(new Vaga());
    sessao.setInicio(LocalDateTime.of(2026, 9, dia, 18, 0));
    sessao.setEnergiaEstimada(energia);
    sessao.setValor(new BigDecimal(valor));
    sessao.setSituacao(SituacaoSessao.ENCERRADA);
    return sessao;
  }

  private static FechamentoMensalRequest requisicao(Integer ano, Integer mes) {
    FechamentoMensalRequest requisicao = new FechamentoMensalRequest();
    requisicao.setAno(ano);
    requisicao.setMes(mes);
    return requisicao;
  }

  // HU-11, testes 01 e 06
  @Test
  void fecharSetembroGravaResponsavelHorarioETotalGeral() {
    FechamentoMensalResponse resposta = service.fechar(sindico, requisicao(2026, 9));

    assertEquals(AS_10H, resposta.getMomento());
    assertEquals("Síndico", resposta.getNomeResponsavel());
    assertEquals(157.0, resposta.getEnergiaTotal());
    assertEquals(new BigDecimal("144.44"), resposta.getValorTotal());
    verify(fechamentoRepository).save(any(FechamentoMensal.class));
  }

  // HU-11, teste 02
  @Test
  void rateioGravaUmItemPorApartamento() {
    FechamentoMensalResponse resposta = service.fechar(sindico, requisicao(2026, 9));

    assertEquals(2, resposta.getItens().size());
    Item do17 = resposta.getItens().get(0);
    assertEquals("17", do17.getApartamento());
    assertEquals(81.0, do17.getEnergia());
    assertEquals(new BigDecimal("74.52"), do17.getValor());
  }

  // HU-11, teste 03
  @Test
  void sessaoAbertaNoMesImpedeOFechamento() {
    Vaga vaga = new Vaga();
    vaga.setIdentificacao("G-02");
    SessaoRecarga aberta = new SessaoRecarga();
    aberta.setUsuario(moradorDo42);
    aberta.setVaga(vaga);
    aberta.setInicio(LocalDateTime.of(2026, 9, 30, 22, 15));
    sessoesDeSetembro.add(aberta);

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> service.fechar(sindico, requisicao(2026, 9)));

    assertEquals(HttpStatus.CONFLICT, recusa.getStatus());
    assertTrue(recusa.getMessage().contains("vaga G-02, apto 42, início 30/09/2026 22:15"));
    verify(fechamentoRepository, never()).save(any());
  }

  @Test
  void mesJaFechadoNaoFechaDeNovo() {
    when(fechamentoRepository.existsByAnoAndMes(2026, 9)).thenReturn(true);

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> service.fechar(sindico, requisicao(2026, 9)));

    assertEquals(HttpStatus.CONFLICT, recusa.getStatus());
    verify(fechamentoRepository, never()).save(any());
  }

  // HU-11, teste 04
  @Test
  void mesQueAindaNaoTerminouERecusado() {
    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> service.fechar(sindico, requisicao(2026, 10)));

    assertEquals(HttpStatus.BAD_REQUEST, recusa.getStatus());
    assertEquals("O mês ainda não terminou.", recusa.getMessage());
    verify(fechamentoRepository, never()).save(any());
  }

  @Test
  void mesSemSessaoEncerradaNaoTemRateio() {
    sessoesDeSetembro.clear();

    RegraNegocioException recusa = assertThrows(RegraNegocioException.class,
        () -> service.fechar(sindico, requisicao(2026, 9)));

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, recusa.getStatus());
    verify(fechamentoRepository, never()).save(any());
  }

  @Test
  void buscarPorIdTrazOsItensGravados() {
    FechamentoMensal fechamento = new FechamentoMensal();
    fechamento.setId(1L);
    fechamento.setAno(2026);
    fechamento.setMes(9);
    fechamento.setValorTotal(new BigDecimal("69.92"));
    fechamento.setResponsavel(sindico);
    ItemRateio item = new ItemRateio();
    item.setApartamento(moradorDo42.getApartamento());
    item.setEnergia(76.0);
    item.setValor(new BigDecimal("69.92"));
    when(fechamentoRepository.findById(1L)).thenReturn(Optional.of(fechamento));
    when(itemRepository.findByFechamentoMensalId(1L)).thenReturn(List.of(item));

    FechamentoMensalResponse resposta = service.buscarPorId(1L).orElseThrow();

    assertEquals("42", resposta.getItens().get(0).getApartamento());
    assertEquals(76.0, resposta.getEnergiaTotal());
  }

  @Test
  void buscarInexistenteDevolveVazio() {
    when(fechamentoRepository.findById(99L)).thenReturn(Optional.empty());

    assertTrue(service.buscarPorId(99L).isEmpty());
  }
}
