package br.edu.ifsp.carregaja.model.fecharmes;

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

import br.edu.ifsp.carregaja.model.compartilhado.Usuario;

@Entity
@Getter
@Setter
public class FechamentoMensal {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private int ano;

  private int mes;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal valorTotal;

  @Column(nullable = false)
  private LocalDateTime momento;

  @JsonIgnore
  @ManyToOne(optional = false)
  private Usuario responsavel;

  public String getNomeResponsavel() {
    return responsavel.getNome();
  }
}
