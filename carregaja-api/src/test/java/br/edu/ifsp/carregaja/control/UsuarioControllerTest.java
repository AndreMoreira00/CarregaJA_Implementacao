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

import br.edu.ifsp.carregaja.service.RegraNegocioException;
import br.edu.ifsp.carregaja.service.UsuarioService;
import br.edu.ifsp.carregaja.view.UsuarioRequest;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest extends ControleTesteBase {

  private static final String MORADOR_VALIDO =
      "{\"nome\":\"Ana\",\"email\":\"ana@carregaja.test\",\"apartamento\":\"42\",\"senhaProvisoria\":\"provisoria1\"}";

  @MockBean
  private UsuarioService usuarioService;

  // HU-09, teste 01
  @Test
  void cadastroValidoDevolve201SemExporASenha() throws Exception {
    when(usuarioService.criar(any(UsuarioRequest.class))).thenReturn(morador());

    mvc.perform(post("/usuarios").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content(MORADOR_VALIDO))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/usuarios/42"))
        .andExpect(jsonPath("$.apartamento.numero").value("42"))
        .andExpect(jsonPath("$.senha").doesNotExist());
  }

  // HU-09, teste 02
  @Test
  void emailDuplicadoDevolve409ComAMensagem() throws Exception {
    when(usuarioService.criar(any(UsuarioRequest.class)))
        .thenThrow(new RegraNegocioException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado."));

    mvc.perform(post("/usuarios").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content(MORADOR_VALIDO))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensagem").value("Este e-mail já está cadastrado."));
  }

  @Test
  void emailInvalidoDevolve400() throws Exception {
    mvc.perform(post("/usuarios").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON)
        .content("{\"nome\":\"Ana\",\"email\":\"nao-e-email\",\"apartamento\":\"42\",\"senhaProvisoria\":\"provisoria1\"}"))
        .andExpect(status().isBadRequest());
    verify(usuarioService, never()).criar(any());
  }

  @Test
  void senhaProvisoriaCurtaDevolve400() throws Exception {
    mvc.perform(post("/usuarios").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON)
        .content("{\"nome\":\"Ana\",\"email\":\"ana@carregaja.test\",\"apartamento\":\"42\",\"senhaProvisoria\":\"curta\"}"))
        .andExpect(status().isBadRequest());
    verify(usuarioService, never()).criar(any());
  }

  @Test
  void moradorNaoAcessaOCadastroDeMoradores() throws Exception {
    mvc.perform(get("/usuarios").with(logado(morador()))).andExpect(status().isForbidden());
    mvc.perform(post("/usuarios").with(logado(morador())).contentType(MediaType.APPLICATION_JSON).content(MORADOR_VALIDO))
        .andExpect(status().isForbidden());
    verify(usuarioService, never()).criar(any());
  }

  @Test
  void sindicoListaOsUsuarios() throws Exception {
    when(usuarioService.listarTodos()).thenReturn(List.of(morador()));

    mvc.perform(get("/usuarios").with(logado(sindico())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].email").value("usuario42@carregaja.test"));
  }

  @Test
  void alterarInexistenteDevolve404() throws Exception {
    when(usuarioService.atualizar(eq(99L), any(UsuarioRequest.class))).thenReturn(Optional.empty());

    mvc.perform(put("/usuarios/99").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON).content(MORADOR_VALIDO))
        .andExpect(status().isNotFound());
  }

  // HU-09, teste 04
  @Test
  void desativarDevolve200() throws Exception {
    when(usuarioService.desativar(42L)).thenReturn(Optional.of(morador()));

    mvc.perform(post("/usuarios/42/desativar").with(logado(sindico()))).andExpect(status().isOk());
  }

  @Test
  void reativarInexistenteDevolve404() throws Exception {
    when(usuarioService.reativar(99L)).thenReturn(Optional.empty());

    mvc.perform(post("/usuarios/99/reativar").with(logado(sindico()))).andExpect(status().isNotFound());
  }

  @Test
  void removerDevolve204OuO404() throws Exception {
    when(usuarioService.remover(42L)).thenReturn(true);
    when(usuarioService.remover(99L)).thenReturn(false);

    mvc.perform(delete("/usuarios/42").with(logado(sindico()))).andExpect(status().isNoContent());
    mvc.perform(delete("/usuarios/99").with(logado(sindico()))).andExpect(status().isNotFound());
  }
}
