package br.edu.ifsp.carregaja.control;

import java.net.URI;
import java.util.List;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.service.FechamentoMensalService;
import br.edu.ifsp.carregaja.view.FechamentoMensalRequest;
import br.edu.ifsp.carregaja.view.FechamentoMensalResponse;

@RestController
@RequestMapping(path = "/fechamentos")
public class FechamentoMensalController {

  private final FechamentoMensalService fechamentoService;

  public FechamentoMensalController(FechamentoMensalService fechamentoService) {
    this.fechamentoService = fechamentoService;
  }

  @PostMapping
  public ResponseEntity<FechamentoMensalResponse> fechar(@AuthenticationPrincipal Usuario usuario,
      @Valid @RequestBody FechamentoMensalRequest requisicao, UriComponentsBuilder uriBuilder) {
    FechamentoMensalResponse fechamento = fechamentoService.fechar(usuario, requisicao);
    URI uri = uriBuilder.path("/fechamentos/{id}").buildAndExpand(fechamento.getId()).toUri();
    return ResponseEntity.created(uri).body(fechamento);
  }

  @GetMapping
  public List<FechamentoMensalResponse> listar() {
    return fechamentoService.listar();
  }

  @GetMapping(path = "/{id}")
  public ResponseEntity<FechamentoMensalResponse> buscarPorId(@PathVariable("id") Long id) {
    return ResponseEntity.of(fechamentoService.buscarPorId(id));
  }
}
