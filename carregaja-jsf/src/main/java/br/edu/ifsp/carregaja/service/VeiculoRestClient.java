package br.edu.ifsp.carregaja.service;

import java.io.Serializable;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;

import br.edu.ifsp.carregaja.model.Veiculo;
import br.edu.ifsp.carregaja.model.VeiculoRequest;

@ApplicationScoped
public class VeiculoRestClient implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final String CAMINHO = "/veiculos";

  @Inject
  private ApiClient api;

  public Optional<Veiculo> buscarMeu() throws ApiException {
    return api.getOpcional(CAMINHO + "/meu", new TypeReference<Veiculo>() {
    }, "carregar o veículo");
  }

  public Veiculo criar(Veiculo veiculo) throws ApiException {
    return api.post(CAMINHO, VeiculoRequest.de(veiculo), new TypeReference<Veiculo>() {
    }, "cadastrar o veículo");
  }

  public Veiculo atualizar(Veiculo veiculo) throws ApiException {
    return api.put(CAMINHO + "/" + veiculo.getId(), VeiculoRequest.de(veiculo), new TypeReference<Veiculo>() {
    }, "alterar o veículo");
  }
}
