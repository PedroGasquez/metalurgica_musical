package aula_bd;

/** Erro ao falar com a API do Deezer, com mensagem pronta para mostrar ao usuário. */
public class DeezerException extends RuntimeException {

    public DeezerException(String mensagem) {
        super(mensagem);
    }

    public DeezerException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
