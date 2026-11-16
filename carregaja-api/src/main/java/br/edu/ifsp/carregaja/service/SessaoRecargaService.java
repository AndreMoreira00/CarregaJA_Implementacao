package br.edu.ifsp.carregaja.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifsp.carregaja.model.RecusaVaga;
import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.SituacaoSessao;
import br.edu.ifsp.carregaja.model.Tarifa;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.model.Vaga;
import br.edu.ifsp.carregaja.model.Veiculo;
import br.edu.ifsp.carregaja.repository.RecusaVagaRepository;
import br.edu.ifsp.carregaja.repository.SessaoRecargaRepository;
import br.edu.ifsp.carregaja.repository.UsuarioRepository;
import br.edu.ifsp.carregaja.repository.VagaRepository;
import br.edu.ifsp.carregaja.repository.VeiculoRepository;
import br.edu.ifsp.carregaja.view.SessaoRequest;
import br.edu.ifsp.carregaja.view.SessaoResponse;

@Service
public class SessaoRecargaService {

  private static final int BATERIA_CHEIA = 100;
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

  private final SessaoRecargaRepository sessaoRepository;
  private final RecusaVagaRepository recusaRepository;
  private final UsuarioRepository usuarioRepository;
  private final VagaRepository vagaRepository;
  private final VeiculoRepository veiculoRepository;
  private final TarifaService tarifaService;
  private final ApuracaoEnergiaService apuracao;
  private final Clock relogio;
  private final Duration duracaoMaxima;

  public SessaoRecargaService(SessaoRecargaRepository sessaoRepository, RecusaVagaRepository recusaRepository,
      UsuarioRepository usuarioRepository, VagaRepository vagaRepository, VeiculoRepository veiculoRepository,
      TarifaService tarifaService, ApuracaoEnergiaService apuracao, Clock relogio,
      @Value("${carregaja.sessao.duracao-maxima-horas}") long duracaoMaximaHoras) {
    this.sessaoRepository = sessaoRepository;
    this.recusaRepository = recusaRepository;
    this.usuarioRepository = usuarioRepository;
    this.vagaRepository = vagaRepository;
    this.veiculoRepository = veiculoRepository;
    this.tarifaService = tarifaService;
    this.apuracao = apuracao;
    this.relogio = relogio;
    this.duracaoMaxima = Duration.ofHours(duracaoMaximaHoras);
  }

  public Optional<SessaoResponse> buscarAberta(Usuario usuario) {
    return sessaoRepository.findByUsuarioIdAndSituacao(usuario.getId(), SituacaoSessao.EM_ANDAMENTO)
        .map(this::responder);
  }

  // As travas, sempre na mesma ordem, fazem dois pedidos simultâneos passarem um de cada vez: o primeiro
  // abre a sessão e o segundo já a encontra (RN-06). READ_COMMITTED é obrigatório: no padrão do MySQL
  // (REPEATABLE_READ) o segundo, mesmo depois de esperar a trava, leria a foto do banco anterior à sessão do
  // primeiro, e os dois abririam sessão na mesma vaga. A recusa precisa ser gravada mesmo com a exceção
  @Transactional(isolation = Isolation.READ_COMMITTED, noRollbackFor = RegraNegocioException.class)
  public SessaoResponse iniciar(Usuario usuario, SessaoRequest requisicao) {
    Usuario morador = usuarioRepository.findComTravaById(usuario.getId())
        .orElseThrow(() -> recusa(HttpStatus.NOT_FOUND, "Morador não encontrado."));
    Vaga vaga = vagaRepository.findComTravaById(requisicao.getVagaId())
        .orElseThrow(() -> recusa(HttpStatus.NOT_FOUND, "Vaga não encontrada."));
    LocalDateTime agora = LocalDateTime.now(relogio);

    exigirMoradorSemSessaoAberta(morador);
    Veiculo veiculo = veiculoRepository.findByUsuarioId(morador.getId())
        .orElseThrow(() -> recusa(HttpStatus.UNPROCESSABLE_ENTITY, "Cadastre seu veículo antes de iniciar uma recarga."));
    exigirBateriaNaoCheia(requisicao.getNivelBateria());
    exigirVagaDisponivel(vaga, morador, agora);
    Tarifa tarifa = tarifaService.buscarVigenteEm(agora)
        .orElseThrow(() -> recusa(HttpStatus.CONFLICT, "Ainda não há tarifa cadastrada. Procure o síndico."));

    return responder(sessaoRepository.save(montar(morador, vaga, veiculo, tarifa, requisicao.getNivelBateria(), agora)));
  }

  private void exigirMoradorSemSessaoAberta(Usuario morador) {
    if (sessaoRepository.findByUsuarioIdAndSituacao(morador.getId(), SituacaoSessao.EM_ANDAMENTO).isPresent()) {
      throw recusa(HttpStatus.CONFLICT, "Você já tem uma sessão de recarga aberta.");
    }
  }

  private void exigirBateriaNaoCheia(int nivelBateria) {
    if (nivelBateria >= BATERIA_CHEIA) {
      throw recusa(HttpStatus.UNPROCESSABLE_ENTITY, "A bateria já está cheia: não há recarga a fazer.");
    }
  }

  private void exigirVagaDisponivel(Vaga vaga, Usuario morador, LocalDateTime agora) {
    if (!vaga.isAtiva()) {
      throw recusa(HttpStatus.CONFLICT, "A vaga " + vaga.getIdentificacao() + " está desativada.");
    }
    sessaoRepository.findByVagaIdAndSituacao(vaga.getId(), SituacaoSessao.EM_ANDAMENTO).ifPresent(ocupante -> {
      registrarRecusa(vaga, morador, agora);
      throw recusa(HttpStatus.CONFLICT, "A vaga " + vaga.getIdentificacao() + " está ocupada. Previsão de liberação: "
          + ocupante.getPrevisaoConclusao().format(HORA) + ".");
    });
  }

  private void registrarRecusa(Vaga vaga, Usuario morador, LocalDateTime agora) {
    RecusaVaga recusa = new RecusaVaga();
    recusa.setVaga(vaga);
    recusa.setUsuario(morador);
    recusa.setMomento(agora);
    recusaRepository.save(recusa);
  }

  private SessaoRecarga montar(Usuario morador, Vaga vaga, Veiculo veiculo, Tarifa tarifa, int nivelBateria,
      LocalDateTime inicio) {
    double potencia = apuracao.potenciaEfetiva(vaga.getPotenciaCarregador(), veiculo.getPotenciaMaxima());
    double energia = apuracao.energiaARepor(veiculo.getCapacidadeBateria(), nivelBateria);

    SessaoRecarga sessao = new SessaoRecarga();
    sessao.setUsuario(morador);
    sessao.setVaga(vaga);
    sessao.setVeiculo(veiculo);
    sessao.setTarifa(tarifa);
    sessao.setInicio(inicio);
    sessao.setNivelBateriaInicial(nivelBateria);
    sessao.setPotenciaEfetiva(potencia);
    sessao.setEnergiaARepor(energia);
    sessao.setPrevisaoConclusao(inicio.plus(apuracao.tempoEstimado(energia, potencia)));
    return sessao;
  }

  private SessaoResponse responder(SessaoRecarga sessao) {
    return new SessaoResponse(sessao, apuracao.estado(sessao, LocalDateTime.now(relogio), duracaoMaxima));
  }

  private static RegraNegocioException recusa(HttpStatus status, String mensagem) {
    return new RegraNegocioException(status, mensagem);
  }
}
