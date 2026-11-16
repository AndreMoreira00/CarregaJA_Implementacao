import { iniciar, chamar, esc, kwh, brl, dataHora, selo, nomeDoMes, aviso } from '/js/app.js';

const usuario = iniciar({ perfis: ['MORADOR'] });
const area = document.getElementById('area');
const agora = new Date();
const atual = { ano: agora.getFullYear(), mes: agora.getMonth() + 1 };
let ano = atual.ano;
let mes = atual.mes;

function mover(delta) {
  mes += delta;
  if (mes < 1) { mes = 12; ano -= 1; }
  if (mes > 12) { mes = 1; ano += 1; }
  carregar();
}

function itemDaSessao(sessao) {
  const administrativa = sessao.situacao === 'ENCERRADA_ADMINISTRATIVAMENTE';
  return `<li class="item">
      <div class="item-linha"><span class="item-titulo">${esc(sessao.vaga)}</span><span class="numero-medio" style="font-size:32px">${brl(sessao.valor)}</span></div>
      <div class="item-sub">${dataHora(sessao.inicio)} até ${dataHora(sessao.fim)} · ${kwh(sessao.energiaKwh)} kWh</div>
      ${administrativa ? `<div>${selo('ENCERRADA_ADMINISTRATIVAMENTE')} <span class="nota">O síndico encerrou esta sessão porque o veículo já tinha saído.</span></div>` : ''}
    </li>`;
}

async function carregar() {
  document.getElementById('mes').textContent = nomeDoMes(ano, mes);
  document.getElementById('proximo').disabled = ano === atual.ano && mes === atual.mes;
  area.innerHTML = '<p class="carregando">Carregando…</p>';
  try {
    const consumo = await chamar('GET', `/consumos/meu?ano=${ano}&mes=${mes}`);
    aviso('');
    if (consumo.semRegistros) {
      area.innerHTML = `<p class="vazio">${esc(consumo.mensagem || 'Seu apartamento não teve consumo neste mês.')}</p>`;
      return;
    }
    area.innerHTML = `<div class="cartao">
        <p class="rotulo" style="margin:0 0 10px">Apartamento ${esc(consumo.apartamento)}</p>
        <div class="estatisticas">
          <div class="estatistica"><span class="rotulo">Energia no mês</span><span class="numero-grande">${kwh(consumo.energiaKwh)}<span class="unidade">kWh</span></span></div>
          <div class="estatistica"><span class="rotulo">Valor</span><span class="numero-grande">${brl(consumo.valor)}</span></div>
        </div></div>
      <section class="secao"><h2>Sessões</h2><ul class="lista revelar">${consumo.sessoes.map(itemDaSessao).join('')}</ul></section>`;
  } catch (erro) {
    area.innerHTML = '';
    aviso(erro.message, 'erro');
  }
}

if (usuario) {
  document.getElementById('anterior').addEventListener('click', () => mover(-1));
  document.getElementById('proximo').addEventListener('click', () => mover(1));
  carregar();
}
