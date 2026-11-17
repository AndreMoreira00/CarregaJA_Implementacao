// UC10: o síndico encerra a sessão de quem já tirou o veículo da vaga.
let sessoes = [];

async function carregar() {
  try {
    sessoes = await api('GET', '/sessoes/abertas');
    $('linhas').innerHTML = sessoes.map(sessao => `
      <tr>
        <td>${esc(sessao.vaga)}</td>
        <td>Apto ${esc(sessao.apartamento)} - ${esc(sessao.morador)}</td>
        <td>${dataHora(sessao.inicio)}</td>
        <td>${hora(sessao.previsaoConclusao)}</td>
        <td>${selo(SITUACAO[sessao.estado], COR[sessao.estado])}</td>
        <td>${sessao.estado === 'EM_ANDAMENTO' ? 'Dentro da previsão' : `<button onclick="encerrar(${sessao.id})">Encerrar</button>`}</td>
      </tr>`).join('');
  } catch (erro) {
    aviso(erro.message, true);
  }
}

async function encerrar(id) {
  const sessao = sessoes.find(s => s.id === id);
  if (!confirm(`Você viu na garagem que o veículo da vaga ${sessao.vaga} já saiu? O encerramento fica registrado com o seu nome.`)) return;
  try {
    const resultado = await api('POST', `/sessoes/${id}/encerrar-administrativamente`);
    aviso(`Sessão encerrada: ${kwh(resultado.energiaEstimada)}, ${reais(resultado.valor)}.`);
    carregar();
  } catch (erro) {
    aviso(erro.message, true);
  }
}

if (iniciarPagina()) carregar();
