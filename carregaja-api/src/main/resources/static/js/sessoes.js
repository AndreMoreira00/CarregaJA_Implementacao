import { iniciar, chamar, esc, hora, dataHora, kwh, brl, selo, aviso, confirmar, comBotao } from '/js/app.js';

const usuario = iniciar({ perfis: ['SINDICO'] });
const lista = document.getElementById('lista');
let abertas = [];

function item(sessao) {
  const dentroDaPrevisao = sessao.estado === 'EM_ANDAMENTO';
  const quem = `${sessao.apartamento ? 'Apto ' + esc(sessao.apartamento) + ' · ' : ''}${esc(sessao.morador)}`;
  return `<li class="item" data-id="${sessao.id}">
      <div class="item-linha"><span class="item-titulo">${esc(sessao.vaga)}</span>${selo(sessao.estado)}</div>
      <div class="item-sub">${quem} · desde ${dataHora(sessao.inicio)} · previsão ${hora(sessao.previsaoConclusao)}</div>
      <div class="acoes"><button type="button" class="botao pequeno" data-acao="encerrar"${dentroDaPrevisao ? ' disabled' : ''}>Encerrar sessão órfã</button></div>
      ${dentroDaPrevisao ? '<p class="nota" style="margin:0">Ainda dentro da previsão: até lá só o morador encerra, porque a recarga pode estar acontecendo.</p>' : ''}
    </li>`;
}

async function carregar() {
  try {
    abertas = await chamar('GET', '/sessoes/abertas');
    lista.innerHTML = abertas.length
      ? abertas.map(item).join('')
      : '<li class="vazio">Nenhuma sessão aberta. Não há recarga em andamento nem veículo esquecido nas vagas.</li>';
  } catch (erro) {
    aviso(erro.message, 'erro');
  }
}

if (usuario) {
  document.getElementById('atualizar').addEventListener('click', carregar);
  lista.addEventListener('click', async evento => {
    const botao = evento.target.closest('[data-acao]');
    if (!botao) return;
    const id = Number(botao.closest('[data-id]').dataset.id);
    const sessao = abertas.find(item => item.id === id);
    const certeza = await confirmar({
      titulo: `Encerrar a sessão da ${sessao.vaga}?`,
      texto: `A recarga de ${sessao.morador} é apurada agora e a vaga é liberada. O encerramento fica registrado com o seu nome e o horário, e aparece para o morador no consumo.`,
      ok: 'Encerrar sessão',
      perigo: true,
      marcar: `Confirmo que vi na garagem que o veículo já saiu da vaga ${sessao.vaga}.`,
    });
    if (!certeza) return;
    try {
      const resultado = await comBotao(botao, () => chamar('POST', `/sessoes/${id}/encerrar-administrativamente`));
      await carregar();
      aviso(`Sessão da ${resultado.vaga} encerrada: ${resultado.duracao}, ${kwh(resultado.energiaEstimada)} kWh, ${brl(resultado.valor)} para o apartamento ${sessao.apartamento || '—'}.`, 'ok');
    } catch (erro) {
      aviso(erro.message, 'erro');
      carregar();
    }
  });
  carregar();
}
