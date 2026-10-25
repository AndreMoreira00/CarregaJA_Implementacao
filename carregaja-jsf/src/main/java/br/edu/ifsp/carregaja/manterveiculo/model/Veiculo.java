package br.edu.ifsp.carregaja.manterveiculo.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Veiculo implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id;
	private String modelo;
	private Double capacidadeBateria;
	private Double potenciaMaxima;
}
