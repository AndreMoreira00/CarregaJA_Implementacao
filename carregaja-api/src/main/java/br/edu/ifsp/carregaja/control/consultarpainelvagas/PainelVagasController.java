package br.edu.ifsp.carregaja.control.consultarpainelvagas;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifsp.carregaja.model.compartilhado.Usuario;
import br.edu.ifsp.carregaja.service.consultarpainelvagas.PainelVagasService;
import br.edu.ifsp.carregaja.view.consultarpainelvagas.PainelVagasResponse;

@RestController
@RequestMapping(path = "/painel")
public class PainelVagasController {

  private final PainelVagasService painelService;

  public PainelVagasController(PainelVagasService painelService) {
    this.painelService = painelService;
  }

  @GetMapping
  public PainelVagasResponse consultar(@AuthenticationPrincipal Usuario usuario) {
    return painelService.consultar(usuario);
  }
}
