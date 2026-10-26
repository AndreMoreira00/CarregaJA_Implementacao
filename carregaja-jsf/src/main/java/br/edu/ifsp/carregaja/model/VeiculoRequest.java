package br.edu.ifsp.carregaja.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VeiculoRequest {

	private String modelo;
	private Double capacidadeBateria;
	private Double potenciaMaxima;

	public static VeiculoRequest de(Veiculo veiculo) {
		VeiculoRequest requisicao = new VeiculoRequest();
		requisicao.setModelo(veiculo.getModelo());
		requisicao.setCapacidadeBateria(veiculo.getCapacidadeBateria());
		requisicao.setPotenciaMaxima(veiculo.getPotenciaMaxima());
		return requisicao;
	}
}
