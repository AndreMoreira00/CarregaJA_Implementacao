package br.edu.ifsp.carregaja.repository;

import org.springframework.data.repository.CrudRepository;

import br.edu.ifsp.carregaja.model.RecusaVaga;

public interface RecusaVagaRepository extends CrudRepository<RecusaVaga, Long> {

  boolean existsByVagaId(Long vagaId);

  boolean existsByUsuarioId(Long usuarioId);
}
