import { iniciar, chamar, esc, hora, potencia, selo, classeDaVaga, aviso } from '/js/app.js';

const usuario = iniciar({ perfis: ['MORADOR', 'SINDICO'] });
const REFRESCO_MS = 30000;

function cartaoDaVaga(vaga, ehSindico) {
  const ocupada = vaga.estado !== 'LIVRE';
  let previsao = '';
  if (ocupada) {
    const rotulo = vaga.estado === 'OCUPADA' ? 'Libera por volta de' : 'Previsão que já passou';
    previsao = `<div class="previsao"><span class="rotulo">${rotulo}</span><span class="hora">${hora(vaga.previsaoLiberacao)}</span></div>`;
  }
  const excedida = vaga.estado === 'EXCEDIDA'
    ? '<p class="nota" style="margin:0">Passou da duração máxima e segue aberta até o encerramento.</p>' : '';
  const quem = ehSindico && vaga.morador
    ? `<div class="quem">${vaga.apartamento ? 'Apto ' + esc(vaga.apartamento) + ' · ' : ''}${esc(vaga.morador)} · desde ${hora(vaga.inicio)}</div>` : '';
  return `<article class="vaga ${classeDaVaga(vaga.estado)}">
      <div class="vaga-topo"><span class="vaga-numero">${esc(vaga.identificacao)}</span><span class="potencia">${potencia(vaga.potenciaCarregador)}</span></div>
      <div>${selo(vaga.estado)}</div>${previsao}${excedida}${quem}</article>`;
}

async function carregar() {
  try {
    const painel = await chamar('GET', '/painel');
    const n = painel.vagasLivres;
    const resumo = document.getElementById('resumo');
    if (n > 0) {
      resumo.innerHTML = `<div><span class="numero-grande">${n}</span><span class="unidade">${n === 1 ? 'vaga livre' : 'vagas livres'}</span></div>`;
    } else {
      const proxima = painel.proximaLiberacao;
      resumo.innerHTML = `<div class="aviso aviso-info" style="margin:0">${esc(painel.mensagem || 'Nenhuma vaga livre agora.')}</div>`
        + (proxima ? `<div class="previsao"><span class="rotulo">Próxima liberação · ${esc(proxima.identificacao)}</span><span class="hora">${hora(proxima.previsaoLiberacao)}</span></div>` : '');
    }
    const vagas = document.getElementById('vagas');
    vagas.innerHTML = painel.vagas.length
      ? painel.vagas.map(vaga => cartaoDaVaga(vaga, usuario.perfis.includes('SINDICO'))).join('')
      : '<p class="vazio" style="grid-column:1/-1">Ainda não há vagas com carregador cadastradas.</p>';
    document.getElementById('atualizado').textContent = 'Atualizado às ' + hora(painel.consultadoEm) + '.';
    aviso('');
  } catch (erro) {
    aviso(erro.message, 'erro');
  }
}

if (usuario) {
  document.getElementById('atualizar').addEventListener('click', carregar);
  carregar();
  // O painel muda sozinho: atualiza enquanto a aba está à vista
  setInterval(() => { if (!document.hidden) carregar(); }, REFRESCO_MS);
}
