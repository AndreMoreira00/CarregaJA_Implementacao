package br.edu.ifsp.carregaja.view.mantersessaorecarga;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

import br.edu.ifsp.carregaja.model.compartilhado.EstadoSessao;
import br.edu.ifsp.carregaja.model.compartilhado.SessaoRecarga;
import br.edu.ifsp.carregaja.model.compartilhado.SituacaoSessao;

/**
 * Não leva quem está na vaga: o morador não vê a identificação alheia (RF-03). O fim, a duração, a energia e o
 * valor só aparecem depois do encerramento (RF-07).
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessaoResponse {
  private final Long id;
  private final String vaga;
  private final String veiculo;
  private final Integer nivelBateriaInicial;
  private final Double potenciaEfetiva;
  private final Double energiaARepor;
  private final BigDecimal valorKwh;
  private final LocalDateTime inicio;
  private final LocalDateTime previsaoConclusao;
  private final EstadoSessao estado;
  private final SituacaoSessao situacao;
  private final LocalDateTime fim;
  private final String duracao;
  private final Double energiaEstimada;
  private final BigDecimal valor;
  private final String encerradaPor;

  public SessaoResponse(SessaoRecarga sessao, EstadoSessao estado) {
    this.id = sessao.getId();
    this.vaga = sessao.getVaga().getIdentificacao();
    this.veiculo = sessao.getVeiculo().getModelo();
    this.nivelBateriaInicial = sessao.getNivelBateriaInicial();
    this.potenciaEfetiva = sessao.getPotenciaEfetiva();
    this.energiaARepor = sessao.getEnergiaARepor();
    this.valorKwh = sessao.getTarifa().getValorKwh();
    this.inicio = sessao.getInicio();
    this.previsaoConclusao = sessao.getPrevisaoConclusao();
    this.estado = estado;
    this.situacao = sessao.getSituacao();
    this.fim = sessao.getFim();
    this.duracao = sessao.getFim() == null ? null : duracao(sessao.getInicio(), sessao.getFim());
    this.energiaEstimada = sessao.getEnergiaEstimada();
    this.valor = sessao.getValor();
    this.encerradaPor = sessao.getEncerradaPor() == null ? null : sessao.getEncerradaPor().getNome();
  }

  // Em minutos inteiros, como a apuração conta (RN-03): de 18h10 a 22h40 dá "4h30"
  private static String duracao(LocalDateTime inicio, LocalDateTime fim) {
    Duration duracao = Duration.between(inicio.truncatedTo(ChronoUnit.MINUTES), fim.truncatedTo(ChronoUnit.MINUTES));
    return String.format("%dh%02d", duracao.toHours(), duracao.toMinutesPart());
  }
}
