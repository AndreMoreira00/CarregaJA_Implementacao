// UC03: iniciar e encerrar a recarga. Se o morador já tem sessão aberta, mostra a sessão; senão, o formulário.
async function carregar() {
  try {
    mostrarSessao(await api('GET', '/sessoes/minha'));
  } catch (erro) {
    if (erro.status === 404) mostrarFormulario();   // 404: não há sessão aberta
    else aviso(erro.message, true);
  }
}

async function mostrarFormulario() {
  $('sessao').innerHTML = '';
  $('nova').hidden = false;
  try {
    const painel = await api('GET', '/painel');
    const escolhida = $('vaga').value;   // a lista é refeita; a vaga que a pessoa já escolheu continua escolhida
    $('vaga').innerHTML = painel.vagas.map(vaga =>
      `<option value="${vaga.id}">${esc(vaga.identificacao)} - ${SITUACAO[vaga.estado]}</option>`).join('');
    if (escolhida) $('vaga').value = escolhida;
  } catch (erro) {
    aviso(erro.message, true);
  }
}

function mostrarSessao(sessao) {
  $('nova').hidden = true;
  $('sessao').innerHTML = `
    <p>Vaga <b>${esc(sessao.vaga)}</b> ${selo(SITUACAO[sessao.estado], COR[sessao.estado])}</p>
    <ul>
      <li>Veículo: ${esc(sessao.veiculo)}, bateria inicial ${sessao.nivelBateriaInicial}%</li>
      <li>Início: ${hora(sessao.inicio)}. Previsão de término: ${hora(sessao.previsaoConclusao)}</li>
      <li>Potência efetiva: ${kw(sessao.potenciaEfetiva)}. Energia a repor: ${kwh(sessao.energiaARepor)}</li>
      <li>Tarifa: ${reais(sessao.valorKwh)} por kWh</li>
    </ul>
    <button onclick="encerrar()">Encerrar recarga</button>`;
}

$('nova').onsubmit = async evento => {
  evento.preventDefault();
  try {
    await api('POST', '/sessoes', { vagaId: Number($('vaga').value), nivelBateria: Number($('nivel').value) });
    aviso('Recarga iniciada.');
    carregar();
  } catch (erro) {
    aviso(erro.message, true);
    mostrarFormulario();   // atualiza as vagas: a escolhida pode ter sido ocupada por outro morador
  }
};

async function encerrar() {
  if (!confirm('Encerrar a recarga? A vaga volta a ficar livre.')) return;
  try {
    const sessao = await api('POST', '/sessoes/minha/encerrar');
    await carregar();
    aviso(`Recarga encerrada. Duração: ${sessao.duracao}. Energia: ${kwh(sessao.energiaEstimada)}. Valor: ${reais(sessao.valor)}.`);
  } catch (erro) {
    aviso(erro.message, true);
  }
}

if (iniciarPagina()) carregar();
