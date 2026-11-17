package br.edu.ifsp.carregaja.control;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import br.edu.ifsp.carregaja.model.Apartamento;
import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.SituacaoSessao;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.model.Vaga;
import br.edu.ifsp.carregaja.repository.SessaoRecargaRepository;
import br.edu.ifsp.carregaja.repository.VagaRepository;
import br.edu.ifsp.carregaja.service.ApuracaoEnergiaService;
import br.edu.ifsp.carregaja.service.PainelVagasService;
import br.edu.ifsp.carregaja.service.TokenService;

/**
 * A rota com o serviço de verdade e só os repositórios simulados: o que importa conferir aqui é o JSON que cada
 * perfil recebe (RF-03). O relógio fica parado às 16h00 do dia dos testes de aceitação.
 */
@WebMvcTest(PainelVagasController.class)
@Import({SegurancaConfig.class, TokenService.class, PainelVagasService.class, ApuracaoEnergiaService.class})
class PainelVagasControllerTest extends ControleTesteBase {

  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
  private static final LocalDateTime AS_16H00 = LocalDateTime.of(2026, 9, 8, 16, 0);

  @TestConfiguration
  static class RelogioParado {
    @Bean
    @Primary
    Clock relogioDoTeste() {
      return Clock.fixed(AS_16H00.atZone(FUSO).toInstant(), FUSO);
    }
  }

  @MockBean
  private VagaRepository vagaRepository;

  @MockBean
  private SessaoRecargaRepository sessaoRepository;

  private Vaga g01;
  private Vaga g02;
  private List<SessaoRecarga> abertas;

  private static Vaga vaga(Long id, String identificacao, double potencia) {
    Vaga vaga = new Vaga();
    vaga.setId(id);
    vaga.setIdentificacao(identificacao);
    vaga.setPotenciaCarregador(potencia);
    return vaga;
  }

  private static Usuario moradorDo17() {
    Usuario morador = usuario(17L, Perfil.MORADOR);
    morador.setNome("Morador do 17");
    Apartamento apartamento = new Apartamento();
    apartamento.setId(2L);
    apartamento.setNumero("17");
    morador.setApartamento(apartamento);
    return morador;
  }

  private void ocupar(Vaga vaga, LocalDateTime inicio, LocalDateTime previsao) {
    SessaoRecarga sessao = new SessaoRecarga();
    sessao.setId((long) abertas.size() + 1);
    sessao.setVaga(vaga);
    sessao.setUsuario(moradorDo17());
    sessao.setInicio(inicio);
    sessao.setPrevisaoConclusao(previsao);
    abertas.add(sessao);
  }

  @BeforeEach
  void preparar() {
    g01 = vaga(1L, "G-01", 11.0);
    g02 = vaga(2L, "G-02", 7.4);
    abertas = new ArrayList<>();
    when(vagaRepository.findAll()).thenReturn(List.of(g01, g02));
    when(sessaoRepository.findBySituacao(SituacaoSessao.EM_ANDAMENTO)).thenReturn(abertas);
  }

  @Test
  void semTokenDevolve401() throws Exception {
    mvc.perform(get("/painel")).andExpect(status().isUnauthorized());
  }

