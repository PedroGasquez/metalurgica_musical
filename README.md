# Metalúrgica Musical

Aplicação desktop em Java (Swing) para cadastrar, listar, editar e excluir músicas em um banco SQL Server.
Também busca músicas no Deezer, toca prévias de 30 segundos e permite adicioná-las ao acervo.

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
   Se o banco já existia, execute o script de novo: ele adiciona as colunas `deezer_id` e `link`.

3. Copie `db.properties.example` para `db.properties` e coloque a sua senha.
   Também é possível usar variáveis de ambiente: `DB_USUARIO`, `DB_SENHA`, `DB_HOST`, `DB_PORTA`, `DB_BANCO`.

4. Execute a classe `aula_bd.Principal`.

## Deezer

A busca usa a [API pública do Deezer](https://developers.deezer.com/api), que não exige cadastro nem chave.

- O botão **Buscar no Deezer** abre uma janela de busca. Dê dois cliques numa música para ouvir a prévia.
- **Adicionar ao acervo** salva título, artista, gênero (do álbum), ID e link da música. O áudio não é salvo.
- Músicas do acervo vindas do Deezer podem ser ouvidas pelo botão **Ouvir prévia** na tela principal.
- O Deezer só libera prévias de 30 segundos; a música completa pode ser aberta no site pelo botão **Abrir no Deezer**.
- A API limita a cerca de 50 requisições a cada 5 segundos.
