package br.edu.ifsp.carregaja.control;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import br.edu.ifsp.carregaja.service.HistoricoService;
import br.edu.ifsp.carregaja.service.RegraNegocioException;
import br.edu.ifsp.carregaja.view.HistoricoResponse;
import br.edu.ifsp.carregaja.view.HistoricoResponse.ConsumoApartamento;
import br.edu.ifsp.carregaja.view.HistoricoResponse.FaixaHoraria;
import br.edu.ifsp.carregaja.view.HistoricoResponse.OcupacaoVaga;
import br.edu.ifsp.carregaja.view.HistoricoResponse.RecusaPorVaga;

@WebMvcTest(HistoricoController.class)
class HistoricoControllerTest extends ControleTesteBase {

  private static final LocalDate DE = LocalDate.of(2026, 9, 1);
  private static final LocalDate ATE = LocalDate.of(2026, 9, 30);

  @MockBean
  private HistoricoService historicoService;

  private static HistoricoResponse setembro() {
    return new HistoricoResponse(DE, ATE, false, null, 19, 7,
        List.of(new OcupacaoVaga("G-01", 4.5, 0.6)),
        List.of(new FaixaHoraria(19, 180, 4)),
        List.of(new ConsumoApartamento("17", 81.0, new BigDecimal("74.52"), 2)),
        List.of(new RecusaPorVaga("G-01", 5)));
  }

  @Test
  void sindicoConsultaOHistoricoDoPeriodo() throws Exception {
    when(historicoService.consultar(DE, ATE, null, null)).thenReturn(setembro());

    mvc.perform(get("/historico?de=2026-09-01&ate=2026-09-30").with(logado(sindico())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.semRegistros").value(false))
        .andExpect(jsonPath("$.faixaPico").value(19))
        .andExpect(jsonPath("$.totalRecusas").value(7))
        .andExpect(jsonPath("$.ocupacaoPorVaga[0].vaga").value("G-01"))
        .andExpect(jsonPath("$.ocupacaoPorVaga[0].horasOcupadas").value(4.5))
        .andExpect(jsonPath("$.faixasHorarias[0].hora").value(19))
        .andExpect(jsonPath("$.consumoPorApartamento[0].apartamento").value("17"))
        .andExpect(jsonPath("$.consumoPorApartamento[0].energiaKwh").value(81.0))
        .andExpect(jsonPath("$.recusasPorVaga[0].recusas").value(5));
  }

  @Test
  void repassaOsFiltrosDeVagaEApartamento() throws Exception {
    when(historicoService.consultar(any(), any(), eq(2L), eq(170L))).thenReturn(setembro());

    mvc.perform(get("/historico?de=2026-09-01&ate=2026-09-30&vagaId=2&apartamentoId=170").with(logado(sindico())))
        .andExpect(status().isOk());
    verify(historicoService).consultar(DE, ATE, 2L, 170L);
  }

  // HU-12, teste 06
  @Test
  void periodoSemRegistrosDevolve200ComAMensagem() throws Exception {
    when(historicoService.consultar(any(), any(), any(), any())).thenReturn(new HistoricoResponse(
        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), true, "Não há registros de utilização no período.", null,
        0, List.of(), List.of(), List.of(), List.of()));

    mvc.perform(get("/historico?de=2026-07-01&ate=2026-07-31").with(logado(sindico())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.semRegistros").value(true))
        .andExpect(jsonPath("$.mensagem").value("Não há registros de utilização no período."))
        .andExpect(jsonPath("$.ocupacaoPorVaga").isEmpty());
  }

  @Test
  void moradorNaoConsultaOHistorico() throws Exception {
    mvc.perform(get("/historico?de=2026-09-01&ate=2026-09-30").with(logado(morador())))
        .andExpect(status().isForbidden());
    verify(historicoService, never()).consultar(any(), any(), any(), any());
  }

  @Test
  void semTokenDevolve401() throws Exception {
    mvc.perform(get("/historico?de=2026-09-01&ate=2026-09-30")).andExpect(status().isUnauthorized());
  }

  @Test
  void dataForaDoFormatoDevolve400ComMensagem() throws Exception {
    mvc.perform(get("/historico?de=01/09/2026&ate=2026-09-30").with(logado(sindico())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensagem").value("O parâmetro 'de' é inválido."));
    verify(historicoService, never()).consultar(any(), any(), any(), any());
  }

  @Test
  void recusaDoServicoViraMensagem() throws Exception {
    when(historicoService.consultar(any(), any(), any(), any())).thenThrow(
        new RegraNegocioException(HttpStatus.BAD_REQUEST, "Informe o período: as datas inicial e final."));

    mvc.perform(get("/historico").with(logado(sindico())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensagem").value("Informe o período: as datas inicial e final."));
  }
}
