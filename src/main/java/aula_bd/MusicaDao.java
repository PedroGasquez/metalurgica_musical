package aula_bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MusicaDao {
	
	public void inserir (Musica musica){
        //1: Definir o comando SQL
        String sql = "INSERT INTO tb_musica(nome, genero, autor) VALUES (?, ?, ?)";
        //2: Abrir uma conex�o
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection c = factory.obtemConexao()){
            //3: Pr� compila o comando
            PreparedStatement ps = c.prepareStatement(sql);
            //4: Preenche os dados faltantes
            ps.setString(1, musica.getNome());
            ps.setString(2, musica.getGenero());
            ps.setString(3, musica.getAutor());
            //5: Executa o comando
            ps.execute();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
	
	public void atualizar (Musica musica){
        //1: Definir o comando SQL
        String sql = "UPDATE tb_musica SET nome = ?, genero = ?, autor = ? WHERE codigo = ?";
        //2: Abrir uma conex�o
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection c = factory.obtemConexao()){
            //3: Pr� compila o comando
            PreparedStatement ps = c.prepareStatement(sql);
            //4: Preenche os dados faltantes
            ps.setString(1, musica.getNome());
            ps.setString(2, musica.getGenero());
            ps.setString(3, musica.getAutor());
            ps.setInt(4, musica.getCodigo());
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
        //2: Abrir uma conex�o
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection c = factory.obtemConexao()){
            //3: Pr� compila o comando
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
        //2: Abrir uma conex�o
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection c = factory.obtemConexao()){
            //3: Pré compila o comando
            PreparedStatement ps = c.prepareStatement(sql);
            //4: Executa o comando e guarda
            //o resultado em um ResultSet
            ResultSet rs = ps.executeQuery();
            //5: itera sobre o resultado
            while (rs.next()){
                int codigo = rs.getInt("codigo");
                String nome = rs.getString("nome");
                String genero = rs.getString("genero");
                String autor = rs.getString("autor");
            	Musica p = new Musica(nome,genero,autor);
            	p.setCodigo(codigo);
                pessoas.add(p);       
            }
        }
            catch (Exception e){
            e.printStackTrace();
        }
        return pessoas;
    }
	
	public Musica mostraPorId(int codigo) {
        String sql = "SELECT * FROM tb_musica WHERE codigo = ?;";
        String nome = "";
        String genero = "";
        String autor = "";
        //2: Abrir uma conex�o
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection c = factory.obtemConexao()){
            //3: Pré compila o comando
            PreparedStatement ps = c.prepareStatement(sql);
            //4: Preenche os dados faltantes
            ps.setInt(1, codigo);
            //5: Executa o comando e guarda
             //o resultado em um ResultSet
            ResultSet rs = ps.executeQuery();
            rs.next();
            nome = rs.getString("nome");
            genero = rs.getString("genero");
            autor = rs.getString("autor");
        }
        	catch (Exception e){
        	e.printStackTrace();
       }		
        Musica pessoa = new Musica(nome,genero,autor);
        pessoa.setCodigo(codigo);
        return pessoa;
	}
}
