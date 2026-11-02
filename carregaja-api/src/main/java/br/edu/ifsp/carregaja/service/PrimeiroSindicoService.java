package br.edu.ifsp.carregaja.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.repository.UsuarioRepository;

import lombok.extern.slf4j.Slf4j;

/** Quem cadastra moradores é o síndico, então o primeiro síndico nasce da configuração. */
@Slf4j
@Service
public class PrimeiroSindicoService implements ApplicationRunner {

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final String nome;
  private final String email;
  private final String senha;

  public PrimeiroSindicoService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
      @Value("${carregaja.sindico.nome}") String nome,
      @Value("${carregaja.sindico.email}") String email,
      @Value("${carregaja.sindico.senha}") String senha) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.nome = nome;
    this.email = email;
    this.senha = senha;
  }

  @Override
  public void run(ApplicationArguments argumentos) {
    if (usuarioRepository.existsByPerfisContaining(Perfil.SINDICO)) {
      return;
    }
    if (email.isBlank() || senha.isBlank()) {
      log.warn("Não há síndico cadastrado: informe CARREGAJA_SINDICO_EMAIL e CARREGAJA_SINDICO_SENHA.");
      return;
    }
    Usuario sindico = new Usuario();
    sindico.setNome(nome);
    sindico.setEmail(AutenticacaoService.normalizarEmail(email));
    sindico.setSenha(passwordEncoder.encode(senha));
    sindico.getPerfis().add(Perfil.SINDICO);
    usuarioRepository.save(sindico);
    log.info("Primeiro síndico criado: {}", sindico.getEmail());
  }
}
