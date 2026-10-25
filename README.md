# CarregaJA_Implementacao

Código fonte do **CARREGAJA**, sistema de controle de vagas com carregador e rateio do consumo de
energia de um condomínio. Os artefatos do projeto ficam no repositório
[CarregaJA](https://github.com/AndreMoreira00/CarregaJA), que inclui este como submódulo em
`3.Implementacao/`.

São dois projetos Maven, como no Modelo de Arquitetura:

| Projeto | O que é | Onde roda |
|---|---|---|
| `carregaja-api` | **Back-end.** API REST (Spring Boot 2.7 + JPA + MySQL), em `control → service → repository → model`, com os dados de entrada validados em `view` | `http://localhost:8081` |
| `carregaja-jsf` | **Front-end.** Telas JSF (Mojarra 2.4) + PrimeFaces 12, que chamam a API pelo `VeiculoRestClient` | Tomcat 9, `http://localhost:8080/carregaja` |

Os dois usam a linha `javax.*`, a que roda no Tomcat 9. Os pacotes são organizados por caso de
uso: tudo do UC05 está em `br.edu.ifsp.carregaja.manterveiculo`.

## O que precisa estar instalado

- **Java 21** e **Maven 3.9** (ou o `mvnw` de cada projeto)
- **MySQL 8**, rodando em `localhost:3306`
- **Tomcat 9** para o front-end

## Como rodar

### 1. Banco

O banco `carregaja` é criado pela API na primeira execução (`createDatabaseIfNotExist=true`), assim
como as tabelas. Informe a senha do usuário `root` do MySQL na variável `MYSQL_SENHA`:

```bash
export MYSQL_SENHA=sua_senha          # Git Bash / Linux
$env:MYSQL_SENHA = "sua_senha"        # PowerShell
```

Sem MySQL instalado, um contêiner serve:

```bash
docker run -d --name carregaja-mysql -e MYSQL_ROOT_PASSWORD=sua_senha -p 3306:3306 mysql:8.4
```

### 2. API (porta 8081)

```bash
cd carregaja-api
mvn spring-boot:run
```

Teste rápido: `curl http://localhost:8081/veiculos`.

| Método | Rota | Faz | Respostas |
|---|---|---|---|
| `GET` | `/veiculos` | Lista os veículos | 200 |
| `GET` | `/veiculos/{id}` | Busca um veículo | 200 · 404 |
| `POST` | `/veiculos` | Cadastra | 201 com `Location` · 400 se inválido |
| `PUT` | `/veiculos/{id}` | Altera | 200 · 400 se inválido · 404 |

Corpo do `POST` e do `PUT`:

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
<http://localhost:8080/carregaja/restricted/manterveiculo/ManterVeiculoView.xhtml>, a tela do UC05.

Se a API estiver em outro endereço, passe `-Dcarregaja.api.url=http://host:porta` ao Tomcat (por
exemplo em `CATALINA_OPTS`).

## Testes

```bash
cd carregaja-api
mvn test
```

| Classe | O que testa |
|---|---|
| `VeiculoControllerTest` | As rotas, com o serviço simulado: cadastro válido (201), modelo em branco ou nulo, capacidade zero ou ausente e potência negativa ou zero (400), veículo inexistente (404). Cobre os testes de aceitação 01 a 03 da HU-05 |
| `VeiculoServiceTest` | A cópia dos dados para a entidade e a alteração de veículo inexistente, com o repositório simulado |

Nenhum dos dois precisa de banco.

## O que já está implementado

| Caso de uso | Situação |
|---|---|
| UC05 — Manter Veículo | Cadastrar e alterar o veículo (modelo, capacidade da bateria em kWh, potência máxima em kW). Implementado para testar a arquitetura: ainda sem login, a tela mostra um único veículo, que será ligado ao morador com o UC01 |
