package br.edu.ifsp.carregaja.control;

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

import br.edu.ifsp.carregaja.model.Tarifa;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.service.TarifaService;
import br.edu.ifsp.carregaja.view.TarifaRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    log.info("POST /tarifas - usuário {} cadastrando R$ {}/kWh", usuario.getId(), requisicao.getValorKwh());
    Tarifa tarifa = tarifaService.criar(usuario, requisicao);
    URI uri = uriBuilder.path("/tarifas/{id}").buildAndExpand(tarifa.getId()).toUri();
    return ResponseEntity.created(uri).body(tarifa);
  }

  @GetMapping
  public List<Tarifa> listarTodas() {
    log.info("GET /tarifas - listando as tarifas");
    return tarifaService.listarTodas();
  }

  @GetMapping(path = "/vigente")
  public ResponseEntity<Tarifa> buscarVigente() {
    log.info("GET /tarifas/vigente");
    return ResponseEntity.of(tarifaService.buscarVigente());
  }
}
