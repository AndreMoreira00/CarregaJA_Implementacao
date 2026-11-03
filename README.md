# CarregaJA_Implementacao

Código fonte do **CARREGAJA**, sistema de controle de vagas com carregador e rateio do consumo de
energia de um condomínio. Os artefatos do projeto ficam no repositório
[CarregaJA](https://github.com/AndreMoreira00/CarregaJA), que inclui este como submódulo em
`3.Implementacao/`.

São dois projetos Maven, como no Modelo de Arquitetura:

| Projeto | O que é | Onde roda |
|---|---|---|
| `carregaja-api` | **Back-end.** API REST (Spring Boot 2.7 + JPA + MySQL), em `control → service → repository → model`, com os dados de entrada validados em `view` | `http://localhost:8081` |
| `carregaja-jsf` | **Front-end.** Telas JSF (Mojarra 2.4) + PrimeFaces 12, que chamam a API pelos clientes REST | Tomcat 9, `http://localhost:8080/carregaja` |

Os dois usam a linha `javax.*`, a que roda no Tomcat 9. Os pacotes são organizados por camada,
como nos exercícios de LP2 (`br.edu.ifsp.carregaja.control`, `.service`, `.repository`, `.model`,
`.view` na API; `.controller`, `.service`, `.model` nas telas). O nome do caso de uso fica na pasta
da página: `webapp/restricted/manterveiculo/`.

## O que precisa estar instalado

- **Java 21** e **Maven 3.9** (ou o `mvnw` de cada projeto)
- **MySQL 8**, rodando em `localhost:3306`
- **Tomcat 9** para o front-end

## Como rodar

### 1. Banco

O banco `carregaja` é criado pela API na primeira execução (`createDatabaseIfNotExist=true`), assim
como as tabelas. Sem MySQL instalado, um contêiner serve:

```bash
docker run -d --name carregaja-mysql -e MYSQL_ROOT_PASSWORD=sua_senha -p 3306:3306 mysql:8.4
```

### 2. API (porta 8081)

A API lê a configuração de variáveis de ambiente, para nenhuma senha ficar no código:

| Variável | O que é |
|---|---|
| `MYSQL_SENHA` | Senha do usuário `root` do MySQL |
| `CARREGAJA_JWT_SEGREDO` | Segredo que assina o token de acesso, com pelo menos 32 caracteres. Sem ela a API não sobe |
| `CARREGAJA_SINDICO_EMAIL` | E-mail do primeiro síndico, criado só se ainda não houver nenhum |
| `CARREGAJA_SINDICO_SENHA` | Senha provisória dele, trocada no primeiro acesso |
| `CARREGAJA_SINDICO_NOME` | Nome dele (opcional) |

```bash
export MYSQL_SENHA=sua_senha                                      # Git Bash / Linux
export CARREGAJA_JWT_SEGREDO=um-segredo-com-pelo-menos-32-caracteres
export CARREGAJA_SINDICO_EMAIL=sindico@condominio.com.br
export CARREGAJA_SINDICO_SENHA=senha-provisoria

cd carregaja-api
mvn spring-boot:run
```

No PowerShell, `$env:MYSQL_SENHA = "sua_senha"`, e assim por diante.

Toda rota, menos a de login, exige o cabeçalho `Authorization: Bearer <token>`. Sem token válido a
resposta é **401**; com perfil que não pode usar a rota, **403**. Enquanto a senha for provisória,
o token só serve para trocá-la. Toda recusa vem como `{"mensagem": "..."}`.

```bash
curl -X POST http://localhost:8081/autenticacao -H "Content-Type: application/json"   -d '{"email":"sindico@condominio.com.br","senha":"senha-provisoria"}'
```

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
| `DELETE` | `/usuarios/{id}` | Síndico | Remove | 204 · 404 · 409 |

Corpo do `POST` e do `PUT` de veículo:

```json
{ "modelo": "Nissan Leaf", "capacidadeBateria": 40, "potenciaMaxima": 6.6 }
```

O modelo é obrigatório, e a capacidade (kWh) e a potência (kW) devem ser maiores que zero. Não há
exclusão: o UC05 só cadastra e altera (RF-10).

### 3. Front-end (Tomcat 9, porta 8080)

```bash
cd carregaja-jsf
mvn package
```

Copie `target/carregaja.war` para a pasta `webapps/` do Tomcat 9, inicie o servidor
(`bin/startup.bat`) e abra
<http://localhost:8080/carregaja>, que leva ao login.

Se a API estiver em outro endereço, passe `-Dcarregaja.api.url=http://host:porta` ao Tomcat (por
exemplo em `CATALINA_OPTS`).

## Testes

```bash
cd carregaja-api
mvn test
```

Os testes das rotas ficam em `*ControllerTest`, com o serviço simulado, e passam pelas regras de
segurança de verdade. Os das regras de negócio ficam em `*ServiceTest`, com o repositório simulado.
Cada teste de aceitação traz o número no comentário (`// HU-01, teste 02`).

Nenhum deles precisa de banco.

## O que já está implementado

Por enquanto, as telas prontas são a de login (UC01), com a troca de senha, e a do veículo (UC05). Os demais
casos de uso estão prontos na API e são usados pelas rotas acima. As telas deles estão guardadas na branch `telas-sprint-5`
do fork [R1ck-dev/CarregaJA_Implementacao](https://github.com/R1ck-dev/CarregaJA_Implementacao/tree/telas-sprint-5).

| Caso de uso | Situação |
|---|---|
| UC01 — Autenticar Usuário | Login por e-mail e senha, perfis Morador e Síndico, troca obrigatória da senha provisória e proteção da API por token |
| UC05 — Manter Veículo | Cadastrar e alterar o veículo do morador (modelo, capacidade da bateria em kWh, potência máxima em kW) |
| UC09 — Manter Moradores | Cadastrar, alterar, desativar e remover moradores, vinculados ao apartamento, com senha provisória |
