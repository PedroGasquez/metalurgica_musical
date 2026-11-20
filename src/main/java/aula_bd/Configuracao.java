package aula_bd;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Leitura de configuração: primeiro variável de ambiente, depois arquivo .properties
 * na pasta do projeto (não versionado), por fim o valor padrão.
 */
final class Configuracao {

    private Configuracao() { }

    static Properties carregar(String nomeArquivo) {
        Properties p = new Properties();
        Path arquivo = Path.of(nomeArquivo);
        if (Files.exists(arquivo)) {
            try (InputStream in = Files.newInputStream(arquivo)) {
                p.load(in);
            } catch (Exception e) {
                System.err.println("Não foi possível ler " + nomeArquivo + ": " + e.getMessage());
            }
        }
        return p;
    }

    static String ler(Properties arquivo, String variavel, String chave, String padrao) {
        String v = System.getenv(variavel);
        if (v != null && !v.isEmpty()) return v;
        return arquivo.getProperty(chave, padrao);
    }
}
