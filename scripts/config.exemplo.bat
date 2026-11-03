@echo off
rem Copie para config.bat (o git ignora) e ajuste para a sua maquina. Os scripts fazem a copia na 1a vez.

rem Banco
rem Pasta do MySQL, com bin\mysqld.exe e my.ini. Deixe vazio se o MySQL ja roda como servico do Windows
rem ou no Docker. Nesse caso os scripts so verificam se a porta 3306 responde.
set "MYSQL_DIR=C:\mysql"
rem Senha do usuario root. Obrigatoria.
set "MYSQL_SENHA="
set "CARREGAJA_BANCO=carregaja"

rem Tomcat 9
set "TOMCAT_DIR=C:\apache-tomcat-9.0.62"

rem API (valores so para desenvolvimento)
rem Chave que assina o token de acesso, com no minimo 32 caracteres.
set "CARREGAJA_JWT_SEGREDO=segredo-so-para-desenvolvimento-local-troque-em-producao"
rem Primeiro sindico, criado so se o banco nao tiver nenhum. A colecao do Postman usa estes dados.
set "CARREGAJA_SINDICO_NOME=Sindica do Condominio"
set "CARREGAJA_SINDICO_EMAIL=sindica@carregaja.test"
set "CARREGAJA_SINDICO_SENHA=provisoria-sindica"
rem Se o apartamento for informado, o sindico tambem e morador e pode cadastrar veiculo. O Postman usa este valor.
set "CARREGAJA_SINDICO_APARTAMENTO=42"
