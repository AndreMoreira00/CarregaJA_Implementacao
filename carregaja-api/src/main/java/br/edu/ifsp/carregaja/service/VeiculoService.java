package br.edu.ifsp.carregaja.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.model.Veiculo;
import br.edu.ifsp.carregaja.repository.VeiculoRepository;
import br.edu.ifsp.carregaja.view.VeiculoRequest;

@Service
public class VeiculoService {

  private final VeiculoRepository veiculoRepository;

  public VeiculoService(VeiculoRepository veiculoRepository) {
    this.veiculoRepository = veiculoRepository;
  }

  public Veiculo criar(Usuario usuario, VeiculoRequest requisicao) {
    if (veiculoRepository.existsByUsuarioId(usuario.getId())) {
      throw new RegraNegocioException(HttpStatus.CONFLICT, "Você já tem um veículo cadastrado. Altere o existente.");
    }
    Veiculo veiculo = new Veiculo();
    veiculo.setUsuario(usuario);
    copiarDados(requisicao, veiculo);
    return veiculoRepository.save(veiculo);
  }

  public Iterable<Veiculo> listarTodos() {
    return veiculoRepository.findAll();
  }

  public Optional<Veiculo> buscarDoUsuario(Usuario usuario) {
    return veiculoRepository.findByUsuarioId(usuario.getId());
  }

  // O síndico consulta qualquer veículo para conferir o cadastro (RI-01); o morador consulta só o próprio
  public Optional<Veiculo> buscarPorId(Usuario usuario, Long id) {
    Optional<Veiculo> veiculo = veiculoRepository.findById(id);
    if (!usuario.temPerfil(Perfil.SINDICO)) {
      veiculo.ifPresent(encontrado -> exigirDono(usuario, encontrado));
    }
    return veiculo;
  }

  public Optional<Veiculo> atualizar(Usuario usuario, Long id, VeiculoRequest requisicao) {
    return veiculoRepository.findById(id).map(veiculo -> {
      exigirDono(usuario, veiculo);
      copiarDados(requisicao, veiculo);
      return veiculoRepository.save(veiculo);
    });
  }

  private void exigirDono(Usuario usuario, Veiculo veiculo) {
    if (!veiculo.getUsuario().getId().equals(usuario.getId())) {
      throw new RegraNegocioException(HttpStatus.FORBIDDEN, "Este veículo é de outro morador.");
    }
  }

  private void copiarDados(VeiculoRequest requisicao, Veiculo veiculo) {
    veiculo.setModelo(requisicao.getModelo());
    veiculo.setCapacidadeBateria(requisicao.getCapacidadeBateria());
    veiculo.setPotenciaMaxima(requisicao.getPotenciaMaxima());
  }
}
