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
import br.edu.ifsp.carregaja.service.SessaoRecargaService;
import br.edu.ifsp.carregaja.view.SessaoAbertaResponse;
import br.edu.ifsp.carregaja.view.SessaoRequest;
import br.edu.ifsp.carregaja.view.SessaoResponse;

@RestController
@RequestMapping(path = "/sessoes")
public class SessaoRecargaController {

  private final SessaoRecargaService sessaoService;

  public SessaoRecargaController(SessaoRecargaService sessaoService) {
    this.sessaoService = sessaoService;
  }

  @PostMapping
  public ResponseEntity<SessaoResponse> iniciar(@AuthenticationPrincipal Usuario usuario,
      @Valid @RequestBody SessaoRequest requisicao, UriComponentsBuilder uriBuilder) {
    SessaoResponse sessao = sessaoService.iniciar(usuario, requisicao);
    URI uri = uriBuilder.path("/sessoes/{id}").buildAndExpand(sessao.getId()).toUri();
    return ResponseEntity.created(uri).body(sessao);
  }

  @GetMapping(path = "/minha")
  public ResponseEntity<SessaoResponse> buscarMinha(@AuthenticationPrincipal Usuario usuario) {
    return ResponseEntity.of(sessaoService.buscarAberta(usuario));
  }

  @PostMapping(path = "/minha/encerrar")
  public ResponseEntity<SessaoResponse> encerrarMinha(@AuthenticationPrincipal Usuario usuario) {
    return ResponseEntity.ok(sessaoService.encerrar(usuario));
  }

  @GetMapping(path = "/abertas")
  public ResponseEntity<List<SessaoAbertaResponse>> listarAbertas() {
    return ResponseEntity.ok(sessaoService.listarAbertas());
  }

  @PostMapping(path = "/{id}/encerrar-administrativamente")
  public ResponseEntity<SessaoResponse> encerrarAdministrativamente(@AuthenticationPrincipal Usuario usuario,
      @PathVariable Long id) {
    return ResponseEntity.ok(sessaoService.encerrarAdministrativamente(id, usuario));
  }
}
