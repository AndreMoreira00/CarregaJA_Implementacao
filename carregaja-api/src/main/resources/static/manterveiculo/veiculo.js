// UC05: o morador cadastra o veículo uma vez e pode alterar depois.
let veiculo = null;   // o veículo já cadastrado, se houver

async function carregar() {
  try {
    veiculo = await api('GET', '/veiculos/meu');
    $('modelo').value = veiculo.modelo;
    $('capacidade').value = veiculo.capacidadeBateria;
    $('potencia').value = veiculo.potenciaMaxima;
  } catch (erro) {
    if (erro.status !== 404) aviso(erro.message, true);   // 404: ainda não há veículo, o formulário fica vazio
  }
}

$('formulario').onsubmit = async evento => {
  evento.preventDefault();
  const corpo = { modelo: $('modelo').value, capacidadeBateria: Number($('capacidade').value), potenciaMaxima: Number($('potencia').value) };
  try {
    veiculo = veiculo ? await api('PUT', '/veiculos/' + veiculo.id, corpo) : await api('POST', '/veiculos', corpo);
    aviso('Veículo salvo.');
  } catch (erro) {
    aviso(erro.message, true);
  }
};

if (iniciarPagina()) carregar();
