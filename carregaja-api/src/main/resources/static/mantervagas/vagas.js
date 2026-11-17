// UC07: o síndico cadastra, altera, desativa e remove as vagas com carregador.
let vagas = [];

async function carregar() {
  try {
    vagas = await api('GET', '/vagas');
    $('linhas').innerHTML = vagas.map(vaga => `
      <tr>
        <td>${esc(vaga.identificacao)}</td>
        <td>${kw(vaga.potenciaCarregador)}</td>
        <td>${vaga.ativa ? selo('Ativa', 'verde') : selo('Desativada', 'cinza')}</td>
        <td>
          <button class="secundario" onclick="editar(${vaga.id})">Editar</button>
          <button class="secundario" onclick="alternar(${vaga.id})">${vaga.ativa ? 'Desativar' : 'Reativar'}</button>
          <button class="perigo" onclick="remover(${vaga.id})">Remover</button>
        </td>
      </tr>`).join('');
  } catch (erro) {
    aviso(erro.message, true);
  }
}

// Editar passa os dados da vaga para o formulário. Se há um id escondido, salvar altera; senão, cadastra.
function editar(id) {
  const vaga = vagas.find(v => v.id === id);
  $('id').value = id;
  $('identificacao').value = vaga.identificacao;
  $('potencia').value = vaga.potenciaCarregador;
  $('salvar').textContent = 'Salvar alterações';
}

function limpar() {
  $('formulario').reset();
  $('id').value = '';
  $('salvar').textContent = 'Cadastrar';
}

$('formulario').onsubmit = async evento => {
  evento.preventDefault();
  const corpo = { identificacao: $('identificacao').value, potenciaCarregador: Number($('potencia').value) };
  try {
    if ($('id').value) await api('PUT', '/vagas/' + $('id').value, corpo);
    else await api('POST', '/vagas', corpo);
    limpar();
    aviso('Vaga salva.');
    carregar();
  } catch (erro) {
    aviso(erro.message, true);
  }
};

async function alternar(id) {
  const vaga = vagas.find(v => v.id === id);
  try {
    await api('POST', `/vagas/${id}/${vaga.ativa ? 'desativar' : 'reativar'}`);
    aviso('Vaga ' + (vaga.ativa ? 'desativada.' : 'reativada.'));
    carregar();
  } catch (erro) {
    aviso(erro.message, true);
  }
}

async function remover(id) {
  if (!confirm('Remover a vaga? Só vale para vaga que nunca foi usada; senão, desative.')) return;
  try {
    await api('DELETE', '/vagas/' + id);
    aviso('Vaga removida.');
    carregar();
  } catch (erro) {
    aviso(erro.message, true);
  }
}

if (iniciarPagina()) carregar();
