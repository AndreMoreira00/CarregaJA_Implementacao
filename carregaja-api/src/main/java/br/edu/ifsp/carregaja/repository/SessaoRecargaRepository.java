package br.edu.ifsp.carregaja.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.SituacaoSessao;

public interface SessaoRecargaRepository extends CrudRepository<SessaoRecarga, Long> {

  Optional<SessaoRecarga> findByVagaIdAndSituacao(Long vagaId, SituacaoSessao situacao);

  Optional<SessaoRecarga> findByUsuarioIdAndSituacao(Long usuarioId, SituacaoSessao situacao);

  // UC02: todas as sessões abertas de uma vez, para montar o painel
  Iterable<SessaoRecarga> findBySituacao(SituacaoSessao situacao);

  // Encerramento: o morador e o síndico encerrando a mesma sessão ao mesmo tempo passam um de cada vez. A sessão
  // tem de ser lida já com a trava: lida antes sem ela, o Hibernate devolveria a cópia antiga depois da espera
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<SessaoRecarga> findComTravaById(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<SessaoRecarga> findComTravaByUsuarioIdAndSituacao(Long usuarioId, SituacaoSessao situacao);

  boolean existsByVagaId(Long vagaId);

  boolean existsByUsuarioId(Long usuarioId);

  List<SessaoRecarga> findByInicioGreaterThanEqualAndInicioLessThan(LocalDateTime de, LocalDateTime ate);

  List<SessaoRecarga> findByUsuarioApartamentoIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(
      Long apartamentoId, LocalDateTime de, LocalDateTime ate);

  // Sessões que ocuparam alguma vaga dentro do período, inclusive as que começaram antes ou ainda estão abertas (UC12)
  @Query("select s from SessaoRecarga s join fetch s.vaga join fetch s.usuario u left join fetch u.apartamento "
      + "where s.inicio < :fim and (s.fim is null or s.fim > :inicio)")
  List<SessaoRecarga> buscarQueTocamOPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
