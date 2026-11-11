package br.edu.ifsp.carregaja.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifsp.carregaja.model.Apartamento;
import br.edu.ifsp.carregaja.model.RecusaVaga;
import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.SituacaoSessao;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.model.Vaga;
import br.edu.ifsp.carregaja.repository.RecusaVagaRepository;
import br.edu.ifsp.carregaja.repository.SessaoRecargaRepository;
import br.edu.ifsp.carregaja.repository.VagaRepository;
import br.edu.ifsp.carregaja.view.HistoricoResponse;
import br.edu.ifsp.carregaja.view.HistoricoResponse.ConsumoApartamento;
import br.edu.ifsp.carregaja.view.HistoricoResponse.FaixaHoraria;
import br.edu.ifsp.carregaja.view.HistoricoResponse.OcupacaoVaga;
import br.edu.ifsp.carregaja.view.HistoricoResponse.RecusaPorVaga;

/**
 * Consulta de utilização (UC12). Só lê: soma o que o UC03 e o UC04 gravaram. O volume é o de um condomínio, então a
 * soma é feita aqui, sobre as sessões e recusas do período, e não em SQL.
 */
@Service
public class HistoricoService {

  private static final int HORAS_DO_DIA = 24;
  private static final double SEGUNDOS_POR_HORA = 3600.0;
  private static final String SEM_REGISTROS = "Não há registros de utilização no período.";

  private final SessaoRecargaRepository sessaoRepository;
  private final RecusaVagaRepository recusaRepository;
  private final VagaRepository vagaRepository;
  private final Clock relogio;

  public HistoricoService(SessaoRecargaRepository sessaoRepository, RecusaVagaRepository recusaRepository,
      VagaRepository vagaRepository, Clock relogio) {
    this.sessaoRepository = sessaoRepository;
    this.recusaRepository = recusaRepository;
    this.vagaRepository = vagaRepository;
    this.relogio = relogio;
  }

  /**
   * O período vai do início do dia {@code de} ao fim do dia {@code ate}, limitado ao momento atual, para o
   * percentual não ser diluído por dias que ainda não aconteceram. Sessões e recusas podem ser filtradas por vaga e
   * por apartamento (o de quem abriu a sessão ou tentou a vaga).
   */
  @Transactional(readOnly = true)
  public HistoricoResponse consultar(LocalDate de, LocalDate ate, Long vagaId, Long apartamentoId) {
    if (de == null || ate == null) {
      throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Informe o período: as datas inicial e final.");
    }
    if (de.isAfter(ate)) {
      throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser depois da final.");
    }
    LocalDateTime agora = LocalDateTime.now(relogio);
    LocalDateTime inicio = de.atStartOfDay();
    LocalDateTime fim = minimo(ate.plusDays(1).atStartOfDay(), agora);
    if (!fim.isAfter(inicio)) {
      return vazio(de, ate);
    }

    List<SessaoRecarga> sessoes = new ArrayList<>();
    for (SessaoRecarga sessao : sessaoRepository.buscarQueTocamOPeriodo(inicio, fim)) {
      if (passaNoFiltro(sessao.getVaga(), sessao.getUsuario(), vagaId, apartamentoId)) {
        sessoes.add(sessao);
      }
    }
    List<RecusaVaga> recusas = new ArrayList<>();
    for (RecusaVaga recusa : recusaRepository.buscarDoPeriodo(inicio, fim)) {
      if (passaNoFiltro(recusa.getVaga(), recusa.getUsuario(), vagaId, apartamentoId)) {
        recusas.add(recusa);
      }
    }
    if (sessoes.isEmpty() && recusas.isEmpty()) {
      return vazio(de, ate);
    }

    Ocupacao ocupacao = somarOcupacao(sessoes, inicio, fim, agora);
    long segundosDoPeriodo = Duration.between(inicio, fim).getSeconds();
    long[] recusasPorHora = new long[HORAS_DO_DIA];
    Map<String, Long> recusasPorVaga = new HashMap<>();
    for (RecusaVaga recusa : recusas) {
      recusasPorHora[recusa.getMomento().getHour()]++;
      recusasPorVaga.merge(recusa.getVaga().getIdentificacao(), 1L, Long::sum);
    }

    return new HistoricoResponse(de, ate, false, null, faixaDePico(ocupacao.segundosPorHora), recusas.size(),
        ocupacaoPorVaga(ocupacao, vagaId, segundosDoPeriodo), faixasHorarias(ocupacao.segundosPorHora, recusasPorHora),
        consumoPorApartamento(sessoes, inicio, fim), recusasPorVaga(recusasPorVaga));
  }

  private static boolean passaNoFiltro(Vaga vaga, Usuario usuario, Long vagaId, Long apartamentoId) {
    if (vagaId != null && !vagaId.equals(vaga.getId())) {
      return false;
    }
    Apartamento apartamento = usuario.getApartamento();
    return apartamentoId == null || (apartamento != null && apartamentoId.equals(apartamento.getId()));
  }

  // Sessões da mesma vaga não se sobrepõem (RN-06), então somar os trechos não passa do tempo do período.
  // A sessão aberta conta até agora; a que ultrapassou o período conta só a parte de dentro dele.
  private Ocupacao somarOcupacao(List<SessaoRecarga> sessoes, LocalDateTime inicio, LocalDateTime fim,
      LocalDateTime agora) {
    Ocupacao ocupacao = new Ocupacao();
    for (SessaoRecarga sessao : sessoes) {
      LocalDateTime de = maximo(sessao.getInicio(), inicio);
      LocalDateTime ate = minimo(sessao.getFim() != null ? sessao.getFim() : agora, fim);
      if (!ate.isAfter(de)) {
        continue;
      }
      ocupacao.segundosPorVaga.merge(sessao.getVaga().getIdentificacao(), Duration.between(de, ate).getSeconds(), Long::sum);
      // Reparte o trecho pelas horas cheias em que ele passou, somando todos os dias do período
      LocalDateTime cursor = de;
      while (cursor.isBefore(ate)) {
        LocalDateTime proxima = minimo(ate, cursor.truncatedTo(ChronoUnit.HOURS).plusHours(1));
        ocupacao.segundosPorHora[cursor.getHour()] += Duration.between(cursor, proxima).getSeconds();
        cursor = proxima;
      }
    }
    return ocupacao;
  }

