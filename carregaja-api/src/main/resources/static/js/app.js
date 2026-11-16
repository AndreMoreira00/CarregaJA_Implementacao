// Núcleo das telas: sessão do usuário, chamadas à API, topo com o menu por perfil e utilitários de formatação.
// JavaScript simples, sem biblioteca. Cada página importa o que precisa daqui.

const CHAVE = 'carregaja.sessao';

export const MENU = [
  { href: '/painel.html', rotulo: 'Painel', perfis: ['MORADOR', 'SINDICO'], texto: 'Veja quais vagas estão livres e quando as ocupadas liberam.' },
  { href: '/recarga.html', rotulo: 'Recarga', perfis: ['MORADOR'], texto: 'Inicie a sua recarga ao plugar o veículo e encerre ao retirá-lo.' },
  { href: '/veiculo.html', rotulo: 'Veículo', perfis: ['MORADOR'], texto: 'Cadastre o modelo, a bateria e a potência do seu veículo.' },
  { href: '/consumo.html', rotulo: 'Consumo', perfis: ['MORADOR'], texto: 'O que o seu apartamento consumiu no mês, sessão a sessão.' },
  { href: '/sessoes.html', rotulo: 'Sessões', perfis: ['SINDICO'], texto: 'Encerre a sessão de um veículo que já saiu da vaga.' },
  { href: '/vagas.html', rotulo: 'Vagas', perfis: ['SINDICO'], texto: 'Cadastre as vagas com carregador e a potência de cada uma.' },
  { href: '/moradores.html', rotulo: 'Moradores', perfis: ['SINDICO'], texto: 'Cadastre, altere e desative moradores e síndicos.' },
  { href: '/tarifa.html', rotulo: 'Tarifa', perfis: ['SINDICO'], texto: 'Defina o valor do kWh, que vale a partir do cadastro.' },
  { href: '/fechamento.html', rotulo: 'Fechamento', perfis: ['SINDICO'], texto: 'Feche o mês, veja o rateio por apartamento e exporte.' },
  { href: '/historico.html', rotulo: 'Histórico', perfis: ['SINDICO'], texto: 'Ocupação, horário de pico, recusas e consumo por período.' },
];

const BOLT = '<svg viewBox="0 0 32 32" aria-hidden="true"><rect width="32" height="32" rx="7" fill="#F5C400"/>'
  + '<path d="M18 4 8 18h7l-1.5 10L24 14h-7z" fill="#1B1D1F"/></svg>';

// ---------- sessão ----------

export function sessao() {
  try {
    return JSON.parse(localStorage.getItem(CHAVE));
  } catch (e) {
    return null;
  }
}

export function guardarSessao(dados) {
  try {
    localStorage.setItem(CHAVE, JSON.stringify(dados));
  } catch (e) {
    // Navegador sem armazenamento: o login funciona, mas a próxima página pedirá o acesso de novo
  }
}

export function sair() {
  try {
    localStorage.removeItem(CHAVE);
  } catch (e) {
    // sem armazenamento, não há o que limpar
  }
  location.replace('/login.html');
}

// ---------- chamadas à API ----------

export class ErroApi extends Error {
  constructor(status, mensagem) {
    super(mensagem);
    this.status = status;
  }
}

const MENSAGEM_PADRAO = {
  0: 'Sem conexão com o servidor. Confira a internet e tente de novo.',
  401: 'Sua sessão expirou. Entre de novo.',
  403: 'Você não tem acesso a esta função.',
  404: 'Não encontrado.',
  500: 'O servidor falhou. Tente de novo em instantes.',
};

