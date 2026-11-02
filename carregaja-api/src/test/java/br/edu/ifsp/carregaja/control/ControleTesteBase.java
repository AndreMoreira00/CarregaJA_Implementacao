package br.edu.ifsp.carregaja.control;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.edu.ifsp.carregaja.model.Apartamento;
import br.edu.ifsp.carregaja.model.Perfil;
import br.edu.ifsp.carregaja.model.Usuario;
import br.edu.ifsp.carregaja.repository.UsuarioRepository;
import br.edu.ifsp.carregaja.service.TokenService;

/** Os testes das rotas passam pelas regras de segurança de verdade, com um token de verdade. */
@Import({SegurancaConfig.class, TokenService.class})
abstract class ControleTesteBase {

  @Autowired
  protected MockMvc mvc;

  @Autowired
  private TokenService tokenService;

  @MockBean
  protected UsuarioRepository usuarioRepository;

  protected static Usuario usuario(Long id, Perfil... perfis) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setNome("Usuário " + id);
    usuario.setEmail("usuario" + id + "@carregaja.test");
    usuario.setSenha("hash");
    usuario.setTrocarSenha(false);
    usuario.getPerfis().addAll(Arrays.asList(perfis));
    return usuario;
  }

  protected static Usuario morador() {
    Usuario morador = usuario(42L, Perfil.MORADOR);
    Apartamento apartamento = new Apartamento();
    apartamento.setId(1L);
    apartamento.setNumero("42");
    morador.setApartamento(apartamento);
    return morador;
  }

  protected static Usuario sindico() {
    return usuario(1L, Perfil.SINDICO);
  }

  protected RequestPostProcessor logado(Usuario usuario) {
    when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
    String token = tokenService.gerar(usuario);
    return requisicao -> {
      requisicao.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
      return requisicao;
    };
  }
}
