package br.edu.ifsp.carregaja.controller;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import br.edu.ifsp.carregaja.model.Veiculo;
import br.edu.ifsp.carregaja.service.ApiException;
import br.edu.ifsp.carregaja.service.VeiculoRestClient;

import lombok.Getter;
import lombok.Setter;

@Named
@ViewScoped
public class ManterVeiculoBean extends BeanBase {

  private static final long serialVersionUID = 1L;
  @Inject
  private VeiculoRestClient veiculoRestClient;
  @Getter
  @Setter
  private Veiculo veiculo = new Veiculo();

  public void inicializar() {
    if (FacesContext.getCurrentInstance().isPostback()) {
      return;
    }
    try {
      veiculo = veiculoRestClient.buscarMeu().orElseGet(Veiculo::new);
    } catch (ApiException e) {
      reclamar(e);
    }
  }

  public void salvar() {
    try {
      if (veiculo.getId() == null) {
        veiculo = veiculoRestClient.criar(veiculo);
        informar("Veículo cadastrado com sucesso.");
      } else {
        veiculo = veiculoRestClient.atualizar(veiculo);
        informar("Veículo alterado com sucesso.");
      }
    } catch (ApiException e) {
      reclamar(e);
    }
  }
}
