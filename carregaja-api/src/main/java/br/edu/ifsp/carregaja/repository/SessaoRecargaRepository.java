package br.edu.ifsp.carregaja.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.SituacaoSessao;

public interface SessaoRecargaRepository extends CrudRepository<SessaoRecarga, Long> {

  Optional<SessaoRecarga> findByVagaIdAndSituacao(Long vagaId, SituacaoSessao situacao);

  Optional<SessaoRecarga> findByUsuarioIdAndSituacao(Long usuarioId, SituacaoSessao situacao);

  boolean existsByVagaId(Long vagaId);

  boolean existsByUsuarioId(Long usuarioId);

  // Sessões que ocuparam alguma vaga dentro do período, inclusive as que começaram antes ou ainda estão abertas (UC12)
  @Query("select s from SessaoRecarga s join fetch s.vaga join fetch s.usuario u left join fetch u.apartamento "
      + "where s.inicio < :fim and (s.fim is null or s.fim > :inicio)")
  List<SessaoRecarga> buscarQueTocamOPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
