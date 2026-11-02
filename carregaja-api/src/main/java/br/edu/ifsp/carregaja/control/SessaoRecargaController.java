package br.edu.ifsp.carregaja.control;

import java.net.URI;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.service.SessaoRecargaService;
import br.edu.ifsp.carregaja.view.SessaoRequest;
import br.edu.ifsp.carregaja.view.SessaoResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    log.info("POST /sessoes - usuário {} iniciando na vaga {}", usuario.getId(), requisicao.getVagaId());
    SessaoResponse sessao = sessaoService.iniciar(usuario, requisicao);
    URI uri = uriBuilder.path("/sessoes/{id}").buildAndExpand(sessao.getId()).toUri();
    log.info("POST /sessoes - sessão {} aberta -> 201", sessao.getId());
    return ResponseEntity.created(uri).body(sessao);
  }

  @GetMapping(path = "/minha")
  public ResponseEntity<SessaoResponse> buscarMinha(@AuthenticationPrincipal Usuario usuario) {
    log.info("GET /sessoes/minha - sessão aberta do usuário {}", usuario.getId());
    return ResponseEntity.of(sessaoService.buscarAberta(usuario));
  }
}
