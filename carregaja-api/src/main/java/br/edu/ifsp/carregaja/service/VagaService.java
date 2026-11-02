package br.edu.ifsp.carregaja.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import br.edu.ifsp.carregaja.model.Vaga;
import br.edu.ifsp.carregaja.repository.VagaRepository;
import br.edu.ifsp.carregaja.view.VagaRequest;

@Service
public class VagaService {

  private final VagaRepository vagaRepository;

  public VagaService(VagaRepository vagaRepository) {
    this.vagaRepository = vagaRepository;
  }

  public Iterable<Vaga> listarTodas() {
    return vagaRepository.findAll();
  }

  public Optional<Vaga> buscarPorId(Long id) {
    return vagaRepository.findById(id);
  }

  public Vaga criar(VagaRequest requisicao) {
    Vaga vaga = new Vaga();
    copiarDados(requisicao, vaga);
    return vagaRepository.save(vaga);
  }

  public Optional<Vaga> atualizar(Long id, VagaRequest requisicao) {
    return vagaRepository.findById(id).map(vaga -> {
      copiarDados(requisicao, vaga);
      return vagaRepository.save(vaga);
    });
  }

  public Optional<Vaga> desativar(Long id) {
    return alterarSituacao(id, false);
  }

  public Optional<Vaga> reativar(Long id) {
    return alterarSituacao(id, true);
  }

  public boolean remover(Long id) {
    Optional<Vaga> encontrada = vagaRepository.findById(id);
    encontrada.ifPresent(vagaRepository::delete);
    return encontrada.isPresent();
  }

  private Optional<Vaga> alterarSituacao(Long id, boolean ativa) {
    return vagaRepository.findById(id).map(vaga -> {
      vaga.setAtiva(ativa);
      return vagaRepository.save(vaga);
    });
  }

  private void copiarDados(VagaRequest requisicao, Vaga vaga) {
    String identificacao = requisicao.getIdentificacao().trim().toUpperCase(Locale.ROOT);
    vagaRepository.findByIdentificacao(identificacao)
        .filter(outra -> !outra.getId().equals(vaga.getId()))
        .ifPresent(outra -> {
          throw new RegraNegocioException(HttpStatus.CONFLICT, "Já existe a vaga " + identificacao + ".");
        });
    vaga.setIdentificacao(identificacao);
    vaga.setPotenciaCarregador(requisicao.getPotenciaCarregador());
  }
}
