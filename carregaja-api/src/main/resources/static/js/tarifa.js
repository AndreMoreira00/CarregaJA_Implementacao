import { iniciar, chamar, esc, tarifa, dataHora, selo, aviso, comBotao } from '/js/app.js';

const usuario = iniciar({ perfis: ['SINDICO'] });

function mostrar(tarifas) {
  tarifas.sort((a, b) => String(b.inicioVigencia).localeCompare(String(a.inicioVigencia)));
  const vigente = tarifas[0];
  document.getElementById('vigente').innerHTML = vigente
    ? `<span class="rotulo">Vigente desde ${dataHora(vigente.inicioVigencia)}</span><br>
       <span class="numero-grande">${tarifa(vigente.valorKwh)}</span>
       <p class="nota" style="margin:8px 0 0">Cadastrada por ${esc(vigente.nomeResponsavel)}. Vale para as sessões iniciadas a partir daí.</p>`
    : '<span class="rotulo">Tarifa vigente</span><p class="vazio" style="margin:8px 0 0">Ainda não há tarifa cadastrada: ninguém consegue iniciar uma recarga.</p>';

  document.getElementById('lista').innerHTML = tarifas.length
    ? tarifas.map((item, indice) => {
      const seguinte = tarifas[indice - 1];
      const periodo = seguinte ? `de ${dataHora(item.inicioVigencia)} até ${dataHora(seguinte.inicioVigencia)}` : `desde ${dataHora(item.inicioVigencia)}`;
      return `<li class="item"><div class="item-linha"><span class="item-titulo">${tarifa(item.valorKwh)}</span>${indice === 0 ? selo('LIVRE', 'Vigente') : selo('ENCERRADA', 'Anterior')}</div>
        <div class="item-sub">${periodo} · cadastrada por ${esc(item.nomeResponsavel)}</div></li>`;
    }).join('')
    : '<li class="vazio">Nenhuma tarifa cadastrada.</li>';
}

async function carregar() {
  try {
    mostrar(await chamar('GET', '/tarifas'));
  } catch (erro) {
    aviso(erro.message, 'erro');
  }
}

if (usuario) {
  document.getElementById('nova').addEventListener('submit', async evento => {
    evento.preventDefault();
    const texto = document.getElementById('valor').value.trim();
    const valor = Number(texto);
    if (!texto || !(valor > 0)) return aviso('O valor do kWh deve ser maior que zero.', 'erro');
    if (!/^\d{1,6}([.,]\d{1,4})?$/.test(texto)) return aviso('O valor do kWh aceita até 4 casas decimais.', 'erro');
    try {
      await comBotao(evento.target.querySelector('button[type="submit"]'), () => chamar('POST', '/tarifas', { valorKwh: valor }));
      evento.target.reset();
      await carregar();
      aviso('Tarifa cadastrada. Vale a partir de agora, e as sessões que já começaram seguem a tarifa do início.', 'ok');
    } catch (erro) {
      aviso(erro.message, 'erro');
    }
  });
  carregar();
}
