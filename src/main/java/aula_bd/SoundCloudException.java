package aula_bd;

/** Erro ao falar com a API do SoundCloud, com mensagem pronta para mostrar ao usuário. */
public class SoundCloudException extends RuntimeException {

    public SoundCloudException(String mensagem) {
        super(mensagem);
    }

    public SoundCloudException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
