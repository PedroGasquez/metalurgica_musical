package aula_bd;

/** Uma faixa retornada pela API do SoundCloud. */
public record FaixaSoundCloud(
        long id,
        String titulo,
        String artista,
        String genero,
        long duracaoMs,
        String link,
        String acesso) {

    /** A API só libera 30 segundos das faixas com acesso "preview". */
    public boolean previa() {
        return "preview".equals(acesso);
    }

    public String duracaoFormatada() {
        long segundos = duracaoMs / 1000;
        return String.format("%d:%02d", segundos / 60, segundos % 60);
    }
}
