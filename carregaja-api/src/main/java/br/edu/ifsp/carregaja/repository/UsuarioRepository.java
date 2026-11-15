package br.edu.ifsp.carregaja.repository;

import java.util.Optional;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;

import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.Usuario;

public interface UsuarioRepository extends CrudRepository<Usuario, Long> {

  Optional<Usuario> findByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Usuario> findComTravaById(Long id);

  boolean existsByPerfisContaining(Perfil perfil);

  long countByPerfisContainingAndAtivoTrue(Perfil perfil);
}
