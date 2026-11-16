import { iniciar, chamar, guardarSessao, aviso, comBotao } from '/js/app.js';

const usuario = iniciar({ livreDeTroca: true });
if (usuario) {
  if (usuario.trocarSenha) document.getElementById('provisoria').hidden = false;

  document.getElementById('formulario').addEventListener('submit', async evento => {
    evento.preventDefault();
    const senhaAtual = document.getElementById('atual').value;
    const senhaNova = document.getElementById('nova').value;
    const repetida = document.getElementById('repetida').value;
    if (!senhaAtual) return aviso('Informe a senha atual.', 'erro');
    if (senhaNova.length < 8) return aviso('A senha nova deve ter pelo menos 8 caracteres.', 'erro');
    if (senhaNova.length > 72) return aviso('A senha nova deve ter no máximo 72 caracteres.', 'erro');
    if (senhaNova === senhaAtual) return aviso('A senha nova deve ser diferente da atual.', 'erro');
    if (senhaNova !== repetida) return aviso('As duas senhas novas não são iguais.', 'erro');

    const botao = evento.target.querySelector('button[type="submit"]');
    try {
      await comBotao(botao, async () => {
        await chamar('PUT', '/autenticacao/senha', { senhaAtual, senhaNova });
        // O cadastro deixou de ser provisório: entra de novo para guardar o usuário atualizado
        const resposta = await chamar('POST', '/autenticacao', { email: usuario.email, senha: senhaNova });
        guardarSessao({ token: resposta.token, usuario: resposta.usuario });
        location.replace('/index.html');
      });
    } catch (erro) {
      aviso(erro.message, 'erro');
    }
  });
}
