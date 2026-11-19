package aula_bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class MusicaDao {

	public void inserir (Musica musica){
        //1: Definir o comando SQL
        String sql = "INSERT INTO tb_musica(nome, genero, autor, deezer_id, link) VALUES (?, ?, ?, ?, ?)";
        //2: Abrir uma conexão
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection c = factory.obtemConexao()){
            //3: Pré compila o comando
            PreparedStatement ps = c.prepareStatement(sql);
            //4: Preenche os dados faltantes
            ps.setString(1, musica.getNome());
            ps.setString(2, musica.getGenero());
            ps.setString(3, musica.getAutor());
            ps.setObject(4, musica.getDeezerId(), Types.BIGINT);
            ps.setString(5, musica.getLink());
            //5: Executa o comando
            ps.execute();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

	public void atualizar (Musica musica){
        //1: Definir o comando SQL
        String sql = "UPDATE tb_musica SET nome = ?, genero = ?, autor = ?, deezer_id = ?, link = ? WHERE codigo = ?";
        //2: Abrir uma conexão
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection c = factory.obtemConexao()){
            //3: Pré compila o comando
            PreparedStatement ps = c.prepareStatement(sql);
            //4: Preenche os dados faltantes
            ps.setString(1, musica.getNome());
            ps.setString(2, musica.getGenero());
            ps.setString(3, musica.getAutor());
            ps.setObject(4, musica.getDeezerId(), Types.BIGINT);
            ps.setString(5, musica.getLink());
            ps.setInt(6, musica.getCodigo());
            //5: Executa o comando
            ps.execute();

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

	public void apagar (int codigo){
        //1: Definir o comando SQL
        String sql = "DELETE FROM tb_musica WHERE codigo = ?";
        //2: Abrir uma conexão
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection c = factory.obtemConexao()){
            //3: Pré compila o comando
            PreparedStatement ps = c.prepareStatement(sql);
            //4: Preenche os dados faltantes
            ps.setInt(1, codigo);
            //5: Executa o comando
            ps.execute();
        }
            catch (Exception e){
            e.printStackTrace();
        }
	}

	public List<Musica> listar (){
		List<Musica> pessoas = new ArrayList<> ();
        //1: Definir o comando SQL
        String sql = "SELECT * FROM tb_musica";
        //2: Abrir uma conexão
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection c = factory.obtemConexao()){
            //3: Pré compila o comando
            PreparedStatement ps = c.prepareStatement(sql);
            //4: Executa o comando e guarda
            //o resultado em um ResultSet
            ResultSet rs = ps.executeQuery();
            //5: itera sobre o resultado
            while (rs.next()){
                pessoas.add(lerMusica(rs));
            }
        }
            catch (Exception e){
            e.printStackTrace();
        }
        return pessoas;
    }

	public Musica mostraPorId(int codigo) {
        String sql = "SELECT * FROM tb_musica WHERE codigo = ?;";
        Musica musica = new Musica("", "", "");
        musica.setCodigo(codigo);
        //2: Abrir uma conexão
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection c = factory.obtemConexao()){
            //3: Pré compila o comando
            PreparedStatement ps = c.prepareStatement(sql);
            //4: Preenche os dados faltantes
            ps.setInt(1, codigo);
            //5: Executa o comando e guarda
             //o resultado em um ResultSet
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                musica = lerMusica(rs);
            }
        }
        	catch (Exception e){
        	e.printStackTrace();
       }
        return musica;
	}

	private Musica lerMusica(ResultSet rs) throws SQLException {
        Musica m = new Musica(rs.getString("nome"), rs.getString("genero"), rs.getString("autor"));
        m.setCodigo(rs.getInt("codigo"));
        long deezerId = rs.getLong("deezer_id");
        m.setDeezerId(rs.wasNull() ? null : deezerId);
        m.setLink(rs.getString("link"));
        return m;
	}
}
