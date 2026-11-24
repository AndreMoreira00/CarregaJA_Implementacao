package br.edu.ifsp.carregaja.service.autenticarusuario;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import br.edu.ifsp.carregaja.model.compartilhado.Usuario;

/** Gera e lê o token, que guarda só a identificação do usuário. Perfis e situação vêm do banco a cada requisição. */
@Service
public class TokenService {

  private final SecretKey chave;
  private final Duration validade;

  public TokenService(@Value("${carregaja.jwt.segredo}") String segredo,
      @Value("${carregaja.jwt.validade-horas}") long validadeHoras) {
    this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
    this.validade = Duration.ofHours(validadeHoras);
  }

  public String gerar(Usuario usuario) {
    Instant agora = Instant.now();
    return Jwts.builder()
        .subject(String.valueOf(usuario.getId()))
        .issuedAt(Date.from(agora))
        .expiration(Date.from(agora.plus(validade)))
        .signWith(chave)
        .compact();
  }

  public Optional<Long> lerUsuarioId(String token) {
    return ler(token).map(dados -> Long.valueOf(dados.getSubject()));
  }

  /** O token vale só se foi emitido depois da última troca de senha do usuário (Roteiro de Teste, UC01 cenário 08). */
  public boolean emitidoDepoisDaTrocaDeSenha(String token, Usuario usuario) {
    if (usuario.getSenhaTrocadaEm() == null) {
      return true;
    }
    return ler(token)
        .map(dados -> !dados.getIssuedAt().toInstant().isBefore(usuario.getSenhaTrocadaEm()))
        .orElse(false);
  }

  private Optional<Claims> ler(String token) {
    try {
      return Optional.of(Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload());
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
