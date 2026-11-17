package br.edu.ifsp.carregaja.control.mantervagas;

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

import br.edu.ifsp.carregaja.model.compartilhado.Vaga;
import br.edu.ifsp.carregaja.service.mantervagas.VagaService;
import br.edu.ifsp.carregaja.view.mantervagas.VagaRequest;

@RestController
@RequestMapping(path = "/vagas")
public class VagaController {

  private final VagaService vagaService;

  public VagaController(VagaService vagaService) {
    this.vagaService = vagaService;
  }

  @GetMapping
  public Iterable<Vaga> listarTodas() {
    return vagaService.listarTodas();
  }

  @GetMapping(path = "/{id}")
  public ResponseEntity<Vaga> buscarPorId(@PathVariable("id") Long id) {
    return ResponseEntity.of(vagaService.buscarPorId(id));
  }

  @PostMapping
  public ResponseEntity<Vaga> criar(@Valid @RequestBody VagaRequest requisicao, UriComponentsBuilder uriBuilder) {
    Vaga vaga = vagaService.criar(requisicao);
    URI uri = uriBuilder.path("/vagas/{id}").buildAndExpand(vaga.getId()).toUri();
    return ResponseEntity.created(uri).body(vaga);
  }

  @PutMapping(path = "/{id}")
  public ResponseEntity<Vaga> atualizar(@PathVariable("id") Long id, @Valid @RequestBody VagaRequest requisicao) {
    return ResponseEntity.of(vagaService.atualizar(id, requisicao));
  }

  @PostMapping(path = "/{id}/desativar")
  public ResponseEntity<Vaga> desativar(@PathVariable("id") Long id) {
    return ResponseEntity.of(vagaService.desativar(id));
  }

  @PostMapping(path = "/{id}/reativar")
  public ResponseEntity<Vaga> reativar(@PathVariable("id") Long id) {
    return ResponseEntity.of(vagaService.reativar(id));
  }

  @DeleteMapping(path = "/{id}")
  public ResponseEntity<Void> remover(@PathVariable("id") Long id) {
    return vagaService.remover(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }
}
