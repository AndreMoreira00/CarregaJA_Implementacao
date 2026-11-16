package br.edu.ifsp.carregaja.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import br.edu.ifsp.carregaja.model.EstadoSessao;
import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.SituacaoSessao;

/** As regras RN-01 a RN-05, sem banco nem tela, para serem testadas isoladamente (RI-08). */
@Service
public class ApuracaoEnergiaService {

  private static final double MINUTOS_POR_HORA = 60.0;
  private static final int CASAS_DA_ENERGIA = 3;
  private static final int CASAS_DO_VALOR = 2;

  /** RN-01: um carregador de 22 kW não recarrega mais rápido um veículo que aceita no máximo 6,6 kW. */
  public double potenciaEfetiva(double potenciaCarregador, double potenciaMaximaVeiculo) {
    return Math.min(potenciaCarregador, potenciaMaximaVeiculo);
  }

  /** RN-02: capacidade da bateria × (100% − nível informado). */
  public double energiaARepor(double capacidadeBateria, int nivelBateria) {
    return arredondarEnergia(capacidadeBateria * (100 - nivelBateria) / 100.0);
  }

  /** RN-02: energia a repor ÷ potência efetiva, no minuto mais próximo. */
  public Duration tempoEstimado(double energiaARepor, double potenciaEfetiva) {
    return Duration.ofMinutes(Math.round(energiaARepor / potenciaEfetiva * MINUTOS_POR_HORA));
  }

  /** RN-03: min(potência efetiva × duração, energia a repor), contando minutos inteiros. */
  public double energiaEstimada(double potenciaEfetiva, LocalDateTime inicio, LocalDateTime fim, double energiaARepor) {
    long minutos = ChronoUnit.MINUTES.between(inicio.truncatedTo(ChronoUnit.MINUTES), fim.truncatedTo(ChronoUnit.MINUTES));
    return arredondarEnergia(Math.min(potenciaEfetiva * minutos / MINUTOS_POR_HORA, energiaARepor));
  }

  /** RN-03: energia estimada × tarifa vigente no início da sessão. */
  public BigDecimal valor(double energiaEstimada, BigDecimal tarifa) {
    return BigDecimal.valueOf(energiaEstimada).multiply(tarifa).setScale(CASAS_DO_VALOR, RoundingMode.HALF_UP);
  }

  /** UC04: usa só o que a sessão gravou no início, então trocar o veículo ou a tarifa não muda o resultado (RN-04). */
  public void apurar(SessaoRecarga sessao, LocalDateTime fim) {
    double energia = energiaEstimada(sessao.getPotenciaEfetiva(), sessao.getInicio(), fim, sessao.getEnergiaARepor());
    sessao.setFim(fim);
    sessao.setEnergiaEstimada(energia);
    sessao.setValor(valor(energia, sessao.getTarifa().getValorKwh()));
  }

  /** RN-05: atingida a duração máxima a sessão é excedida, e continua aberta. */
  public EstadoSessao estado(SessaoRecarga sessao, LocalDateTime agora, Duration duracaoMaxima) {
    if (sessao.getSituacao() != SituacaoSessao.EM_ANDAMENTO) {
      return EstadoSessao.ENCERRADA;
    }
    if (!agora.isBefore(sessao.getInicio().plus(duracaoMaxima))) {
      return EstadoSessao.EXCEDIDA;
    }
    return agora.isAfter(sessao.getPrevisaoConclusao()) ? EstadoSessao.CONCLUIDA_AINDA_OCUPADA : EstadoSessao.EM_ANDAMENTO;
  }

  private double arredondarEnergia(double energia) {
    return BigDecimal.valueOf(energia).setScale(CASAS_DA_ENERGIA, RoundingMode.HALF_UP).doubleValue();
  }
}
