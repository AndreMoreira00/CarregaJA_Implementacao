package br.edu.ifsp.carregaja.control.consultarconsumo;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifsp.carregaja.model.compartilhado.Usuario;
import br.edu.ifsp.carregaja.service.consultarconsumo.ConsumoService;
import br.edu.ifsp.carregaja.view.consultarconsumo.ConsumoResponse;

@RestController
@RequestMapping(path = "/consumos")
public class ConsumoController {

  private final ConsumoService consumoService;

  public ConsumoController(ConsumoService consumoService) {
    this.consumoService = consumoService;
  }

  @GetMapping(path = "/meu")
  public ConsumoResponse consultarMeu(@AuthenticationPrincipal Usuario usuario,
      @RequestParam(name = "ano", required = false) Integer ano,
      @RequestParam(name = "mes", required = false) Integer mes) {
    return consumoService.consultarDoMes(usuario, ano, mes);
  }
}
