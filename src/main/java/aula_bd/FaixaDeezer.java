package aula_bd;

/** Uma faixa retornada pela busca do Deezer. */
public record FaixaDeezer(
        long id,
        String titulo,
        String artista,
        String album,
        long idAlbum,
        int duracaoSegundos,
        String link) {

    public String duracaoFormatada() {
        return String.format("%d:%02d", duracaoSegundos / 60, duracaoSegundos % 60);
    }
}
