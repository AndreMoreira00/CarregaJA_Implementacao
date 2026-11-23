package br.edu.ifsp.carregaja.control.consultarconsumo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import br.edu.ifsp.carregaja.control.ControleTesteBase;
import br.edu.ifsp.carregaja.model.compartilhado.SituacaoSessao;
import br.edu.ifsp.carregaja.model.compartilhado.Usuario;
import br.edu.ifsp.carregaja.service.RegraNegocioException;
import br.edu.ifsp.carregaja.service.consultarconsumo.ConsumoService;
import br.edu.ifsp.carregaja.view.consultarconsumo.ConsumoResponse.SessaoDoMes;
import br.edu.ifsp.carregaja.view.consultarconsumo.ConsumoResponse;

@WebMvcTest(ConsumoController.class)
class ConsumoControllerTest extends ControleTesteBase {

  @MockBean
  private ConsumoService consumoService;

  private static ConsumoResponse setembro() {
    List<SessaoDoMes> sessoes = new ArrayList<>();
    sessoes.add(new SessaoDoMes("G-02", LocalDateTime.of(2026, 9, 3, 18, 10), LocalDateTime.of(2026, 9, 3, 23, 0),
        28.0, new BigDecimal("25.76"), SituacaoSessao.ENCERRADA));
    sessoes.add(new SessaoDoMes("G-02", LocalDateTime.of(2026, 9, 10, 18, 10), LocalDateTime.of(2026, 9, 10, 23, 0),
        48.0, new BigDecimal("44.16"), SituacaoSessao.ENCERRADA_ADMINISTRATIVAMENTE));
    return new ConsumoResponse("42", 2026, 9, false, null, 76.0, new BigDecimal("69.92"), sessoes);
  }

  // HU-06, teste 01
  @Test
  void consumoDoMesDevolve200ComOTotal() throws Exception {
    when(consumoService.consultarDoMes(any(Usuario.class), eq(2026), eq(9))).thenReturn(setembro());

    mvc.perform(get("/consumos/meu?ano=2026&mes=9").with(logado(morador())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apartamento").value("42"))
        .andExpect(jsonPath("$.semRegistros").value(false))
        .andExpect(jsonPath("$.energiaKwh").value(76.0))
        .andExpect(jsonPath("$.valor").value(69.92))
        .andExpect(jsonPath("$.sessoes[0].vaga").value("G-02"))
        .andExpect(jsonPath("$.sessoes[1].situacao").value("ENCERRADA_ADMINISTRATIVAMENTE"));
  }

  @Test
  void mesForaDe1a12Devolve400() throws Exception {
    when(consumoService.consultarDoMes(any(Usuario.class), eq(2026), eq(13)))
        .thenThrow(new RegraNegocioException(HttpStatus.BAD_REQUEST, "O mês deve estar entre 1 e 12."));

    mvc.perform(get("/consumos/meu?ano=2026&mes=13").with(logado(morador())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensagem").value("O mês deve estar entre 1 e 12."));
  }

  @Test
  void sindicoSemApartamentoNaoConsultaOConsumo() throws Exception {
    mvc.perform(get("/consumos/meu?ano=2026&mes=9").with(logado(sindico())))
        .andExpect(status().isForbidden());
    verify(consumoService, never()).consultarDoMes(any(), any(), any());
  }

  @Test
  void semTokenDevolve401() throws Exception {
    mvc.perform(get("/consumos/meu?ano=2026&mes=9")).andExpect(status().isUnauthorized());
    verify(consumoService, never()).consultarDoMes(any(), any(), any());
  }
}
