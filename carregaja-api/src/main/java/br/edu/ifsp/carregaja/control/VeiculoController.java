package br.edu.ifsp.carregaja.control;

import java.net.URI;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.model.Veiculo;
import br.edu.ifsp.carregaja.service.VeiculoService;
import br.edu.ifsp.carregaja.view.VeiculoRequest;

@RestController
@RequestMapping(path = "/veiculos")
public class VeiculoController {

  private final VeiculoService veiculoService;

  public VeiculoController(VeiculoService veiculoService) {
    this.veiculoService = veiculoService;
  }

  @PostMapping
  public ResponseEntity<Veiculo> criar(@AuthenticationPrincipal Usuario usuario,
      @Valid @RequestBody VeiculoRequest requisicao, UriComponentsBuilder uriBuilder) {
    Veiculo veiculo = veiculoService.criar(usuario, requisicao);
    URI uri = uriBuilder.path("/veiculos/{id}").buildAndExpand(veiculo.getId()).toUri();
    return ResponseEntity.created(uri).body(veiculo);
  }

  @GetMapping
  public Iterable<Veiculo> listarTodos() {
    return veiculoService.listarTodos();
  }

  @GetMapping(path = "/meu")
  public ResponseEntity<Veiculo> buscarMeu(@AuthenticationPrincipal Usuario usuario) {
    return ResponseEntity.of(veiculoService.buscarDoUsuario(usuario));
  }

  @GetMapping(path = "/{id}")
  public ResponseEntity<Veiculo> buscarPorId(@AuthenticationPrincipal Usuario usuario, @PathVariable("id") Long id) {
    return ResponseEntity.of(veiculoService.buscarPorId(usuario, id));
  }

  @PutMapping(path = "/{id}")
  public ResponseEntity<Veiculo> atualizar(@AuthenticationPrincipal Usuario usuario, @PathVariable("id") Long id,
      @Valid @RequestBody VeiculoRequest requisicao) {
    return ResponseEntity.of(veiculoService.atualizar(usuario, id, requisicao));
  }
}
