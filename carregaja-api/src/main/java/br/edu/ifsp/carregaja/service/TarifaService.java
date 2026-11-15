package br.edu.ifsp.carregaja.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.edu.ifsp.carregaja.model.Tarifa;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.repository.TarifaRepository;
import br.edu.ifsp.carregaja.view.TarifaRequest;

@Service
public class TarifaService {

  private final TarifaRepository tarifaRepository;
  private final Clock relogio;

  public TarifaService(TarifaRepository tarifaRepository, Clock relogio) {
    this.tarifaRepository = tarifaRepository;
    this.relogio = relogio;
  }

  // A anterior não é alterada: ela vale até o início desta, e continua consultável (RN-10)
  public Tarifa criar(Usuario responsavel, TarifaRequest requisicao) {
    Tarifa tarifa = new Tarifa();
    tarifa.setValorKwh(requisicao.getValorKwh());
    tarifa.setInicioVigencia(LocalDateTime.now(relogio));
    tarifa.setResponsavel(responsavel);
    return tarifaRepository.save(tarifa);
  }

  public List<Tarifa> listarTodas() {
    return tarifaRepository.findAllByOrderByInicioVigenciaDescIdDesc();
  }

  public Optional<Tarifa> buscarVigente() {
    return buscarVigenteEm(LocalDateTime.now(relogio));
  }

  public Optional<Tarifa> buscarVigenteEm(LocalDateTime momento) {
    return tarifaRepository.findFirstByInicioVigenciaLessThanEqualOrderByInicioVigenciaDescIdDesc(momento);
  }
}
