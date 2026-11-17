package br.edu.ifsp.carregaja.repository.definirtarifa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import br.edu.ifsp.carregaja.model.compartilhado.Tarifa;

public interface TarifaRepository extends CrudRepository<Tarifa, Long> {

  List<Tarifa> findAllByOrderByInicioVigenciaDescIdDesc();

  Optional<Tarifa> findFirstByInicioVigenciaLessThanEqualOrderByInicioVigenciaDescIdDesc(LocalDateTime momento);
}
