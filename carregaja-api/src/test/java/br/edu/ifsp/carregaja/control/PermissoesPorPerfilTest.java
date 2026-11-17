package br.edu.ifsp.carregaja.control;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import br.edu.ifsp.carregaja.service.AutenticacaoService;
import br.edu.ifsp.carregaja.service.ConsumoService;
import br.edu.ifsp.carregaja.service.FechamentoMensalService;
import br.edu.ifsp.carregaja.service.HistoricoService;
import br.edu.ifsp.carregaja.service.PainelVagasService;
import br.edu.ifsp.carregaja.service.SessaoRecargaService;
import br.edu.ifsp.carregaja.service.TarifaService;
import br.edu.ifsp.carregaja.service.UsuarioService;
import br.edu.ifsp.carregaja.service.VagaService;
import br.edu.ifsp.carregaja.service.VeiculoService;

/**
 * Cada rota de um perfil é negada ao outro, com e sem a barra no fim do endereço. O Spring MVC atende "/tarifas/" como
 * "/tarifas": se a regra de acesso casar só o caminho exato, a barra passa por uma regra mais frouxa (RF-03, RN-07).
 */
@WebMvcTest
class PermissoesPorPerfilTest extends ControleTesteBase {

  @MockBean private AutenticacaoService autenticacaoService;
  @MockBean private ConsumoService consumoService;
  @MockBean private FechamentoMensalService fechamentoService;
  @MockBean private HistoricoService historicoService;
  @MockBean private PainelVagasService painelService;
  @MockBean private SessaoRecargaService sessaoService;
  @MockBean private TarifaService tarifaService;
  @MockBean private UsuarioService usuarioService;
  @MockBean private VagaService vagaService;
  @MockBean private VeiculoService veiculoService;

  private static final String CORPO = "{}";

  // Rotas que só o síndico usa
  static Stream<Arguments> rotasDoSindico() {
    return Stream.of(
        Arguments.of(HttpMethod.GET, "/usuarios"),
        Arguments.of(HttpMethod.GET, "/usuarios/1"),
        Arguments.of(HttpMethod.POST, "/usuarios"),
        Arguments.of(HttpMethod.PUT, "/usuarios/1"),
        Arguments.of(HttpMethod.POST, "/usuarios/1/desativar"),
        Arguments.of(HttpMethod.DELETE, "/usuarios/1"),
        Arguments.of(HttpMethod.POST, "/vagas"),
        Arguments.of(HttpMethod.PUT, "/vagas/1"),
        Arguments.of(HttpMethod.POST, "/vagas/1/desativar"),
        Arguments.of(HttpMethod.DELETE, "/vagas/1"),
        Arguments.of(HttpMethod.GET, "/tarifas"),
        Arguments.of(HttpMethod.POST, "/tarifas"),
        Arguments.of(HttpMethod.GET, "/sessoes/abertas"),
        Arguments.of(HttpMethod.POST, "/sessoes/1/encerrar-administrativamente"),
        Arguments.of(HttpMethod.POST, "/fechamentos"),
        Arguments.of(HttpMethod.GET, "/fechamentos/1"),
        Arguments.of(HttpMethod.GET, "/historico"),
        Arguments.of(HttpMethod.GET, "/veiculos"));
  }

  // Rotas que só o morador usa (o síndico sem apartamento não tem veículo, sessão nem consumo)
  static Stream<Arguments> rotasDoMorador() {
    return Stream.of(
        Arguments.of(HttpMethod.POST, "/sessoes"),
        Arguments.of(HttpMethod.GET, "/sessoes/minha"),
        Arguments.of(HttpMethod.POST, "/sessoes/minha/encerrar"),
        Arguments.of(HttpMethod.GET, "/consumos/meu"),
        Arguments.of(HttpMethod.GET, "/veiculos/meu"),
        Arguments.of(HttpMethod.POST, "/veiculos"),
        Arguments.of(HttpMethod.PUT, "/veiculos/1"));
  }

  private org.springframework.test.web.servlet.ResultActions chamar(HttpMethod metodo, String caminho,
      br.edu.ifsp.carregaja.model.Usuario quem) throws Exception {
    return mvc.perform(request(metodo, caminho).with(logado(quem)).contentType(MediaType.APPLICATION_JSON).content(CORPO));
  }

  @ParameterizedTest(name = "morador não acessa {0} {1}")
  @MethodSource("rotasDoSindico")
  void moradorNaoAcessaRotaDoSindico(HttpMethod metodo, String caminho) throws Exception {
    chamar(metodo, caminho, morador()).andExpect(status().isForbidden());
  }

  @ParameterizedTest(name = "morador não acessa {0} {1}/ (com a barra final)")
  @MethodSource("rotasDoSindico")
  void moradorNaoAcessaRotaDoSindicoComBarraFinal(HttpMethod metodo, String caminho) throws Exception {
    chamar(metodo, caminho + "/", morador()).andExpect(status().isForbidden());
  }

  @ParameterizedTest(name = "síndico sem apartamento não acessa {0} {1}")
  @MethodSource("rotasDoMorador")
  void sindicoNaoAcessaRotaDoMorador(HttpMethod metodo, String caminho) throws Exception {
    chamar(metodo, caminho, sindico()).andExpect(status().isForbidden());
  }

  @ParameterizedTest(name = "síndico sem apartamento não acessa {0} {1}/ (com a barra final)")
  @MethodSource("rotasDoMorador")
  void sindicoNaoAcessaRotaDoMoradorComBarraFinal(HttpMethod metodo, String caminho) throws Exception {
    chamar(metodo, caminho + "/", sindico()).andExpect(status().isForbidden());
  }

  @Test
  void semTokenNenhumaRotaRestritaAbre() throws Exception {
    mvc.perform(request(HttpMethod.GET, "/tarifas/")).andExpect(status().isUnauthorized());
    mvc.perform(request(HttpMethod.GET, "/sessoes/abertas/")).andExpect(status().isUnauthorized());
    mvc.perform(request(HttpMethod.POST, "/sessoes/1/encerrar-administrativamente/")).andExpect(status().isUnauthorized());
  }
}
