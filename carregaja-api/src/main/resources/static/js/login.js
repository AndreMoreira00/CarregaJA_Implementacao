// Entrar: envia e-mail e senha, guarda o token que a API devolve e vai para a primeira tela.
$('formulario').onsubmit = async evento => {
  evento.preventDefault();
  try {
    const resposta = await api('POST', '/autenticacao', { email: $('email').value, senha: $('senha').value });
    guardarLogin(resposta);
    location.href = resposta.usuario.trocarSenha ? '/trocar-senha.html' : '/painel.html';
  } catch (erro) {
    aviso(erro.message, true);
  }
};
