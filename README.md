# CarregaJA_Implementacao

Código fonte do **CARREGAJA**, sistema de controle de vagas com carregador e rateio do consumo de
energia de um condomínio. Os artefatos do projeto ficam no repositório
[CarregaJA](https://github.com/AndreMoreira00/CarregaJA), que inclui este como submódulo em
`3.Implementacao/`.

É um projeto Maven só, o `carregaja-api`:

| Parte | O que é | Onde roda |
|---|---|---|
| **API** | Spring Boot 2.7 + JPA + MySQL, em `control → service → repository → model`, com os dados de entrada validados em `view` | `http://localhost:8081` |
| **Telas** | HTML, CSS e JavaScript simples, sem framework nem passo de build, em `src/main/resources/static`. A própria API as serve | `http://localhost:8081`, no celular primeiro |

As telas e a API ficam no mesmo endereço: um programa só para subir, sem configuração de acesso entre
endereços (CORS). Os pacotes Java são organizados por camada, como nos exercícios de LP2
(`br.edu.ifsp.carregaja.control`, `.service`, `.repository`, `.model`, `.view`) e, dentro de cada camada, por
caso de uso, com o nome do caso de uso no Astah: `control.manterveiculo`, `service.fecharmes` e assim por diante.
O que mais de um caso de uso usa (`Usuario`, `Vaga`, `SessaoRecarga` e seus repositórios) fica em `compartilhado`, e o
que é de todos (`TratadorDeErros`, `RegraNegocioException`, `ErroResponse`) fica na raiz da camada. O UC10 não tem
classe própria: o back dele está em `SessaoRecargaController` e `SessaoRecargaService`, no pacote `mantersessaorecarga`.
Nas telas é igual: cada caso de uso é uma pasta de `static` com a página e o script dela.

## O que precisa estar instalado

- **Java 21** e **Maven 3.9** (ou o `mvnw`)
- **MySQL 8**, rodando em `localhost:3306`

## Jeito rápido: scripts (Windows)

A pasta `scripts/` sobe tudo com dois cliques. Na primeira vez, rode qualquer um deles: ele cria
`scripts/config.bat`, que o git ignora. Informe ali a senha do MySQL e os caminhos da sua máquina.

| Script | Faz |
|---|---|
| `iniciar-tudo.bat` | Liga o MySQL e a API e abre o navegador nas telas |
| `parar-tudo.bat` | Desliga a API e o MySQL |
| `reiniciar-tudo.bat` | Desliga e liga tudo de novo |
| `status.bat` | Mostra o que está no ar |
| `reiniciar-api.bat` | Depois de mudar algo na API ou nas telas |
| `zerar-banco.bat` | **Apaga o banco** configurado, depois de pedir confirmação, e sobe a API, que recria as tabelas e o primeiro síndico |
| `testar-api.bat` | Roda os testes de unidade da API |
| `iniciar-mysql`, `iniciar-api`, `parar-mysql`, `parar-api` | Cada peça sozinha |

Precisa do `mvn`, do `curl` e da variável `JAVA_HOME` no ambiente. Se o seu MySQL sobe por conta própria
(serviço do Windows ou Docker), deixe `MYSQL_DIR` vazio em `config.bat`: os scripts só conferem a porta,
e o `zerar-banco` pede para apagar o banco à mão.

## As telas

Abra <http://localhost:8081>. Quem não entrou vai para o login. O menu do topo mostra só os links do perfil de quem
entrou (morador, síndico ou os dois). Isso é só organização: a segurança está na API, que responde 403 a quem não tem o
perfil, e a tela mostra a mensagem.

| Tela | Quem | Caso de uso |
|---|---|---|
| `autenticarusuario/login.html`, `autenticarusuario/trocar-senha.html` | Todos | UC01. A senha provisória só abre a troca de senha |
| `index.html` | Todos | Só decide para onde ir: o painel, se há login, ou o login |
| `consultarpainelvagas/painel.html` | Todos | UC02. Vagas livres e ocupadas, com a previsão de liberação; só o síndico vê quem ocupa |
| `mantersessaorecarga/recarga.html` | Morador | UC03. Inicia a recarga, acompanha a previsão e encerra, vendo duração, energia e valor |
| `manterveiculo/veiculo.html` | Morador | UC05. Modelo, capacidade da bateria e potência máxima |
| `consultarconsumo/consumo.html` | Morador | UC06. O mês do apartamento, sessão a sessão, mês a mês |
| `mantervagas/vagas.html` | Síndico | UC07. Cadastra, altera, desativa e remove vagas |
| `definirtarifa/tarifa.html` | Síndico | UC08. Valor do kWh, vigente a partir do cadastro, com o histórico |
| `mantermoradores/moradores.html` | Síndico | UC09. Cadastra, altera, desativa e remove moradores e síndicos |
| `encerrarsessaoorfa/sessoes.html` | Síndico | UC10. Encerra a sessão de quem já tirou o veículo, depois de confirmar que viu na garagem |
| `fecharmes/fechamento.html` | Síndico | UC11. Fecha o mês, mostra o rateio por apartamento e exporta em CSV |
| `consultarhistorico/historico.html` | Síndico | UC12. Ocupação, horário de pico, recusas e consumo por período |

### Como as telas são feitas

Cada tela é **um arquivo HTML e um script**, sem framework e sem `import`:

- `css/estilo.css`: o estilo de todas as telas. As cores ficam em variáveis no começo do arquivo (azul-escuro da barra do
  topo, verde dos botões). O conteúdo de cada tela fica num cartão branco (`<main>`), e as classes `secundario`,
  `perigo` e `selo` dão a cor dos botões e das situações.
- `js/comum.js`: o que todas usam. Guarda o login no navegador, chama a API (`api('GET', '/vagas')`), desenha o menu
  (com a marca, os links do perfil e a página aberta destacada), faz os selos de situação e formata datas, reais e kWh.
- `<caso de uso>/<tela>.js`, ao lado da página (ex.: `manterveiculo/veiculo.js`): o script da tela. Busca os dados com `api(...)`, monta as linhas da tabela e trata o formulário.

Para mudar uma tela, edite o arquivo e rode `scripts\reiniciar-api.bat`: o `spring-boot:run` copia a pasta `static`
para `target/classes` quando sobe.

## Testar a API no Postman

Importe `carregaja-api/CarregaJA.postman_collection.json`. É o roteiro de todos os casos de uso, UC01 a UC12, com os
dados das Histórias de Usuário (Nissan Leaf, 40 kWh e 6,6 kW, tarifa de R$ 0,92) e as recusas principais. Cada
requisição confere a própria resposta.

1. Rode `scripts\zerar-banco.bat`: a coleção começa pelo primeiro acesso da síndica.
2. No Postman, execute a coleção inteira, de cima para baixo (*Run collection*). Os tokens e os ids ficam nas
   variáveis da coleção.
3. Depois disso, qualquer requisição pode ser repetida à mão. Para executar a coleção inteira de novo, zere o banco.

A síndica usa os dados de `scripts/config.bat`; se você os mudou, ajuste as variáveis `emailSindica` e
`senhaSindicaProvisoria` da coleção.

As sessões deste roteiro duram menos de um segundo: o encerramento administrativo (UC10) só vale depois da previsão
de conclusão, então ali ele aparece recusado. Os casos com horários e valores exatos das histórias estão nos testes
de unidade.

## Como rodar

### 1. Banco

O banco `carregaja` é criado pela API na primeira execução (`createDatabaseIfNotExist=true`), assim
como as tabelas. Sem MySQL instalado, um contêiner serve:

```bash
docker run -d --name carregaja-mysql -e MYSQL_ROOT_PASSWORD=sua_senha -p 3306:3306 mysql:8.4
```

### 2. API e telas (porta 8081)

A API lê a configuração de variáveis de ambiente, para nenhuma senha ficar no código:

| Variável | O que é |
|---|---|
| `MYSQL_SENHA` | Senha do usuário `root` do MySQL |
| `CARREGAJA_JWT_SEGREDO` | Segredo que assina o token de acesso, com pelo menos 32 caracteres. Sem ela a API não sobe |
| `CARREGAJA_SINDICO_EMAIL` | E-mail do primeiro síndico, criado só se ainda não houver nenhum |
| `CARREGAJA_SINDICO_SENHA` | Senha provisória dele, trocada no primeiro acesso |
| `CARREGAJA_SINDICO_NOME` | Nome dele (opcional) |
| `CARREGAJA_SINDICO_APARTAMENTO` | Apartamento dele (opcional). Com apartamento, ele também é morador e pode cadastrar veículo e carregar |

```bash
export MYSQL_SENHA=sua_senha                                      # Git Bash / Linux
export CARREGAJA_JWT_SEGREDO=um-segredo-com-pelo-menos-32-caracteres
export CARREGAJA_SINDICO_EMAIL=sindico@condominio.com.br
export CARREGAJA_SINDICO_SENHA=senha-provisoria

cd carregaja-api
mvn spring-boot:run
```

No PowerShell, `$env:MYSQL_SENHA = "sua_senha"`, e assim por diante. Com a API no ar, abra
<http://localhost:8081>.

Toda rota da API, menos a de login, exige o cabeçalho `Authorization: Bearer <token>`. Sem token válido a
resposta é **401**; com perfil que não pode usar a rota, **403**, com ou sem a barra no fim do endereço. Enquanto a
senha for provisória, o token só serve para trocá-la. Toda recusa vem como `{"mensagem": "..."}`. As páginas das
telas são públicas, porque quem protege os dados é a API.

```bash
curl -X POST http://localhost:8081/autenticacao -H "Content-Type: application/json"   -d '{"email":"sindico@condominio.com.br","senha":"senha-provisoria"}'
```

#### Rotas

| Método | Rota | Quem | Faz | Respostas |
|---|---|---|---|---|
| `POST` | `/autenticacao` | Todos | Entra: devolve o token e o usuário | 200 · 401 senha incorreta · 403 desativado |
| `PUT` | `/autenticacao/senha` | Logado | Troca a senha (`senhaAtual`, `senhaNova`) | 204 · 400 · 422 |
| `GET` | `/veiculos/meu` | Morador | Veículo do morador | 200 · 404 |
| `GET` | `/veiculos` | Síndico | Lista os veículos | 200 |
| `GET` | `/veiculos/{id}` | Dono ou síndico | Busca um veículo | 200 · 403 · 404 |
| `POST` | `/veiculos` | Morador | Cadastra o próprio veículo | 201 com `Location` · 400 · 409 se já tem |
| `PUT` | `/veiculos/{id}` | Dono | Altera | 200 · 400 · 403 · 404 |
| `GET` | `/usuarios` | Síndico | Lista moradores e síndicos | 200 |
| `GET` | `/usuarios/{id}` | Síndico | Busca um usuário | 200 · 404 |
| `POST` | `/usuarios` | Síndico | Cadastra, com senha provisória | 201 com `Location` · 400 · 409 e-mail repetido |
| `PUT` | `/usuarios/{id}` | Síndico | Altera; com `senhaProvisoria`, redefine a senha | 200 · 400 · 404 · 409 |
| `POST` | `/usuarios/{id}/desativar` | Síndico | Revoga o acesso e mantém o histórico | 200 · 404 · 409 se é o único síndico |
| `POST` | `/usuarios/{id}/reativar` | Síndico | Devolve o acesso | 200 · 404 |
| `DELETE` | `/usuarios/{id}` | Síndico | Remove quem nunca usou o sistema | 204 · 404 · 409 |
| `GET` | `/vagas` | Logado | Lista as vagas | 200 |
| `GET` | `/vagas/{id}` | Logado | Busca uma vaga | 200 · 404 |
| `POST` | `/vagas` | Síndico | Cadastra (`identificacao`, `potenciaCarregador`) | 201 com `Location` · 400 · 409 código repetido |
| `PUT` | `/vagas/{id}` | Síndico | Altera | 200 · 400 · 404 · 409 |
| `POST` | `/vagas/{id}/desativar` | Síndico | Deixa de aceitar sessões | 200 · 404 |
| `POST` | `/vagas/{id}/reativar` | Síndico | Volta a aceitar sessões | 200 · 404 |
| `DELETE` | `/vagas/{id}` | Síndico | Remove vaga sem uso | 204 · 404 · 409 |
| `GET` | `/tarifas` | Síndico | Lista as tarifas, da mais nova à mais antiga | 200 |
| `GET` | `/tarifas/vigente` | Logado | Tarifa que vale agora | 200 · 404 se não há nenhuma |
| `POST` | `/tarifas` | Síndico | Cadastra (`valorKwh`), vigente a partir de agora | 201 · 400 |
| `GET` | `/painel` | Logado | Situação de cada vaga e previsão de liberação das ocupadas; só o síndico recebe o apartamento e o nome de quem ocupa | 200 |
| `POST` | `/sessoes` | Morador | Inicia a recarga (`vagaId`, `nivelBateria`) e devolve a previsão | 201 · 400 · 404 · 409 vaga ocupada ou desativada, ou sessão já aberta · 422 sem veículo ou bateria cheia |
| `GET` | `/sessoes/minha` | Morador | Sessão aberta do morador | 200 · 404 |
| `POST` | `/sessoes/minha/encerrar` | Morador | Encerra a própria sessão e devolve duração, energia e valor | 200 · 404 sem sessão aberta |
| `GET` | `/sessoes/abertas` | Síndico | Sessões abertas, das mais antigas às mais novas | 200 |
| `POST` | `/sessoes/{id}/encerrar-administrativamente` | Síndico | Encerra a sessão órfã, só depois da previsão, registrando quem encerrou | 200 · 403 · 404 · 409 dentro da previsão ou já encerrada |
| `GET` | `/consumos/meu?ano=&mes=` | Morador | Consumo do mês do apartamento, sessão a sessão | 200 · 400 |
| `POST` | `/fechamentos` | Síndico | Fecha o mês (`ano`, `mes`) e grava o rateio por apartamento | 201 · 400 · 409 já fechado ou sessão aberta · 422 sem sessão |
| `GET` | `/fechamentos` | Síndico | Os meses fechados, do mais recente ao mais antigo, com o rateio | 200 |
| `GET` | `/fechamentos/{id}` | Síndico | Um fechamento com os itens | 200 · 404 |
| `GET` | `/historico?de=&ate=` | Síndico | Histórico de utilização do período (`de` e `ate`, aaaa-mm-dd, e os filtros `vagaId` e `apartamentoId`): ocupação por vaga, faixa de pico, consumo por apartamento e recusas | 200 · 400 |

Corpo do `POST` e do `PUT` de veículo:

```json
{ "modelo": "Nissan Leaf", "capacidadeBateria": 40, "potenciaMaxima": 6.6 }
```

O modelo é obrigatório, e a capacidade (kWh) e a potência (kW) devem ser maiores que zero. Não há
exclusão de veículo: o UC05 só cadastra e altera (RF-10).

## Testes

```bash
cd carregaja-api
mvn test
```

Os testes das rotas ficam em `*ControllerTest`, com o serviço simulado, e passam pelas regras de
segurança de verdade. Os das regras de negócio ficam em `*ServiceTest`, com o repositório simulado.
`PermissoesPorPerfilTest` confere que cada rota de um perfil é negada ao outro, com e sem a barra no fim do
endereço. Cada teste de aceitação traz o número no comentário (`// HU-01, teste 02`).

Nenhum deles precisa de banco. As regras que dependem dele (a corrida de dois moradores pela mesma vaga, o morador
e o síndico encerrando a mesma sessão ao mesmo tempo, o fechamento do mês) foram conferidas com o MySQL, e a
coleção do Postman confere o resto com a API no ar.

## O que já está implementado

Os 12 casos de uso têm API, testes e tela.

| Caso de uso | Situação |
|---|---|
| UC01 — Autenticar Usuário | Login por e-mail e senha, perfis Morador e Síndico, troca obrigatória da senha provisória e proteção da API por token. O primeiro síndico vem da configuração |
| UC02 — Consultar Painel de Vagas | Situação de cada vaga (livre, ocupada, concluída e ainda ocupada, excedida), previsão de liberação e, sem vaga livre, a liberação mais próxima |
| UC03 — Manter Sessão de Recarga | Iniciar com a potência efetiva e a previsão de conclusão, e encerrar com a duração, a energia e o valor. Recusa vaga ocupada ou desativada, sessão já aberta, morador sem veículo e bateria cheia |
| UC04 — Apurar Energia da Sessão | Regra isolada em `ApuracaoEnergiaService`, coberta por testes de unidade e chamada no encerramento pelo morador e pelo síndico |
| UC05 — Manter Veículo | Cadastrar e alterar o veículo do morador (modelo, capacidade da bateria em kWh, potência máxima em kW) |
| UC06 — Consultar Consumo do Mês | Sessões do mês do apartamento, com o total em kWh e em reais, e a sinalização da sessão encerrada pelo síndico |
| UC07 — Manter Vagas com Carregador | Cadastrar, alterar, desativar e remover as vagas e a potência do carregador de cada uma |
| UC08 — Definir Tarifa de Energia | Cadastrar a tarifa do kWh, vigente da data e hora do cadastro até a próxima, com o histórico consultável |
| UC09 — Manter Moradores | Cadastrar, alterar, desativar e remover moradores, vinculados ao apartamento, com senha provisória |
| UC10 — Encerrar Sessão Órfã | Lista as sessões abertas e encerra a que passou da previsão, com o síndico e o horário registrados |
| UC11 — Fechar Mês e Gerar Rateio | Fecha o mês, grava o rateio por apartamento, lista os meses fechados e exporta o rateio em CSV |
| UC12 — Consultar Histórico de Utilização | Ocupação por vaga (horas e % do período), faixa horária de pico, consumo por apartamento e recusas por vaga e por faixa, só para o síndico |
