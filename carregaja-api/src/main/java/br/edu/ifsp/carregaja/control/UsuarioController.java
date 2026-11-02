package br.edu.ifsp.carregaja.control;

import java.net.URI;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.service.UsuarioService;
import br.edu.ifsp.carregaja.view.UsuarioRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path = "/usuarios")
public class UsuarioController {

  private final UsuarioService usuarioService;

  public UsuarioController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  @GetMapping
  public Iterable<Usuario> listarTodos() {
    log.info("GET /usuarios - listando os usuários");
    return usuarioService.listarTodos();
  }

  @GetMapping(path = "/{id}")
  public ResponseEntity<Usuario> buscarPorId(@PathVariable("id") Long id) {
    log.info("GET /usuarios/{} - buscando usuário", id);
    return ResponseEntity.of(usuarioService.buscarPorId(id));
  }

  @PostMapping
  public ResponseEntity<Usuario> criar(@Valid @RequestBody UsuarioRequest requisicao, UriComponentsBuilder uriBuilder) {
    log.info("POST /usuarios - cadastrando usuário");
    Usuario usuario = usuarioService.criar(requisicao);
    URI uri = uriBuilder.path("/usuarios/{id}").buildAndExpand(usuario.getId()).toUri();
    log.info("POST /usuarios - usuário cadastrado com id {} -> 201", usuario.getId());
    return ResponseEntity.created(uri).body(usuario);
  }

  @PutMapping(path = "/{id}")
  public ResponseEntity<Usuario> atualizar(@PathVariable("id") Long id, @Valid @RequestBody UsuarioRequest requisicao) {
    log.info("PUT /usuarios/{} - alterando usuário", id);
    return ResponseEntity.of(usuarioService.atualizar(id, requisicao));
  }

  @PostMapping(path = "/{id}/desativar")
  public ResponseEntity<Usuario> desativar(@PathVariable("id") Long id) {
    log.info("POST /usuarios/{}/desativar", id);
    return ResponseEntity.of(usuarioService.desativar(id));
  }

  @PostMapping(path = "/{id}/reativar")
  public ResponseEntity<Usuario> reativar(@PathVariable("id") Long id) {
    log.info("POST /usuarios/{}/reativar", id);
    return ResponseEntity.of(usuarioService.reativar(id));
  }

  @DeleteMapping(path = "/{id}")
  public ResponseEntity<Void> remover(@PathVariable("id") Long id) {
    log.info("DELETE /usuarios/{} - removendo usuário", id);
    return usuarioService.remover(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }
}
