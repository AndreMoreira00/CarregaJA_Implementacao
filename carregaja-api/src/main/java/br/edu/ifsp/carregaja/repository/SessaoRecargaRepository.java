package br.edu.ifsp.carregaja.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.SituacaoSessao;

public interface SessaoRecargaRepository extends CrudRepository<SessaoRecarga, Long> {

  Optional<SessaoRecarga> findByVagaIdAndSituacao(Long vagaId, SituacaoSessao situacao);

  Optional<SessaoRecarga> findByUsuarioIdAndSituacao(Long usuarioId, SituacaoSessao situacao);

  boolean existsByVagaId(Long vagaId);

  boolean existsByUsuarioId(Long usuarioId);
}
