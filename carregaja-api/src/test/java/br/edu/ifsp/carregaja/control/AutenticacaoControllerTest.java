package br.edu.ifsp.carregaja.control;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.service.AutenticacaoService;
import br.edu.ifsp.carregaja.service.RegraNegocioException;
import br.edu.ifsp.carregaja.service.VeiculoService;
import br.edu.ifsp.carregaja.view.AutenticacaoResponse;
import br.edu.ifsp.carregaja.view.LoginRequest;
import br.edu.ifsp.carregaja.view.TrocarSenhaRequest;

@WebMvcTest({AutenticacaoController.class, VeiculoController.class})
class AutenticacaoControllerTest extends ControleTesteBase {

  private static final String LOGIN = "{\"email\":\"apto42@carregaja.test\",\"senha\":\"senha-valida\"}";
  private static final String TROCA = "{\"senhaAtual\":\"provisoria1\",\"senhaNova\":\"senha-nova-1\"}";

  @MockBean
  private AutenticacaoService autenticacaoService;

  @MockBean
  private VeiculoService veiculoService;

  // HU-01, teste 01
  @Test
  void acessoValidoDevolveTokenPerfilEApartamento() throws Exception {
    when(autenticacaoService.autenticar(any(LoginRequest.class))).thenReturn(new AutenticacaoResponse("abc", morador()));

    mvc.perform(post("/autenticacao").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("abc"))
        .andExpect(jsonPath("$.usuario.perfis[0]").value("MORADOR"))
        .andExpect(jsonPath("$.usuario.apartamento.numero").value("42"))
        .andExpect(jsonPath("$.usuario.senha").doesNotExist());
  }

  // HU-01, teste 02
  @Test
  void senhaIncorretaDevolve401ComMensagemGenerica() throws Exception {
    when(autenticacaoService.autenticar(any(LoginRequest.class)))
        .thenThrow(new RegraNegocioException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos."));

    mvc.perform(post("/autenticacao").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.mensagem").value("E-mail ou senha incorretos."));
  }

  // HU-01, teste 03
  @Test
  void moradorDesativadoDevolve403ComOrientacao() throws Exception {
    when(autenticacaoService.autenticar(any(LoginRequest.class)))
        .thenThrow(new RegraNegocioException(HttpStatus.FORBIDDEN, "Seu acesso está desativado. Procure o síndico."));

    mvc.perform(post("/autenticacao").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.mensagem").value("Seu acesso está desativado. Procure o síndico."));
  }

  // HU-01, teste 04
  @Test
  void usuarioComOsDoisPerfisDevolveOsDois() throws Exception {
    Usuario sindica = usuario(7L, Perfil.MORADOR, Perfil.SINDICO);
    when(autenticacaoService.autenticar(any(LoginRequest.class))).thenReturn(new AutenticacaoResponse("abc", sindica));

    mvc.perform(post("/autenticacao").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.usuario.perfis.length()").value(2));
  }

  @Test
  void loginSemSenhaDevolve400ComAMensagem() throws Exception {
    mvc.perform(post("/autenticacao").contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"apto42@carregaja.test\",\"senha\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensagem").value("Informe a senha."));
    verify(autenticacaoService, never()).autenticar(any());
  }

  // HU-01, teste 05
  @Test
  void senhaProvisoriaSoPermiteATroca() throws Exception {
    Usuario provisorio = morador();
    provisorio.setTrocarSenha(true);

    mvc.perform(get("/veiculos/meu").with(logado(provisorio))).andExpect(status().isForbidden());
    mvc.perform(put("/autenticacao/senha").with(logado(provisorio))
        .contentType(MediaType.APPLICATION_JSON).content(TROCA))
        .andExpect(status().isNoContent());
    verify(autenticacaoService).trocarSenha(eq(provisorio), any(TrocarSenhaRequest.class));
  }

  // HU-01, teste 07
  @Test
  void senhaAtualIncorretaDevolve422() throws Exception {
    doThrow(new RegraNegocioException(HttpStatus.UNPROCESSABLE_ENTITY, "A senha atual está incorreta."))
        .when(autenticacaoService).trocarSenha(any(), any());

    mvc.perform(put("/autenticacao/senha").with(logado(morador()))
        .contentType(MediaType.APPLICATION_JSON).content(TROCA))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.mensagem").value("A senha atual está incorreta."));
  }

  @Test
  void senhaNovaCurtaDevolve400() throws Exception {
    mvc.perform(put("/autenticacao/senha").with(logado(morador())).contentType(MediaType.APPLICATION_JSON)
        .content("{\"senhaAtual\":\"provisoria1\",\"senhaNova\":\"curta\"}"))
        .andExpect(status().isBadRequest());
    verify(autenticacaoService, never()).trocarSenha(any(), any());
  }

  @Test
  void trocarSenhaSemTokenDevolve401() throws Exception {
    mvc.perform(put("/autenticacao/senha").contentType(MediaType.APPLICATION_JSON).content(TROCA))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rotaProtegidaSemTokenDevolve401() throws Exception {
    mvc.perform(get("/veiculos/meu")).andExpect(status().isUnauthorized());
  }

  @Test
  void tokenInvalidoDevolve401() throws Exception {
    mvc.perform(get("/veiculos/meu").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
        .andExpect(status().isUnauthorized());
  }

  // HU-09, teste 04: desativar revoga o acesso mesmo de quem já tinha token
  @Test
  void tokenDeUsuarioDesativadoDevolve401() throws Exception {
    Usuario desativado = morador();
    desativado.setAtivo(false);

    mvc.perform(get("/veiculos/meu").with(logado(desativado))).andExpect(status().isUnauthorized());
  }

  @Test
  void moradorNaoListaOsVeiculosDeTodos() throws Exception {
    mvc.perform(get("/veiculos").with(logado(morador()))).andExpect(status().isForbidden());
  }

  @Test
  void sindicoSemApartamentoNaoCadastraVeiculo() throws Exception {
    mvc.perform(post("/veiculos").with(logado(sindico())).contentType(MediaType.APPLICATION_JSON)
        .content("{\"modelo\":\"Nissan Leaf\",\"capacidadeBateria\":40,\"potenciaMaxima\":6.6}"))
        .andExpect(status().isForbidden());
  }
}
