package br.edu.ifsp.carregaja.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Veiculo implements Serializable {
  private static final long serialVersionUID = 1L;
  private Long id;
  private String modelo;
  private Double capacidadeBateria;
  private Double potenciaMaxima;
}
