import {
  iniciar, chamar, esc, hora, kwh, brl, tarifa, potencia, selo, classeDaVaga, aviso, confirmar, comBotao,
  minutosAte, duracaoEmTexto,
} from '/js/app.js';

const usuario = iniciar({ perfis: ['MORADOR'] });
const area = document.getElementById('area');
let relogio = null;
let nivelLembrado = 30;

function parar() {
  if (relogio) { clearInterval(relogio); relogio = null; }
}

async function carregar() {
  parar();
  try {
    mostrarSessao(await chamar('GET', '/sessoes/minha'));
  } catch (erro) {
    if (erro.status === 404) return mostrarInicio();
    area.innerHTML = '';
    aviso(erro.message, 'erro');
  }
}

// ---------- sessão aberta ----------

function textoDoTempo(sessao) {
  const minutos = minutosAte(sessao.previsaoConclusao);
  if (sessao.estado === 'EXCEDIDA') return 'Passou da duração máxima e segue aberta. Encerre assim que retirar o veículo.';
  if (minutos >= 0) return `Faltam cerca de ${duracaoEmTexto(minutos)} para a previsão (${hora(sessao.previsaoConclusao)}).`;
  return `A carga já deveria ter terminado há ${duracaoEmTexto(minutos)}. Se já retirou o veículo, encerre para liberar a vaga.`;
}

function mostrarSessao(sessao) {
  area.innerHTML = `<article class="cartao">
      <div class="vaga-topo"><span class="vaga-numero">${esc(sessao.vaga)}</span>${selo(sessao.estado)}</div>
      <p class="nota" style="margin:6px 0 0">${esc(sessao.veiculo)} · bateria inicial ${sessao.nivelBateriaInicial}%</p>
      <dl class="dados">
        <div><dt>Potência efetiva</dt><dd>${potencia(sessao.potenciaEfetiva)}</dd></div>
        <div><dt>Energia a repor</dt><dd>${kwh(sessao.energiaARepor)} kWh</dd></div>
        <div><dt>Início</dt><dd>${hora(sessao.inicio)}</dd></div>
        <div><dt>Previsão de término</dt><dd>${hora(sessao.previsaoConclusao)}</dd></div>
        <div><dt>Tarifa</dt><dd>${tarifa(sessao.valorKwh)}</dd></div>
      </dl>
      <p id="tempo">${esc(textoDoTempo(sessao))}</p>
      <button type="button" id="encerrar" class="botao grande bloco">Encerrar recarga</button>
    </article>`;
  relogio = setInterval(() => { document.getElementById('tempo').textContent = textoDoTempo(sessao); }, 30000);

  document.getElementById('encerrar').addEventListener('click', async evento => {
    const certeza = await confirmar({
      titulo: 'Encerrar a recarga?',
      texto: `O consumo da vaga ${sessao.vaga} é apurado agora, lançado no seu apartamento, e a vaga volta a ficar livre.`,
      ok: 'Encerrar',
    });
    if (!certeza) return;
    try {
      const resultado = await comBotao(evento.target, () => chamar('POST', '/sessoes/minha/encerrar'));
      parar();
      mostrarResultado(resultado);
    } catch (erro) {
      aviso(erro.message, 'erro');
    }
  });
}

function mostrarResultado(sessao) {
  aviso('');
  area.innerHTML = `<article class="cartao">
      <h2>Recarga encerrada</h2>
      <div class="estatisticas">
        <div class="estatistica"><span class="rotulo">Duração</span><span class="numero-grande">${esc(sessao.duracao)}</span></div>
        <div class="estatistica"><span class="rotulo">Energia</span><span class="numero-grande">${kwh(sessao.energiaEstimada)}<span class="unidade">kWh</span></span></div>
        <div class="estatistica"><span class="rotulo">Valor</span><span class="numero-grande">${brl(sessao.valor)}</span></div>
      </div>
      <p class="nota" style="margin-top:14px">A vaga ${esc(sessao.vaga)} foi liberada. O valor usa a tarifa de ${tarifa(sessao.valorKwh)}, a que valia quando você começou, e vai para o consumo do seu apartamento.</p>
      <div class="acoes"><a class="botao" href="/painel.html">Ver o painel</a><a class="botao secundario" href="/consumo.html">Meu consumo</a></div>
    </article>`;
}

