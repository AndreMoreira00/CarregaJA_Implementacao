package br.edu.ifsp.carregaja.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.edu.ifsp.carregaja.model.EstadoSessao;
import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.SituacaoSessao;
import br.edu.ifsp.carregaja.model.Tarifa;

class ApuracaoEnergiaServiceTest {

  private static final double LEAF_POTENCIA = 6.6;
  private static final double LEAF_BATERIA = 40.0;
  private static final double DOLPHIN_POTENCIA = 11.0;
  private static final double DOLPHIN_BATERIA = 60.0;
  private static final double G01 = 11.0;
  private static final double G02 = 7.4;
  private static final BigDecimal TARIFA = new BigDecimal("0.92");
  private static final Duration SEIS_HORAS = Duration.ofHours(6);
  private static final LocalDateTime INICIO = LocalDateTime.of(2026, 9, 8, 18, 10);

  private final ApuracaoEnergiaService service = new ApuracaoEnergiaService();

  private static Duration horas(int horas, int minutos) {
    return Duration.ofHours(horas).plusMinutes(minutos);
  }

  // HU-03, teste 08
  @Test
  void veiculoLimitaAPotencia() {
    double potencia = service.potenciaEfetiva(G02, LEAF_POTENCIA);
    double energia = service.energiaARepor(LEAF_BATERIA, 30);

    assertEquals(6.6, potencia);
    assertEquals(28.0, energia);
    assertEquals(horas(4, 15), service.tempoEstimado(energia, potencia));
  }

  // HU-03, teste 09
  @Test
  void carregadorLimitaAPotencia() {
    double potencia = service.potenciaEfetiva(G02, DOLPHIN_POTENCIA);
    double energia = service.energiaARepor(DOLPHIN_BATERIA, 20);

    assertEquals(7.4, potencia);
    assertEquals(48.0, energia);
    assertEquals(horas(6, 29), service.tempoEstimado(energia, potencia));
  }

  // HU-03, teste 10
  @Test
  void potenciasIguais() {
    double potencia = service.potenciaEfetiva(G01, DOLPHIN_POTENCIA);
    double energia = service.energiaARepor(DOLPHIN_BATERIA, 20);

    assertEquals(11.0, potencia);
    assertEquals(48.0, energia);
    assertEquals(horas(4, 22), service.tempoEstimado(energia, potencia));
  }

  // HU-03, teste 11
  @Test
  void carregadorMaiorNaoAceleraVeiculoLento() {
    double energia = service.energiaARepor(LEAF_BATERIA, 30);

    assertEquals(service.tempoEstimado(energia, service.potenciaEfetiva(G02, LEAF_POTENCIA)),
        service.tempoEstimado(energia, service.potenciaEfetiva(G01, LEAF_POTENCIA)));
  }

  // HU-03, teste 12
  @Test
  void bateriaCheiaNaoTemEnergiaARepor() {
    assertEquals(0.0, service.energiaARepor(LEAF_BATERIA, 100));
  }

  private SessaoRecarga sessao(double potencia, double energiaARepor) {
    Tarifa tarifa = new Tarifa();
    tarifa.setValorKwh(TARIFA);
    SessaoRecarga sessao = new SessaoRecarga();
    sessao.setInicio(INICIO);
    sessao.setPotenciaEfetiva(potencia);
    sessao.setEnergiaARepor(energiaARepor);
    sessao.setTarifa(tarifa);
    return sessao;
  }

  // HU-04, teste 01
  @Test
  void tetoDeEnergiaNaoAcionado() {
    SessaoRecarga sessao = sessao(6.6, 28.0);

    service.apurar(sessao, INICIO.plus(horas(2, 0)));

    assertEquals(13.2, sessao.getEnergiaEstimada());
    assertEquals(new BigDecimal("12.14"), sessao.getValor());
  }

  // HU-04, teste 02
  @Test
  void tetoDeEnergiaAcionado() {
    SessaoRecarga sessao = sessao(6.6, 28.0);

    service.apurar(sessao, INICIO.plus(horas(4, 30)));

    assertEquals(28.0, sessao.getEnergiaEstimada());
    assertEquals(new BigDecimal("25.76"), sessao.getValor());
  }

  // HU-04, teste 03
  @Test
  void tetoAcionadoEmSessaoOrfaLonga() {
    SessaoRecarga sessao = sessao(11.0, 48.0);

    service.apurar(sessao, INICIO.plus(horas(7, 0)));

    assertEquals(48.0, sessao.getEnergiaEstimada());
    assertEquals(new BigDecimal("44.16"), sessao.getValor());
  }

