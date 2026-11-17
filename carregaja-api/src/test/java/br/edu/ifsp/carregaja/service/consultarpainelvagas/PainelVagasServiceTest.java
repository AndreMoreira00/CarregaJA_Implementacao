package br.edu.ifsp.carregaja.service.consultarpainelvagas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.ifsp.carregaja.model.compartilhado.Apartamento;
import br.edu.ifsp.carregaja.model.compartilhado.Perfil;
import br.edu.ifsp.carregaja.model.compartilhado.SessaoRecarga;
import br.edu.ifsp.carregaja.model.compartilhado.SituacaoSessao;
import br.edu.ifsp.carregaja.model.compartilhado.Usuario;
import br.edu.ifsp.carregaja.model.compartilhado.Vaga;
import br.edu.ifsp.carregaja.model.consultarpainelvagas.EstadoVaga;
import br.edu.ifsp.carregaja.repository.compartilhado.SessaoRecargaRepository;
import br.edu.ifsp.carregaja.repository.compartilhado.VagaRepository;
import br.edu.ifsp.carregaja.service.apurarenergia.ApuracaoEnergiaService;
import br.edu.ifsp.carregaja.view.consultarpainelvagas.PainelVagasResponse;
import br.edu.ifsp.carregaja.view.consultarpainelvagas.VagaPainelResponse;

class PainelVagasServiceTest {

  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
  private static final LocalDateTime AS_14H00 = hora(14, 0);
  private static final LocalDateTime AS_18H22 = hora(18, 22);

  private VagaRepository vagaRepository;
  private SessaoRecargaRepository sessaoRepository;
  private List<Vaga> vagas;
  private List<SessaoRecarga> abertas;
  private Vaga g01;
  private Vaga g02;
  private Usuario moradorDo42;
  private Usuario moradorDo17;
  private Usuario sindica;

  private static LocalDateTime hora(int hora, int minuto) {
    return LocalDateTime.of(2026, 9, 8, hora, minuto);
  }

  private static Vaga vaga(Long id, String identificacao, double potencia) {
    Vaga vaga = new Vaga();
    vaga.setId(id);
    vaga.setIdentificacao(identificacao);
    vaga.setPotenciaCarregador(potencia);
    return vaga;
  }

