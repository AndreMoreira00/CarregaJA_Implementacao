// UC08: a tarifa nova vale a partir do momento em que é cadastrada, até a próxima.
async function carregar() {
  try {
    const tarifas = await api('GET', '/tarifas');
    tarifas.sort((a, b) => b.inicioVigencia.localeCompare(a.inicioVigencia));   // a mais nova primeiro
    $('vigente').innerHTML = tarifas.length
      ? `Tarifa vigente: <b>${reais(tarifas[0].valorKwh)}</b> por kWh, desde ${dataHora(tarifas[0].inicioVigencia)}.`
      : 'Ainda não há tarifa: ninguém consegue iniciar uma recarga.';
    $('linhas').innerHTML = tarifas.map(tarifa => `
      <tr>
        <td>${dataHora(tarifa.inicioVigencia)}</td>
        <td>${reais(tarifa.valorKwh)} por kWh</td>
        <td>${esc(tarifa.nomeResponsavel)}</td>
      </tr>`).join('');
  } catch (erro) {
    aviso(erro.message, true);
  }
}

$('formulario').onsubmit = async evento => {
  evento.preventDefault();
  try {
    await api('POST', '/tarifas', { valorKwh: Number($('valor').value) });
    $('formulario').reset();
    aviso('Tarifa cadastrada. As sessões que já começaram seguem a tarifa do início.');
    carregar();
  } catch (erro) {
    aviso(erro.message, true);
  }
};

if (iniciarPagina()) carregar();
