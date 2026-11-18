package aula_bd;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Configuração lida, nesta ordem, de:
 *  1. variáveis de ambiente (DB_USUARIO, DB_SENHA, DB_HOST, DB_PORTA, DB_BANCO)
 *  2. arquivo db.properties na pasta do projeto (não versionado; veja db.properties.example)
 *  3. valores padrão abaixo (exceto a senha, que não tem padrão)
 */
public class ConnectionFactory {

    private static final Properties ARQUIVO = carregarArquivo();

    private String usuario = config("DB_USUARIO", "usuario", "sa");
    private String senha   = config("DB_SENHA",   "senha",   "");
    private String host    = config("DB_HOST",    "host",    "localhost");
    private String porta   = config("DB_PORTA",   "porta",   "1433");
    private String bd      = config("DB_BANCO",   "banco",   "aula_bd");

    public Connection obtemConexao (){
        try{
            Connection c = DriverManager.getConnection(
                "jdbc:sqlserver://" + host + ":" + porta
                    + ";databaseName=" + bd
                    + ";encrypt=false;trustServerCertificate=true",
                usuario,
                senha
            );
            return c;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private static String config(String variavel, String chave, String padrao) {
        String v = System.getenv(variavel);
        if (v != null && !v.isEmpty()) return v;
        return ARQUIVO.getProperty(chave, padrao);
    }

    private static Properties carregarArquivo() {
        Properties p = new Properties();
        Path arquivo = Path.of("db.properties");
        if (Files.exists(arquivo)) {
            try (InputStream in = Files.newInputStream(arquivo)) {
                p.load(in);
            } catch (Exception e) {
                System.err.println("Não foi possível ler db.properties: " + e.getMessage());
            }
        }
        return p;
    }
}
