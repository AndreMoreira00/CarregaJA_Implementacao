package br.edu.ifsp.carregaja.cadastro.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import br.edu.ifsp.carregaja.cadastro.model.Veiculo;
import br.edu.ifsp.carregaja.cadastro.service.VeiculoService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

@Component
@RequestScope
public class ManterVeiculoBean {

    @Autowired
    private VeiculoService veiculoService;

    private String modelo;
    private Double capacidadeBateria;
    private Double potenciaMaxima;

    @PostConstruct
    public void carregar() {
        Veiculo veiculo = veiculoService.buscar();
        if (veiculo != null) {
            modelo = veiculo.getModelo();
            capacidadeBateria = veiculo.getCapacidadeBateria();
            potenciaMaxima = veiculo.getPotenciaMaxima();
        }
    }

    public void salvar() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            veiculoService.salvar(modelo, capacidadeBateria, potenciaMaxima);
            context.addMessage(null, new FacesMessage("Veículo salvo com sucesso."));
        } catch (IllegalArgumentException e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Double getCapacidadeBateria() {
        return capacidadeBateria;
    }

    public void setCapacidadeBateria(Double capacidadeBateria) {
        this.capacidadeBateria = capacidadeBateria;
    }

    public Double getPotenciaMaxima() {
        return potenciaMaxima;
    }

    public void setPotenciaMaxima(Double potenciaMaxima) {
        this.potenciaMaxima = potenciaMaxima;
    }
}
