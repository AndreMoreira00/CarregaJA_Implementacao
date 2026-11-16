import { iniciar, chamar, MENU, esc, aviso, hora, primeiroNome, selo } from '/js/app.js';

const usuario = iniciar();
if (usuario) montar();

function descricaoDoPerfil() {
  const partes = [];
  if (usuario.perfis.includes('MORADOR') && usuario.apartamento) partes.push(`Morador do apartamento ${usuario.apartamento.numero}`);
  if (usuario.perfis.includes('SINDICO')) partes.push('Síndico');
  return partes.join(' · ');
}

async function montar() {
  document.querySelector('.pagina-titulo').textContent = 'Olá, ' + primeiroNome(usuario.nome);
  document.querySelector('.subtitulo').textContent = descricaoDoPerfil();
  if (new URLSearchParams(location.search).has('sem-acesso')) aviso('Essa tela é de outro perfil de acesso.', 'info');

  const itens = MENU.filter(item => item.perfis.some(perfil => usuario.perfis.includes(perfil)));
  const doMorador = usuario.perfis.includes('MORADOR') ? itens.filter(item => item.perfis.includes('MORADOR')) : [];
  const daAdministracao = itens.filter(item => !doMorador.includes(item));
  const grupo = (titulo, lista) => (lista.length
    ? `<section class="secao"><h2>${titulo}</h2><div class="atalhos revelar">${lista.map(item =>
      `<a class="atalho" href="${item.href}"><b>${esc(item.rotulo)}</b><span>${esc(item.texto)}</span></a>`).join('')}</div></section>`
    : '');
  document.getElementById('atalhos').innerHTML = grupo('Para o seu apartamento', doMorador) + grupo('Administração do condomínio', daAdministracao);

  const resumo = document.getElementById('resumo');
  try {
    const [painel, minha] = await Promise.all([
      chamar('GET', '/painel'),
      usuario.perfis.includes('MORADOR')
        ? chamar('GET', '/sessoes/minha').catch(erro => { if (erro.status === 404) return null; throw erro; })
        : Promise.resolve(null),
    ]);
    const n = painel.vagasLivres;
    resumo.innerHTML = `<div><span class="numero-grande">${n}</span><span class="unidade">${n === 1 ? 'vaga livre agora' : 'vagas livres agora'}</span></div>`
      + (minha ? `<a class="cartao" href="/recarga.html" style="text-decoration:none"><span class="rotulo">Sua recarga</span><br>
        <b class="numero-medio">${esc(minha.vaga)}</b> ${selo(minha.estado)}<br><span class="nota">Previsão de término às ${hora(minha.previsaoConclusao)}</span></a>` : '');
  } catch (erro) {
    resumo.innerHTML = '';
  }
}
