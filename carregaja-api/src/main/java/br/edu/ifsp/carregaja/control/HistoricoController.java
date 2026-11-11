package br.edu.ifsp.carregaja.control;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifsp.carregaja.service.HistoricoService;
import br.edu.ifsp.carregaja.view.HistoricoResponse;

@RestController
@RequestMapping(path = "/historico")
public class HistoricoController {

  private final HistoricoService historicoService;

  public HistoricoController(HistoricoService historicoService) {
    this.historicoService = historicoService;
  }

  @GetMapping
  public HistoricoResponse consultar(
      @RequestParam(name = "de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
      @RequestParam(name = "ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
      @RequestParam(name = "vagaId", required = false) Long vagaId,
      @RequestParam(name = "apartamentoId", required = false) Long apartamentoId) {
    return historicoService.consultar(de, ate, vagaId, apartamentoId);
  }
}
