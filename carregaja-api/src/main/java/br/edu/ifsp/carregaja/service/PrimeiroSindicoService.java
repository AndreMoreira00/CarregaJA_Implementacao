package br.edu.ifsp.carregaja.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.edu.ifsp.carregaja.model.Apartamento;
import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.repository.ApartamentoRepository;
import br.edu.ifsp.carregaja.repository.UsuarioRepository;

import lombok.extern.slf4j.Slf4j;

/** Cria o primeiro síndico a partir da configuração, já que é o síndico quem cadastra os moradores. */
@Slf4j
@Service
public class PrimeiroSindicoService implements ApplicationRunner {

  private final UsuarioRepository usuarioRepository;
  private final ApartamentoRepository apartamentoRepository;
  private final PasswordEncoder passwordEncoder;
  private final String nome;
  private final String email;
  private final String senha;
  private final String apartamento;

  public PrimeiroSindicoService(UsuarioRepository usuarioRepository, ApartamentoRepository apartamentoRepository,
      PasswordEncoder passwordEncoder,
      @Value("${carregaja.sindico.nome}") String nome,
      @Value("${carregaja.sindico.email}") String email,
      @Value("${carregaja.sindico.senha}") String senha,
      @Value("${carregaja.sindico.apartamento}") String apartamento) {
    this.usuarioRepository = usuarioRepository;
    this.apartamentoRepository = apartamentoRepository;
    this.passwordEncoder = passwordEncoder;
    this.nome = nome;
    this.email = email;
    this.senha = senha;
    this.apartamento = apartamento;
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
    if (!apartamento.isBlank()) {
      sindico.setApartamento(buscarOuCriarApartamento());
      sindico.getPerfis().add(Perfil.MORADOR);
    }
    usuarioRepository.save(sindico);
    log.info("Primeiro síndico criado: {}", sindico.getEmail());
  }

  // Usado quando o síndico também mora no condomínio e fica com os dois perfis (HU-01, teste 04)
  private Apartamento buscarOuCriarApartamento() {
    return apartamentoRepository.findByNumero(apartamento.trim()).orElseGet(() -> {
      Apartamento novo = new Apartamento();
      novo.setNumero(apartamento.trim());
      return apartamentoRepository.save(novo);
    });
  }
}
