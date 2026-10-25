package br.edu.ifsp.carregaja.manterveiculo.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifsp.carregaja.manterveiculo.model.Veiculo;

public interface VeiculoDAO extends JpaRepository<Veiculo, Long> {

}
