package br.edu.ifsp.carregaja.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import br.edu.ifsp.carregaja.model.Veiculo;

public interface VeiculoRepository extends CrudRepository<Veiculo, Long> {

  Optional<Veiculo> findByUsuarioId(Long usuarioId);

  boolean existsByUsuarioId(Long usuarioId);
}
