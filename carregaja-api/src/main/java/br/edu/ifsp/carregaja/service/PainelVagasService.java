package br.edu.ifsp.carregaja.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.edu.ifsp.carregaja.model.EstadoSessao;
import br.edu.ifsp.carregaja.model.EstadoVaga;
import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.SituacaoSessao;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.model.Vaga;
import br.edu.ifsp.carregaja.repository.SessaoRecargaRepository;
import br.edu.ifsp.carregaja.repository.VagaRepository;
import br.edu.ifsp.carregaja.view.PainelVagasResponse;
import br.edu.ifsp.carregaja.view.VagaPainelResponse;

/** UC02: a situação de cada vaga na hora da consulta. Só lê: sinalizar a sessão como excedida não a encerra (RN-05). */
@Service
public class PainelVagasService {

  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

  private final VagaRepository vagaRepository;
  private final SessaoRecargaRepository sessaoRepository;
  private final ApuracaoEnergiaService apuracao;
  private final Clock relogio;
  private final Duration duracaoMaxima;

  public PainelVagasService(VagaRepository vagaRepository, SessaoRecargaRepository sessaoRepository,
      ApuracaoEnergiaService apuracao, Clock relogio,
      @Value("${carregaja.sessao.duracao-maxima-horas}") long duracaoMaximaHoras) {
    this.vagaRepository = vagaRepository;
    this.sessaoRepository = sessaoRepository;
    this.apuracao = apuracao;
    this.relogio = relogio;
    this.duracaoMaxima = Duration.ofHours(duracaoMaximaHoras);
  }

  public PainelVagasResponse consultar(Usuario usuario) {
    LocalDateTime agora = LocalDateTime.now(relogio);
    // RF-03: quem tem o perfil de síndico vê quem ocupa a vaga, mesmo que também seja morador
    boolean identificar = usuario.temPerfil(Perfil.SINDICO);
    Map<Long, SessaoRecarga> abertas = sessoesAbertasPorVaga();

    List<VagaPainelResponse> vagas = new ArrayList<>();
    for (Vaga vaga : vagaRepository.findAll()) {
      SessaoRecarga sessao = abertas.get(vaga.getId());
      if (sessao != null) {
        vagas.add(VagaPainelResponse.ocupada(vaga, sessao, estado(sessao, agora), identificar));
      } else if (vaga.isAtiva()) {
        vagas.add(VagaPainelResponse.livre(vaga));
      }
      // Vaga desativada e sem sessão fica fora do painel: não aceita recarga, então não pode aparecer como livre
    }
    vagas.sort(Comparator.comparing(VagaPainelResponse::getIdentificacao));

    long livres = vagas.stream().filter(vaga -> vaga.getEstado() == EstadoVaga.LIVRE).count();
    if (livres > 0) {
      return new PainelVagasResponse(agora, livres, null, null, vagas);
    }
    Optional<VagaPainelResponse> proxima = proximaLiberacao(vagas);
    return new PainelVagasResponse(agora, livres, mensagemSemVagaLivre(vagas, proxima), proxima.orElse(null), vagas);
  }

  private Map<Long, SessaoRecarga> sessoesAbertasPorVaga() {
    Map<Long, SessaoRecarga> abertas = new HashMap<>();
    for (SessaoRecarga sessao : sessaoRepository.findBySituacao(SituacaoSessao.EM_ANDAMENTO)) {
      abertas.put(sessao.getVaga().getId(), sessao);
    }
    return abertas;
  }

  private EstadoVaga estado(SessaoRecarga sessao, LocalDateTime agora) {
    EstadoSessao estado = apuracao.estado(sessao, agora, duracaoMaxima);
    if (estado == EstadoSessao.EXCEDIDA) {
      return EstadoVaga.EXCEDIDA;
    }
    return estado == EstadoSessao.CONCLUIDA_AINDA_OCUPADA ? EstadoVaga.CONCLUIDA_AINDA_OCUPADA : EstadoVaga.OCUPADA;
  }

  // Só a vaga ainda dentro do previsto tem liberação a esperar: nas demais a previsão já passou
  private Optional<VagaPainelResponse> proximaLiberacao(List<VagaPainelResponse> vagas) {
    return vagas.stream()
        .filter(vaga -> vaga.getEstado() == EstadoVaga.OCUPADA)
        .min(Comparator.comparing(VagaPainelResponse::getPrevisaoLiberacao));
  }

  private String mensagemSemVagaLivre(List<VagaPainelResponse> vagas, Optional<VagaPainelResponse> proxima) {
    if (vagas.isEmpty()) {
      return "Ainda não há vagas cadastradas.";
    }
    return proxima
        .map(vaga -> "Nenhuma vaga livre. Liberação mais próxima: " + vaga.getIdentificacao() + ", às "
            + vaga.getPrevisaoLiberacao().format(HORA) + ".")
        .orElse("Nenhuma vaga livre. As recargas previstas já terminaram, mas as vagas continuam ocupadas.");
  }
}
