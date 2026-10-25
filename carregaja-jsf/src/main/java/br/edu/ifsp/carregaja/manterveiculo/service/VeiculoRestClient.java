package br.edu.ifsp.carregaja.manterveiculo.service;

import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ifsp.carregaja.manterveiculo.model.Veiculo;
import br.edu.ifsp.carregaja.manterveiculo.model.VeiculoRequest;

@ApplicationScoped
public class VeiculoRestClient implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String URL_PADRAO = "http://localhost:8081";
	private final HttpClient http = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();
	private final ObjectMapper json = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	private String baseUrl() {
		return System.getProperty("carregaja.api.url", URL_PADRAO) + "/veiculos";
	}

	public List<Veiculo> listarTodos() throws ApiException {
		HttpRequest requisicao = HttpRequest.newBuilder(URI.create(baseUrl()))
				.header("Accept", "application/json")
				.GET()
				.build();
		HttpResponse<String> resposta = enviar(requisicao);
		exigirSucesso(resposta, "carregar o veículo");
		return ler(resposta.body(), new TypeReference<List<Veiculo>>() {
		});
	}

	public Veiculo criar(Veiculo veiculo) throws ApiException {
		HttpRequest requisicao = HttpRequest.newBuilder(URI.create(baseUrl()))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(escrever(VeiculoRequest.de(veiculo))))
				.build();
		HttpResponse<String> resposta = enviar(requisicao);
		exigirSucesso(resposta, "cadastrar o veículo");
		return ler(resposta.body(), new TypeReference<Veiculo>() {
		});
	}

	public Veiculo atualizar(Veiculo veiculo) throws ApiException {
		HttpRequest requisicao = HttpRequest.newBuilder(URI.create(baseUrl() + "/" + veiculo.getId()))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.PUT(HttpRequest.BodyPublishers.ofString(escrever(VeiculoRequest.de(veiculo))))
				.build();
		HttpResponse<String> resposta = enviar(requisicao);
		exigirSucesso(resposta, "alterar o veículo");
		return ler(resposta.body(), new TypeReference<Veiculo>() {
		});
	}

	private HttpResponse<String> enviar(HttpRequest requisicao) throws ApiException {
		try {
			return http.send(requisicao, HttpResponse.BodyHandlers.ofString());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiException("Chamada à carregaja-api interrompida.", e);
		}
		catch (Exception e) {
			throw new ApiException("Não foi possível falar com a carregaja-api em "
					+ System.getProperty("carregaja.api.url", URL_PADRAO) + ". Ela está no ar?", e);
		}
	}

	private void exigirSucesso(HttpResponse<String> resposta, String acao) throws ApiException {
		int status = resposta.statusCode();
		if (status >= 200 && status < 300) {
			return;
		}
		throw new ApiException("Falha ao " + acao + " (HTTP " + status + "). " + resumirErro(resposta.body()));
	}

	private String resumirErro(String corpo) {
		if (corpo == null || corpo.isBlank()) {
			return "";
		}
		return corpo.length() > 300 ? corpo.substring(0, 300) + "..." : corpo;
	}

	private <T> T ler(String corpo, TypeReference<T> tipo) throws ApiException {
		try {
			return json.readValue(corpo, tipo);
		}
		catch (Exception e) {
			throw new ApiException("Resposta da API em formato diferente.", e);
		}
	}

	private String escrever(Object valor) throws ApiException {
		try {
			return json.writeValueAsString(valor);
		}
		catch (Exception e) {
			throw new ApiException("Não foi possível montar a requisição.", e);
		}
	}

	public static class ApiException extends Exception {
		private static final long serialVersionUID = 1L;

		public ApiException(String mensagem) {
			super(mensagem);
		}

		public ApiException(String mensagem, Throwable causa) {
			super(mensagem, causa);
		}
	}
}
