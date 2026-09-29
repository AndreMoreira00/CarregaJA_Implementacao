package br.edu.ifsp.carregaja.cadastro.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String modelo;

    private Double capacidadeBateria;

    private Double potenciaMaxima;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
