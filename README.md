# CarregaJA_Implementacao

Código fonte do **CARREGAJA**, sistema de controle de vagas com carregador e rateio do consumo de
energia de um condomínio. Os artefatos do projeto ficam no repositório
[CarregaJA](https://github.com/AndreMoreira00/CarregaJA), que inclui este como submódulo em
`3.Implementacao/`.

Spring Boot + JSF (PrimeFaces, via JoinFaces) + MySQL, em camadas
`controller → service → dao → model`, como no Modelo de Arquitetura.

## O que precisa estar instalado

- **Java 21**
- **MySQL 8**, rodando em `localhost:3306`
- O Maven não precisa ser instalado: o projeto traz o `mvnw`

## Como rodar

1. Crie o banco (as tabelas são criadas pela aplicação na primeira execução):

   ```sql
   CREATE DATABASE carregaja;
   ```

2. Informe a senha do usuário `root` do MySQL na variável `MYSQL_SENHA`:

   ```bash
   export MYSQL_SENHA=sua_senha          # Git Bash / Linux
   $env:MYSQL_SENHA = "sua_senha"        # PowerShell
   ```

3. Suba a aplicação:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Abra <http://localhost:8080/restricted/manterveiculo/ManterVeiculo.xhtml> — tela do UC05 (Manter
   Veículo).

Para usar outro endereço de banco sem mexer no `application.properties`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:mysql://localhost:3307/carregaja"
```

## Testes

```bash
./mvnw test
```

Os testes de unidade do `VeiculoService` não precisam de banco.

## O que já está implementado

| Caso de uso | Situação |
|---|---|
| UC05 — Manter Veículo | Cadastrar e alterar o veículo (modelo, capacidade da bateria em kWh, potência máxima em kW). Implementado para testar a arquitetura: ainda sem login, há um único veículo no sistema, que será ligado ao morador com o UC01 |
