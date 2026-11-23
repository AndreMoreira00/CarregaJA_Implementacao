// UC09: o síndico cadastra, altera, desativa e remove moradores (e síndicos).
let pessoas = [];

async function carregar() {
  try {
    pessoas = await api('GET', '/usuarios');
    $('linhas').innerHTML = pessoas.map(pessoa => `
      <tr>
        <td>${esc(pessoa.nome)}</td>
        <td>${esc(pessoa.email)}</td>
        <td>${pessoa.apartamento ? esc(pessoa.apartamento.numero) : ''}</td>
        <td>${pessoa.perfis.includes('SINDICO') ? 'Síndico' : 'Morador'}</td>
        <td>${pessoa.ativo ? selo('Ativo', 'verde') : selo('Desativado', 'cinza')}</td>
        <td>
          <button class="secundario" onclick="editar(${pessoa.id})">Editar</button>
          <button class="secundario" onclick="alternar(${pessoa.id})">${pessoa.ativo ? 'Desativar' : 'Reativar'}</button>
          <button class="perigo" onclick="remover(${pessoa.id})">Remover</button>
        </td>
      </tr>`).join('');
  } catch (erro) {
    aviso(erro.message, true);
  }
}

// Editar passa os dados para o formulário. Se há um id escondido, salvar altera; senão, cadastra.
function editar(id) {
  const pessoa = pessoas.find(p => p.id === id);
  $('id').value = id;
  $('nome').value = pessoa.nome;
  $('email').value = pessoa.email;
  $('apartamento').value = pessoa.apartamento ? pessoa.apartamento.numero : '';
  $('sindico').checked = pessoa.perfis.includes('SINDICO');
  $('senha').value = '';
  $('senha').required = false;   // ao editar, a senha só é preenchida se a pessoa esqueceu a dela
  $('salvar').textContent = 'Salvar alterações';
}

function limpar() {
  $('formulario').reset();
  $('id').value = '';
  $('senha').required = true;
  $('salvar').textContent = 'Cadastrar';
}

$('formulario').onsubmit = async evento => {
  evento.preventDefault();
  const corpo = {
    nome: $('nome').value,
    email: $('email').value,
    apartamento: $('apartamento').value || null,
    sindico: $('sindico').checked,
  };
  if ($('senha').value) corpo.senhaProvisoria = $('senha').value;
  try {
    if ($('id').value) await api('PUT', '/usuarios/' + $('id').value, corpo);
    else await api('POST', '/usuarios', corpo);
    limpar();
    aviso('Salvo. Quem tem senha provisória troca no primeiro acesso.');
    carregar();
  } catch (erro) {
    aviso(erro.message, true);
  }
};

async function alternar(id) {
  const pessoa = pessoas.find(p => p.id === id);
  try {
    await api('POST', `/usuarios/${id}/${pessoa.ativo ? 'desativar' : 'reativar'}`);
    aviso(pessoa.ativo ? 'Desativado: o acesso foi cortado.' : 'Reativado.');
    carregar();
  } catch (erro) {
    aviso(erro.message, true);
  }
}

async function remover(id) {
  if (!confirm('Remover? Só vale para quem nunca usou o sistema; senão, desative.')) return;
  try {
    await api('DELETE', '/usuarios/' + id);
    aviso('Removido.');
    carregar();
  } catch (erro) {
    aviso(erro.message, true);
  }
}

if (iniciarPagina()) carregar();
