package br.edu.ifsp.carregaja.cadastro.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VeiculoServiceTest {

    private VeiculoService service = new VeiculoService();

    @Test
    void modeloEmBrancoNaoPodeSerSalvo() {
        assertThrows(IllegalArgumentException.class, () -> service.salvar("  ", 40.0, 6.6));
    }

    @Test
    void modeloNuloNaoPodeSerSalvo() {
        assertThrows(IllegalArgumentException.class, () -> service.salvar(null, 40.0, 6.6));
    }

    @Test
    void capacidadeZeroNaoPodeSerSalva() {
        assertThrows(IllegalArgumentException.class, () -> service.salvar("Nissan Leaf", 0.0, 6.6));
    }

    @Test
    void capacidadeNegativaNaoPodeSerSalva() {
        assertThrows(IllegalArgumentException.class, () -> service.salvar("Nissan Leaf", -40.0, 6.6));
    }

    @Test
    void potenciaZeroNaoPodeSerSalva() {
        assertThrows(IllegalArgumentException.class, () -> service.salvar("Nissan Leaf", 40.0, 0.0));
    }

    @Test
    void potenciaNegativaNaoPodeSerSalva() {
        assertThrows(IllegalArgumentException.class, () -> service.salvar("Nissan Leaf", 40.0, -2.0));
    }
}