  // HU-04, teste 04; HU-03, teste 17; HU-08, testes 04 e 05
  @Test
  void apuraPelaTarifaGravadaNoInicio() {
    SessaoRecarga sessao = sessao(6.6, 28.0);
    Tarifa tarifaDoInicio = sessao.getTarifa();
    Tarifa reajuste = new Tarifa();
    reajuste.setValorKwh(new BigDecimal("1.05"));

    service.apurar(sessao, INICIO.plus(horas(4, 30)));

    assertEquals(tarifaDoInicio, sessao.getTarifa());
    assertEquals(new BigDecimal("25.76"), sessao.getValor());
    assertEquals(new BigDecimal("29.40"), service.valor(28.0, reajuste.getValorKwh()));
  }

  // HU-04, teste 05
  @Test
  void duracaoNulaNoMesmoMinuto() {
    SessaoRecarga sessao = sessao(6.6, 28.0);

    service.apurar(sessao, INICIO.plusSeconds(40));

    assertEquals(0.0, sessao.getEnergiaEstimada());
    assertEquals(new BigDecimal("0.00"), sessao.getValor());
  }

  // HU-05, teste 05: a apuração só usa o que a sessão gravou no início
  @Test
  void trocarOVeiculoDepoisNaoMudaAApuracao() {
    SessaoRecarga sessao = sessao(6.6, 28.0);

    service.apurar(sessao, INICIO.plus(horas(2, 0)));

    assertEquals(6.6, sessao.getPotenciaEfetiva());
    assertEquals(28.0, sessao.getEnergiaARepor());
    assertEquals(13.2, sessao.getEnergiaEstimada());
  }

  private SessaoRecarga sessaoComPrevisao(LocalDateTime inicio, LocalDateTime previsao) {
    SessaoRecarga sessao = new SessaoRecarga();
    sessao.setInicio(inicio);
    sessao.setPrevisaoConclusao(previsao);
    return sessao;
  }

  // HU-02, teste 02
  @Test
  void dentroDaPrevisaoEstaEmAndamento() {
    SessaoRecarga sessao = sessaoComPrevisao(LocalDateTime.of(2026, 9, 8, 14, 0), LocalDateTime.of(2026, 9, 8, 18, 22));

    assertEquals(EstadoSessao.EM_ANDAMENTO, service.estado(sessao, LocalDateTime.of(2026, 9, 8, 16, 0), SEIS_HORAS));
  }

  // HU-02, teste 03
  @Test
  void alemDaPrevisaoEDentroDasSeisHorasEstaConcluida() {
    SessaoRecarga sessao = sessaoComPrevisao(LocalDateTime.of(2026, 9, 8, 14, 0), LocalDateTime.of(2026, 9, 8, 18, 22));

    assertEquals(EstadoSessao.CONCLUIDA_AINDA_OCUPADA,
        service.estado(sessao, LocalDateTime.of(2026, 9, 8, 19, 0), SEIS_HORAS));
  }

  // HU-03, teste 07; HU-02, teste 04
  @Test
  void aoAtingirADuracaoMaximaFicaExcedidaEAberta() {
    SessaoRecarga sessao = sessaoComPrevisao(INICIO, INICIO.plus(horas(4, 15)));

    assertEquals(EstadoSessao.EXCEDIDA, service.estado(sessao, INICIO.plus(SEIS_HORAS), SEIS_HORAS));
    assertEquals(SituacaoSessao.EM_ANDAMENTO, sessao.getSituacao());
  }

  @Test
  void aDuracaoMaximaEParametro() {
    SessaoRecarga sessao = sessaoComPrevisao(INICIO, INICIO.plus(horas(4, 15)));

    assertEquals(EstadoSessao.CONCLUIDA_AINDA_OCUPADA,
        service.estado(sessao, INICIO.plus(SEIS_HORAS), Duration.ofHours(12)));
  }

  @Test
  void sessaoEncerradaNaoEExcedida() {
    SessaoRecarga sessao = sessaoComPrevisao(INICIO, INICIO.plus(horas(4, 15)));
    sessao.setSituacao(SituacaoSessao.ENCERRADA);

    assertEquals(EstadoSessao.ENCERRADA, service.estado(sessao, INICIO.plus(SEIS_HORAS), SEIS_HORAS));
  }
}
