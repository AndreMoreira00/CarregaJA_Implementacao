// UC11: fecha o mês, mostra o rateio por apartamento e exporta o resultado em um arquivo CSV.
let fechamentos = [];
let atual = null;   // o rateio que está na tela

async function carregarLista() {
  try {
    fechamentos = await api('GET', '/fechamentos');
    $('lista').innerHTML = fechamentos.map(f => `
      <li>${f.mes}/${f.ano}: ${reais(f.valorTotal)}, fechado em ${dataHora(f.momento)}
        <button class="secundario" onclick="ver(${f.id})">Ver rateio</button></li>`).join('');
  } catch (erro) {
    aviso(erro.message, true);
  }
}

function ver(id) {
  mostrarRateio(fechamentos.find(f => f.id === id));
}

async function fechar() {
  const [ano, mes] = $('mes').value.split('-');
  if (!confirm(`Fechar ${mes}/${ano}? Depois de fechado, o mês não é recalculado.`)) return;
  try {
    mostrarRateio(await api('POST', '/fechamentos', { ano: Number(ano), mes: Number(mes) }));
    aviso('Mês fechado.');
    carregarLista();
  } catch (erro) {
    aviso(erro.message, true);
  }
}

function mostrarRateio(fechamento) {
  atual = fechamento;
  $('resultado').innerHTML = `
    <h2>Rateio de ${fechamento.mes}/${fechamento.ano}</h2>
    <div class="rolagem"><table>
      <thead><tr><th>Apartamento</th><th>Energia</th><th>Valor</th></tr></thead>
      <tbody>${fechamento.itens.map(item => `
        <tr><td>${esc(item.apartamento)}</td><td>${kwh(item.energia)}</td><td>${reais(item.valor)}</td></tr>`).join('')}
      </tbody>
      <tfoot><tr><th>Total</th><th>${kwh(fechamento.energiaTotal)}</th><th>${reais(fechamento.valorTotal)}</th></tr></tfoot>
    </table></div>
    <p>Fechado em ${dataHora(fechamento.momento)} por ${esc(fechamento.nomeResponsavel)}.</p>
    <button class="secundario" onclick="exportar()">Exportar CSV</button>`;
}

// Monta o texto do arquivo (colunas separadas por ponto e vírgula, que o Excel em português abre direto) e baixa.
function exportar() {
  const linhas = ['Apartamento;Energia (kWh);Valor (R$)'];
  atual.itens.forEach(item => linhas.push(`${item.apartamento};${virgula(item.energia, 2)};${virgula(item.valor, 2)}`));
  linhas.push(`Total;${virgula(atual.energiaTotal, 2)};${virgula(atual.valorTotal, 2)}`);

  const arquivo = new Blob(['﻿' + linhas.join('\r\n')], { type: 'text/csv;charset=utf-8' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(arquivo);
  link.download = `rateio-${atual.ano}-${String(atual.mes).padStart(2, '0')}.csv`;
  link.click();
}

$('mes').value = anoMes(-1);   // por padrão, o mês que acabou de terminar
if (iniciarPagina()) carregarLista();
