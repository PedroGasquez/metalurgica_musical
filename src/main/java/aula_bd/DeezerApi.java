package aula_bd;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Cliente da API pública do Deezer (https://developers.deezer.com/api).
 * Busca e prévias de 30 segundos não exigem cadastro nem chave de acesso.
 */
public class DeezerApi {

    private static final String API = "https://api.deezer.com";
    private static final int LIMITE_BUSCA = 30;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public List<FaixaDeezer> buscar(String termo) {
        JsonObject raiz = get("/search?limit=" + LIMITE_BUSCA + "&q=" + URLEncoder.encode(termo, StandardCharsets.UTF_8));
        List<FaixaDeezer> faixas = new ArrayList<>();
        JsonArray itens = raiz.has("data") ? raiz.getAsJsonArray("data") : new JsonArray();
        for (JsonElement e : itens) {
            JsonObject t = e.getAsJsonObject();
            if (t.has("readable") && !t.get("readable").getAsBoolean()) continue;
            JsonObject artista = objeto(t, "artist");
            JsonObject album = objeto(t, "album");
            faixas.add(new FaixaDeezer(
                    t.get("id").getAsLong(),
                    texto(t, "title"),
                    texto(artista, "name"),
                    texto(album, "title"),
                    album.has("id") ? album.get("id").getAsLong() : 0,
                    t.has("duration") ? t.get("duration").getAsInt() : 0,
                    texto(t, "link")));
        }
        return faixas;
    }

    /**
     * Endereço da prévia de 30 segundos. Ele expira depois de algum tempo,
     * por isso é buscado de novo a cada reprodução.
     */
    public String urlDaPrevia(long idFaixa) {
        String url = texto(get("/track/" + idFaixa), "preview");
        if (url.isEmpty()) throw new DeezerException("Esta música não tem prévia disponível.");
        return url;
    }

    /** Gênero principal do álbum, ou texto vazio se o Deezer não informar. */
    public String generoDoAlbum(long idAlbum) {
        if (idAlbum <= 0) return "";
        JsonObject generos = objeto(get("/album/" + idAlbum), "genres");
        JsonArray lista = generos.has("data") ? generos.getAsJsonArray("data") : new JsonArray();
        return lista.isEmpty() ? "" : texto(lista.get(0).getAsJsonObject(), "name");
    }

    // ---------- HTTP ----------

    private JsonObject get(String caminho) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(API + caminho))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> r;
        try {
            r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new DeezerException("Falha de conexão com o Deezer: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DeezerException("Operação interrompida.", e);
        }
        if (r.statusCode() != 200) {
            throw new DeezerException("O Deezer respondeu com erro HTTP " + r.statusCode() + ".");
        }
        JsonObject json = JsonParser.parseString(r.body()).getAsJsonObject();
        // O Deezer devolve erros com HTTP 200 e um objeto "error"
        if (json.has("error")) {
            JsonObject erro = json.getAsJsonObject("error");
            int codigo = erro.has("code") ? erro.get("code").getAsInt() : 0;
            switch (codigo) {
                case 4 -> throw new DeezerException("Limite de requisições do Deezer atingido. Aguarde alguns segundos.");
                case 800 -> throw new DeezerException("Música não encontrada no Deezer.");
                default -> throw new DeezerException("Erro do Deezer: " + texto(erro, "message"));
            }
        }
        return json;
    }

    private static JsonObject objeto(JsonObject o, String campo) {
        JsonElement e = o.get(campo);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }

    private static String texto(JsonObject o, String campo) {
        JsonElement e = o.get(campo);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }
}
