package br.edu.ifsp.carregaja.repository.fecharmes;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import br.edu.ifsp.carregaja.model.fecharmes.ItemRateio;

public interface ItemRateioRepository extends CrudRepository<ItemRateio, Long> {

  List<ItemRateio> findByFechamentoMensalId(Long fechamentoMensalId);
}
