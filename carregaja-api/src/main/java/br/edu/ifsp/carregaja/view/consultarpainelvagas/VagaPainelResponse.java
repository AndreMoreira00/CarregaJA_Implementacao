package br.edu.ifsp.carregaja.view.consultarpainelvagas;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

import br.edu.ifsp.carregaja.model.compartilhado.Apartamento;
import br.edu.ifsp.carregaja.model.compartilhado.SessaoRecarga;
import br.edu.ifsp.carregaja.model.compartilhado.Vaga;
import br.edu.ifsp.carregaja.model.consultarpainelvagas.EstadoVaga;

/**
 * Uma vaga do painel. O que não se aplica não vai na resposta: a vaga livre não tem previsão, e o morador não
 * recebe o apartamento nem o nome de quem ocupa a vaga (RF-03).
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VagaPainelResponse {
  private final Long id;
  private final String identificacao;
  private final Double potenciaCarregador;
  private final EstadoVaga estado;
  private final LocalDateTime inicio;
  private final LocalDateTime previsaoLiberacao;
  private final String apartamento;
  private final String morador;

  private VagaPainelResponse(Vaga vaga, EstadoVaga estado, LocalDateTime inicio, LocalDateTime previsaoLiberacao,
      String apartamento, String morador) {
    this.id = vaga.getId();
    this.identificacao = vaga.getIdentificacao();
    this.potenciaCarregador = vaga.getPotenciaCarregador();
    this.estado = estado;
    this.inicio = inicio;
    this.previsaoLiberacao = previsaoLiberacao;
    this.apartamento = apartamento;
    this.morador = morador;
  }

  public static VagaPainelResponse livre(Vaga vaga) {
    return new VagaPainelResponse(vaga, EstadoVaga.LIVRE, null, null, null, null);
  }

  public static VagaPainelResponse ocupada(Vaga vaga, SessaoRecarga sessao, EstadoVaga estado, boolean identificar) {
    if (!identificar) {
      return new VagaPainelResponse(vaga, estado, sessao.getInicio(), sessao.getPrevisaoConclusao(), null, null);
    }
    Apartamento apartamento = sessao.getUsuario().getApartamento();
    return new VagaPainelResponse(vaga, estado, sessao.getInicio(), sessao.getPrevisaoConclusao(),
        apartamento == null ? null : apartamento.getNumero(), sessao.getUsuario().getNome());
  }
}
