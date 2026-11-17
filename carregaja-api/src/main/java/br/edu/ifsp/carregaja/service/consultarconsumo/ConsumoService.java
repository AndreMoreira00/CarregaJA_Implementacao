package br.edu.ifsp.carregaja.service.consultarconsumo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.edu.ifsp.carregaja.model.compartilhado.Apartamento;
import br.edu.ifsp.carregaja.model.compartilhado.SessaoRecarga;
import br.edu.ifsp.carregaja.model.compartilhado.Usuario;
import br.edu.ifsp.carregaja.repository.compartilhado.SessaoRecargaRepository;
import br.edu.ifsp.carregaja.service.fecharmes.RateioService;
import br.edu.ifsp.carregaja.view.consultarconsumo.ConsumoResponse;
import br.edu.ifsp.carregaja.view.consultarconsumo.ConsumoResponse.SessaoDoMes;

@Service
public class ConsumoService {

  private static final String SEM_CONSUMO = "Não há consumo registrado no mês.";

  private final SessaoRecargaRepository sessaoRepository;
  private final RateioService rateioService;

  public ConsumoService(SessaoRecargaRepository sessaoRepository, RateioService rateioService) {
    this.sessaoRepository = sessaoRepository;
    this.rateioService = rateioService;
  }

  public ConsumoResponse consultarDoMes(Usuario usuario, Integer ano, Integer mes) {
    LocalDateTime inicioDoMes = rateioService.inicioDoMes(ano, mes);
    Apartamento apartamento = usuario.getApartamento();

    List<SessaoRecarga> doApartamento = sessaoRepository
        .findByUsuarioApartamentoIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(apartamento.getId(),
            inicioDoMes, inicioDoMes.plusMonths(1));
    List<SessaoRecarga> apuradas = rateioService.apuradasDoMes(doApartamento, inicioDoMes);

    List<SessaoDoMes> linhas = new ArrayList<>();
    for (SessaoRecarga sessao : apuradas) {
      linhas.add(new SessaoDoMes(sessao.getVaga().getIdentificacao(), sessao.getInicio(), sessao.getFim(),
          sessao.getEnergiaEstimada(), sessao.getValor(), sessao.getSituacao()));
    }

    boolean semRegistros = apuradas.isEmpty();
    String mensagem = semRegistros ? SEM_CONSUMO : null;
    return new ConsumoResponse(apartamento.getNumero(), ano, mes, semRegistros, mensagem,
        rateioService.somarEnergia(apuradas), rateioService.somarValor(apuradas), linhas);
  }
}
