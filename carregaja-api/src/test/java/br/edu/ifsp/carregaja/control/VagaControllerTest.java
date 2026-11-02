package br.edu.ifsp.carregaja.control;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import br.edu.ifsp.carregaja.model.Vaga;
import br.edu.ifsp.carregaja.service.RegraNegocioException;
import br.edu.ifsp.carregaja.service.VagaService;
import br.edu.ifsp.carregaja.view.VagaRequest;

@WebMvcTest(VagaController.class)
class VagaControllerTest extends ControleTesteBase {

  @MockBean
  private VagaService vagaService;

  private static Vaga g01() {
    Vaga vaga = new Vaga();
    vaga.setId(1L);
    vaga.setIdentificacao("G-01");
    vaga.setPotenciaCarregador(11.0);
    return vaga;
  }

  private static String json(String identificacao, Object potencia) {
    return "{\"identificacao\":\"" + identificacao + "\",\"potenciaCarregador\":" + potencia + "}";
  }

  // HU-07, teste 01
  @Test
  void cadastroValidoDevolve201() throws Exception {
    when(vagaService.criar(any(VagaRequest.class))).thenReturn(g01());

    mvc.perform(post("/vagas").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content(json("G-01", 11.0)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/vagas/1"))
        .andExpect(jsonPath("$.identificacao").value("G-01"))
        .andExpect(jsonPath("$.ativa").value(true));
  }

  // HU-07, teste 02
  @Test
  void codigoDuplicadoDevolve409() throws Exception {
    when(vagaService.criar(any(VagaRequest.class)))
        .thenThrow(new RegraNegocioException(HttpStatus.CONFLICT, "Já existe a vaga G-01."));

    mvc.perform(post("/vagas").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content(json("G-01", 11.0)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensagem").value("Já existe a vaga G-01."));
  }

  // HU-07, teste 03
  @Test
  void potenciaZeroDevolve400() throws Exception {
    mvc.perform(post("/vagas").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content(json("G-03", 0)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensagem").value("A potência do carregador deve ser maior que zero."));
    verify(vagaService, never()).criar(any());
  }

  @Test
  void identificacaoEmBrancoDevolve400() throws Exception {
    mvc.perform(post("/vagas").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content(json(" ", 11.0)))
        .andExpect(status().isBadRequest());
    verify(vagaService, never()).criar(any());
  }

  @Test
  void moradorNaoCadastraNemAlteraVaga() throws Exception {
    mvc.perform(post("/vagas").with(logado(morador())).contentType(MediaType.APPLICATION_JSON).content(json("G-01", 11.0)))
        .andExpect(status().isForbidden());
    mvc.perform(put("/vagas/1").with(logado(morador())).contentType(MediaType.APPLICATION_JSON).content(json("G-01", 11.0)))
        .andExpect(status().isForbidden());
    mvc.perform(post("/vagas/1/desativar").with(logado(morador()))).andExpect(status().isForbidden());
    mvc.perform(delete("/vagas/1").with(logado(morador()))).andExpect(status().isForbidden());
  }

  @Test
  void listarDevolveAsVagas() throws Exception {
    when(vagaService.listarTodas()).thenReturn(List.of(g01()));

    mvc.perform(get("/vagas").with(logado(sindico())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].potenciaCarregador").value(11.0));
  }

  @Test
  void alterarInexistenteDevolve404() throws Exception {
    when(vagaService.atualizar(eq(99L), any(VagaRequest.class))).thenReturn(Optional.empty());

    mvc.perform(put("/vagas/99").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content(json("G-09", 7.4)))
        .andExpect(status().isNotFound());
  }

  // HU-07, teste 04
  @Test
  void desativarDevolve200ComAVagaInativa() throws Exception {
    Vaga desativada = g01();
    desativada.setAtiva(false);
    when(vagaService.desativar(1L)).thenReturn(Optional.of(desativada));

    mvc.perform(post("/vagas/1/desativar").with(logado(sindico())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ativa").value(false));
  }

  @Test
  void removerDevolve204OuO404() throws Exception {
    when(vagaService.remover(1L)).thenReturn(true);
    when(vagaService.remover(99L)).thenReturn(false);

    mvc.perform(delete("/vagas/1").with(logado(sindico()))).andExpect(status().isNoContent());
    mvc.perform(delete("/vagas/99").with(logado(sindico()))).andExpect(status().isNotFound());
  }
}
