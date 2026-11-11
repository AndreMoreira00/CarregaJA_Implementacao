package br.edu.ifsp.carregaja.control;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.service.AutenticacaoService;
import br.edu.ifsp.carregaja.view.AutenticacaoResponse;
import br.edu.ifsp.carregaja.view.LoginRequest;
import br.edu.ifsp.carregaja.view.TrocarSenhaRequest;

@RestController
@RequestMapping(path = "/autenticacao")
public class AutenticacaoController {

  private final AutenticacaoService autenticacaoService;

  public AutenticacaoController(AutenticacaoService autenticacaoService) {
    this.autenticacaoService = autenticacaoService;
  }

  @PostMapping
  public AutenticacaoResponse autenticar(@Valid @RequestBody LoginRequest requisicao) {
    return autenticacaoService.autenticar(requisicao);
  }

  @PutMapping(path = "/senha")
  public ResponseEntity<Void> trocarSenha(@AuthenticationPrincipal Usuario usuario,
      @Valid @RequestBody TrocarSenhaRequest requisicao) {
    autenticacaoService.trocarSenha(usuario, requisicao);
    return ResponseEntity.noContent().build();
  }
}
