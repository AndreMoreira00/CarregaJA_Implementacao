package br.edu.ifsp.carregaja.service.fecharmes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import br.edu.ifsp.carregaja.model.compartilhado.SessaoRecarga;
import br.edu.ifsp.carregaja.model.compartilhado.SituacaoSessao;
import br.edu.ifsp.carregaja.service.RegraNegocioException;

@Service
public class RateioService {

  private static final int CASAS_DA_ENERGIA = 2;

  public LocalDateTime inicioDoMes(Integer ano, Integer mes) {
    if (ano == null || mes == null) {
      throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Informe o mês e o ano.");
    }
    if (mes < 1 || mes > 12) {
      throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "O mês deve estar entre 1 e 12.");
    }
    return LocalDate.of(ano, mes, 1).atStartOfDay();
  }

  public List<SessaoRecarga> apuradasDoMes(List<SessaoRecarga> sessoes, LocalDateTime inicioDoMes) {
    LocalDateTime fimDoMes = inicioDoMes.plusMonths(1);
    List<SessaoRecarga> apuradas = new ArrayList<>();
    for (SessaoRecarga sessao : sessoes) {
      boolean apurada = sessao.getSituacao() != SituacaoSessao.EM_ANDAMENTO && sessao.getEnergiaEstimada() != null;
      boolean iniciouNoMes = !sessao.getInicio().isBefore(inicioDoMes) && sessao.getInicio().isBefore(fimDoMes);
      if (apurada && iniciouNoMes && sessao.getUsuario().getApartamento() != null) {
        apuradas.add(sessao);
      }
    }
    return apuradas;
  }

  public double somarEnergia(List<SessaoRecarga> sessoes) {
    double soma = 0;
    for (SessaoRecarga sessao : sessoes) {
      soma += sessao.getEnergiaEstimada();
    }
    return BigDecimal.valueOf(soma).setScale(CASAS_DA_ENERGIA, RoundingMode.HALF_UP).doubleValue();
  }

  public BigDecimal somarValor(List<SessaoRecarga> sessoes) {
    BigDecimal soma = BigDecimal.ZERO;
    for (SessaoRecarga sessao : sessoes) {
      soma = soma.add(sessao.getValor() != null ? sessao.getValor() : BigDecimal.ZERO);
    }
    return soma;
  }

  public Map<String, List<SessaoRecarga>> separarPorApartamento(List<SessaoRecarga> sessoes) {
    Map<String, List<SessaoRecarga>> porApartamento = new TreeMap<>();
    for (SessaoRecarga sessao : sessoes) {
      String numero = sessao.getUsuario().getApartamento().getNumero();
      List<SessaoRecarga> doApartamento = porApartamento.get(numero);
      if (doApartamento == null) {
        doApartamento = new ArrayList<>();
        porApartamento.put(numero, doApartamento);
      }
      doApartamento.add(sessao);
    }
    return porApartamento;
  }
}
