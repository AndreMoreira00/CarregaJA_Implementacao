package br.edu.ifsp.carregaja.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Tarifa {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, precision = 10, scale = 4)
  private BigDecimal valorKwh;

  @Column(nullable = false)
  private LocalDateTime inicioVigencia;

  @JsonIgnore
  @ManyToOne(optional = false)
  private Usuario responsavel;

  public String getNomeResponsavel() {
    return responsavel.getNome();
  }
}