  // Toda vaga ativa aparece, mesmo sem uso; a desativada só aparece se foi usada no período
  private List<OcupacaoVaga> ocupacaoPorVaga(Ocupacao ocupacao, Long vagaId, long segundosDoPeriodo) {
    Map<String, Vaga> porIdentificacao = new TreeMap<>();
    for (Vaga vaga : vagaRepository.findAll()) {
      boolean filtrada = vagaId != null && !vagaId.equals(vaga.getId());
      if (!filtrada && (vaga.isAtiva() || ocupacao.segundosPorVaga.containsKey(vaga.getIdentificacao()))) {
        porIdentificacao.put(vaga.getIdentificacao(), vaga);
      }
    }
    List<OcupacaoVaga> resultado = new ArrayList<>();
    for (Vaga vaga : porIdentificacao.values()) {
      long segundos = ocupacao.segundosPorVaga.getOrDefault(vaga.getIdentificacao(), 0L);
      resultado.add(new OcupacaoVaga(vaga.getIdentificacao(), arredondar(segundos / SEGUNDOS_POR_HORA, 2),
          arredondar(segundos * 100.0 / segundosDoPeriodo, 1)));
    }
    return resultado;
  }

  private static List<FaixaHoraria> faixasHorarias(long[] segundosPorHora, long[] recusasPorHora) {
    List<FaixaHoraria> faixas = new ArrayList<>();
    for (int hora = 0; hora < HORAS_DO_DIA; hora++) {
      faixas.add(new FaixaHoraria(hora, Math.round(segundosPorHora[hora] / 60.0), recusasPorHora[hora]));
    }
    return faixas;
  }

  // Empatando, vale a faixa mais cedo
  private static Integer faixaDePico(long[] segundosPorHora) {
    Integer pico = null;
    long maior = 0;
    for (int hora = 0; hora < HORAS_DO_DIA; hora++) {
      if (segundosPorHora[hora] > maior) {
        maior = segundosPorHora[hora];
        pico = hora;
      }
    }
    return pico;
  }

  // Consumo é o apurado das sessões encerradas e iniciadas no período, como o fechamento do mês faz (RN-08);
  // a sessão aberta ainda não tem energia apurada
  private static List<ConsumoApartamento> consumoPorApartamento(List<SessaoRecarga> sessoes, LocalDateTime inicio,
      LocalDateTime fim) {
    Map<String, Double> energia = new HashMap<>();
    Map<String, BigDecimal> valor = new HashMap<>();
    Map<String, Long> quantidade = new HashMap<>();
    for (SessaoRecarga sessao : sessoes) {
      Apartamento apartamento = sessao.getUsuario().getApartamento();
      boolean apurada = sessao.getSituacao() != SituacaoSessao.EM_ANDAMENTO && sessao.getEnergiaEstimada() != null;
      boolean iniciouNoPeriodo = !sessao.getInicio().isBefore(inicio) && sessao.getInicio().isBefore(fim);
      if (apartamento == null || !apurada || !iniciouNoPeriodo) {
        continue;
      }
      String numero = apartamento.getNumero();
      energia.merge(numero, sessao.getEnergiaEstimada(), Double::sum);
      valor.merge(numero, sessao.getValor() != null ? sessao.getValor() : BigDecimal.ZERO, BigDecimal::add);
      quantidade.merge(numero, 1L, Long::sum);
    }
    List<ConsumoApartamento> consumo = new ArrayList<>();
    energia.forEach((numero, soma) -> consumo.add(
        new ConsumoApartamento(numero, arredondar(soma, 2), valor.get(numero), quantidade.get(numero))));
    consumo.sort(Comparator.comparingDouble(ConsumoApartamento::getEnergiaKwh).reversed()
        .thenComparing(ConsumoApartamento::getApartamento));
    return consumo;
  }

  private static List<RecusaPorVaga> recusasPorVaga(Map<String, Long> recusas) {
    List<RecusaPorVaga> resultado = new ArrayList<>();
    recusas.forEach((vaga, quantidade) -> resultado.add(new RecusaPorVaga(vaga, quantidade)));
    resultado.sort(Comparator.comparing(RecusaPorVaga::getVaga));
    return resultado;
  }

  private static HistoricoResponse vazio(LocalDate de, LocalDate ate) {
    return new HistoricoResponse(de, ate, true, SEM_REGISTROS, null, 0, List.of(), List.of(), List.of(), List.of());
  }

  private static double arredondar(double valor, int casas) {
    return BigDecimal.valueOf(valor).setScale(casas, RoundingMode.HALF_UP).doubleValue();
  }

  private static LocalDateTime minimo(LocalDateTime a, LocalDateTime b) {
    return a.isBefore(b) ? a : b;
  }

  private static LocalDateTime maximo(LocalDateTime a, LocalDateTime b) {
    return a.isAfter(b) ? a : b;
  }

  private static class Ocupacao {
    private final Map<String, Long> segundosPorVaga = new HashMap<>();
    private final long[] segundosPorHora = new long[HORAS_DO_DIA];
  }
}
