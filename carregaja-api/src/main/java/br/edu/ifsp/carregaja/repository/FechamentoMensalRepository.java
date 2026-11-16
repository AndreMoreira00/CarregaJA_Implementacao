package br.edu.ifsp.carregaja.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import br.edu.ifsp.carregaja.model.FechamentoMensal;

public interface FechamentoMensalRepository extends CrudRepository<FechamentoMensal, Long> {

  boolean existsByAnoAndMes(int ano, int mes);

  List<FechamentoMensal> findAllByOrderByAnoDescMesDesc();
}
