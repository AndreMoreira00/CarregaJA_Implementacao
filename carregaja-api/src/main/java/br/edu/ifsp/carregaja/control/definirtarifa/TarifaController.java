package br.edu.ifsp.carregaja.control.definirtarifa;

import java.net.URI;
import java.util.List;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import br.edu.ifsp.carregaja.model.compartilhado.Tarifa;
import br.edu.ifsp.carregaja.model.compartilhado.Usuario;
import br.edu.ifsp.carregaja.service.definirtarifa.TarifaService;
import br.edu.ifsp.carregaja.view.definirtarifa.TarifaRequest;


@RestController
@RequestMapping(path = "/tarifas")
public class TarifaController {

  private final TarifaService tarifaService;

  public TarifaController(TarifaService tarifaService) {
    this.tarifaService = tarifaService;
  }

  @PostMapping
  public ResponseEntity<Tarifa> criar(@AuthenticationPrincipal Usuario usuario,
      @Valid @RequestBody TarifaRequest requisicao, UriComponentsBuilder uriBuilder) {
    Tarifa tarifa = tarifaService.criar(usuario, requisicao);
    URI uri = uriBuilder.path("/tarifas/{id}").buildAndExpand(tarifa.getId()).toUri();
    return ResponseEntity.created(uri).body(tarifa);
  }

  @GetMapping
  public List<Tarifa> listarTodas() {
    return tarifaService.listarTodas();
  }

  @GetMapping(path = "/vigente")
  public ResponseEntity<Tarifa> buscarVigente() {
    return ResponseEntity.of(tarifaService.buscarVigente());
  }
}
