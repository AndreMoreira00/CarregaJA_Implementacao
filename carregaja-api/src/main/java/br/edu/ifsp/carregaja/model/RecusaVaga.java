package br.edu.ifsp.carregaja.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Getter;
import lombok.Setter;

/** Tentativa recusada por vaga ocupada: é a demanda reprimida que o UC12 leva à assembleia (RF-06). */
@Entity
@Getter
@Setter
public class RecusaVaga {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Vaga vaga;

  @ManyToOne(optional = false)
  private Usuario usuario;

  @Column(nullable = false)
  private LocalDateTime momento;
}
