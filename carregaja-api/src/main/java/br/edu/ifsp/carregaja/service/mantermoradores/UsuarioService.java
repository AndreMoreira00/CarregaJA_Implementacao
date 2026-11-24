package br.edu.ifsp.carregaja.service.mantermoradores;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifsp.carregaja.model.compartilhado.Apartamento;
import br.edu.ifsp.carregaja.model.compartilhado.Perfil;
import br.edu.ifsp.carregaja.model.compartilhado.Usuario;
import br.edu.ifsp.carregaja.repository.compartilhado.ApartamentoRepository;
import br.edu.ifsp.carregaja.repository.compartilhado.RecusaVagaRepository;
import br.edu.ifsp.carregaja.repository.compartilhado.SessaoRecargaRepository;
import br.edu.ifsp.carregaja.repository.compartilhado.UsuarioRepository;
import br.edu.ifsp.carregaja.repository.compartilhado.VeiculoRepository;
import br.edu.ifsp.carregaja.service.RegraNegocioException;
import br.edu.ifsp.carregaja.service.autenticarusuario.AutenticacaoService;
import br.edu.ifsp.carregaja.view.mantermoradores.UsuarioRequest;

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
      exigirMesmoApartamentoSeTemHistorico(usuario, requisicao);
      copiarDados(requisicao, usuario);
      if (requisicao.getSenhaProvisoria() != null) {
        usuario.setSenha(passwordEncoder.encode(requisicao.getSenhaProvisoria()));
        usuario.setTrocarSenha(true);
        usuario.marcarTrocaDeSenha();
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
      if (temHistorico(id)) {
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

  private boolean temHistorico(Long id) {
    return sessaoRepository.existsByUsuarioId(id) || recusaRepository.existsByUsuarioId(id);
  }

  // Consumo, rateio e histórico seguem o apartamento atual do morador: trocá-lo levaria
  // as sessões passadas junto, e sem apartamento o fechamento do mês quebraria
  private void exigirMesmoApartamentoSeTemHistorico(Usuario usuario, UsuarioRequest requisicao) {
    String atual = usuario.getApartamento() == null ? "" : usuario.getApartamento().getNumero();
    String novo = requisicao.getApartamento() == null ? "" : requisicao.getApartamento().trim();
    if (!atual.equals(novo) && temHistorico(usuario.getId())) {
      throw new RegraNegocioException(HttpStatus.CONFLICT, "Este morador tem sessões registradas: o apartamento "
          + "não pode ser trocado nem retirado. Se ele se mudou, desative-o e faça um cadastro novo, com outro "
          + "e-mail, no apartamento novo.");
    }
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
