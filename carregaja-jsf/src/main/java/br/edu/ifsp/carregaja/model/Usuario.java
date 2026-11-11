package br.edu.ifsp.carregaja.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Usuario implements Serializable {
  private static final long serialVersionUID = 1L;
  private Long id;
  private String nome;
  private String email;
  private boolean ativo;
  private boolean trocarSenha;
  private Apartamento apartamento;
  private Set<String> perfis = new HashSet<>();
}
