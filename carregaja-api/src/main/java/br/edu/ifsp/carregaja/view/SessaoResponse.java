package br.edu.ifsp.carregaja.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.ifsp.carregaja.model.EstadoSessao;
import br.edu.ifsp.carregaja.model.SessaoRecarga;

import lombok.Getter;

/** Não leva quem está na vaga: o morador não vê a identificação alheia (RF-03). */
@Getter
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
  }
}
