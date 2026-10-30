package br.edu.ifsp.carregaja.controller;

import java.io.Serializable;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import br.edu.ifsp.carregaja.model.Veiculo;
import br.edu.ifsp.carregaja.service.VeiculoRestClient;
import br.edu.ifsp.carregaja.service.VeiculoRestClient.ApiException;

import lombok.Getter;
import lombok.Setter;

@Named
@ViewScoped
public class ManterVeiculoBean implements Serializable {

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
		carregarVeiculo();
	}

	public void salvar() {
		try {
			if (veiculo.getId() == null) {
				veiculo = veiculoRestClient.criar(veiculo);
				informar("Veículo cadastrado com sucesso.");
			}
			else {
				veiculo = veiculoRestClient.atualizar(veiculo);
				informar("Veículo alterado com sucesso.");
			}
		}
		catch (ApiException e) {
			reclamar(e);
		}
	}

	// Sem o login (UC01) ainda não há morador: o sistema guarda um único veículo
	private void carregarVeiculo() {
		try {
			List<Veiculo> veiculos = veiculoRestClient.listarTodos();
			veiculo = veiculos.isEmpty() ? new Veiculo() : veiculos.get(0);
		}
		catch (ApiException e) {
			veiculo = new Veiculo();
			reclamar(e);
		}
	}

	private void informar(String mensagem) {
		adicionar(FacesMessage.SEVERITY_INFO, mensagem);
	}

	private void reclamar(ApiException e) {
		System.out.println("ManterVeiculo: " + e.getMessage());
		adicionar(FacesMessage.SEVERITY_ERROR, e.getMessage());
	}

	private void adicionar(FacesMessage.Severity severidade, String mensagem) {
		FacesContext contexto = FacesContext.getCurrentInstance();
		if (contexto != null) {
			contexto.addMessage(null, new FacesMessage(severidade, mensagem, null));
		}
	}
}
