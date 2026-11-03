package br.edu.ifsp.carregaja.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false, unique = true)
  private String email;

  @JsonIgnore
  @Column(nullable = false)
  private String senha;

  private boolean ativo = true;

  // Começa como true porque a senha inicial é provisória e precisa ser trocada no primeiro acesso
  private boolean trocarSenha = true;

  // Pode ficar nulo, porque o síndico pode ser um preposto da administradora que não mora no condomínio
  @ManyToOne
  private Apartamento apartamento;

  @ElementCollection(fetch = FetchType.EAGER)
  @Enumerated(EnumType.STRING)
  private Set<Perfil> perfis = new HashSet<>();

  public boolean temPerfil(Perfil perfil) {
    return perfis.contains(perfil);
  }
}
