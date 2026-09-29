package br.edu.ifsp.carregaja.cadastro.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.edu.ifsp.carregaja.cadastro.dao.VeiculoDAO;
import br.edu.ifsp.carregaja.cadastro.model.Veiculo;

@Service
public class VeiculoService {

    @Autowired
    private VeiculoDAO veiculoDAO;

    public Veiculo buscar() {
        List<Veiculo> veiculos = veiculoDAO.findAll();
        if (veiculos.isEmpty()) {
            return null;
        }
        return veiculos.get(0);
    }

    public void salvar(String modelo, Double capacidadeBateria, Double potenciaMaxima) {
        if (capacidadeBateria == null || capacidadeBateria <= 0) {
            throw new IllegalArgumentException("A capacidade da bateria deve ser maior que zero.");
        }
        if (potenciaMaxima == null || potenciaMaxima <= 0) {
            throw new IllegalArgumentException("A potência máxima deve ser maior que zero.");
        }

        Veiculo veiculo = buscar();
        if (veiculo == null) {
            veiculo = new Veiculo();
        }
        veiculo.setModelo(modelo);
        veiculo.setCapacidadeBateria(capacidadeBateria);
        veiculo.setPotenciaMaxima(potenciaMaxima);
        veiculoDAO.save(veiculo);
    }
}
