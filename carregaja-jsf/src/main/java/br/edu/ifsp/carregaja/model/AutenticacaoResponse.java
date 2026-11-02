package br.edu.ifsp.carregaja.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutenticacaoResponse {
  private String token;
  private Usuario usuario;
}
