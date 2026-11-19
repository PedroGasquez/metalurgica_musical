package aula_bd;

import static aula_bd.TelaPrincipal.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/** Janela para buscar músicas no SoundCloud, ouvir e adicionar ao acervo. */
public class TelaSoundCloud extends JDialog {

    private final SoundCloudApi api;
    private final Reprodutor reprodutor;
    private final Runnable aoAdicionar;
    private final MusicaDao dao = new MusicaDao();

    private final List<FaixaSoundCloud> faixas = new ArrayList<>();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[] { "Título", "Artista", "Gênero", "Duração" }, 0) {
        @Override public boolean isCellEditable(int l, int c) { return false; }
    };
    private final JTable tabela = new JTable(modelo);

    private final CampoTexto txtBusca = new CampoTexto("Buscar músicas ou artistas no SoundCloud...");
    private final Botao btnBuscar = new Botao("Buscar", DESTAQUE, DESTAQUE_H, Color.WHITE, false);
    private final Botao btnTocar = new Botao("Tocar", DESTAQUE, DESTAQUE_H, Color.WHITE, false);
    private final Botao btnAdicionar = new Botao("Adicionar ao acervo", CAMPO, BORDA, TEXTO, false);
    private final Botao btnAbrir = new Botao("Abrir no SoundCloud", CAMPO, BORDA, TEXTO, false);
    private final JLabel lblStatus = rotulo(" ", 12, Font.PLAIN, SUAVE);

    public TelaSoundCloud(Frame dono, SoundCloudApi api, Reprodutor reprodutor, Runnable aoAdicionar) {
        super(dono, "SoundCloud", false);
        this.api = api;
        this.reprodutor = reprodutor;
        this.aoAdicionar = aoAdicionar;

        setSize(980, 640);
        setMinimumSize(new Dimension(780, 480));
        setLocationRelativeTo(dono);

        JPanel raiz = new JPanel(new BorderLayout(16, 16));
        raiz.setBackground(FUNDO);
        raiz.setBorder(new EmptyBorder(24, 28, 16, 28));
        raiz.add(criarTopo(), BorderLayout.NORTH);
        raiz.add(criarAreaTabela(), BorderLayout.CENTER);
        raiz.add(criarRodape(), BorderLayout.SOUTH);
        setContentPane(raiz);

        configurarAcoes();
        atualizarBotoes();
        if (!api.isConfigurada()) {
            status("Configure SOUNDCLOUD_CLIENT_ID e SOUNDCLOUD_CLIENT_SECRET (veja soundcloud.properties.example).", PERIGO);
        } else {
            status("Digite um termo e pressione Enter. Clique duas vezes numa música para tocar.", SUAVE);
        }
    }

    // ---------- Montagem da tela ----------

    private JComponent criarTopo() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel titulo = rotulo("SoundCloud", 26, Font.BOLD, TEXTO);
        JLabel subtitulo = rotulo("Busque e ouça músicas públicas. O áudio é transmitido, nada é baixado.", 13, Font.PLAIN, SUAVE);
        titulo.setAlignmentX(LEFT_ALIGNMENT);
        subtitulo.setAlignmentX(LEFT_ALIGNMENT);
        p.add(titulo);
        p.add(Box.createVerticalStrut(2));
        p.add(subtitulo);
        p.add(Box.createVerticalStrut(16));

        JPanel busca = new JPanel(new BorderLayout(10, 0));
        busca.setOpaque(false);
        busca.setAlignmentX(LEFT_ALIGNMENT);
        busca.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnBuscar.setPreferredSize(new Dimension(120, 42));
        busca.add(txtBusca, BorderLayout.CENTER);
        busca.add(btnBuscar, BorderLayout.EAST);
        p.add(busca);
        return p;
    }

    private JComponent criarAreaTabela() {
        PainelArredondado card = new PainelArredondado();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        estilizarTabela(tabela, false);
        TableColumn duracao = tabela.getColumnModel().getColumn(3);
        duracao.setPreferredWidth(120);
        duracao.setMaxWidth(160);

        card.add(rolagem(tabela), BorderLayout.CENTER);
        return card;
    }

    private JComponent criarRodape() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botoes.setOpaque(false);
        btnAbrir.setPreferredSize(new Dimension(190, 44));
        btnAdicionar.setPreferredSize(new Dimension(190, 44));
        btnTocar.setPreferredSize(new Dimension(130, 44));
        botoes.add(btnAbrir);
        botoes.add(btnAdicionar);
        botoes.add(btnTocar);

        p.add(botoes, BorderLayout.NORTH);
        p.add(lblStatus, BorderLayout.SOUTH);
        return p;
    }

    // ---------- Comportamento ----------

    private void configurarAcoes() {
        btnBuscar.addActionListener(e -> buscar());
        txtBusca.addActionListener(e -> buscar());
        btnTocar.addActionListener(e -> tocarOuParar());
        btnAdicionar.addActionListener(e -> adicionarAoAcervo());
        btnAbrir.addActionListener(e -> abrirNoNavegador());

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) atualizarBotoes();
        });
        tabela.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabela.rowAtPoint(e.getPoint()) >= 0) tocarSelecionada();
            }
        });

        getRootPane().registerKeyboardAction(e -> setVisible(false),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        reprodutor.aoMudar(() -> {
            atualizarBotoes();
            status(reprodutor.descricao(), reprodutor.getEstado() == Reprodutor.Estado.TOCANDO ? SUCESSO : SUAVE);
        });
    }

    private void buscar() {
        String termo = txtBusca.getText().trim();
        if (termo.isEmpty()) {
            status("Digite o nome de uma música ou artista.", PERIGO);
            txtBusca.requestFocusInWindow();
            return;
        }
        btnBuscar.setEnabled(false);
        status("Buscando \"" + termo + "\" no SoundCloud...", SUAVE);
        emSegundoPlano(() -> api.buscar(termo), lista -> {
            btnBuscar.setEnabled(true);
            faixas.clear();
            faixas.addAll(lista);
            modelo.setRowCount(0);
            for (FaixaSoundCloud f : lista) {
                String duracao = f.duracaoFormatada() + (f.previa() ? "  · prévia" : "");
                modelo.addRow(new Object[] { f.titulo(), f.artista(), f.genero(), duracao });
            }
            atualizarBotoes();
            status(lista.isEmpty() ? "Nenhuma música encontrada para \"" + termo + "\"."
                    : lista.size() + " músicas encontradas. Clique duas vezes para tocar.", SUAVE);
        }, msg -> {
            btnBuscar.setEnabled(true);
            status(msg, PERIGO);
        });
    }

    private void tocarOuParar() {
        if (reprodutor.getEstado() != Reprodutor.Estado.PARADO) {
            reprodutor.parar();
        } else {
            tocarSelecionada();
        }
    }

    private void tocarSelecionada() {
        FaixaSoundCloud f = faixaSelecionada();
        if (f == null) return;
        reprodutor.tocar(f.id(), f.titulo() + " — " + f.artista(), msg -> status(msg, PERIGO));
    }

    private void adicionarAoAcervo() {
        FaixaSoundCloud f = faixaSelecionada();
        if (f == null) return;
        // Respeita o tamanho das colunas de tb_musica
        Musica m = new Musica(limitar(f.titulo(), 100), limitar(f.genero(), 50), limitar(f.artista(), 100));
        m.setSoundcloudId(f.id());
        m.setLink(limitar(f.link(), 500));
        btnAdicionar.setEnabled(false);
        emSegundoPlano(() -> { dao.inserir(m); return Boolean.TRUE; }, ok -> {
            atualizarBotoes();
            status("\"" + f.titulo() + "\" adicionada ao acervo.", SUCESSO);
            aoAdicionar.run();
        }, msg -> {
            atualizarBotoes();
            status("Erro ao salvar: " + msg, PERIGO);
        });
    }

    private void abrirNoNavegador() {
        FaixaSoundCloud f = faixaSelecionada();
        if (f == null || f.link().isEmpty()) return;
        try {
            Desktop.getDesktop().browse(URI.create(f.link()));
        } catch (Exception e) {
            status("Não foi possível abrir o navegador: " + e.getMessage(), PERIGO);
        }
    }

    private void atualizarBotoes() {
        boolean selecionada = faixaSelecionada() != null;
        boolean tocando = reprodutor.getEstado() != Reprodutor.Estado.PARADO;
        btnTocar.setText(tocando ? TEXTO_PARAR : "Tocar");
        btnTocar.setEnabled(tocando || selecionada);
        btnAdicionar.setEnabled(selecionada);
        btnAbrir.setEnabled(selecionada);
    }

    private FaixaSoundCloud faixaSelecionada() {
        int linha = tabela.getSelectedRow();
        return linha < 0 || linha >= faixas.size() ? null : faixas.get(linha);
    }

    private void status(String msg, Color cor) {
        lblStatus.setText(msg);
        lblStatus.setForeground(cor);
    }

    private static String limitar(String texto, int tamanho) {
        if (texto == null) return null;
        return texto.length() <= tamanho ? texto : texto.substring(0, tamanho);
    }
}