  // HU-02, testes 01 e 02
  @Test
  void devolveALivreEAOcupadaComAPrevisao() throws Exception {
    ocupar(g01, LocalDateTime.of(2026, 9, 8, 14, 0), LocalDateTime.of(2026, 9, 8, 18, 22));

    mvc.perform(get("/painel").with(logado(morador())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consultadoEm").value("2026-09-08T16:00:00"))
        .andExpect(jsonPath("$.vagasLivres").value(1))
        .andExpect(jsonPath("$.mensagem").doesNotExist())
        .andExpect(jsonPath("$.proximaLiberacao").doesNotExist())
        .andExpect(jsonPath("$.vagas.length()").value(2))
        .andExpect(jsonPath("$.vagas[0].identificacao").value("G-01"))
        .andExpect(jsonPath("$.vagas[0].potenciaCarregador").value(11.0))
        .andExpect(jsonPath("$.vagas[0].estado").value("OCUPADA"))
        .andExpect(jsonPath("$.vagas[0].inicio").value("2026-09-08T14:00:00"))
        .andExpect(jsonPath("$.vagas[0].previsaoLiberacao").value("2026-09-08T18:22:00"))
        .andExpect(jsonPath("$.vagas[1].identificacao").value("G-02"))
        .andExpect(jsonPath("$.vagas[1].estado").value("LIVRE"))
        .andExpect(jsonPath("$.vagas[1].inicio").doesNotExist())
        .andExpect(jsonPath("$.vagas[1].previsaoLiberacao").doesNotExist());
  }

  // HU-02, teste 05; RF-03: os campos nem vão na resposta do morador
  @Test
  void moradorNaoRecebeAIdentificacaoDeQuemOcupa() throws Exception {
    ocupar(g01, LocalDateTime.of(2026, 9, 8, 14, 0), LocalDateTime.of(2026, 9, 8, 18, 22));

    mvc.perform(get("/painel").with(logado(morador())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vagas[0].estado").value("OCUPADA"))
        .andExpect(jsonPath("$.vagas[0].previsaoLiberacao").value("2026-09-08T18:22:00"))
        .andExpect(jsonPath("$.vagas[0].apartamento").doesNotExist())
        .andExpect(jsonPath("$.vagas[0].morador").doesNotExist())
        .andExpect(jsonPath("$.vagas[0].usuario").doesNotExist())
        .andExpect(jsonPath("$..email").doesNotExist());
  }

  // HU-02, teste 06
  @Test
  void sindicoRecebeOApartamentoEONome() throws Exception {
    ocupar(g01, LocalDateTime.of(2026, 9, 8, 14, 0), LocalDateTime.of(2026, 9, 8, 18, 22));

    mvc.perform(get("/painel").with(logado(sindico())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vagas[0].estado").value("OCUPADA"))
        .andExpect(jsonPath("$.vagas[0].previsaoLiberacao").value("2026-09-08T18:22:00"))
        .andExpect(jsonPath("$.vagas[0].apartamento").value("17"))
        .andExpect(jsonPath("$.vagas[0].morador").value("Morador do 17"))
        .andExpect(jsonPath("$..email").doesNotExist());
  }

  // HU-02, testes 03 e 04: às 16h00, uma previsão das 15h30 já passou e um início das 10h00 completou 6 horas
  @Test
  void devolveAConcluidaAindaOcupadaEAExcedida() throws Exception {
    ocupar(g01, LocalDateTime.of(2026, 9, 8, 12, 0), LocalDateTime.of(2026, 9, 8, 15, 30));
    ocupar(g02, LocalDateTime.of(2026, 9, 8, 10, 0), LocalDateTime.of(2026, 9, 8, 13, 0));

    mvc.perform(get("/painel").with(logado(morador())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vagas[0].estado").value("CONCLUIDA_AINDA_OCUPADA"))
        .andExpect(jsonPath("$.vagas[1].estado").value("EXCEDIDA"))
        .andExpect(jsonPath("$.vagasLivres").value(0))
        .andExpect(jsonPath("$.proximaLiberacao").doesNotExist());
  }

  // HU-02, teste 07
  @Test
  void semVagaLivreDevolveAMensagemEALiberacaoMaisProxima() throws Exception {
    ocupar(g01, LocalDateTime.of(2026, 9, 8, 15, 8), LocalDateTime.of(2026, 9, 8, 19, 30));
    ocupar(g02, LocalDateTime.of(2026, 9, 8, 15, 30), LocalDateTime.of(2026, 9, 8, 21, 0));

    mvc.perform(get("/painel").with(logado(morador())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vagasLivres").value(0))
        .andExpect(jsonPath("$.mensagem").value("Nenhuma vaga livre. Liberação mais próxima: G-01, às 19:30."))
        .andExpect(jsonPath("$.proximaLiberacao.identificacao").value("G-01"))
        .andExpect(jsonPath("$.proximaLiberacao.previsaoLiberacao").value("2026-09-08T19:30:00"))
        .andExpect(jsonPath("$.proximaLiberacao.apartamento").doesNotExist());
  }

  // HU-01, teste 05: com a senha provisória o usuário só troca a senha
  @Test
  void senhaProvisoriaNaoConsultaOPainel() throws Exception {
    Usuario comSenhaProvisoria = morador();
    comSenhaProvisoria.setTrocarSenha(true);

    mvc.perform(get("/painel").with(logado(comSenhaProvisoria))).andExpect(status().isForbidden());
  }
}
