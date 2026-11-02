package br.edu.ifsp.carregaja.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifsp.carregaja.model.Apartamento;
import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.repository.ApartamentoRepository;
import br.edu.ifsp.carregaja.repository.RecusaVagaRepository;
import br.edu.ifsp.carregaja.repository.SessaoRecargaRepository;
import br.edu.ifsp.carregaja.repository.UsuarioRepository;
import br.edu.ifsp.carregaja.repository.VeiculoRepository;
import br.edu.ifsp.carregaja.view.UsuarioRequest;

@Service
public class UsuarioService {

  private final UsuarioRepository usuarioRepository;
  private final ApartamentoRepository apartamentoRepository;
  private final VeiculoRepository veiculoRepository;
  private final SessaoRecargaRepository sessaoRepository;
  private final RecusaVagaRepository recusaRepository;
  private final PasswordEncoder passwordEncoder;

  public UsuarioService(UsuarioRepository usuarioRepository, ApartamentoRepository apartamentoRepository,
      VeiculoRepository veiculoRepository, SessaoRecargaRepository sessaoRepository,
      RecusaVagaRepository recusaRepository, PasswordEncoder passwordEncoder) {
    this.usuarioRepository = usuarioRepository;
    this.apartamentoRepository = apartamentoRepository;
    this.veiculoRepository = veiculoRepository;
    this.sessaoRepository = sessaoRepository;
    this.recusaRepository = recusaRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public Iterable<Usuario> listarTodos() {
    return usuarioRepository.findAll();
  }

  public Optional<Usuario> buscarPorId(Long id) {
    return usuarioRepository.findById(id);
  }

  @Transactional
  public Usuario criar(UsuarioRequest requisicao) {
    if (requisicao.getSenhaProvisoria() == null) {
      throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Informe a senha provisória.");
    }
    Usuario usuario = new Usuario();
    copiarDados(requisicao, usuario);
    usuario.setSenha(passwordEncoder.encode(requisicao.getSenhaProvisoria()));
    return usuarioRepository.save(usuario);
  }

  @Transactional
  public Optional<Usuario> atualizar(Long id, UsuarioRequest requisicao) {
    return usuarioRepository.findById(id).map(usuario -> {
      if (usuario.temPerfil(Perfil.SINDICO) && !requisicao.isSindico()) {
        exigirOutroSindicoAtivo();
      }
      copiarDados(requisicao, usuario);
      if (requisicao.getSenhaProvisoria() != null) {
        usuario.setSenha(passwordEncoder.encode(requisicao.getSenhaProvisoria()));
        usuario.setTrocarSenha(true);
      }
      return usuarioRepository.save(usuario);
    });
  }

  // HU-09, teste 04: o token do desativado para de valer na requisição seguinte (TokenFiltro)
  public Optional<Usuario> desativar(Long id) {
    return usuarioRepository.findById(id).map(usuario -> {
      if (usuario.isAtivo() && usuario.temPerfil(Perfil.SINDICO)) {
        exigirOutroSindicoAtivo();
      }
      usuario.setAtivo(false);
      return usuarioRepository.save(usuario);
    });
  }

  public Optional<Usuario> reativar(Long id) {
    return usuarioRepository.findById(id).map(usuario -> {
      usuario.setAtivo(true);
      return usuarioRepository.save(usuario);
    });
  }

  @Transactional
  public boolean remover(Long id) {
    Optional<Usuario> encontrado = usuarioRepository.findById(id);
    encontrado.ifPresent(usuario -> {
      // HU-09, teste 05; RN-09: quem tem histórico é desativado, e não removido
      if (sessaoRepository.existsByUsuarioId(id) || recusaRepository.existsByUsuarioId(id)) {
        throw new RegraNegocioException(HttpStatus.CONFLICT,
            "Este morador tem sessões registradas: desative-o em vez de remover.");
      }
      if (usuario.isAtivo() && usuario.temPerfil(Perfil.SINDICO)) {
        exigirOutroSindicoAtivo();
      }
      veiculoRepository.findByUsuarioId(id).ifPresent(veiculoRepository::delete);
      usuarioRepository.delete(usuario);
    });
    return encontrado.isPresent();
  }

  private void copiarDados(UsuarioRequest requisicao, Usuario usuario) {
    String email = AutenticacaoService.normalizarEmail(requisicao.getEmail());
    usuarioRepository.findByEmail(email)
        .filter(outro -> !outro.getId().equals(usuario.getId()))
        .ifPresent(outro -> {
          throw new RegraNegocioException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
        });
    usuario.setNome(requisicao.getNome().trim());
    usuario.setEmail(email);
    definirApartamentoEPerfis(requisicao, usuario);
  }

  // Morador é quem tem apartamento. O preposto da administradora é síndico sem apartamento
  private void definirApartamentoEPerfis(UsuarioRequest requisicao, Usuario usuario) {
    String numero = requisicao.getApartamento() == null ? "" : requisicao.getApartamento().trim();
    if (numero.isEmpty() && !requisicao.isSindico()) {
      throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Informe o apartamento do morador.");
    }
    usuario.getPerfis().clear();
    usuario.setApartamento(null);
    if (!numero.isEmpty()) {
      usuario.setApartamento(buscarOuCriarApartamento(numero));
      usuario.getPerfis().add(Perfil.MORADOR);
    }
    if (requisicao.isSindico()) {
      usuario.getPerfis().add(Perfil.SINDICO);
    }
  }

  private Apartamento buscarOuCriarApartamento(String numero) {
    return apartamentoRepository.findByNumero(numero).orElseGet(() -> {
      Apartamento apartamento = new Apartamento();
      apartamento.setNumero(numero);
      return apartamentoRepository.save(apartamento);
    });
  }

  // Sem nenhum síndico ativo, ninguém mais consegue administrar o sistema
  private void exigirOutroSindicoAtivo() {
    if (usuarioRepository.countByPerfisContainingAndAtivoTrue(Perfil.SINDICO) <= 1) {
      throw new RegraNegocioException(HttpStatus.CONFLICT, "O condomínio ficaria sem síndico. Cadastre outro antes.");
    }
  }
}
