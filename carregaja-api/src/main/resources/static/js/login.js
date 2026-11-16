import { chamar, guardarSessao, sessao, aviso, comBotao } from '/js/app.js';

const atual = sessao();
if (atual && atual.token && atual.usuario) {
  location.replace(atual.usuario.trocarSenha ? '/trocar-senha.html' : '/index.html');
}

document.getElementById('formulario').addEventListener('submit', async evento => {
  evento.preventDefault();
  const email = document.getElementById('email').value.trim();
  const senha = document.getElementById('senha').value;
  if (!email) return aviso('Informe o e-mail.', 'erro');
  if (!senha) return aviso('Informe a senha.', 'erro');
  const botao = evento.target.querySelector('button[type="submit"]');
  try {
    await comBotao(botao, async () => {
      const resposta = await chamar('POST', '/autenticacao', { email, senha });
      guardarSessao({ token: resposta.token, usuario: resposta.usuario });
      location.replace(resposta.usuario.trocarSenha ? '/trocar-senha.html' : '/index.html');
    });
  } catch (erro) {
    aviso(erro.message, 'erro');
  }
});
