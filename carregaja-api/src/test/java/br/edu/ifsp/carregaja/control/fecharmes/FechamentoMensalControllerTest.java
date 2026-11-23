package br.edu.ifsp.carregaja.control.fecharmes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import br.edu.ifsp.carregaja.control.ControleTesteBase;
import br.edu.ifsp.carregaja.model.compartilhado.Usuario;
import br.edu.ifsp.carregaja.service.RegraNegocioException;
import br.edu.ifsp.carregaja.service.fecharmes.FechamentoMensalService;
import br.edu.ifsp.carregaja.view.fecharmes.FechamentoMensalRequest;
import br.edu.ifsp.carregaja.view.fecharmes.FechamentoMensalResponse.Item;
import br.edu.ifsp.carregaja.view.fecharmes.FechamentoMensalResponse;

@WebMvcTest(FechamentoMensalController.class)
class FechamentoMensalControllerTest extends ControleTesteBase {

  @MockBean
  private FechamentoMensalService fechamentoService;

  private static FechamentoMensalResponse setembro() {
    Item item = new Item("42", 76.0, new BigDecimal("69.92"));
    return new FechamentoMensalResponse(1L, 2026, 9, LocalDateTime.of(2026, 10, 1, 9, 0), "Usuário 1", 76.0,
        new BigDecimal("69.92"), List.of(item));
  }

  private static String json(Object ano, Object mes) {
    return "{\"ano\":" + ano + ",\"mes\":" + mes + "}";
  }

  @Test
  void fechamentoValidoDevolve201ComOEndereco() throws Exception {
    when(fechamentoService.fechar(any(Usuario.class), any(FechamentoMensalRequest.class))).thenReturn(setembro());

    mvc.perform(post("/fechamentos").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content(json(2026, 9)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/fechamentos/1"))
        .andExpect(jsonPath("$.mes").value(9))
        .andExpect(jsonPath("$.nomeResponsavel").value("Usuário 1"));
  }

  @Test
  void mesForaDe1a12Devolve400() throws Exception {
    mvc.perform(post("/fechamentos").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content(json(2026, 13)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensagem").value("O mês deve estar entre 1 e 12."));
    verify(fechamentoService, never()).fechar(any(), any());
  }

  @Test
  void anoAusenteDevolve400() throws Exception {
    mvc.perform(post("/fechamentos").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content(json(null, 9)))
        .andExpect(status().isBadRequest());
    verify(fechamentoService, never()).fechar(any(), any());
  }

  @Test
  void mesJaFechadoDevolve409() throws Exception {
    when(fechamentoService.fechar(any(Usuario.class), any(FechamentoMensalRequest.class)))
        .thenThrow(new RegraNegocioException(HttpStatus.CONFLICT, "Este mês já foi fechado."));

    mvc.perform(post("/fechamentos").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content(json(2026, 9)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensagem").value("Este mês já foi fechado."));
  }

  // HU-11, teste 03
  @Test
  void sessaoAbertaDevolve409ComAPendencia() throws Exception {
    String mensagem = "Há sessões em aberto no mês: vaga G-02, apto 42, início 30/09/2026 22:15.";
    when(fechamentoService.fechar(any(Usuario.class), any(FechamentoMensalRequest.class)))
        .thenThrow(new RegraNegocioException(HttpStatus.CONFLICT, mensagem));

    mvc.perform(post("/fechamentos").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content(json(2026, 9)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensagem").value(mensagem));
  }

  @Test
  void moradorNaoFechaOMes() throws Exception {
    mvc.perform(post("/fechamentos").with(logado(morador())).contentType(MediaType.APPLICATION_JSON).content(json(2026, 9)))
        .andExpect(status().isForbidden());
    verify(fechamentoService, never()).fechar(any(), any());
  }

  @Test
  void semTokenDevolve401() throws Exception {
    mvc.perform(post("/fechamentos").contentType(MediaType.APPLICATION_JSON).content(json(2026, 9)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void buscarFechamentoDevolveOsItens() throws Exception {
    when(fechamentoService.buscarPorId(1L)).thenReturn(Optional.of(setembro()));

    mvc.perform(get("/fechamentos/1").with(logado(sindico())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itens[0].apartamento").value("42"))
        .andExpect(jsonPath("$.itens[0].energia").value(76.0))
        .andExpect(jsonPath("$.itens[0].valor").value(69.92));
  }

  // HU-11, teste 05: o síndico acha o rateio de um mês já fechado, sem precisar do número do fechamento
  @Test
  void sindicoListaOsMesesFechadosComOsItens() throws Exception {
    when(fechamentoService.listar()).thenReturn(List.of(setembro()));

    mvc.perform(get("/fechamentos").with(logado(sindico())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ano").value(2026))
        .andExpect(jsonPath("$[0].mes").value(9))
        .andExpect(jsonPath("$[0].valorTotal").value(69.92))
        .andExpect(jsonPath("$[0].itens[0].apartamento").value("42"));
  }

  @Test
  void moradorNaoListaOsFechamentos() throws Exception {
    mvc.perform(get("/fechamentos").with(logado(morador()))).andExpect(status().isForbidden());
    verify(fechamentoService, never()).listar();
  }

  @Test
  void buscarInexistenteDevolve404() throws Exception {
    when(fechamentoService.buscarPorId(99L)).thenReturn(Optional.empty());

    mvc.perform(get("/fechamentos/99").with(logado(sindico())))
        .andExpect(status().isNotFound());
  }
}
