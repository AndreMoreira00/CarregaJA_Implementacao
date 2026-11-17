// UC12: como a garagem foi usada no período. Cada bloco é uma tabela.
function tabela(titulo, cabecalhos, linhas) {
  if (linhas.length === 0) return `<h2>${titulo}</h2><p>Nada no período.</p>`;
  return `<h2>${titulo}</h2><div class="rolagem"><table>
    <thead><tr>${cabecalhos.map(c => `<th>${c}</th>`).join('')}</tr></thead>
    <tbody>${linhas.map(linha => `<tr>${linha.map(celula => `<td>${celula}</td>`).join('')}</tr>`).join('')}</tbody>
  </table></div>`;
}

async function consultar() {
  try {
    const h = await api('GET', `/historico?de=${$('de').value}&ate=${$('ate').value}`);
    aviso('');
    if (h.semRegistros) {
      $('resultado').innerHTML = '<p>' + esc(h.mensagem) + '</p>';
      return;
    }
    $('resultado').innerHTML =
      `<p>Horário de pico: <b>${h.faixaPico === null ? 'nenhum' : h.faixaPico + 'h'}</b>.
          Recusas por falta de vaga: <b>${h.totalRecusas}</b>.</p>` +
      tabela('Ocupação por vaga', ['Vaga', 'Horas ocupada', '% do período'],
        h.ocupacaoPorVaga.map(o => [esc(o.vaga), o.horasOcupadas, o.percentualPeriodo + '%'])) +
      tabela('Uso por hora do dia', ['Hora', 'Minutos ocupada', 'Recusas'],
        h.faixasHorarias.filter(f => f.minutosOcupados > 0 || f.recusas > 0).map(f => [f.hora + 'h', f.minutosOcupados, f.recusas])) +
      tabela('Consumo por apartamento', ['Apartamento', 'Sessões', 'Energia', 'Valor'],
        h.consumoPorApartamento.map(c => [esc(c.apartamento), c.sessoes, kwh(c.energiaKwh), reais(c.valor)])) +
      tabela('Recusas por vaga ocupada', ['Vaga', 'Recusas'],
        h.recusasPorVaga.map(r => [esc(r.vaga), r.recusas]));
  } catch (erro) {
    aviso(erro.message, true);
  }
}

$('de').value = anoMes(0) + '-01';   // do primeiro dia do mês...
$('ate').value = new Date().toLocaleDateString('sv-SE');   // ...até hoje (o formato sueco é aaaa-mm-dd)
if (iniciarPagina()) consultar();