  private static Usuario usuario(Long id, String nome, String apartamento, Perfil... perfis) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setNome(nome);
    if (apartamento != null) {
      Apartamento apto = new Apartamento();
      apto.setNumero(apartamento);
      usuario.setApartamento(apto);
    }
    for (Perfil perfil : perfis) {
      usuario.getPerfis().add(perfil);
    }
    return usuario;
  }

  private SessaoRecarga ocupar(Vaga vaga, Usuario morador, LocalDateTime inicio, LocalDateTime previsao) {
    SessaoRecarga sessao = new SessaoRecarga();
    sessao.setVaga(vaga);
    sessao.setUsuario(morador);
    sessao.setInicio(inicio);
    sessao.setPrevisaoConclusao(previsao);
    abertas.add(sessao);
    return sessao;
  }

  private PainelVagasResponse painel(Usuario usuario, LocalDateTime momento) {
    Clock relogio = Clock.fixed(momento.atZone(FUSO).toInstant(), FUSO);
    return new PainelVagasService(vagaRepository, sessaoRepository, new ApuracaoEnergiaService(), relogio, 6)
        .consultar(usuario);
  }

  private static VagaPainelResponse vagaDoPainel(PainelVagasResponse painel, String identificacao) {
    return painel.getVagas().stream()
        .filter(vaga -> vaga.getIdentificacao().equals(identificacao))
        .findFirst()
        .orElseThrow(() -> new AssertionError("A vaga " + identificacao + " não está no painel."));
  }

  @BeforeEach
  void preparar() {
    vagaRepository = mock(VagaRepository.class);
    sessaoRepository = mock(SessaoRecargaRepository.class);
    g01 = vaga(1L, "G-01", 11.0);
    g02 = vaga(2L, "G-02", 7.4);
    vagas = new ArrayList<>(List.of(g01, g02));
    abertas = new ArrayList<>();
    when(vagaRepository.findAll()).thenReturn(vagas);
    when(sessaoRepository.findBySituacao(SituacaoSessao.EM_ANDAMENTO)).thenReturn(abertas);

    moradorDo42 = usuario(42L, "Morador do 42", "42", Perfil.MORADOR);
    moradorDo17 = usuario(17L, "Morador do 17", "17", Perfil.MORADOR);
    sindica = usuario(1L, "Síndica", null, Perfil.SINDICO);
  }

  // HU-02, teste 01
  @Test
  void vagaSemSessaoFicaLivre() {
    PainelVagasResponse painel = painel(moradorDo42, hora(16, 0));

    VagaPainelResponse vaga = vagaDoPainel(painel, "G-02");
    assertEquals(EstadoVaga.LIVRE, vaga.getEstado());
    assertNull(vaga.getPrevisaoLiberacao());
    assertEquals(2, painel.getVagasLivres());
    assertNull(painel.getMensagem());
  }

  // HU-02, teste 02
  @Test
  void sessaoDentroDoPrevistoFicaOcupadaComAPrevisao() {
    ocupar(g01, moradorDo17, AS_14H00, AS_18H22);

    VagaPainelResponse vaga = vagaDoPainel(painel(moradorDo42, hora(16, 0)), "G-01");

    assertEquals(EstadoVaga.OCUPADA, vaga.getEstado());
    assertEquals(AS_14H00, vaga.getInicio());
    assertEquals(AS_18H22, vaga.getPrevisaoLiberacao());
  }

  // HU-02, teste 03
  @Test
  void sessaoAlemDaPrevisaoFicaConcluidaAindaOcupada() {
    ocupar(g01, moradorDo17, AS_14H00, AS_18H22);

    VagaPainelResponse vaga = vagaDoPainel(painel(moradorDo42, hora(19, 0)), "G-01");

    assertEquals(EstadoVaga.CONCLUIDA_AINDA_OCUPADA, vaga.getEstado());
    assertEquals(AS_18H22, vaga.getPrevisaoLiberacao());
  }

  // HU-02, teste 04; RN-05: às 6 horas a sessão é sinalizada, e o painel não a encerra
  @Test
  void sessaoAlemDaDuracaoMaximaFicaExcedidaEContinuaAberta() {
    SessaoRecarga sessao = ocupar(g01, moradorDo17, AS_14H00, AS_18H22);

    VagaPainelResponse vaga = vagaDoPainel(painel(moradorDo42, hora(20, 0)), "G-01");

    assertEquals(EstadoVaga.EXCEDIDA, vaga.getEstado());
    assertEquals(SituacaoSessao.EM_ANDAMENTO, sessao.getSituacao());
    assertNull(sessao.getFim());
    verify(sessaoRepository, never()).save(any());
  }

  @Test
  void umMinutoAntesDaDuracaoMaximaAindaNaoEExcedida() {
    ocupar(g01, moradorDo17, AS_14H00, AS_18H22);

    assertEquals(EstadoVaga.CONCLUIDA_AINDA_OCUPADA,
        vagaDoPainel(painel(moradorDo42, hora(19, 59)), "G-01").getEstado());
  }

  // HU-02, teste 05; RF-03
  @Test
  void moradorVeOEstadoEAPrevisaoSemSaberQuemOcupa() {
    ocupar(g01, moradorDo17, AS_14H00, AS_18H22);

    VagaPainelResponse vaga = vagaDoPainel(painel(moradorDo42, hora(16, 0)), "G-01");

    assertEquals(EstadoVaga.OCUPADA, vaga.getEstado());
    assertEquals(AS_18H22, vaga.getPrevisaoLiberacao());
    assertNull(vaga.getApartamento());
    assertNull(vaga.getMorador());
  }

  // RF-03: a regra vale também para a vaga que o próprio morador ocupa, que ele consulta em /sessoes/minha
  @Test
  void moradorTambemNaoRecebeAIdentificacaoDaPropriaVaga() {
    ocupar(g01, moradorDo42, AS_14H00, AS_18H22);

    VagaPainelResponse vaga = vagaDoPainel(painel(moradorDo42, hora(16, 0)), "G-01");

    assertNull(vaga.getApartamento());
    assertNull(vaga.getMorador());
  }

  // HU-02, teste 06; RF-03
  @Test
  void sindicoVeOApartamentoEONomeDeQuemOcupa() {
    ocupar(g01, moradorDo17, AS_14H00, AS_18H22);

    VagaPainelResponse vaga = vagaDoPainel(painel(sindica, hora(16, 0)), "G-01");

    assertEquals(EstadoVaga.OCUPADA, vaga.getEstado());
    assertEquals(AS_18H22, vaga.getPrevisaoLiberacao());
    assertEquals("17", vaga.getApartamento());
    assertEquals("Morador do 17", vaga.getMorador());
  }

  // HU-01, teste 04: a síndica que também mora no condomínio vê a identificação
  @Test
  void quemTemOsDoisPerfisVeAIdentificacao() {
    ocupar(g01, moradorDo17, AS_14H00, AS_18H22);
    Usuario sindicaMoradora = usuario(2L, "Síndica moradora", "42", Perfil.MORADOR, Perfil.SINDICO);

    assertEquals("17", vagaDoPainel(painel(sindicaMoradora, hora(16, 0)), "G-01").getApartamento());
  }

  // HU-02, teste 07
  @Test
  void semVagaLivreDestacaALiberacaoMaisProxima() {
    ocupar(g02, moradorDo42, hora(17, 0), hora(21, 0));
    ocupar(g01, moradorDo17, hora(15, 8), hora(19, 30));

    PainelVagasResponse painel = painel(moradorDo42, hora(18, 0));

    assertEquals(0, painel.getVagasLivres());
    assertEquals("G-01", painel.getProximaLiberacao().getIdentificacao());
    assertEquals(hora(19, 30), painel.getProximaLiberacao().getPrevisaoLiberacao());
    assertEquals("Nenhuma vaga livre. Liberação mais próxima: G-01, às 19:30.", painel.getMensagem());
  }

  @Test
  void liberacaoMaisProximaIgnoraAVagaCujaPrevisaoJaPassou() {
    ocupar(g01, moradorDo17, AS_14H00, AS_18H22);
    ocupar(g02, moradorDo42, hora(17, 0), hora(21, 0));

    PainelVagasResponse painel = painel(moradorDo42, hora(19, 0));

    assertEquals("G-02", painel.getProximaLiberacao().getIdentificacao());
    assertTrue(painel.getMensagem().contains("G-02, às 21:00"));
  }

  @Test
  void semVagaLivreNemPrevisaoAVencerNaoDestacaLiberacao() {
    ocupar(g01, moradorDo17, AS_14H00, AS_18H22);
    ocupar(g02, moradorDo42, AS_14H00, hora(17, 0));

    PainelVagasResponse painel = painel(moradorDo42, hora(19, 0));

    assertEquals(0, painel.getVagasLivres());
    assertNull(painel.getProximaLiberacao());
    assertEquals("Nenhuma vaga livre. As recargas previstas já terminaram, mas as vagas continuam ocupadas.",
        painel.getMensagem());
  }

  // HU-07, teste 04: a vaga desativada não aceita sessão, então não aparece como livre
  @Test
  void vagaDesativadaSemSessaoFicaForaDoPainel() {
    g02.setAtiva(false);

    PainelVagasResponse painel = painel(moradorDo42, hora(16, 0));

    assertEquals(List.of("G-01"), identificacoes(painel));
    assertEquals(1, painel.getVagasLivres());
  }

  @Test
  void vagaDesativadaComSessaoAbertaContinuaNoPainel() {
    g02.setAtiva(false);
    ocupar(g02, moradorDo17, AS_14H00, AS_18H22);

    assertEquals(EstadoVaga.OCUPADA, vagaDoPainel(painel(moradorDo42, hora(16, 0)), "G-02").getEstado());
  }

  @Test
  void semVagasCadastradasInformaQueNaoHa() {
    vagas.clear();

    PainelVagasResponse painel = painel(moradorDo42, hora(16, 0));

    assertTrue(painel.getVagas().isEmpty());
    assertEquals("Ainda não há vagas cadastradas.", painel.getMensagem());
    assertNull(painel.getProximaLiberacao());
  }

  @Test
  void vagasVemEmOrdemDeIdentificacao() {
    vagas.add(0, vaga(3L, "G-03", 22.0));

    assertEquals(List.of("G-01", "G-02", "G-03"), identificacoes(painel(moradorDo42, hora(16, 0))));
  }

  @Test
  void informaOMomentoDaConsulta() {
    assertEquals(hora(16, 0), painel(moradorDo42, hora(16, 0)).getConsultadoEm());
  }

  private static List<String> identificacoes(PainelVagasResponse painel) {
    return painel.getVagas().stream().map(VagaPainelResponse::getIdentificacao).collect(Collectors.toList());
  }
}
