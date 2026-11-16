package br.edu.ifsp.carregaja.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.SituacaoSessao;

public interface SessaoRecargaRepository extends CrudRepository<SessaoRecarga, Long> {

  Optional<SessaoRecarga> findByVagaIdAndSituacao(Long vagaId, SituacaoSessao situacao);

  Optional<SessaoRecarga> findByUsuarioIdAndSituacao(Long usuarioId, SituacaoSessao situacao);

  // UC02: todas as sessões abertas de uma vez, para montar o painel
  Iterable<SessaoRecarga> findBySituacao(SituacaoSessao situacao);

  boolean existsByVagaId(Long vagaId);

  boolean existsByUsuarioId(Long usuarioId);

  List<SessaoRecarga> findByInicioGreaterThanEqualAndInicioLessThan(LocalDateTime de, LocalDateTime ate);

  List<SessaoRecarga> findByUsuarioApartamentoIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(
      Long apartamentoId, LocalDateTime de, LocalDateTime ate);
}
