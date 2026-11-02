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
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import br.edu.ifsp.carregaja.model.EstadoSessao;
import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.Tarifa;
import br.edu.ifsp.carregaja.model.Vaga;
import br.edu.ifsp.carregaja.model.Veiculo;
import br.edu.ifsp.carregaja.service.RegraNegocioException;
import br.edu.ifsp.carregaja.service.SessaoRecargaService;
import br.edu.ifsp.carregaja.view.SessaoRequest;
import br.edu.ifsp.carregaja.view.SessaoResponse;

@WebMvcTest(SessaoRecargaController.class)
class SessaoRecargaControllerTest extends ControleTesteBase {

  private static final String VALIDA = "{\"vagaId\":2,\"nivelBateria\":30}";

  @MockBean
  private SessaoRecargaService sessaoService;

  private static SessaoResponse resposta() {
    Vaga vaga = new Vaga();
    vaga.setIdentificacao("G-02");
    Veiculo veiculo = new Veiculo();
    veiculo.setModelo("Nissan Leaf");
    Tarifa tarifa = new Tarifa();
    tarifa.setValorKwh(new BigDecimal("0.92"));
    SessaoRecarga sessao = new SessaoRecarga();
    sessao.setId(1L);
    sessao.setVaga(vaga);
    sessao.setVeiculo(veiculo);
    sessao.setTarifa(tarifa);
    sessao.setNivelBateriaInicial(30);
    sessao.setPotenciaEfetiva(6.6);
    sessao.setEnergiaARepor(28.0);
    sessao.setInicio(LocalDateTime.of(2026, 9, 8, 18, 10));
    sessao.setPrevisaoConclusao(LocalDateTime.of(2026, 9, 8, 22, 25));
    return new SessaoResponse(sessao, EstadoSessao.EM_ANDAMENTO);
  }

  // HU-03, teste 01
  @Test
  void inicioValidoDevolve201ComAPrevisao() throws Exception {
    when(sessaoService.iniciar(any(), any(SessaoRequest.class))).thenReturn(resposta());

    mvc.perform(post("/sessoes").with(logado(morador())).contentType(MediaType.APPLICATION_JSON).content(VALIDA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.vaga").value("G-02"))
        .andExpect(jsonPath("$.potenciaEfetiva").value(6.6))
        .andExpect(jsonPath("$.energiaARepor").value(28.0))
        .andExpect(jsonPath("$.previsaoConclusao").value("2026-09-08T22:25:00"))
        .andExpect(jsonPath("$.estado").value("EM_ANDAMENTO"))
        .andExpect(jsonPath("$.usuario").doesNotExist());
  }

  // HU-03, teste 02
  @Test
  void vagaOcupadaDevolve409ComAMensagem() throws Exception {
    when(sessaoService.iniciar(any(), any(SessaoRequest.class))).thenThrow(new RegraNegocioException(HttpStatus.CONFLICT,
        "A vaga G-01 está ocupada. Previsão de liberação: 19:30."));

    mvc.perform(post("/sessoes").with(logado(morador())).contentType(MediaType.APPLICATION_JSON).content(VALIDA))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensagem").value("A vaga G-01 está ocupada. Previsão de liberação: 19:30."));
  }

  @Test
  void nivelForaDeZeroACemDevolve400() throws Exception {
    mvc.perform(post("/sessoes").with(logado(morador())).contentType(MediaType.APPLICATION_JSON)
        .content("{\"vagaId\":2,\"nivelBateria\":101}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/sessoes").with(logado(morador())).contentType(MediaType.APPLICATION_JSON)
        .content("{\"vagaId\":2,\"nivelBateria\":-1}"))
        .andExpect(status().isBadRequest());
    verify(sessaoService, never()).iniciar(any(), any());
  }

  @Test
  void semVagaDevolve400() throws Exception {
    mvc.perform(post("/sessoes").with(logado(morador())).contentType(MediaType.APPLICATION_JSON)
        .content("{\"nivelBateria\":30}"))
        .andExpect(status().isBadRequest());
    verify(sessaoService, never()).iniciar(any(), any());
  }

  @Test
  void sindicoSemApartamentoNaoIniciaSessao() throws Exception {
    mvc.perform(post("/sessoes").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content(VALIDA))
        .andExpect(status().isForbidden());
    verify(sessaoService, never()).iniciar(any(), any());
  }

  @Test
  void buscarAMinhaDevolveASessaoOuO404() throws Exception {
    when(sessaoService.buscarAberta(any())).thenReturn(Optional.of(resposta()));
    mvc.perform(get("/sessoes/minha").with(logado(morador())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vaga").value("G-02"));

    when(sessaoService.buscarAberta(any())).thenReturn(Optional.empty());
    mvc.perform(get("/sessoes/minha").with(logado(morador()))).andExpect(status().isNotFound());
  }
}
