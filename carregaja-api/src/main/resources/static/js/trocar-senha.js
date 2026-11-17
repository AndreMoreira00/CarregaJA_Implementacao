// Trocar a senha. Quem acabou de ser cadastrado só consegue usar esta tela até trocar a senha provisória.
iniciarPagina();

$('formulario').onsubmit = async evento => {
  evento.preventDefault();
  if ($('nova').value !== $('repetida').value) return aviso('As duas senhas novas não são iguais.', true);
  try {
    await api('PUT', '/autenticacao/senha', { senhaAtual: $('atual').value, senhaNova: $('nova').value });
    // Entra de novo, para guardar o usuário já sem a marca de senha provisória
    guardarLogin(await api('POST', '/autenticacao', { email: login().usuario.email, senha: $('nova').value }));
    location.href = '/painel.html';
  } catch (erro) {
    aviso(erro.message, true);
  }
};
