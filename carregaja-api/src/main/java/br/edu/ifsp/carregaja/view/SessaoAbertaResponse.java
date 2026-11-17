package br.edu.ifsp.carregaja.view;

import java.time.LocalDateTime;

import br.edu.ifsp.carregaja.model.Apartamento;
import br.edu.ifsp.carregaja.model.EstadoSessao;
import br.edu.ifsp.carregaja.model.SessaoRecarga;

import lombok.Getter;

/** UC10: o que o síndico vê para decidir que sessão encerrar. Ele pode ver quem ocupa a vaga (RF-03). */
@Getter
public class SessaoAbertaResponse {
  private final Long id;
  private final String vaga;
  private final String apartamento;
  private final String morador;
  private final LocalDateTime inicio;
  private final LocalDateTime previsaoConclusao;
  private final EstadoSessao estado;

  public SessaoAbertaResponse(SessaoRecarga sessao, EstadoSessao estado) {
    Apartamento apartamento = sessao.getUsuario().getApartamento();
    this.id = sessao.getId();
    this.vaga = sessao.getVaga().getIdentificacao();
    this.apartamento = apartamento == null ? null : apartamento.getNumero();
    this.morador = sessao.getUsuario().getNome();
    this.inicio = sessao.getInicio();
    this.previsaoConclusao = sessao.getPrevisaoConclusao();
    this.estado = estado;
  }
}
