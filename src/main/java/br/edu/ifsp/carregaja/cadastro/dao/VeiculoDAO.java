package br.edu.ifsp.carregaja.cadastro.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifsp.carregaja.cadastro.model.Veiculo;

public interface VeiculoDAO extends JpaRepository<Veiculo, Long> {

}
