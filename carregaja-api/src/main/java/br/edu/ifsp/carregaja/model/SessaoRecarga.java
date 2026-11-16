package br.edu.ifsp.carregaja.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class SessaoRecarga {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Usuario usuario;

  @ManyToOne(optional = false)
  private Vaga vaga;

  @ManyToOne(optional = false)
  private Veiculo veiculo;

  @ManyToOne(optional = false)
  private Tarifa tarifa;

  @Column(nullable = false)
  private LocalDateTime inicio;

  private LocalDateTime fim;

  @Column(nullable = false)
  private Integer nivelBateriaInicial;

  // Fixados no início: alterar o veículo ou a vaga depois não muda a sessão (RN-04)
  @Column(nullable = false)
  private Double potenciaEfetiva;

  @Column(nullable = false)
  private Double energiaARepor;

  @Column(nullable = false)
  private LocalDateTime previsaoConclusao;

  private Double energiaEstimada;

  @Column(precision = 10, scale = 2)
  private BigDecimal valor;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private SituacaoSessao situacao = SituacaoSessao.EM_ANDAMENTO;
}
