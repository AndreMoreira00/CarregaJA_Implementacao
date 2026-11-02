package br.edu.ifsp.carregaja.service;

import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** O que todo cliente REST precisa: endereço da API, token do usuário e tradução das falhas em mensagem. */
@ApplicationScoped
public class ApiClient implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final String URL_PADRAO = "http://localhost:8081";
  private static final int NAO_ENCONTRADO = 404;

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();
  private final ObjectMapper json = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Inject
  private UsuarioLogado usuarioLogado;

  public <T> T get(String caminho, TypeReference<T> tipo, String acao) throws ApiException {
    return ler(exigirSucesso(enviar("GET", caminho, null), acao), tipo);
  }

  public <T> Optional<T> getOpcional(String caminho, TypeReference<T> tipo, String acao) throws ApiException {
    HttpResponse<String> resposta = enviar("GET", caminho, null);
    if (resposta.statusCode() == NAO_ENCONTRADO) {
      return Optional.empty();
    }
    return Optional.of(ler(exigirSucesso(resposta, acao), tipo));
  }

  public <T> T post(String caminho, Object corpo, TypeReference<T> tipo, String acao) throws ApiException {
    return ler(exigirSucesso(enviar("POST", caminho, corpo), acao), tipo);
  }

  public <T> T put(String caminho, Object corpo, TypeReference<T> tipo, String acao) throws ApiException {
    return ler(exigirSucesso(enviar("PUT", caminho, corpo), acao), tipo);
  }

  public void enviarSemResposta(String metodo, String caminho, Object corpo, String acao) throws ApiException {
    exigirSucesso(enviar(metodo, caminho, corpo), acao);
  }

  private HttpResponse<String> enviar(String metodo, String caminho, Object corpo) throws ApiException {
    HttpRequest.BodyPublisher conteudo = corpo == null
        ? HttpRequest.BodyPublishers.noBody()
        : HttpRequest.BodyPublishers.ofString(escrever(corpo));
    HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create(urlBase() + caminho))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .method(metodo, conteudo);
    if (usuarioLogado.isLogado()) {
      requisicao.header("Authorization", "Bearer " + usuarioLogado.getToken());
    }
    try {
      return http.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException("Chamada à carregaja-api interrompida.", e);
    } catch (Exception e) {
      throw new ApiException("Não foi possível falar com a carregaja-api em " + urlBase() + ". Ela está no ar?", e);
    }
  }

  private String urlBase() {
    return System.getProperty("carregaja.api.url", URL_PADRAO);
  }

  private String exigirSucesso(HttpResponse<String> resposta, String acao) throws ApiException {
    int status = resposta.statusCode();
    if (status >= 200 && status < 300) {
      return resposta.body();
    }
    throw new ApiException(mensagemDeErro(status, resposta.body(), acao));
  }

  private String mensagemDeErro(int status, String corpo, String acao) {
    String mensagem = lerMensagem(corpo);
    if (mensagem != null) {
      return mensagem;
    }
    switch (status) {
      case 401:
        return "Sua sessão expirou. Entre de novo.";
      case 403:
        return "Você não tem permissão para esta função.";
      default:
        return "Não foi possível " + acao + " (HTTP " + status + ").";
    }
  }

  private String lerMensagem(String corpo) {
    try {
      JsonNode raiz = json.readTree(corpo);
      return raiz != null && raiz.hasNonNull("mensagem") ? raiz.get("mensagem").asText() : null;
    } catch (Exception e) {
      return null;
    }
  }

  private <T> T ler(String corpo, TypeReference<T> tipo) throws ApiException {
    try {
      return json.readValue(corpo, tipo);
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
