import { iniciar, chamar, esc, potencia, selo, aviso, confirmar, comBotao } from '/js/app.js';

const usuario = iniciar({ perfis: ['SINDICO'] });
const lista = document.getElementById('lista');
let vagas = [];
let editando = null;

function validar(identificacao, potenciaCarregador) {
  if (!identificacao) return 'Informe a identificação da vaga.';
  if (!(potenciaCarregador > 0)) return 'A potência do carregador deve ser maior que zero.';
  return null;
}

function item(vaga) {
  if (editando === vaga.id) {
    return `<li class="item" data-id="${vaga.id}"><form class="form-edicao" novalidate>
        <div class="grade-campos">
          <label class="campo"><span>Identificação</span><input name="identificacao" type="text" maxlength="20" autocomplete="off" value="${esc(vaga.identificacao)}"></label>
          <label class="campo"><span>Potência do carregador (kW)</span><input name="potencia" type="number" inputmode="decimal" step="any" min="0" value="${esc(vaga.potenciaCarregador)}"></label>
        </div>
        <div class="acoes"><button class="botao pequeno" type="submit">Salvar</button><button type="button" class="botao secundario pequeno" data-acao="cancelar">Cancelar</button></div>
      </form></li>`;
  }
  return `<li class="item${vaga.ativa ? '' : ' inativo'}" data-id="${vaga.id}">
      <div class="item-linha"><span class="item-titulo">${esc(vaga.identificacao)}</span>${vaga.ativa ? selo('LIVRE', 'Ativa') : selo('ENCERRADA', 'Desativada')}</div>
      <div class="item-sub">Carregador de ${potencia(vaga.potenciaCarregador)}${vaga.ativa ? '' : ' · não aceita novas sessões, o histórico fica guardado'}</div>
      <div class="acoes">
        <button type="button" class="botao secundario pequeno" data-acao="editar">Editar</button>
        <button type="button" class="botao secundario pequeno" data-acao="${vaga.ativa ? 'desativar' : 'reativar'}">${vaga.ativa ? 'Desativar' : 'Reativar'}</button>
        <button type="button" class="botao secundario pequeno" data-acao="remover">Remover</button>
      </div></li>`;
}

function mostrar() {
  vagas.sort((a, b) => a.identificacao.localeCompare(b.identificacao, 'pt-BR', { numeric: true }));
  lista.innerHTML = vagas.length ? vagas.map(item).join('') : '<li class="vazio">Nenhuma vaga cadastrada ainda. Cadastre a primeira acima.</li>';
}

async function carregar() {
  try {
    vagas = await chamar('GET', '/vagas');
    mostrar();
  } catch (erro) {
    aviso(erro.message, 'erro');
  }
}

function trocar(atualizada) {
  vagas = vagas.map(vaga => (vaga.id === atualizada.id ? atualizada : vaga));
  mostrar();
}

async function acao(botao) {
  const id = Number(botao.closest('[data-id]').dataset.id);
  const vaga = vagas.find(item => item.id === id);
  const nome = botao.dataset.acao;
  try {
    if (nome === 'editar') { editando = id; return mostrar(); }
    if (nome === 'cancelar') { editando = null; return mostrar(); }
    if (nome === 'desativar') {
      const certeza = await confirmar({
        titulo: `Desativar a ${vaga.identificacao}?`,
        texto: 'Ela deixa de aceitar novas recargas e sai do painel. O histórico continua guardado, e você pode reativá-la depois.',
        ok: 'Desativar',
      });
      if (!certeza) return;
      trocar(await comBotao(botao, () => chamar('POST', `/vagas/${id}/desativar`)));
      aviso(`Vaga ${vaga.identificacao} desativada.`, 'ok');
    } else if (nome === 'reativar') {
      trocar(await comBotao(botao, () => chamar('POST', `/vagas/${id}/reativar`)));
      aviso(`Vaga ${vaga.identificacao} reativada.`, 'ok');
    } else if (nome === 'remover') {
      const certeza = await confirmar({
        titulo: `Remover a ${vaga.identificacao}?`,
        texto: 'A remoção é definitiva e só vale para vaga que nunca foi usada. Se já teve sessões, o sistema recusa e você pode desativá-la.',
        ok: 'Remover',
        perigo: true,
      });
      if (!certeza) return;
      await comBotao(botao, () => chamar('DELETE', `/vagas/${id}`));
      vagas = vagas.filter(item => item.id !== id);
      mostrar();
      aviso(`Vaga ${vaga.identificacao} removida.`, 'ok');
    }
  } catch (erro) {
    aviso(erro.message, 'erro');
  }
}

if (usuario) {
  lista.addEventListener('click', evento => {
    const botao = evento.target.closest('[data-acao]');
    if (botao) acao(botao);
  });

  lista.addEventListener('submit', async evento => {
    evento.preventDefault();
    const formulario = evento.target;
    const id = Number(formulario.closest('[data-id]').dataset.id);
    const identificacao = formulario.identificacao.value.trim();
    const potenciaCarregador = Number(formulario.potencia.value);
    const problema = validar(identificacao, potenciaCarregador);
    if (problema) return aviso(problema, 'erro');
    try {
      const salva = await comBotao(formulario.querySelector('button[type="submit"]'),
        () => chamar('PUT', `/vagas/${id}`, { identificacao, potenciaCarregador }));
      editando = null;
      trocar(salva);
      aviso(`Vaga ${salva.identificacao} atualizada.`, 'ok');
    } catch (erro) {
      aviso(erro.message, 'erro');
    }
  });

  document.getElementById('nova').addEventListener('submit', async evento => {
    evento.preventDefault();
    const identificacao = document.getElementById('identificacao').value.trim();
    const potenciaCarregador = Number(document.getElementById('potencia').value);
    const problema = validar(identificacao, potenciaCarregador);
    if (problema) return aviso(problema, 'erro');
    try {
      const criada = await comBotao(evento.target.querySelector('button[type="submit"]'),
        () => chamar('POST', '/vagas', { identificacao, potenciaCarregador }));
      vagas.push(criada);
      mostrar();
      evento.target.reset();
      aviso(`Vaga ${criada.identificacao} cadastrada.`, 'ok');
    } catch (erro) {
      aviso(erro.message, 'erro');
    }
  });

  carregar();
}
