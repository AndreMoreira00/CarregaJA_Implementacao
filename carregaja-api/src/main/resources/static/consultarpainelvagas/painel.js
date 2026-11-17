// UC02: o painel mostra cada vaga, a situação dela e a previsão de liberação.
async function carregar() {
  try {
    const painel = await api('GET', '/painel');
    $('resumo').textContent = painel.vagasLivres > 0 ? painel.vagasLivres + ' vaga(s) livre(s).' : painel.mensagem;
    $('linhas').innerHTML = painel.vagas.map(vaga => `
      <tr>
        <td>${esc(vaga.identificacao)}</td>
        <td>${kw(vaga.potenciaCarregador)}</td>
        <td>${selo(SITUACAO[vaga.estado], COR[vaga.estado])}</td>
        <td>${vaga.previsaoLiberacao ? hora(vaga.previsaoLiberacao) : ''}</td>
        <td>${vaga.morador ? 'Apto ' + esc(vaga.apartamento) + ' - ' + esc(vaga.morador) : ''}</td>
      </tr>`).join('');
    aviso('');
  } catch (erro) {
    aviso(erro.message, true);
  }
}

if (iniciarPagina()) carregar();