// ---------- nova sessão ----------

async function mostrarInicio() {
  let veiculo;
  let painel;
  try {
    [veiculo, painel] = await Promise.all([
      chamar('GET', '/veiculos/meu').catch(erro => { if (erro.status === 404) return null; throw erro; }),
      chamar('GET', '/painel'),
    ]);
  } catch (erro) {
    area.innerHTML = '';
    aviso(erro.message, 'erro');
    return;
  }
  if (!veiculo) {
    area.innerHTML = `<div class="cartao"><h2>Cadastre seu veículo primeiro</h2>
      <p>Para calcular a previsão e o valor da recarga, o sistema precisa do modelo, da bateria e da potência do seu veículo.</p>
      <a class="botao" href="/veiculo.html">Cadastrar veículo</a></div>`;
    return;
  }

  const opcoes = painel.vagas.map(vaga => {
    const livre = vaga.estado === 'LIVRE';
    const detalhe = livre
      ? 'Pronta para uso.'
      : `Libera por volta de ${hora(vaga.previsaoLiberacao)}. Se tentar, a recusa fica registrada como demanda.`;
    return `<label class="opcao-vaga ${classeDaVaga(vaga.estado)}">
        <input type="radio" name="vaga" value="${vaga.id}"${livre ? '' : ''}>
        <div><span class="vaga-numero">${esc(vaga.identificacao)}</span><span class="potencia">${potencia(vaga.potenciaCarregador)}</span>${selo(vaga.estado)}
        <span class="detalhe">${esc(detalhe)}</span></div></label>`;
  }).join('');

  area.innerHTML = `<form id="formulario" class="cartao" novalidate>
      <h2>Iniciar recarga</h2>
      <p class="nota">Veículo: ${esc(veiculo.modelo)}. Escolha a vaga em que você plugou e informe quanto a bateria tem agora.</p>
      <fieldset class="opcoes-vaga"><legend class="rotulo" style="margin-bottom:8px">Vaga</legend>${opcoes || '<p class="vazio">Não há vagas cadastradas.</p>'}</fieldset>
      <label class="campo"><span>Nível da bateria agora (%)</span>
        <div class="nivel"><input id="faixa" type="range" min="0" max="100" value="${nivelLembrado}" aria-label="Nível da bateria"><input id="nivel" type="number" min="0" max="100" inputmode="numeric" value="${nivelLembrado}"></div>
      </label>
      <button class="botao grande bloco" type="submit"${opcoes ? '' : ' disabled'}>Iniciar recarga</button>
    </form>`;

  const faixa = document.getElementById('faixa');
  const nivel = document.getElementById('nivel');
  // Se a vaga for recusada a tela se refaz; o nível que a pessoa já digitou não pode voltar para o padrão
  faixa.addEventListener('input', () => { nivel.value = faixa.value; nivelLembrado = faixa.value; });
  nivel.addEventListener('input', () => { if (nivel.value !== '') { faixa.value = nivel.value; nivelLembrado = nivel.value; } });

  document.getElementById('formulario').addEventListener('submit', async evento => {
    evento.preventDefault();
    const escolhida = document.querySelector('input[name="vaga"]:checked');
    const valor = nivel.value.trim();
    if (!escolhida) return aviso('Escolha a vaga em que você plugou o veículo.', 'erro');
    if (valor === '' || !/^\d+$/.test(valor) || Number(valor) > 100) return aviso('Informe o nível da bateria, de 0 a 100.', 'erro');
    if (Number(valor) >= 100) return aviso('A bateria já está cheia: não há recarga a fazer.', 'erro');
    try {
      const sessao = await comBotao(evento.target.querySelector('button[type="submit"]'),
        () => chamar('POST', '/sessoes', { vagaId: Number(escolhida.value), nivelBateria: Number(valor) }));
      mostrarSessao(sessao);
      aviso(`Recarga iniciada na ${sessao.vaga}. Previsão de término às ${hora(sessao.previsaoConclusao)}.`, 'ok');
    } catch (erro) {
      await mostrarInicio();
      aviso(erro.message, 'erro');
    }
  });
}

if (usuario) carregar();
