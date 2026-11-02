package br.edu.ifsp.carregaja.service;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;

import br.edu.ifsp.carregaja.model.AutenticacaoResponse;
import br.edu.ifsp.carregaja.model.LoginRequest;
import br.edu.ifsp.carregaja.model.TrocarSenhaRequest;

@ApplicationScoped
public class AutenticacaoRestClient implements Serializable {
  private static final long serialVersionUID = 1L;

  @Inject
  private ApiClient api;

  public AutenticacaoResponse autenticar(LoginRequest requisicao) throws ApiException {
    return api.post("/autenticacao", requisicao, new TypeReference<AutenticacaoResponse>() {
    }, "entrar");
  }

  public void trocarSenha(TrocarSenhaRequest requisicao) throws ApiException {
    api.enviarSemResposta("PUT", "/autenticacao/senha", requisicao, "trocar a senha");
  }
}
