package br.edu.ifsp.carregaja.control;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import br.edu.ifsp.carregaja.model.Tarifa;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.service.TarifaService;
import br.edu.ifsp.carregaja.view.TarifaRequest;

@WebMvcTest(TarifaController.class)
class TarifaControllerTest extends ControleTesteBase {

  @MockBean
  private TarifaService tarifaService;

  private static Tarifa tarifa() {
    Tarifa tarifa = new Tarifa();
    tarifa.setId(1L);
    tarifa.setValorKwh(new BigDecimal("0.92"));
    tarifa.setInicioVigencia(LocalDateTime.of(2026, 9, 1, 8, 0));
    tarifa.setResponsavel(sindico());
    return tarifa;
  }

  // HU-08, teste 01
  @Test
  void definicaoValidaDevolve201ComAVigencia() throws Exception {
    when(tarifaService.criar(any(Usuario.class), any(TarifaRequest.class))).thenReturn(tarifa());

    mvc.perform(post("/tarifas").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content("{\"valorKwh\":0.92}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.valorKwh").value(0.92))
        .andExpect(jsonPath("$.inicioVigencia").value("2026-09-01T08:00:00"))
        .andExpect(jsonPath("$.nomeResponsavel").value("Usuário 1"))
        .andExpect(jsonPath("$.responsavel").doesNotExist());
  }

  // HU-08, teste 02
  @Test
  void valorZeroDevolve400() throws Exception {
    mvc.perform(post("/tarifas").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content("{\"valorKwh\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensagem").value("O valor do kWh deve ser maior que zero."));
    verify(tarifaService, never()).criar(any(), any());
  }

  @Test
  void valorNegativoOuAusenteDevolve400() throws Exception {
    mvc.perform(post("/tarifas").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content("{\"valorKwh\":-1}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/tarifas").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
    verify(tarifaService, never()).criar(any(), any());
  }

  // RN-10: mesmo que o corpo traga a data, ela é ignorada
  @Test
  void dataNoCorpoNaoDefineAVigencia() throws Exception {
    when(tarifaService.criar(any(Usuario.class), any(TarifaRequest.class))).thenReturn(tarifa());

    mvc.perform(post("/tarifas").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON)
        .content("{\"valorKwh\":0.92,\"inicioVigencia\":\"2020-01-01T00:00:00\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.inicioVigencia").value("2026-09-01T08:00:00"));
  }

  @Test
  void moradorNaoDefineTarifaNemVeOHistorico() throws Exception {
    mvc.perform(post("/tarifas").with(logado(morador())).contentType(MediaType.APPLICATION_JSON).content("{\"valorKwh\":0.92}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tarifas").with(logado(morador()))).andExpect(status().isForbidden());
  }

  // HU-08, teste 03
  @Test
  void sindicoConsultaAsTarifasEMoradorAVigente() throws Exception {
    when(tarifaService.listarTodas()).thenReturn(List.of(tarifa()));
    when(tarifaService.buscarVigente()).thenReturn(Optional.of(tarifa()));

    mvc.perform(get("/tarifas").with(logado(sindico())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].valorKwh").value(0.92));
    mvc.perform(get("/tarifas/vigente").with(logado(morador())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valorKwh").value(0.92));
  }

  @Test
  void semTarifaVigenteDevolve404() throws Exception {
    when(tarifaService.buscarVigente()).thenReturn(Optional.empty());

    mvc.perform(get("/tarifas/vigente").with(logado(morador()))).andExpect(status().isNotFound());
  }
}
