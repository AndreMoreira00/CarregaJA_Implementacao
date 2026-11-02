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

import br.edu.ifsp.carregaja.model.Vaga;
import br.edu.ifsp.carregaja.service.VagaService;
import br.edu.ifsp.carregaja.view.VagaRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path = "/vagas")
public class VagaController {

  private final VagaService vagaService;

  public VagaController(VagaService vagaService) {
    this.vagaService = vagaService;
  }

  @GetMapping
  public Iterable<Vaga> listarTodas() {
    log.info("GET /vagas - listando as vagas");
    return vagaService.listarTodas();
  }

  @GetMapping(path = "/{id}")
  public ResponseEntity<Vaga> buscarPorId(@PathVariable("id") Long id) {
    log.info("GET /vagas/{} - buscando vaga", id);
    return ResponseEntity.of(vagaService.buscarPorId(id));
  }

  @PostMapping
  public ResponseEntity<Vaga> criar(@Valid @RequestBody VagaRequest requisicao, UriComponentsBuilder uriBuilder) {
    log.info("POST /vagas - cadastrando vaga '{}'", requisicao.getIdentificacao());
    Vaga vaga = vagaService.criar(requisicao);
    URI uri = uriBuilder.path("/vagas/{id}").buildAndExpand(vaga.getId()).toUri();
    log.info("POST /vagas - vaga cadastrada com id {} -> 201", vaga.getId());
    return ResponseEntity.created(uri).body(vaga);
  }

  @PutMapping(path = "/{id}")
  public ResponseEntity<Vaga> atualizar(@PathVariable("id") Long id, @Valid @RequestBody VagaRequest requisicao) {
    log.info("PUT /vagas/{} - alterando vaga", id);
    return ResponseEntity.of(vagaService.atualizar(id, requisicao));
  }

  @PostMapping(path = "/{id}/desativar")
  public ResponseEntity<Vaga> desativar(@PathVariable("id") Long id) {
    log.info("POST /vagas/{}/desativar", id);
    return ResponseEntity.of(vagaService.desativar(id));
  }

  @PostMapping(path = "/{id}/reativar")
  public ResponseEntity<Vaga> reativar(@PathVariable("id") Long id) {
    log.info("POST /vagas/{}/reativar", id);
    return ResponseEntity.of(vagaService.reativar(id));
  }

  @DeleteMapping(path = "/{id}")
  public ResponseEntity<Void> remover(@PathVariable("id") Long id) {
    log.info("DELETE /vagas/{} - removendo vaga", id);
    return vagaService.remover(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }
}
