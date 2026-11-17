// Funções usadas por todas as telas.

const $ = id => document.getElementById(id);   // atalho: $('nome') acha o elemento com id="nome"

// ---------- login: o token e os dados do usuário ficam guardados no navegador ----------

function login() {
  return JSON.parse(localStorage.getItem('login'));   // null se ninguém entrou
}

function guardarLogin(resposta) {
  localStorage.setItem('login', JSON.stringify(resposta));
}

function sair() {
  localStorage.removeItem('login');
  location.href = '/login.html';
}

// Toda tela, menos o login, começa por aqui: confere se há login e desenha o menu.
// Devolve o usuário, ou nada se mandou a pessoa para outra tela.
function iniciarPagina() {
  if (!login()) {
    location.href = '/login.html';
    return;
  }
  const usuario = login().usuario;
  if (usuario.trocarSenha && !location.pathname.includes('trocar-senha')) {
    location.href = '/trocar-senha.html';   // senha provisória: só deixa trocar a senha
    return;
  }
  $('menu').innerHTML =
    '<a href="/painel.html">Painel</a> | ' +
    'Morador: <a href="/recarga.html">Recarga</a> <a href="/veiculo.html">Veículo</a> <a href="/consumo.html">Consumo</a> | ' +
    'Síndico: <a href="/sessoes.html">Sessões</a> <a href="/vagas.html">Vagas</a> <a href="/moradores.html">Moradores</a> ' +
    '<a href="/tarifa.html">Tarifa</a> <a href="/fechamento.html">Fechamento</a> <a href="/historico.html">Histórico</a>' +
    '<br>' + esc(usuario.nome) + ' · <a href="/trocar-senha.html">Trocar senha</a> · <a href="#" onclick="sair()">Sair</a>';
  return usuario;
}

// ---------- chamada à API ----------

// Devolve os dados em JSON. Se a API recusar, lança um erro com a mensagem para mostrar na tela.
async function api(metodo, url, corpo) {
  const cabecalhos = { 'Content-Type': 'application/json' };
  if (login()) cabecalhos.Authorization = 'Bearer ' + login().token;

  const resposta = await fetch(url, {
    method: metodo,
    headers: cabecalhos,
    body: corpo ? JSON.stringify(corpo) : undefined,
  });
  const texto = await resposta.text();
  const dados = texto ? JSON.parse(texto) : null;

  if (!resposta.ok) {
    if (resposta.status === 401 && url !== '/autenticacao') sair();   // login vencido ou usuário desativado
    const padrao = resposta.status === 403 ? 'Você não tem acesso a esta função.' : 'Erro ' + resposta.status + '.';
    const erro = new Error(dados && dados.mensagem ? dados.mensagem : padrao);
    erro.status = resposta.status;
    throw erro;
  }
  return dados;
}

// ---------- mensagens e formatação ----------

function aviso(texto, ehErro) {
  $('aviso').className = ehErro ? 'erro' : 'ok';
  $('aviso').textContent = texto;
}

// Protege contra texto com < ou &: tudo que vem do usuário passa por aqui antes de ir para o HTML.
function esc(texto) {
  return String(texto).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

const reais = valor => Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const kwh = valor => Number(valor).toLocaleString('pt-BR', { maximumFractionDigits: 3 }) + ' kWh';
const kw = valor => Number(valor).toLocaleString('pt-BR') + ' kW';
const virgula = (valor, casas) => Number(valor).toFixed(casas).replace('.', ',');

// A API manda datas como 2026-11-16T20:28:33: basta cortar o texto.
const hora = data => data.substring(11, 16);                                                      // 20:28
const dataHora = data => data.substring(8, 10) + '/' + data.substring(5, 7) + ' ' + hora(data);   // 16/11 20:28

// O mês no formato do campo <input type="month">: anoMes(0) é este mês, anoMes(-1) é o anterior.
function anoMes(deslocamento) {
  const data = new Date();
  data.setMonth(data.getMonth() + deslocamento, 1);
  return data.getFullYear() + '-' + String(data.getMonth() + 1).padStart(2, '0');
}

const SITUACAO = {
  LIVRE: 'Livre',
  OCUPADA: 'Ocupada',
  EM_ANDAMENTO: 'Carregando',
  CONCLUIDA_AINDA_OCUPADA: 'Carga concluída, vaga ainda ocupada',
  EXCEDIDA: 'Excedida (passou da duração máxima)',
};
