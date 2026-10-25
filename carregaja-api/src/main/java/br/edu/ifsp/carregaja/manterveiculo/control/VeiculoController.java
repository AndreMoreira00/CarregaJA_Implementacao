package br.edu.ifsp.carregaja.manterveiculo.control;

import java.net.URI;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import br.edu.ifsp.carregaja.manterveiculo.model.Veiculo;
import br.edu.ifsp.carregaja.manterveiculo.service.VeiculoService;
import br.edu.ifsp.carregaja.manterveiculo.view.VeiculoRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path = "/veiculos")
public class VeiculoController {

  private final VeiculoService veiculoService;

  public VeiculoController(VeiculoService veiculoService) {
    this.veiculoService = veiculoService;
  }

  @PostMapping
  public ResponseEntity<Veiculo> criar(@Valid @RequestBody VeiculoRequest requisicao, UriComponentsBuilder uriBuilder) {
    log.info("POST /veiculos - cadastrando veículo '{}'", requisicao.getModelo());
    Veiculo veiculo = veiculoService.criar(requisicao);
    URI uri = uriBuilder.path("/veiculos/{id}").buildAndExpand(veiculo.getId()).toUri();
    log.info("POST /veiculos - veículo cadastrado com id {} -> 201", veiculo.getId());
    return ResponseEntity.created(uri).body(veiculo);
  }

  @GetMapping
  public Iterable<Veiculo> listarTodos() {
    log.info("GET /veiculos - listando os veículos");
    return veiculoService.listarTodos();
  }

  @GetMapping(path = "/{id}")
  public ResponseEntity<Veiculo> buscarPorId(@PathVariable("id") Long id) {
    log.info("GET /veiculos/{} - buscando veículo", id);
    return veiculoService.buscarPorId(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> {
          log.warn("GET /veiculos/{} - veículo não encontrado -> 404", id);
          return ResponseEntity.notFound().build();
        });
  }

  @PutMapping(path = "/{id}")
  public ResponseEntity<Veiculo> atualizar(@PathVariable("id") Long id, @Valid @RequestBody VeiculoRequest requisicao) {
    log.info("PUT /veiculos/{} - alterando veículo", id);
    return veiculoService.atualizar(id, requisicao)
        .map(veiculo -> {
          log.info("PUT /veiculos/{} - veículo alterado para '{}' -> 200", id, veiculo.getModelo());
          return ResponseEntity.ok(veiculo);
        })
        .orElseGet(() -> {
          log.warn("PUT /veiculos/{} - veículo não encontrado, nada foi alterado -> 404", id);
          return ResponseEntity.notFound().build();
        });
  }
}
