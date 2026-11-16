import { iniciar, chamar, esc, aviso, comBotao } from '/js/app.js';

const usuario = iniciar({ perfis: ['MORADOR'] });
const area = document.getElementById('area');
let veiculo = null;

async function carregar() {
  try {
    veiculo = await chamar('GET', '/veiculos/meu');
  } catch (erro) {
    if (erro.status !== 404) {
      area.innerHTML = '';
      aviso(erro.message, 'erro');
      return;
    }
    veiculo = null;
  }
  mostrar();
}

function mostrar() {
  const valor = campo => (veiculo ? esc(veiculo[campo]) : '');
  area.innerHTML = `<form id="formulario" class="cartao" novalidate>
      <h2>${veiculo ? 'Seu veículo' : 'Cadastrar veículo'}</h2>
      <label class="campo"><span>Modelo</span><input id="modelo" type="text" maxlength="100" autocomplete="off" placeholder="Nissan Leaf" value="${valor('modelo')}"></label>
      <div class="grade-campos">
        <label class="campo"><span>Capacidade da bateria (kWh)</span>
          <input id="capacidade" type="number" inputmode="decimal" step="any" min="0" placeholder="40" value="${valor('capacidadeBateria')}">
          <small>Está no manual ou no documento do veículo.</small></label>
        <label class="campo"><span>Potência máxima de carga (kW)</span>
          <input id="potencia" type="number" inputmode="decimal" step="any" min="0" placeholder="6,6" value="${valor('potenciaMaxima')}">
          <small>O máximo que o carro aceita em corrente alternada, mesmo que o carregador seja mais forte.</small></label>
      </div>
      <button class="botao" type="submit">${veiculo ? 'Salvar alterações' : 'Cadastrar veículo'}</button>
    </form>
    ${veiculo ? '<p style="margin-top:16px"><a class="botao secundario" href="/recarga.html">Ir para a recarga</a></p>' : ''}`;

  document.getElementById('formulario').addEventListener('submit', async evento => {
    evento.preventDefault();
    const modelo = document.getElementById('modelo').value.trim();
    const capacidade = Number(document.getElementById('capacidade').value);
    const potenciaMaxima = Number(document.getElementById('potencia').value);
    if (!modelo) return aviso('Informe o modelo do veículo.', 'erro');
    if (!(capacidade > 0)) return aviso('A capacidade da bateria deve ser maior que zero.', 'erro');
    if (!(potenciaMaxima > 0)) return aviso('A potência máxima de carga deve ser maior que zero.', 'erro');
    const corpo = { modelo, capacidadeBateria: capacidade, potenciaMaxima };
    const novo = !veiculo;
    try {
      veiculo = await comBotao(evento.target.querySelector('button[type="submit"]'),
        () => (novo ? chamar('POST', '/veiculos', corpo) : chamar('PUT', `/veiculos/${veiculo.id}`, corpo)));
      mostrar();
      aviso(novo ? 'Veículo cadastrado. Agora você pode iniciar uma recarga.' : 'Veículo atualizado. As sessões já abertas mantêm os valores do início.', 'ok');
    } catch (erro) {
      aviso(erro.message, 'erro');
    }
  });
}

if (usuario) carregar();
