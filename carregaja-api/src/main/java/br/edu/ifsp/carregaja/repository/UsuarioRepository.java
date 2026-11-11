package br.edu.ifsp.carregaja.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.Usuario;

public interface UsuarioRepository extends CrudRepository<Usuario, Long> {

  Optional<Usuario> findByEmail(String email);

  boolean existsByPerfisContaining(Perfil perfil);
}
