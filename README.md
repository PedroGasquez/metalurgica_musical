# Metalúrgica Musical

Aplicação desktop em Java (Swing) para cadastrar, listar, editar e excluir músicas em um banco SQL Server.

## Requisitos

- JDK 25
- Maven
- SQL Server (por exemplo, via Docker)

## Como rodar

1. Suba o SQL Server (a senha precisa ter maiúsculas, minúsculas, números e símbolos):

   ```
   docker run -d --name sqlserver -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=<sua senha>" -p 1433:1433 mcr.microsoft.com/mssql/server
   ```

2. Crie o banco e a tabela executando `script_sqlserver.sql`.

3. Copie `db.properties.example` para `db.properties` e coloque a sua senha.
   Também é possível usar variáveis de ambiente: `DB_USUARIO`, `DB_SENHA`, `DB_HOST`, `DB_PORTA`, `DB_BANCO`.

4. Execute a classe `aula_bd.Principal`.
