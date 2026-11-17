package br.edu.ifsp.carregaja.model.fecharmes;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Getter;
import lombok.Setter;

import br.edu.ifsp.carregaja.model.compartilhado.Apartamento;

@Entity
@Getter
@Setter
public class ItemRateio {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private double energia;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal valor;

  @ManyToOne(optional = false)
  private FechamentoMensal fechamentoMensal;

  @ManyToOne(optional = false)
  private Apartamento apartamento;
}
