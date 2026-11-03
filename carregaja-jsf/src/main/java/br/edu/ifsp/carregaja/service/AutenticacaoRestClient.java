package br.edu.ifsp.carregaja.service;

import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ifsp.carregaja.model.AutenticacaoResponse;
import br.edu.ifsp.carregaja.model.LoginRequest;
import br.edu.ifsp.carregaja.model.TrocarSenhaRequest;

@ApplicationScoped
public class AutenticacaoRestClient implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final String URL_PADRAO = "http://localhost:8081";

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();
  private final ObjectMapper json = new ObjectMapper()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Inject
  private UsuarioLogado usuarioLogado;

  private String baseUrl() {
    return System.getProperty("carregaja.api.url", URL_PADRAO) + "/autenticacao";
  }

  public AutenticacaoResponse autenticar(LoginRequest login) throws ApiException {
    HttpRequest requisicao = HttpRequest.newBuilder(URI.create(baseUrl()))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(escrever(login)))
        .build();
    HttpResponse<String> resposta = enviar(requisicao);
    exigirSucesso(resposta, "entrar");
    return ler(resposta.body());
  }

  public void trocarSenha(TrocarSenhaRequest troca) throws ApiException {
    HttpRequest requisicao = HttpRequest.newBuilder(URI.create(baseUrl() + "/senha"))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer " + usuarioLogado.getToken())
        .PUT(HttpRequest.BodyPublishers.ofString(escrever(troca)))
        .build();
    exigirSucesso(enviar(requisicao), "trocar a senha");
  }

  private HttpResponse<String> enviar(HttpRequest requisicao) throws ApiException {
    try {
      return http.send(requisicao, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException("Chamada à carregaja-api interrompida.", e);
    } catch (Exception e) {
      throw new ApiException("Não foi possível falar com a carregaja-api em "
          + System.getProperty("carregaja.api.url", URL_PADRAO) + ". Ela está no ar?", e);
    }
  }

  private void exigirSucesso(HttpResponse<String> resposta, String acao) throws ApiException {
    int status = resposta.statusCode();
    if (status >= 200 && status < 300) {
      return;
    }
    String mensagem = lerMensagem(resposta.body());
    throw new ApiException(mensagem != null ? mensagem : "Não foi possível " + acao + " (HTTP " + status + ").");
  }

  // A API explica as recusas no campo "mensagem"; as recusas de permissão vêm sem corpo
  private String lerMensagem(String corpo) {
    try {
      JsonNode raiz = json.readTree(corpo);
      return raiz != null && raiz.hasNonNull("mensagem") ? raiz.get("mensagem").asText() : null;
    } catch (Exception e) {
      return null;
    }
  }

  private AutenticacaoResponse ler(String corpo) throws ApiException {
    try {
      return json.readValue(corpo, AutenticacaoResponse.class);
    } catch (Exception e) {
      throw new ApiException("Resposta da API em formato diferente.", e);
    }
  }

  private String escrever(Object valor) throws ApiException {
    try {
      return json.writeValueAsString(valor);
    } catch (Exception e) {
      throw new ApiException("Não foi possível montar a requisição.", e);
    }
  }
}
