package br.edu.ifsp.carregaja.control;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.edu.ifsp.carregaja.model.Veiculo;
import br.edu.ifsp.carregaja.service.VeiculoService;
import br.edu.ifsp.carregaja.view.VeiculoRequest;

@WebMvcTest(VeiculoController.class)
class VeiculoControllerTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private VeiculoService veiculoService;

  private static Veiculo leaf() {
    Veiculo veiculo = new Veiculo();
    veiculo.setId(1L);
    veiculo.setModelo("Nissan Leaf");
    veiculo.setCapacidadeBateria(40.0);
    veiculo.setPotenciaMaxima(6.6);
    return veiculo;
  }

  private static String json(String modelo, Object capacidade, Object potencia) {
    String texto = modelo == null ? "null" : "\"" + modelo + "\"";
    return "{\"modelo\":" + texto + ",\"capacidadeBateria\":" + capacidade + ",\"potenciaMaxima\":" + potencia + "}";
  }

  // HU-05, teste 01
  @Test
  void cadastroValidoDevolve201ComOEndereco() throws Exception {
    when(veiculoService.criar(any(VeiculoRequest.class))).thenReturn(leaf());

    mvc.perform(post("/veiculos").contentType(MediaType.APPLICATION_JSON).content(json("Nissan Leaf", 40, 6.6)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/veiculos/1"))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.modelo").value("Nissan Leaf"))
        .andExpect(jsonPath("$.capacidadeBateria").value(40.0))
        .andExpect(jsonPath("$.potenciaMaxima").value(6.6));
  }

  @Test
  void modeloEmBrancoDevolve400() throws Exception {
    mvc.perform(post("/veiculos").contentType(MediaType.APPLICATION_JSON).content(json("  ", 40, 6.6)))
        .andExpect(status().isBadRequest());
    verify(veiculoService, never()).criar(any());
  }

  @Test
  void modeloNuloDevolve400() throws Exception {
    mvc.perform(post("/veiculos").contentType(MediaType.APPLICATION_JSON).content(json(null, 40, 6.6)))
        .andExpect(status().isBadRequest());
    verify(veiculoService, never()).criar(any());
  }

  // HU-05, teste 02
  @Test
  void capacidadeZeroDevolve400() throws Exception {
    mvc.perform(post("/veiculos").contentType(MediaType.APPLICATION_JSON).content(json("Nissan Leaf", 0, 7)))
        .andExpect(status().isBadRequest());
    verify(veiculoService, never()).criar(any());
  }

  @Test
  void capacidadeAusenteDevolve400() throws Exception {
    mvc.perform(post("/veiculos").contentType(MediaType.APPLICATION_JSON).content(json("Nissan Leaf", null, 7)))
        .andExpect(status().isBadRequest());
    verify(veiculoService, never()).criar(any());
  }

  // HU-05, teste 03
  @Test
  void potenciaNegativaDevolve400() throws Exception {
    mvc.perform(post("/veiculos").contentType(MediaType.APPLICATION_JSON).content(json("Nissan Leaf", 40, -2)))
        .andExpect(status().isBadRequest());
    verify(veiculoService, never()).criar(any());
  }

  @Test
  void listarDevolveOsVeiculos() throws Exception {
    when(veiculoService.listarTodos()).thenReturn(List.of(leaf()));

    mvc.perform(get("/veiculos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].modelo").value("Nissan Leaf"));
  }

  @Test
  void buscarInexistenteDevolve404() throws Exception {
    when(veiculoService.buscarPorId(99L)).thenReturn(Optional.empty());

    mvc.perform(get("/veiculos/99"))
        .andExpect(status().isNotFound());
  }

  @Test
  void alterarDevolve200ComOsNovosDados() throws Exception {
    Veiculo alterado = leaf();
    alterado.setCapacidadeBateria(60.0);
    when(veiculoService.atualizar(eq(1L), any(VeiculoRequest.class))).thenReturn(Optional.of(alterado));

    mvc.perform(put("/veiculos/1").contentType(MediaType.APPLICATION_JSON).content(json("Nissan Leaf", 60, 6.6)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.capacidadeBateria").value(60.0));
  }

  @Test
  void alterarInexistenteDevolve404() throws Exception {
    when(veiculoService.atualizar(eq(99L), any(VeiculoRequest.class))).thenReturn(Optional.empty());

    mvc.perform(put("/veiculos/99").contentType(MediaType.APPLICATION_JSON).content(json("Nissan Leaf", 40, 6.6)))
        .andExpect(status().isNotFound());
  }

  @Test
  void alterarComPotenciaZeroDevolve400() throws Exception {
    mvc.perform(put("/veiculos/1").contentType(MediaType.APPLICATION_JSON).content(json("Nissan Leaf", 40, 0)))
        .andExpect(status().isBadRequest());
    verify(veiculoService, never()).atualizar(any(), any());
  }
}
