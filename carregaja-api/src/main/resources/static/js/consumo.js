// UC06: o consumo do mês do apartamento do morador, sessão a sessão.
async function carregar() {
  const [ano, mes] = $('mes').value.split('-');
  try {
    const consumo = await api('GET', `/consumos/meu?ano=${ano}&mes=${Number(mes)}`);
    aviso('');
    if (consumo.semRegistros) {
      $('resultado').innerHTML = '<p>' + esc(consumo.mensagem) + '</p>';
      return;
    }
    $('resultado').innerHTML = `
      <p>Apartamento ${esc(consumo.apartamento)}: <b>${kwh(consumo.energiaKwh)}</b>, <b>${reais(consumo.valor)}</b> no mês.</p>
      <div class="rolagem"><table>
        <thead><tr><th>Vaga</th><th>Início</th><th>Fim</th><th>Energia</th><th>Valor</th><th></th></tr></thead>
        <tbody>${consumo.sessoes.map(sessao => `
          <tr>
            <td>${esc(sessao.vaga)}</td>
            <td>${dataHora(sessao.inicio)}</td>
            <td>${dataHora(sessao.fim)}</td>
            <td>${kwh(sessao.energiaKwh)}</td>
            <td>${reais(sessao.valor)}</td>
            <td>${sessao.situacao === 'ENCERRADA_ADMINISTRATIVAMENTE' ? 'Encerrada pelo síndico' : ''}</td>
          </tr>`).join('')}
        </tbody>
      </table></div>`;
  } catch (erro) {
    aviso(erro.message, true);
  }
}

$('mes').value = anoMes(0);
$('mes').onchange = carregar;
if (iniciarPagina()) carregar();
