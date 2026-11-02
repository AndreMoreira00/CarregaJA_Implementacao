package br.edu.ifsp.carregaja.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import br.edu.ifsp.carregaja.model.Vaga;

public interface VagaRepository extends CrudRepository<Vaga, Long> {

  Optional<Vaga> findByIdentificacao(String identificacao);
}
