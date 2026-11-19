# Metalúrgica Musical

Aplicação desktop em Java (Swing) para cadastrar, listar, editar e excluir músicas em um banco SQL Server.
Também busca e toca músicas do SoundCloud, e permite adicioná-las ao acervo.

## Requisitos

- JDK 25
- Maven
- SQL Server (por exemplo, via Docker)
- Para o SoundCloud: um app registrado em https://soundcloud.com/you/apps

## Como rodar

1. Suba o SQL Server (a senha precisa ter maiúsculas, minúsculas, números e símbolos):

   ```
   docker run -d --name sqlserver -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=<sua senha>" -p 1433:1433 mcr.microsoft.com/mssql/server
   ```

2. Crie o banco e a tabela executando `script_sqlserver.sql`.
   Se o banco já existia, execute o script de novo: ele adiciona as colunas `soundcloud_id` e `link`.

3. Copie `db.properties.example` para `db.properties` e coloque a sua senha.
   Também é possível usar variáveis de ambiente: `DB_USUARIO`, `DB_SENHA`, `DB_HOST`, `DB_PORTA`, `DB_BANCO`.

4. (Opcional) Copie `soundcloud.properties.example` para `soundcloud.properties` e coloque o Client ID e o Client Secret do seu app.
   Também é possível usar as variáveis de ambiente `SOUNDCLOUD_CLIENT_ID` e `SOUNDCLOUD_CLIENT_SECRET`.

5. Execute a classe `aula_bd.Principal`.

## SoundCloud

- O botão **Buscar no SoundCloud** abre uma janela de busca. Dê dois cliques numa música para tocar.
- **Adicionar ao acervo** salva no banco apenas os dados da música (título, artista, gênero, ID e link), nunca o áudio.
- Músicas do acervo vindas do SoundCloud podem ser tocadas pelo botão **Tocar no SoundCloud** na tela principal.
- Algumas faixas só liberam uma prévia de 30 segundos. Elas aparecem marcadas como "prévia".
- A reprodução usa o JavaFX Media (incluído pelo Maven) e funciona com streams MP3 e HLS.
- Respeite os [termos da API](https://developers.soundcloud.com/docs/api/terms-of-use): dê crédito ao artista e não baixe nem redistribua o áudio.