export async function chamar(metodo, url, corpo) {
  const atual = sessao();
  const cabecalhos = {};
  if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json';
  if (atual && atual.token) cabecalhos.Authorization = 'Bearer ' + atual.token;

  let resposta;
  try {
    resposta = await fetch(url, {
      method: metodo,
      headers: cabecalhos,
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    });
  } catch (e) {
    throw new ErroApi(0, MENSAGEM_PADRAO[0]);
  }

  const texto = await resposta.text();
  let dados = null;
  if (texto) {
    try {
      dados = JSON.parse(texto);
    } catch (e) {
      dados = null;
    }
  }
  // Token vencido ou usuário desativado: volta ao login. O próprio login devolve 401 para senha errada
  if (resposta.status === 401 && atual && url !== '/autenticacao') {
    sair();
    throw new ErroApi(401, MENSAGEM_PADRAO[401]);
  }
  if (!resposta.ok) {
    const mensagem = (dados && dados.mensagem) || MENSAGEM_PADRAO[resposta.status] || 'Não foi possível concluir. Tente de novo.';
    throw new ErroApi(resposta.status, mensagem);
  }
  return dados;
}

// ---------- página ----------

/**
 * Confere o login e o perfil da página e monta o topo. Devolve o usuário, ou null se redirecionou.
 * opcoes.perfis: perfis que podem ver a página. opcoes.livreDeTroca: página acessível com a senha provisória.
 */
export function iniciar(opcoes = {}) {
  const atual = sessao();
  if (!atual || !atual.token || !atual.usuario) {
    location.replace('/login.html');
    return null;
  }
  const usuario = atual.usuario;
  if (usuario.trocarSenha && !opcoes.livreDeTroca) {
    location.replace('/trocar-senha.html');
    return null;
  }
  if (opcoes.perfis && !opcoes.perfis.some(perfil => usuario.perfis.includes(perfil))) {
    location.replace('/index.html?sem-acesso=1');
    return null;
  }
  montarTopo(usuario);
  return usuario;
}

function montarTopo(usuario) {
  const topo = document.getElementById('topo');
  if (!topo) return;
  const aqui = location.pathname;
  const itens = usuario.trocarSenha ? [] : MENU.filter(item => item.perfis.some(perfil => usuario.perfis.includes(perfil)));
  const link = (href, rotulo) => `<a href="${href}"${aqui === href ? ' aria-current="page"' : ''}>${rotulo}</a>`;
  const nav = itens.length
    ? `<nav class="nav" aria-label="Menu">${itens.map(item => link(item.href, item.rotulo)).join('')}${link('/trocar-senha.html', 'Senha')}</nav>`
    : '';
  const apartamento = usuario.apartamento ? ` · apto ${esc(usuario.apartamento.numero)}` : '';
  topo.innerHTML = `<div class="topo-linha">
      <a class="marca" href="/index.html">${BOLT}<span>CarregaJA</span></a>
      <div class="topo-usuario"><span class="nome">${esc(primeiroNome(usuario.nome))}${apartamento}</span>
        <button type="button" class="botao secundario pequeno" id="sair">Sair</button></div>
    </div>${nav}`;
  document.getElementById('sair').addEventListener('click', sair);
  const ativo = topo.querySelector('[aria-current="page"]');
  if (ativo) ativo.scrollIntoView({ inline: 'center', block: 'nearest' });
}

export function primeiroNome(nome) {
  return String(nome || '').trim().split(/\s+/)[0] || '';
}

// ---------- avisos e confirmação ----------

export function aviso(texto, tipo = 'info') {
  let area = document.getElementById('aviso');
  if (!area) {
    area = document.createElement('div');
    area.id = 'aviso';
    area.setAttribute('role', 'status');
    area.setAttribute('aria-live', 'polite');
    const conteudo = document.getElementById('conteudo');
    conteudo.insertBefore(area, conteudo.children[1] || null);
  }
  area.innerHTML = texto ? `<div class="aviso aviso-${tipo}">${esc(texto)}</div>` : '';
  if (texto) area.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
}

