package aula_bd;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;

/**
 * Toca as prévias de 30 segundos do Deezer (MP3) usando o JavaFX Media.
 * O áudio é apenas transmitido: nada é gravado em disco.
 *
 * Os ouvintes registrados em {@link #aoMudar(Runnable)} são chamados na thread do Swing.
 */
public class Reprodutor {

    public enum Estado { PARADO, CARREGANDO, TOCANDO }

    private static boolean javaFxIniciado;

    private final DeezerApi api;
    private final List<Runnable> ouvintes = new CopyOnWriteArrayList<>();
    // Cada pedido de tocar recebe um número; respostas de pedidos antigos são ignoradas
    private final AtomicLong pedidoAtual = new AtomicLong();

    private volatile Estado estado = Estado.PARADO;
    private volatile String titulo;
    private MediaPlayer player; // acessado só na thread do JavaFX

    public Reprodutor(DeezerApi api) {
        this.api = api;
    }

    public void aoMudar(Runnable ouvinte) {
        ouvintes.add(ouvinte);
    }

    public Estado getEstado() {
        return estado;
    }

    public String getTitulo() {
        return titulo;
    }

    public String descricao() {
        return switch (estado) {
            case CARREGANDO -> "Carregando \"" + titulo + "\"...";
            case TOCANDO -> "Tocando \"" + titulo + "\"  ·  prévia do Deezer";
            case PARADO -> "Reprodução parada.";
        };
    }

    /** Busca o endereço do áudio em segundo plano e começa a tocar. */
    public void tocar(long idFaixa, String titulo, Consumer<String> aoErro) {
        long pedido = pedidoAtual.incrementAndGet();
        mudar(Estado.CARREGANDO, titulo);
        Thread.ofVirtual().start(() -> {
            try {
                String url = api.urlDaPrevia(idFaixa);
                iniciarJavaFx();
                Platform.runLater(() -> iniciarPlayer(pedido, url, titulo, aoErro));
            } catch (Exception e) {
                falhar(pedido, e.getMessage(), aoErro);
            }
        });
    }

    public void parar() {
        pedidoAtual.incrementAndGet();
        mudar(Estado.PARADO, null);
        if (javaFxIniciado()) Platform.runLater(this::liberarPlayer);
    }

    // ---------- Thread do JavaFX ----------

    private void iniciarPlayer(long pedido, String url, String titulo, Consumer<String> aoErro) {
        if (pedidoAtual.get() != pedido) return;
        liberarPlayer();
        MediaPlayer p;
        try {
            p = new MediaPlayer(new Media(url));
        } catch (MediaException e) {
            falhar(pedido, "Formato de áudio não suportado: " + e.getMessage(), aoErro);
            return;
        }
        p.setOnPlaying(() -> { if (player == p) mudar(Estado.TOCANDO, titulo); });
        p.setOnEndOfMedia(() -> {
            if (player == p) {
                liberarPlayer();
                mudar(Estado.PARADO, null);
            }
        });
        p.setOnError(() -> {
            if (player == p) {
                liberarPlayer();
                falhar(pedido, "Não foi possível tocar a música: " + p.getError().getMessage(), aoErro);
            }
        });
        player = p;
        p.play();
    }

    private void liberarPlayer() {
        if (player != null) {
            player.dispose();
            player = null;
        }
    }

    // ---------- Apoio ----------

    private void falhar(long pedido, String mensagem, Consumer<String> aoErro) {
        if (pedidoAtual.get() != pedido) return;
        mudar(Estado.PARADO, null);
        SwingUtilities.invokeLater(() -> aoErro.accept(mensagem));
    }

    private void mudar(Estado novo, String novoTitulo) {
        estado = novo;
        titulo = novoTitulo;
        for (Runnable r : ouvintes) SwingUtilities.invokeLater(r);
    }

    private static synchronized boolean javaFxIniciado() {
        return javaFxIniciado;
    }

    /** Inicia o JavaFX sem janela própria, só para usar o player de mídia dentro do Swing. */
    private static synchronized void iniciarJavaFx() {
        if (javaFxIniciado) return;
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException jaIniciado) {
            // Outro componente já iniciou o JavaFX
        }
        Platform.setImplicitExit(false);
        javaFxIniciado = true;
    }
}
