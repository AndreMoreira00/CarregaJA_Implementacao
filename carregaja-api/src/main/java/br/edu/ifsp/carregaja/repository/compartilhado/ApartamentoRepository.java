package br.edu.ifsp.carregaja.repository.compartilhado;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import br.edu.ifsp.carregaja.model.compartilhado.Apartamento;

public interface ApartamentoRepository extends CrudRepository<Apartamento, Long> {

  Optional<Apartamento> findByNumero(String numero);
}
