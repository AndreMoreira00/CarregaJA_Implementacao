package br.edu.ifsp.carregaja.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import br.edu.ifsp.carregaja.model.RecusaVaga;

public interface RecusaVagaRepository extends CrudRepository<RecusaVaga, Long> {

  boolean existsByVagaId(Long vagaId);

  boolean existsByUsuarioId(Long usuarioId);

  @Query("select r from RecusaVaga r join fetch r.vaga join fetch r.usuario u left join fetch u.apartamento "
      + "where r.momento >= :inicio and r.momento < :fim")
  List<RecusaVaga> buscarDoPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
