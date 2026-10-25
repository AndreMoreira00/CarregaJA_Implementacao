package br.edu.ifsp.carregaja.manterveiculo.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import br.edu.ifsp.carregaja.manterveiculo.model.Veiculo;
import br.edu.ifsp.carregaja.manterveiculo.repository.VeiculoRepository;
import br.edu.ifsp.carregaja.manterveiculo.view.VeiculoRequest;

@Service
public class VeiculoService {

  private final VeiculoRepository veiculoRepository;

  public VeiculoService(VeiculoRepository veiculoRepository) {
    this.veiculoRepository = veiculoRepository;
  }

  public Veiculo criar(VeiculoRequest requisicao) {
    Veiculo veiculo = new Veiculo();
    copiarDados(requisicao, veiculo);
    return veiculoRepository.save(veiculo);
  }

  public Iterable<Veiculo> listarTodos() {
    return veiculoRepository.findAll();
  }

  public Optional<Veiculo> buscarPorId(Long id) {
    return veiculoRepository.findById(id);
  }

  public Optional<Veiculo> atualizar(Long id, VeiculoRequest requisicao) {
    return veiculoRepository.findById(id).map(veiculo -> {
      copiarDados(requisicao, veiculo);
      return veiculoRepository.save(veiculo);
    });
  }

  private void copiarDados(VeiculoRequest requisicao, Veiculo veiculo) {
    veiculo.setModelo(requisicao.getModelo());
    veiculo.setCapacidadeBateria(requisicao.getCapacidadeBateria());
    veiculo.setPotenciaMaxima(requisicao.getPotenciaMaxima());
  }
}
