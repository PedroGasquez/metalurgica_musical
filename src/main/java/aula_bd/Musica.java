package aula_bd;

public class Musica {
	private int codigo;
	private String nome;
	private String genero;
	private String autor;
	// Preenchidos apenas para músicas importadas do SoundCloud
	private Long soundcloudId;
	private String link;

	public Musica() {
		super();
	}

	public Musica(String nome, String genero, String autor) {
		super();
		this.nome = nome;
		this.genero = genero;
		this.autor = autor;
	}

	public int getCodigo() {
		return codigo;
	}
	public void setCodigo(int codigo) {
		this.codigo = codigo;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getGenero() {
		return genero;
	}

	public void setGenero(String genero) {
		this.genero = genero;
	}

	public String getAutor() {
		return autor;
	}

	public void setAutor(String autor) {
		this.autor = autor;
	}

	public Long getSoundcloudId() {
		return soundcloudId;
	}

	public void setSoundcloudId(Long soundcloudId) {
		this.soundcloudId = soundcloudId;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	@Override
	public String toString() {
		return "Musica [codigo=" + codigo + ", nome=" + nome + ", genero=" + genero + ", autor=" + autor
				+ ", soundcloudId=" + soundcloudId + ", link=" + link + "]";
	}



}
