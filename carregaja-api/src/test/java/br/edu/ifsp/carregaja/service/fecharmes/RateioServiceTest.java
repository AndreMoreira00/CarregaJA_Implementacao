package br.edu.ifsp.carregaja.service.fecharmes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import br.edu.ifsp.carregaja.model.compartilhado.Apartamento;
import br.edu.ifsp.carregaja.model.compartilhado.SessaoRecarga;
import br.edu.ifsp.carregaja.model.compartilhado.SituacaoSessao;
import br.edu.ifsp.carregaja.model.compartilhado.Tarifa;
import br.edu.ifsp.carregaja.model.compartilhado.Usuario;
import br.edu.ifsp.carregaja.service.RegraNegocioException;

class RateioServiceTest {

  private static final LocalDateTime SETEMBRO = LocalDateTime.of(2026, 9, 1, 0, 0);

  private final RateioService service = new RateioService();

  private static SessaoRecarga sessao(String numero, LocalDateTime inicio, Double energia, String valor,
      SituacaoSessao situacao) {
    Apartamento apartamento = new Apartamento();
    apartamento.setNumero(numero);
    Usuario usuario = new Usuario();
    usuario.setApartamento(apartamento);
    Tarifa tarifa = new Tarifa();
    tarifa.setValorKwh(new BigDecimal("0.92"));
    SessaoRecarga sessao = new SessaoRecarga();
    sessao.setUsuario(usuario);
    sessao.setTarifa(tarifa);
    sessao.setInicio(inicio);
    sessao.setEnergiaEstimada(energia);
    sessao.setValor(valor == null ? null : new BigDecimal(valor));
    sessao.setSituacao(situacao);
    return sessao;
  }

  private static SessaoRecarga encerrada(String numero, int dia, double energia, String valor) {
    return sessao(numero, LocalDateTime.of(2026, 9, dia, 18, 10), energia, valor, SituacaoSessao.ENCERRADA);
  }

  // HU-06, teste 01
  @Test
  void somaAsSessoesDoApartamentoNoMes() {
    List<SessaoRecarga> sessoes = new ArrayList<>();
    sessoes.add(encerrada("42", 3, 28.0, "25.76"));
    sessoes.add(encerrada("42", 10, 24.5, "22.54"));
    sessoes.add(encerrada("42", 17, 23.5, "21.62"));

    List<SessaoRecarga> apuradas = service.apuradasDoMes(sessoes, SETEMBRO);

    assertEquals(76.0, service.somarEnergia(apuradas));
    assertEquals(new BigDecimal("69.92"), service.somarValor(apuradas));
  }

  // HU-11, teste 02
  @Test
  void agrupaAsSessoesDoMesmoApartamentoNumaLinha() {
    List<SessaoRecarga> sessoes = new ArrayList<>();
    sessoes.add(encerrada("17", 4, 48.0, "44.16"));
    sessoes.add(encerrada("17", 12, 33.0, "30.36"));

    Map<String, List<SessaoRecarga>> porApartamento = service.separarPorApartamento(sessoes);

    assertEquals(1, porApartamento.size());
    assertEquals(2, porApartamento.get("17").size());
    assertEquals(81.0, service.somarEnergia(porApartamento.get("17")));
    assertEquals(new BigDecimal("74.52"), service.somarValor(porApartamento.get("17")));
  }

  @Test
  void sessaoEmAndamentoNaoEntraNaSoma() {
    List<SessaoRecarga> sessoes = new ArrayList<>();
    sessoes.add(encerrada("42", 3, 28.0, "25.76"));
    sessoes.add(sessao("42", LocalDateTime.of(2026, 9, 20, 18, 10), null, null, SituacaoSessao.EM_ANDAMENTO));

    List<SessaoRecarga> apuradas = service.apuradasDoMes(sessoes, SETEMBRO);

    assertEquals(1, apuradas.size());
    assertEquals(28.0, service.somarEnergia(apuradas));
  }

  @Test
  void sessaoDeOutroMesNaoEntra() {
    List<SessaoRecarga> sessoes = new ArrayList<>();
    sessoes.add(encerrada("42", 3, 28.0, "25.76"));
    sessoes.add(sessao("42", LocalDateTime.of(2026, 8, 31, 22, 0), 10.0, "9.20", SituacaoSessao.ENCERRADA));
    sessoes.add(sessao("42", LocalDateTime.of(2026, 10, 1, 0, 0), 10.0, "9.20", SituacaoSessao.ENCERRADA));

    List<SessaoRecarga> apuradas = service.apuradasDoMes(sessoes, SETEMBRO);

    assertEquals(1, apuradas.size());
    assertEquals(new BigDecimal("25.76"), service.somarValor(apuradas));
  }

  @Test
  void somaOValorGravadoENaoRecalculaPelaTarifa() {
    SessaoRecarga sessao = encerrada("42", 3, 28.0, "25.76");
    sessao.getTarifa().setValorKwh(new BigDecimal("1.05"));

    assertEquals(new BigDecimal("25.76"), service.somarValor(List.of(sessao)));
  }

  @Test
  void mesForaDe1a12ERecusado() {
    RegraNegocioException zero = assertThrows(RegraNegocioException.class, () -> service.inicioDoMes(2026, 0));
    RegraNegocioException treze = assertThrows(RegraNegocioException.class, () -> service.inicioDoMes(2026, 13));

    assertEquals(HttpStatus.BAD_REQUEST, zero.getStatus());
    assertEquals(HttpStatus.BAD_REQUEST, treze.getStatus());
  }
}