/** Diálogo de confirmação. Devolve uma promessa de true (confirmou) ou false. */
export function confirmar({ titulo, texto, ok = 'Confirmar', perigo = false, marcar = null }) {
  return new Promise(resolver => {
    const dialogo = document.createElement('dialog');
    dialogo.innerHTML = `<form method="dialog">
        <h2>${esc(titulo)}</h2><p>${esc(texto)}</p>
        ${marcar ? `<label class="marcar"><input type="checkbox" id="dialogo-marca"><span>${esc(marcar)}</span></label>` : ''}
        <div class="acoes">
          <button value="cancelar" class="botao secundario">Cancelar</button>
          <button value="ok" id="dialogo-ok" class="botao${perigo ? ' perigo' : ''}"${marcar ? ' disabled' : ''}>${esc(ok)}</button>
        </div></form>`;
    document.body.appendChild(dialogo);
    const caixa = dialogo.querySelector('#dialogo-marca');
    if (caixa) caixa.addEventListener('change', () => { dialogo.querySelector('#dialogo-ok').disabled = !caixa.checked; });
    dialogo.addEventListener('close', () => {
      const confirmou = dialogo.returnValue === 'ok';
      dialogo.remove();
      resolver(confirmou);
    });
    dialogo.showModal();
  });
}

/** Desliga o botão durante a chamada, para o toque duplo não enviar duas vezes. */
export async function comBotao(botao, tarefa) {
  // A mensagem da ação anterior sai da tela: durante a espera ela passaria por resposta da ação nova
  const area = document.getElementById('aviso');
  if (area) area.innerHTML = '';
  if (botao) botao.disabled = true;
  try {
    return await tarefa();
  } finally {
    if (botao) botao.disabled = false;
  }
}

// ---------- formatação ----------

export function esc(texto) {
  return String(texto === null || texto === undefined ? '' : texto)
    .replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

const formato = (minimo, maximo) => new Intl.NumberFormat('pt-BR', { minimumFractionDigits: minimo, maximumFractionDigits: maximo });
const REAL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export const brl = valor => REAL.format(valor || 0);
export const kwh = (valor, casas = 1) => formato(casas, 3).format(valor || 0);
export const potencia = valor => formato(1, 1).format(valor || 0) + ' kW';
export const tarifa = valor => 'R$ ' + formato(2, 4).format(valor || 0) + '/kWh';

// A API manda LocalDateTime sem fuso e com microssegundos: o que importa são os 19 primeiros caracteres
function lerData(iso) {
  return iso ? new Date(String(iso).slice(0, 19)) : null;
}
export const hora = iso => (iso ? lerData(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }) : '—');
export const dataHora = iso => (iso
  ? lerData(iso).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }).replace(',', '')
  : '—');
export const diaMes = iso => (iso ? lerData(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—');
export const minutosAte = iso => Math.round((lerData(iso).getTime() - Date.now()) / 60000);

export function duracaoEmTexto(minutos) {
  const total = Math.abs(minutos);
  const h = Math.floor(total / 60);
  const m = total % 60;
  return h ? `${h} h ${String(m).padStart(2, '0')} min` : `${m} min`;
}

const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];
export const nomeDoMes = (ano, mes) => `${MESES[mes - 1]} de ${ano}`;

export function hojeISO(deslocamentoDias = 0) {
  const d = new Date();
  d.setDate(d.getDate() + deslocamentoDias);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

// ---------- estados ----------

const ESTADOS = {
  LIVRE: ['Livre', 'livre'],
  OCUPADA: ['Ocupada', 'ocupada'],
  EM_ANDAMENTO: ['Carregando', 'ocupada'],
  CONCLUIDA_AINDA_OCUPADA: ['Carga concluída, vaga ocupada', 'atencao'],
  EXCEDIDA: ['Excedida', 'excedida'],
  ENCERRADA: ['Encerrada', 'neutro'],
  ENCERRADA_ADMINISTRATIVAMENTE: ['Encerrada pelo síndico', 'atencao'],
};

export function selo(estado, texto) {
  const [rotulo, classe] = ESTADOS[estado] || [estado, 'neutro'];
  return `<span class="selo selo-${classe}">${esc(texto || rotulo)}</span>`;
}

/** Classe da vaga pintada (a cor da faixa lateral) para um estado. */
export function classeDaVaga(estado) {
  return 'vaga-' + ((ESTADOS[estado] || [0, 'neutro'])[1]);
}
