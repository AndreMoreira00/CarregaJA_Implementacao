import { iniciar, chamar, esc, kwh, brl, dataHora, nomeDoMes, aviso, confirmar, comBotao } from '/js/app.js';

const usuario = iniciar({ perfis: ['SINDICO'] });
const resultado = document.getElementById('resultado');
const lista = document.getElementById('lista');
const agora = new Date();
const mesAtual = agora.getMonth() + 1;
// Por padrão fecha o mês que acabou de terminar
let ano = mesAtual === 1 ? agora.getFullYear() - 1 : agora.getFullYear();
let mes = mesAtual === 1 ? 12 : mesAtual - 1;
let fechados = [];

// Só a primeira letra: o text-transform do CSS poria maiúscula também no "de"
const maiuscula = texto => texto.charAt(0).toUpperCase() + texto.slice(1);

function naoTerminou(a, m) {
  return a > agora.getFullYear() || (a === agora.getFullYear() && m >= mesAtual);
}

function atualizarSeletor() {
  document.getElementById('mes').textContent = nomeDoMes(ano, mes);
  const seguinte = mes === 12 ? [ano + 1, 1] : [ano, mes + 1];
  document.getElementById('proximo').disabled = naoTerminou(seguinte[0], seguinte[1]);
}

function mover(delta) {
  mes += delta;
  if (mes < 1) { mes = 12; ano -= 1; }
  if (mes > 12) { mes = 1; ano += 1; }
  atualizarSeletor();
}

// ---------- exportação ----------

// Ponto e vírgula e vírgula decimal: é o que o Excel em português abre sem pedir nada. O BOM garante os acentos
function montarCsv(fechamento) {
  const decimal = (valor, casas) => Number(valor).toFixed(casas).replace('.', ',');
  const linhas = [['Apartamento', 'Energia (kWh)', 'Valor (R$)']];
  fechamento.itens.forEach(item => linhas.push([item.apartamento, decimal(item.energia, 2), decimal(item.valor, 2)]));
  linhas.push(['Total', decimal(fechamento.energiaTotal, 2), decimal(fechamento.valorTotal, 2)]);
  return '﻿' + linhas.map(linha => linha.map(celula => `"${String(celula).replace(/"/g, '""')}"`).join(';')).join('\r\n');
}

function exportar(fechamento) {
  const arquivo = new Blob([montarCsv(fechamento)], { type: 'text/csv;charset=utf-8' });
  const endereco = URL.createObjectURL(arquivo);
  const link = document.createElement('a');
  link.href = endereco;
  link.download = `rateio-${fechamento.ano}-${String(fechamento.mes).padStart(2, '0')}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(endereco), 1000);
}

// ---------- rateio ----------

function mostrarRateio(fechamento) {
  const linhas = fechamento.itens.map(item =>
    `<tr><td><b>Apto ${esc(item.apartamento)}</b></td><td class="num">${kwh(item.energia, 2)} kWh</td><td class="num">${brl(item.valor)}</td></tr>`).join('');
  resultado.innerHTML = `<h2>Rateio de ${esc(nomeDoMes(fechamento.ano, fechamento.mes))}</h2>
    <div class="cartao">
      <div class="estatisticas">
        <div class="estatistica"><span class="rotulo">Energia total</span><span class="numero-grande">${kwh(fechamento.energiaTotal, 2)}<span class="unidade">kWh</span></span></div>
        <div class="estatistica"><span class="rotulo">Valor total</span><span class="numero-grande">${brl(fechamento.valorTotal)}</span></div>
      </div>
      <p class="nota" style="margin:12px 0">Fechado em ${dataHora(fechamento.momento)} por ${esc(fechamento.nomeResponsavel)}. A administradora lança o valor de cada apartamento na cota condominial.</p>
      <div class="rolagem"><table class="tabela">
        <thead><tr><th>Apartamento</th><th class="num">Energia</th><th class="num">Valor</th></tr></thead>
        <tbody>${linhas}</tbody>
        <tfoot><tr><td>Total</td><td class="num">${kwh(fechamento.energiaTotal, 2)} kWh</td><td class="num">${brl(fechamento.valorTotal)}</td></tr></tfoot>
      </table></div>
      <div class="acoes" style="margin-top:16px"><button type="button" id="exportar" class="botao">Exportar CSV</button></div>
    </div>`;
  document.getElementById('exportar').addEventListener('click', () => exportar(fechamento));
  resultado.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function mostrarLista() {
  lista.innerHTML = fechados.length
    ? fechados.map(f => `<li class="item" data-id="${f.id}">
        <div class="item-linha"><span class="item-titulo">${esc(maiuscula(nomeDoMes(f.ano, f.mes)))}</span>
          <span class="numero-medio" style="font-size:32px">${brl(f.valorTotal)}</span></div>
        <div class="item-sub">${kwh(f.energiaTotal, 2)} kWh · ${f.itens.length} ${f.itens.length === 1 ? 'apartamento' : 'apartamentos'} · fechado em ${dataHora(f.momento)} por ${esc(f.nomeResponsavel)}</div>
        <div class="acoes"><button type="button" class="botao secundario pequeno" data-acao="ver">Ver rateio e exportar</button></div></li>`).join('')
    : '<li class="vazio">Nenhum mês fechado ainda.</li>';
}

async function carregarLista() {
  fechados = await chamar('GET', '/fechamentos');
  mostrarLista();
}

if (usuario) {
  atualizarSeletor();
  document.getElementById('anterior').addEventListener('click', () => mover(-1));
  document.getElementById('proximo').addEventListener('click', () => mover(1));

  lista.addEventListener('click', evento => {
    const botao = evento.target.closest('[data-acao="ver"]');
    if (!botao) return;
    const id = Number(botao.closest('[data-id]').dataset.id);
    mostrarRateio(fechados.find(f => f.id === id));
  });

  document.getElementById('fechar').addEventListener('click', async evento => {
    const nome = nomeDoMes(ano, mes);
    const certeza = await confirmar({
      titulo: `Fechar ${nome}?`,
      texto: 'O consumo de cada apartamento no mês é consolidado e registrado com o seu nome e o horário. Depois de fechado, o mês não é recalculado, mesmo que a tarifa mude.',
      ok: 'Fechar o mês',
    });
    if (!certeza) return;
    try {
      const fechamento = await comBotao(evento.target, () => chamar('POST', '/fechamentos', { ano, mes }));
      await carregarLista();
      mostrarRateio(fechamento);
      aviso(`${nome} fechado.`, 'ok');
    } catch (erro) {
      aviso(erro.message, 'erro');
    }
  });

  carregarLista().catch(erro => aviso(erro.message, 'erro'));
}
