import { iniciar, chamar, esc, kwh, brl, hojeISO, aviso, comBotao } from '/js/app.js';

const usuario = iniciar({ perfis: ['SINDICO'] });
const resultado = document.getElementById('resultado');

const duasCasas = n => String(n).padStart(2, '0');

async function preencherFiltros() {
  const [vagas, pessoas] = await Promise.all([chamar('GET', '/vagas'), chamar('GET', '/usuarios')]);
  const seletorVaga = document.getElementById('vaga');
  vagas.sort((a, b) => a.identificacao.localeCompare(b.identificacao, 'pt-BR', { numeric: true }))
    .forEach(vaga => seletorVaga.add(new Option(vaga.identificacao + (vaga.ativa ? '' : ' (desativada)'), vaga.id)));
  const apartamentos = new Map();
  pessoas.forEach(pessoa => { if (pessoa.apartamento) apartamentos.set(pessoa.apartamento.id, pessoa.apartamento.numero); });
  const seletorApto = document.getElementById('apartamento');
  [...apartamentos.entries()].sort((a, b) => String(a[1]).localeCompare(String(b[1]), 'pt-BR', { numeric: true }))
    .forEach(([id, numero]) => seletorApto.add(new Option('Apto ' + numero, id)));
}

function secao(titulo, conteudo) {
  return `<section class="secao"><h2>${titulo}</h2>${conteudo}</section>`;
}

function graficoDasHoras(faixas) {
  const maiorOcupacao = Math.max(1, ...faixas.map(f => f.minutosOcupados));
  const colunas = faixas.map(f => {
    const altura = Math.round((f.minutosOcupados / maiorOcupacao) * 100);
    const dica = `${duasCasas(f.hora)}h: ${f.minutosOcupados} min de ocupação, ${f.recusas} ${f.recusas === 1 ? 'recusa' : 'recusas'}`;
    return `<div class="coluna" title="${esc(dica)}">${f.recusas ? `<i class="recusa" style="height:${Math.min(48, f.recusas * 12)}px"></i>` : ''}`
      + `${f.minutosOcupados ? `<i class="ocup" style="height:${Math.max(altura, 3)}%"></i>` : ''}</div>`;
  }).join('');
  const eixo = faixas.map(f => `<span>${f.hora % 3 === 0 ? f.hora : ''}</span>`).join('');
  return `<div class="cartao"><div class="grafico-horas" role="img" aria-label="Ocupação e recusas por hora do dia">${colunas}</div>
    <div class="eixo-horas" aria-hidden="true">${eixo}</div>
    <div class="legenda"><span><i style="background:var(--faixa);outline:1px solid var(--tinta)"></i>Minutos de ocupação</span><span><i style="background:var(--ocupada)"></i>Recusas por vaga ocupada</span></div></div>`;
}

function mostrar(historico) {
  if (historico.semRegistros) {
    resultado.innerHTML = `<p class="vazio" style="margin-top:22px">${esc(historico.mensagem || 'Não há registros neste período.')}</p>`;
    return;
  }
  const pico = historico.faixaPico === null || historico.faixaPico === undefined
    ? '—' : `${duasCasas(historico.faixaPico)}h às ${duasCasas((historico.faixaPico + 1) % 24)}h`;
  const resumo = `<div class="estatisticas" style="margin-top:22px">
      <div class="estatistica"><span class="rotulo">Horário de pico</span><span class="numero-medio">${pico}</span></div>
      <div class="estatistica"><span class="rotulo">Recusas por falta de vaga</span><span class="numero-medio">${historico.totalRecusas}</span></div>
    </div>`;

  const barras = historico.ocupacaoPorVaga.map(vaga => `<div class="barra-linha">
      <div class="barra-topo"><b>${esc(vaga.vaga)}</b><span>${kwh(vaga.horasOcupadas)} h ocupada · ${kwh(vaga.percentualPeriodo)}% do período</span></div>
      <div class="barra-h"><i style="width:${Math.min(100, vaga.percentualPeriodo)}%"></i></div></div>`).join('');

  const consumo = historico.consumoPorApartamento;
  const totalKwh = consumo.reduce((soma, c) => soma + c.energiaKwh, 0);
  const totalValor = consumo.reduce((soma, c) => soma + Number(c.valor), 0);
  const tabela = consumo.length
    ? `<div class="cartao"><div class="rolagem"><table class="tabela"><thead><tr><th>Apartamento</th><th class="num">Sessões</th><th class="num">Energia</th><th class="num">Valor</th></tr></thead><tbody>
        ${consumo.map(c => `<tr><td><b>Apto ${esc(c.apartamento)}</b></td><td class="num">${c.sessoes}</td><td class="num">${kwh(c.energiaKwh)} kWh</td><td class="num">${brl(c.valor)}</td></tr>`).join('')}
        </tbody><tfoot><tr><td>Total</td><td class="num">${consumo.reduce((s, c) => s + c.sessoes, 0)}</td><td class="num">${kwh(totalKwh)} kWh</td><td class="num">${brl(totalValor)}</td></tr></tfoot></table></div></div>`
    : '<p class="nota">Nenhuma sessão encerrada no período.</p>';

  const recusas = historico.recusasPorVaga.length
    ? `<ul class="lista">${historico.recusasPorVaga.map(r => `<li class="item"><div class="item-linha"><span class="item-titulo">${esc(r.vaga)}</span><span class="numero-medio">${r.recusas}</span></div>
        <div class="item-sub">${r.recusas === 1 ? 'tentativa recusada' : 'tentativas recusadas'}: a vaga estava ocupada</div></li>`).join('')}</ul>`
    : '<p class="nota">Nenhuma tentativa foi recusada no período.</p>';

  resultado.innerHTML = resumo
    + secao('Ocupação por vaga', `<div class="cartao"><div class="barras">${barras}</div></div>`)
    + secao('Uso ao longo do dia', graficoDasHoras(historico.faixasHorarias))
    + secao('Consumo por apartamento', tabela)
    + secao('Demanda reprimida', recusas);
}

async function consultar(evento) {
  if (evento) evento.preventDefault();
  const de = document.getElementById('de').value;
  const ate = document.getElementById('ate').value;
  if (!de || !ate) return aviso('Informe as duas datas do período.', 'erro');
  if (de > ate) return aviso('A data inicial não pode ser depois da final.', 'erro');
  const parametros = new URLSearchParams({ de, ate });
  const vaga = document.getElementById('vaga').value;
  const apartamento = document.getElementById('apartamento').value;
  if (vaga) parametros.set('vagaId', vaga);
  if (apartamento) parametros.set('apartamentoId', apartamento);
  try {
    const botao = document.querySelector('#filtros button[type="submit"]');
    const historico = await comBotao(botao, () => chamar('GET', '/historico?' + parametros.toString()));
    aviso('');
    mostrar(historico);
  } catch (erro) {
    resultado.innerHTML = '';
    aviso(erro.message, 'erro');
  }
}

if (usuario) {
  const hoje = hojeISO();
  document.getElementById('de').value = hoje.slice(0, 8) + '01';
  document.getElementById('ate').value = hoje;
  document.getElementById('filtros').addEventListener('submit', consultar);
  preencherFiltros().then(() => consultar()).catch(erro => aviso(erro.message, 'erro'));
}
