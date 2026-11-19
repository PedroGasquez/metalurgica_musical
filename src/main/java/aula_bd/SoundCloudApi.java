package aula_bd;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Cliente da API pública do SoundCloud (https://developers.soundcloud.com/docs/api/guide).
 *
 * Usa o fluxo OAuth "client credentials", suficiente para buscar e tocar faixas públicas.
 * As credenciais são lidas, nesta ordem, de:
 *  1. variáveis de ambiente SOUNDCLOUD_CLIENT_ID e SOUNDCLOUD_CLIENT_SECRET
 *  2. arquivo soundcloud.properties na pasta do projeto (veja soundcloud.properties.example)
 */
public class SoundCloudApi {

    private static final String API = "https://api.soundcloud.com";
    private static final String URL_TOKEN = "https://secure.soundcloud.com/oauth/token";
    private static final int LIMITE_BUSCA = 30;

    private static final Properties ARQUIVO = Configuracao.carregar("soundcloud.properties");

    private final String clientId = Configuracao.ler(ARQUIVO, "SOUNDCLOUD_CLIENT_ID", "client_id", "");
    private final String clientSecret = Configuracao.ler(ARQUIVO, "SOUNDCLOUD_CLIENT_SECRET", "client_secret", "");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    // Para descobrir o endereço final do áudio sem baixá-lo
    private final HttpClient httpSemRedirecionar = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private String token;
    private Instant tokenExpiraEm = Instant.MIN;

    public boolean isConfigurada() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    /** Busca faixas que podem ser tocadas (completas ou prévias). */
    public List<FaixaSoundCloud> buscar(String termo) {
        String caminho = "/tracks?linked_partitioning=true&access=playable,preview"
                + "&limit=" + LIMITE_BUSCA
                + "&q=" + URLEncoder.encode(termo, StandardCharsets.UTF_8);
        JsonElement raiz = JsonParser.parseString(get(caminho));
        JsonArray itens = raiz.isJsonArray() ? raiz.getAsJsonArray() : raiz.getAsJsonObject().getAsJsonArray("collection");

        List<FaixaSoundCloud> faixas = new ArrayList<>();
        if (itens == null) return faixas;
        for (JsonElement e : itens) {
            JsonObject t = e.getAsJsonObject();
            JsonObject usuario = t.has("user") && t.get("user").isJsonObject() ? t.getAsJsonObject("user") : new JsonObject();
            faixas.add(new FaixaSoundCloud(
                    t.get("id").getAsLong(),
                    texto(t, "title"),
                    texto(usuario, "username"),
                    texto(t, "genre"),
                    t.has("duration") && !t.get("duration").isJsonNull() ? t.get("duration").getAsLong() : 0,
                    texto(t, "permalink_url"),
                    texto(t, "access")));
        }
        return faixas;
    }

    /** Endereço de áudio que o reprodutor consegue abrir diretamente. */
    public String urlDeStream(long idFaixa) {
        JsonObject streams = JsonParser.parseString(get("/tracks/" + idFaixa + "/streams")).getAsJsonObject();
        // Em ordem de preferência: MP3 contínuo, HLS com MP3, HLS com AAC, prévia de 30s
        for (String campo : List.of("http_mp3_128_url", "hls_mp3_128_url", "hls_aac_160_url", "preview_mp3_128_url")) {
            String url = texto(streams, campo);
            if (!url.isEmpty()) return resolver(url);
        }
        throw new SoundCloudException("Esta música não está disponível para streaming.");
    }

    // ---------- HTTP ----------

    private String get(String caminho) {
        HttpResponse<String> r = enviar(requisicao(URI.create(API + caminho)).GET().build());
        if (r.statusCode() == 401) {
            // Token expirou ou foi revogado: pede outro e tenta de novo uma vez
            invalidarToken();
            r = enviar(requisicao(URI.create(API + caminho)).GET().build());
        }
        verificar(r);
        return r.body();
    }

    /**
     * As URLs de stream da API exigem o token e redirecionam para o CDN.
     * O reprodutor não envia cabeçalhos, então descobrimos aqui o endereço final.
     */
    private String resolver(String url) {
        if (!url.startsWith(API)) return url;
        try {
            HttpResponse<InputStream> r = httpSemRedirecionar.send(requisicao(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream corpo = r.body()) {
                int status = r.statusCode();
                if (status >= 300 && status < 400) {
                    return r.headers().firstValue("Location")
                            .orElseThrow(() -> new SoundCloudException("O SoundCloud não informou o endereço do áudio."));
                }
                if (status == 200 && r.headers().firstValue("Content-Type").orElse("").contains("json")) {
                    String resolvida = texto(JsonParser.parseString(new String(corpo.readAllBytes(), StandardCharsets.UTF_8))
                            .getAsJsonObject(), "url");
                    if (!resolvida.isEmpty()) return resolvida;
                }
                if (status != 200) throw new SoundCloudException("O SoundCloud recusou o áudio (HTTP " + status + ").");
                return url;
            }
        } catch (IOException e) {
            throw new SoundCloudException("Falha de conexão com o SoundCloud: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SoundCloudException("Operação interrompida.", e);
        }
    }

    private HttpRequest.Builder requisicao(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json; charset=utf-8")
                .header("Authorization", "OAuth " + obterToken());
    }

    private synchronized void invalidarToken() {
        token = null;
    }

    private synchronized String obterToken() {
        if (!isConfigurada()) {
            throw new SoundCloudException("Credenciais do SoundCloud não configuradas. Veja soundcloud.properties.example.");
        }
        if (token != null && Instant.now().isBefore(tokenExpiraEm)) return token;

        String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        HttpRequest req = HttpRequest.newBuilder(URI.create(URL_TOKEN))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json; charset=utf-8")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + basic)
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();
        HttpResponse<String> r = enviar(req);
        if (r.statusCode() == 400 || r.statusCode() == 401) {
            throw new SoundCloudException("Client ID ou Client Secret do SoundCloud inválidos.");
        }
        verificar(r);

        JsonObject json = JsonParser.parseString(r.body()).getAsJsonObject();
        token = texto(json, "access_token");
        long expiraEmSegundos = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600;
        // Renova um minuto antes de expirar
        tokenExpiraEm = Instant.now().plusSeconds(Math.max(0, expiraEmSegundos - 60));
        return token;
    }

    private HttpResponse<String> enviar(HttpRequest req) {
        try {
            return http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SoundCloudException("Falha de conexão com o SoundCloud: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SoundCloudException("Operação interrompida.", e);
        }
    }

    private static void verificar(HttpResponse<String> r) {
        int s = r.statusCode();
        if (s >= 200 && s < 300) return;
        switch (s) {
            case 401 -> throw new SoundCloudException("Acesso negado pelo SoundCloud. Verifique as credenciais.");
            case 403 -> throw new SoundCloudException("O SoundCloud não permite acessar este conteúdo.");
            case 404 -> throw new SoundCloudException("Conteúdo não encontrado no SoundCloud.");
            case 429 -> throw new SoundCloudException("Limite de requisições do SoundCloud atingido. Tente mais tarde.");
            default  -> throw new SoundCloudException("O SoundCloud respondeu com erro HTTP " + s + ".");
        }
    }

    private static String texto(JsonObject o, String campo) {
        JsonElement e = o.get(campo);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }
}
