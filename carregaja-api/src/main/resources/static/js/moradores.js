import { iniciar, chamar, esc, selo, aviso, confirmar, comBotao } from '/js/app.js';

const usuario = iniciar({ perfis: ['SINDICO'] });
const lista = document.getElementById('lista');
const cartao = document.getElementById('formulario-cartao');
const formulario = document.getElementById('formulario');
let pessoas = [];
let editando = null;

function chip(texto) {
  return `<span class="selo selo-neutro">${esc(texto)}</span>`;
}

function item(pessoa) {
  const perfis = [];
  if (pessoa.perfis.includes('MORADOR')) perfis.push(chip('Morador'));
  if (pessoa.perfis.includes('SINDICO')) perfis.push(chip('Síndico'));
  const apto = pessoa.apartamento ? `Apto ${esc(pessoa.apartamento.numero)} · ` : '';
  const eu = pessoa.id === usuario.id;
  return `<li class="item${pessoa.ativo ? '' : ' inativo'}" data-id="${pessoa.id}">
      <div class="item-linha"><span class="item-titulo" style="font-size:24px">${esc(pessoa.nome)}</span>
        ${pessoa.ativo ? selo('LIVRE', 'Ativo') : selo('ENCERRADA', 'Desativado')}</div>
      <div class="item-sub">${apto}${esc(pessoa.email)}</div>
      <div class="chips">${perfis.join('')}${eu ? chip('Você') : ''}</div>
      <div class="acoes">
        <button type="button" class="botao secundario pequeno" data-acao="editar">Editar</button>
        <button type="button" class="botao secundario pequeno" data-acao="${pessoa.ativo ? 'desativar' : 'reativar'}">${pessoa.ativo ? 'Desativar' : 'Reativar'}</button>
        <button type="button" class="botao secundario pequeno" data-acao="remover">Remover</button>
      </div></li>`;
}

function mostrar() {
  pessoas.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
  lista.innerHTML = pessoas.length ? pessoas.map(item).join('') : '<li class="vazio">Nenhum morador cadastrado.</li>';
}

async function carregar() {
  try {
    pessoas = await chamar('GET', '/usuarios');
    mostrar();
  } catch (erro) {
    aviso(erro.message, 'erro');
  }
}

function abrirFormulario(pessoa) {
  editando = pessoa || null;
  formulario.reset();
  document.getElementById('formulario-titulo').textContent = pessoa ? `Editar ${pessoa.nome}` : 'Novo morador';
  document.getElementById('salvar').textContent = pessoa ? 'Salvar alterações' : 'Cadastrar';
  document.getElementById('senha-rotulo').textContent = pessoa ? 'Nova senha provisória (opcional)' : 'Senha provisória';
  document.getElementById('senha-dica').textContent = pessoa
    ? 'Só preencha se a pessoa esqueceu a senha: ela volta a ser provisória e é trocada no próximo acesso.'
    : 'De 8 a 72 caracteres. A pessoa troca no primeiro acesso.';
  if (pessoa) {
    document.getElementById('nome').value = pessoa.nome;
    document.getElementById('email').value = pessoa.email;
    document.getElementById('apartamento').value = pessoa.apartamento ? pessoa.apartamento.numero : '';
    document.getElementById('sindico').checked = pessoa.perfis.includes('SINDICO');
  }
  cartao.hidden = false;
  cartao.scrollIntoView({ behavior: 'smooth', block: 'start' });
  document.getElementById('nome').focus({ preventScroll: true });
}

function fecharFormulario() {
  cartao.hidden = true;
  editando = null;
}

async function acao(botao) {
  const id = Number(botao.closest('[data-id]').dataset.id);
  const pessoa = pessoas.find(item => item.id === id);
  const nome = botao.dataset.acao;
  try {
    if (nome === 'editar') return abrirFormulario(pessoa);
    if (nome === 'desativar') {
      const certeza = await confirmar({
        titulo: `Desativar ${pessoa.nome}?`,
        texto: 'O acesso é cortado na hora, mesmo que a pessoa esteja com o sistema aberto. O cadastro e as sessões continuam guardados.',
        ok: 'Desativar',
      });
      if (!certeza) return;
      const atualizada = await comBotao(botao, () => chamar('POST', `/usuarios/${id}/desativar`));
      pessoas = pessoas.map(item => (item.id === id ? atualizada : item));
      aviso(`${pessoa.nome} foi desativado(a).`, 'ok');
    } else if (nome === 'reativar') {
      const atualizada = await comBotao(botao, () => chamar('POST', `/usuarios/${id}/reativar`));
      pessoas = pessoas.map(item => (item.id === id ? atualizada : item));
      aviso(`${pessoa.nome} voltou a ter acesso.`, 'ok');
    } else if (nome === 'remover') {
      const certeza = await confirmar({
        titulo: `Remover ${pessoa.nome}?`,
        texto: 'A remoção é definitiva e só vale para quem nunca usou o sistema. Se houver sessões, o sistema recusa e você pode desativar.',
        ok: 'Remover',
        perigo: true,
      });
      if (!certeza) return;
      await comBotao(botao, () => chamar('DELETE', `/usuarios/${id}`));
      pessoas = pessoas.filter(item => item.id !== id);
      aviso(`${pessoa.nome} foi removido(a).`, 'ok');
    }
    mostrar();
  } catch (erro) {
    aviso(erro.message, 'erro');
  }
}

if (usuario) {
  lista.addEventListener('click', evento => {
    const botao = evento.target.closest('[data-acao]');
    if (botao) acao(botao);
  });
  document.getElementById('abrir').addEventListener('click', () => abrirFormulario(null));
  document.getElementById('cancelar').addEventListener('click', fecharFormulario);

  formulario.addEventListener('submit', async evento => {
    evento.preventDefault();
    const nome = document.getElementById('nome').value.trim();
    const email = document.getElementById('email').value.trim();
    const apartamento = document.getElementById('apartamento').value.trim();
    const sindico = document.getElementById('sindico').checked;
    const senha = document.getElementById('senha').value;
    if (!nome) return aviso('Informe o nome.', 'erro');
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return aviso('Informe um e-mail válido.', 'erro');
    if (!sindico && !apartamento) return aviso('Informe o apartamento do morador.', 'erro');
    if (!editando && !senha) return aviso('Informe a senha provisória.', 'erro');
    if (senha && (senha.length < 8 || senha.length > 72)) return aviso('A senha provisória deve ter de 8 a 72 caracteres.', 'erro');

    const corpo = { nome, email, apartamento: apartamento || null, sindico };
    if (senha) corpo.senhaProvisoria = senha;
    try {
      const salva = await comBotao(document.getElementById('salvar'),
        () => (editando ? chamar('PUT', `/usuarios/${editando.id}`, corpo) : chamar('POST', '/usuarios', corpo)));
      const novo = !editando;
      pessoas = novo ? [...pessoas, salva] : pessoas.map(item => (item.id === salva.id ? salva : item));
      fecharFormulario();
      mostrar();
      aviso(novo ? `${salva.nome} cadastrado(a). A pessoa entra com a senha provisória e troca no primeiro acesso.` : `${salva.nome} atualizado(a).`, 'ok');
    } catch (erro) {
      aviso(erro.message, 'erro');
    }
  });

  carregar();
}
