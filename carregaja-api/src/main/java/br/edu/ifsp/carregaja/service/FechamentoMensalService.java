package br.edu.ifsp.carregaja.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifsp.carregaja.model.FechamentoMensal;
import br.edu.ifsp.carregaja.model.ItemRateio;
import br.edu.ifsp.carregaja.model.SessaoRecarga;
import br.edu.ifsp.carregaja.model.SituacaoSessao;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.repository.FechamentoMensalRepository;
import br.edu.ifsp.carregaja.repository.ItemRateioRepository;
import br.edu.ifsp.carregaja.repository.SessaoRecargaRepository;
import br.edu.ifsp.carregaja.view.FechamentoMensalRequest;
import br.edu.ifsp.carregaja.view.FechamentoMensalResponse;
import br.edu.ifsp.carregaja.view.FechamentoMensalResponse.Item;

@Service
public class FechamentoMensalService {

  private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final FechamentoMensalRepository fechamentoRepository;
  private final ItemRateioRepository itemRepository;
  private final SessaoRecargaRepository sessaoRepository;
  private final RateioService rateioService;
  private final Clock relogio;

  public FechamentoMensalService(FechamentoMensalRepository fechamentoRepository, ItemRateioRepository itemRepository,
      SessaoRecargaRepository sessaoRepository, RateioService rateioService, Clock relogio) {
    this.fechamentoRepository = fechamentoRepository;
    this.itemRepository = itemRepository;
    this.sessaoRepository = sessaoRepository;
    this.rateioService = rateioService;
    this.relogio = relogio;
  }

  @Transactional
  public FechamentoMensalResponse fechar(Usuario responsavel, FechamentoMensalRequest requisicao) {
    LocalDateTime inicio = rateioService.inicioDoMes(requisicao.getAno(), requisicao.getMes());
    if (fechamentoRepository.existsByAnoAndMes(requisicao.getAno(), requisicao.getMes())) {
      throw new RegraNegocioException(HttpStatus.CONFLICT, "Este mês já foi fechado.");
    }
    if (LocalDateTime.now(relogio).isBefore(inicio.plusMonths(1))) {
      throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "O mês ainda não terminou.");
    }
    List<SessaoRecarga> sessoes = sessaoRepository.findByInicioGreaterThanEqualAndInicioLessThan(inicio,
        inicio.plusMonths(1));
    exigirSemSessaoAberta(sessoes);
    List<SessaoRecarga> apuradas = rateioService.apuradasDoMes(sessoes, inicio);
    if (apuradas.isEmpty()) {
      throw new RegraNegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
          "Não há sessão encerrada no mês: não há rateio a fazer.");
    }

    FechamentoMensal fechamento = new FechamentoMensal();
    fechamento.setAno(requisicao.getAno());
    fechamento.setMes(requisicao.getMes());
    fechamento.setValorTotal(rateioService.somarValor(apuradas));
    fechamento.setMomento(LocalDateTime.now(relogio));
    fechamento.setResponsavel(responsavel);
    fechamento = fechamentoRepository.save(fechamento);

    List<ItemRateio> itens = new ArrayList<>();
    for (List<SessaoRecarga> doApartamento : rateioService.separarPorApartamento(apuradas).values()) {
      ItemRateio item = new ItemRateio();
      item.setFechamentoMensal(fechamento);
      item.setApartamento(doApartamento.get(0).getUsuario().getApartamento());
      item.setEnergia(rateioService.somarEnergia(doApartamento));
      item.setValor(rateioService.somarValor(doApartamento));
      itens.add(itemRepository.save(item));
    }
    return responder(fechamento, itens);
  }

  public Optional<FechamentoMensalResponse> buscarPorId(Long id) {
    return fechamentoRepository.findById(id)
        .map(fechamento -> responder(fechamento, itemRepository.findByFechamentoMensalId(fechamento.getId())));
  }

  private void exigirSemSessaoAberta(List<SessaoRecarga> sessoes) {
    String abertas = "";
    for (SessaoRecarga sessao : sessoes) {
      if (sessao.getSituacao() == SituacaoSessao.EM_ANDAMENTO) {
        if (!abertas.isEmpty()) {
          abertas += "; ";
        }
        abertas += "vaga " + sessao.getVaga().getIdentificacao() + ", apto "
            + sessao.getUsuario().getApartamento().getNumero() + ", início " + sessao.getInicio().format(DATA_HORA);
      }
    }
    if (!abertas.isEmpty()) {
      throw new RegraNegocioException(HttpStatus.CONFLICT, "Há sessões em aberto no mês: " + abertas + ".");
    }
  }

  private FechamentoMensalResponse responder(FechamentoMensal fechamento, List<ItemRateio> itens) {
    List<ItemRateio> ordenados = new ArrayList<>(itens);
    ordenados.sort(Comparator.comparingDouble(ItemRateio::getEnergia).reversed()
        .thenComparing(item -> item.getApartamento().getNumero()));
    double energiaTotal = 0;
    List<Item> linhas = new ArrayList<>();
    for (ItemRateio item : ordenados) {
      energiaTotal += item.getEnergia();
      linhas.add(new Item(item.getApartamento().getNumero(), item.getEnergia(), item.getValor()));
    }
    return new FechamentoMensalResponse(fechamento.getId(), fechamento.getAno(), fechamento.getMes(),
        fechamento.getMomento(), fechamento.getNomeResponsavel(),
        BigDecimal.valueOf(energiaTotal).setScale(2, RoundingMode.HALF_UP).doubleValue(), fechamento.getValorTotal(),
        linhas);
  }
}
