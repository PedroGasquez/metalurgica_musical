package aula_bd;

import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.*;

public class TelaPrincipal extends JFrame {

    // Paleta
    static final Color FUNDO      = new Color(0x0F1115);
    static final Color SUPERFICIE = new Color(0x171A21);
    static final Color CAMPO      = new Color(0x1F232C);
    static final Color BORDA      = new Color(0x2A2F3A);
    static final Color TEXTO      = new Color(0xE6E8EE);
    static final Color SUAVE      = new Color(0x8A92A6);
    static final Color DESTAQUE   = new Color(0x7C5CFF);
    static final Color DESTAQUE_H = new Color(0x9479FF);
    static final Color PERIGO     = new Color(0xFF5C7A);
    static final Color SUCESSO    = new Color(0x3DDC97);
    static final Color SELECAO    = new Color(0x2D2550);
    static final Color LINHA_ALT  = new Color(0x1A1D25);

    static final String FONTE = "Segoe UI";

    private final MusicaDao dao = new MusicaDao();
    private final SoundCloudApi soundCloud = new SoundCloudApi();
    private final Reprodutor reprodutor = new Reprodutor(soundCloud);
    private final Map<Integer, Musica> musicas = new HashMap<>();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[] { "Código", "Nome", "Gênero", "Autor" }, 0) {
        @Override public boolean isCellEditable(int l, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) { return c == 0 ? Integer.class : String.class; }
    };
    private final TableRowSorter<DefaultTableModel> ordenador = new TableRowSorter<>(modelo);

    private final JTable tabela = new JTable(modelo);
    private final CampoTexto txtNome = new CampoTexto("Ex.: Bohemian Rhapsody");
    private final CampoTexto txtGenero = new CampoTexto("Ex.: Rock");
    private final CampoTexto txtAutor = new CampoTexto("Ex.: Queen");
    private final CampoTexto txtBusca = new CampoTexto("Buscar por nome, gênero ou autor...");
    private final JLabel lblModo = rotulo("Nova música", 20, Font.BOLD, TEXTO);
    private final JLabel lblDica = rotulo("Preencha os dados e clique em Salvar", 12, Font.PLAIN, SUAVE);
    private final JLabel lblTotal = rotulo("", 12, Font.PLAIN, SUAVE);
    private final JLabel lblStatus = rotulo(" ", 12, Font.PLAIN, SUAVE);
    private final Pilula pilConexao = new Pilula("Conectando...", SUAVE);
    private final Botao btnSalvar = new Botao("Salvar", DESTAQUE, DESTAQUE_H, Color.WHITE, false);
    private final Botao btnLimpar = new Botao("Limpar", CAMPO, BORDA, TEXTO, false);
    private final Botao btnExcluir = new Botao("Excluir", PERIGO, PERIGO, PERIGO, true);
    private final Botao btnTocar = new Botao(TEXTO_TOCAR, CAMPO, BORDA, TEXTO, false);
    private final Botao btnSoundCloud = new Botao("Buscar no SoundCloud", DESTAQUE, DESTAQUE_H, Color.WHITE, false);

    static final String TEXTO_TOCAR = "Tocar no SoundCloud";
    static final String TEXTO_PARAR = "Parar";

    private Integer codigoSelecionado = null;
    private TelaSoundCloud telaSoundCloud;

    public TelaPrincipal() {
        super("Biblioteca Musical");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1120, 700);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout(20, 20));
        raiz.setBackground(FUNDO);
        raiz.setBorder(new EmptyBorder(24, 28, 16, 28));
        raiz.add(criarCabecalho(), BorderLayout.NORTH);
        raiz.add(criarFormulario(), BorderLayout.WEST);
        raiz.add(criarAreaTabela(), BorderLayout.CENTER);
        raiz.add(lblStatus, BorderLayout.SOUTH);
        setContentPane(raiz);

        configurarAcoes();
        recarregar();
    }

    // ---------- Montagem da tela ----------

    private JComponent criarCabecalho() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JPanel titulos = new JPanel();
        titulos.setOpaque(false);
        titulos.setLayout(new BoxLayout(titulos, BoxLayout.Y_AXIS));
        titulos.add(rotulo("Biblioteca Musical", 28, Font.BOLD, TEXTO));
        titulos.add(Box.createVerticalStrut(2));
        titulos.add(rotulo("Gerencie seu acervo de músicas", 13, Font.PLAIN, SUAVE));

        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        direita.setOpaque(false);
        direita.add(pilConexao);
        btnSoundCloud.setPreferredSize(new Dimension(200, 40));
        direita.add(btnSoundCloud);

        p.add(titulos, BorderLayout.WEST);
        p.add(direita, BorderLayout.EAST);
        return p;
    }

    private JComponent criarFormulario() {
        PainelArredondado card = new PainelArredondado();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(24, 24, 24, 24));
        card.setPreferredSize(new Dimension(340, 0));

        card.add(alinhar(lblModo));
        card.add(Box.createVerticalStrut(4));
        card.add(alinhar(lblDica));
        card.add(Box.createVerticalStrut(24));
        card.add(campoComRotulo("NOME", txtNome));
        card.add(Box.createVerticalStrut(16));
        card.add(campoComRotulo("GÊNERO", txtGenero));
        card.add(Box.createVerticalStrut(16));
        card.add(campoComRotulo("AUTOR", txtAutor));
        card.add(Box.createVerticalStrut(28));

        btnSalvar.setAlignmentX(LEFT_ALIGNMENT);
        btnSalvar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        card.add(btnSalvar);
        card.add(Box.createVerticalStrut(10));

        JPanel linha = new JPanel(new GridLayout(1, 2, 10, 0));
        linha.setOpaque(false);
        linha.setAlignmentX(LEFT_ALIGNMENT);
        linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        linha.add(btnLimpar);
        linha.add(btnExcluir);
        card.add(linha);
        card.add(Box.createVerticalStrut(10));

        btnTocar.setAlignmentX(LEFT_ALIGNMENT);
        btnTocar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        card.add(btnTocar);
        card.add(Box.createVerticalGlue());

        btnExcluir.setEnabled(false);
        btnTocar.setEnabled(false);
        return card;
    }

    private JComponent criarAreaTabela() {
        PainelArredondado card = new PainelArredondado();
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(20, 20, 12, 20));

        JPanel topo = new JPanel(new BorderLayout(16, 0));
        topo.setOpaque(false);
        JPanel titulo = new JPanel();
        titulo.setOpaque(false);
        titulo.setLayout(new BoxLayout(titulo, BoxLayout.Y_AXIS));
        titulo.add(rotulo("Acervo", 18, Font.BOLD, TEXTO));
        titulo.add(lblTotal);
        topo.add(titulo, BorderLayout.WEST);
        txtBusca.setPreferredSize(new Dimension(300, 40));
        JPanel busca = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        busca.setOpaque(false);
        busca.add(txtBusca);
        topo.add(busca, BorderLayout.EAST);

        tabela.setRowSorter(ordenador);
        estilizarTabela(tabela, true);
        TableColumn col = tabela.getColumnModel().getColumn(0);
        col.setPreferredWidth(80);
        col.setMaxWidth(100);

        card.add(topo, BorderLayout.NORTH);
        card.add(rolagem(tabela), BorderLayout.CENTER);
        return card;
    }

    /** Aplica o visual padrão. Com {@code temCodigo}, a primeira coluna é o código (#1, #2...). */
    static void estilizarTabela(JTable tabela, boolean temCodigo) {
        tabela.setRowHeight(40);
        tabela.setShowGrid(false);
        tabela.setIntercellSpacing(new Dimension(0, 0));
        tabela.setBackground(SUPERFICIE);
        tabela.setForeground(TEXTO);
        tabela.setFont(new Font(FONTE, Font.PLAIN, 14));
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setFillsViewportHeight(true);
        tabela.setFocusable(false);

        JTableHeader cab = tabela.getTableHeader();
        cab.setReorderingAllowed(false);
        cab.setPreferredSize(new Dimension(0, 40));
        cab.setDefaultRenderer(new RenderizadorCabecalho(temCodigo));

        RenderizadorCelula r = new RenderizadorCelula(temCodigo);
        for (int i = 0; i < tabela.getColumnCount(); i++) {
            tabela.getColumnModel().getColumn(i).setCellRenderer(r);
        }
    }

    static JScrollPane rolagem(JTable tabela) {
        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        rolagem.getViewport().setBackground(SUPERFICIE);
        rolagem.setBackground(SUPERFICIE);
        rolagem.getVerticalScrollBar().setUI(new BarraFina());
        rolagem.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        rolagem.getVerticalScrollBar().setOpaque(false);
        JPanel canto = new JPanel();
        canto.setBackground(CAMPO);
        rolagem.setCorner(JScrollPane.UPPER_RIGHT_CORNER, canto);
        return rolagem;
    }

    // ---------- Comportamento ----------

    private void configurarAcoes() {
        btnSalvar.addActionListener(e -> salvar());
        btnLimpar.addActionListener(e -> limparFormulario());
        btnExcluir.addActionListener(e -> excluir());
        btnTocar.addActionListener(e -> tocarOuParar());
        btnSoundCloud.addActionListener(e -> abrirSoundCloud());
        reprodutor.aoMudar(this::reprodutorMudou);

        ActionListener enter = e -> salvar();
        txtNome.addActionListener(enter);
        txtGenero.addActionListener(enter);
        txtAutor.addActionListener(enter);

        getRootPane().registerKeyboardAction(e -> limparFormulario(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) selecionarLinha();
        });

        txtBusca.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
    }

    private void recarregar() {
        emSegundoPlano(() -> {
            Connection c = new ConnectionFactory().obtemConexao();
            if (c == null) return null;
            try { c.close(); } catch (SQLException ignorada) { }
            return dao.listar();
        }, lista -> {
            if (lista == null) {
                pilConexao.definir("Desconectado", PERIGO);
                status("Não foi possível conectar ao banco de dados. Verifique o SQL Server.", PERIGO);
                return;
            }
            pilConexao.definir("SQL Server conectado", SUCESSO);
            modelo.setRowCount(0);
            musicas.clear();
            for (Musica m : lista) {
                musicas.put(m.getCodigo(), m);
                modelo.addRow(new Object[] { m.getCodigo(), m.getNome(), m.getGenero(), m.getAutor() });
            }
            atualizarTotal();
        });
    }

    private void salvar() {
        String nome = txtNome.getText().trim();
        if (nome.isEmpty()) {
            status("Informe o nome da música.", PERIGO);
            txtNome.requestFocusInWindow();
            return;
        }
        Musica m = new Musica(nome, txtGenero.getText().trim(), txtAutor.getText().trim());
        Integer codigo = codigoSelecionado;
        btnSalvar.setEnabled(false);
        emSegundoPlano(() -> {
            if (codigo == null) {
                dao.inserir(m);
            } else {
                m.setCodigo(codigo);
                // Mantém o vínculo com o SoundCloud ao editar
                Musica atual = musicas.get(codigo);
                if (atual != null) {
                    m.setSoundcloudId(atual.getSoundcloudId());
                    m.setLink(atual.getLink());
                }
                dao.atualizar(m);
            }
            return Boolean.TRUE;
        }, ok -> {
            btnSalvar.setEnabled(true);
            status(codigo == null ? "Música \"" + nome + "\" cadastrada." : "Música #" + codigo + " atualizada.", SUCESSO);
            limparFormulario();
            recarregar();
        });
    }

    private void excluir() {
        if (codigoSelecionado == null) return;
        Integer codigo = codigoSelecionado;
        String nome = txtNome.getText();
        int r = JOptionPane.showConfirmDialog(this,
                "Excluir a música \"" + nome + "\"?\nEssa ação não pode ser desfeita.",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        emSegundoPlano(() -> { dao.apagar(codigo); return Boolean.TRUE; }, ok -> {
            status("Música \"" + nome + "\" excluída.", SUCESSO);
            limparFormulario();
            recarregar();
        });
    }

    private void selecionarLinha() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) return;
        int i = tabela.convertRowIndexToModel(linha);
        codigoSelecionado = (Integer) modelo.getValueAt(i, 0);
        txtNome.setText((String) modelo.getValueAt(i, 1));
        txtGenero.setText((String) modelo.getValueAt(i, 2));
        txtAutor.setText((String) modelo.getValueAt(i, 3));
        lblModo.setText("Editando #" + codigoSelecionado);
        lblDica.setText("Altere os dados e clique em Atualizar  ·  Esc cancela");
        btnSalvar.setText("Atualizar");
        btnExcluir.setEnabled(true);
        atualizarBotaoTocar();
    }

    private void limparFormulario() {
        codigoSelecionado = null;
        tabela.clearSelection();
        txtNome.setText("");
        txtGenero.setText("");
        txtAutor.setText("");
        lblModo.setText("Nova música");
        lblDica.setText("Preencha os dados e clique em Salvar");
        btnSalvar.setText("Salvar");
        btnExcluir.setEnabled(false);
        atualizarBotaoTocar();
        txtNome.requestFocusInWindow();
    }

    // ---------- SoundCloud ----------

    private void abrirSoundCloud() {
        if (telaSoundCloud == null) {
            telaSoundCloud = new TelaSoundCloud(this, soundCloud, reprodutor, this::recarregar);
        }
        telaSoundCloud.setVisible(true);
        telaSoundCloud.toFront();
    }

    private void tocarOuParar() {
        if (reprodutor.getEstado() != Reprodutor.Estado.PARADO) {
            reprodutor.parar();
            return;
        }
        Musica m = musicaSelecionada();
        if (m == null || m.getSoundcloudId() == null) return;
        reprodutor.tocar(m.getSoundcloudId(), m.getNome(), msg -> status(msg, PERIGO));
    }

    private void reprodutorMudou() {
        atualizarBotaoTocar();
        status(reprodutor.descricao(), reprodutor.getEstado() == Reprodutor.Estado.TOCANDO ? SUCESSO : SUAVE);
    }

    private void atualizarBotaoTocar() {
        if (reprodutor.getEstado() != Reprodutor.Estado.PARADO) {
            btnTocar.setText(TEXTO_PARAR);
            btnTocar.setEnabled(true);
            return;
        }
        Musica m = musicaSelecionada();
        btnTocar.setText(TEXTO_TOCAR);
        btnTocar.setEnabled(m != null && m.getSoundcloudId() != null);
    }

    private Musica musicaSelecionada() {
        return codigoSelecionado == null ? null : musicas.get(codigoSelecionado);
    }

    private void filtrar() {
        String t = txtBusca.getText().trim();
        ordenador.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(t), 1, 2, 3));
        atualizarTotal();
    }

    private void atualizarTotal() {
        int total = modelo.getRowCount();
        int visiveis = tabela.getRowCount();
        String s = total == 1 ? "1 música" : total + " músicas";
        lblTotal.setText(visiveis == total ? s : visiveis + " de " + s);
    }

    private void status(String msg, Color cor) {
        lblStatus.setText(msg);
        lblStatus.setForeground(cor);
    }

    private <T> void emSegundoPlano(Supplier<T> tarefa, Consumer<T> aoTerminar) {
        emSegundoPlano(tarefa, aoTerminar, msg -> {
            btnSalvar.setEnabled(true);
            status("Erro: " + msg, PERIGO);
        });
    }

    /** Executa {@code tarefa} fora da thread do Swing e entrega o resultado (ou a mensagem de erro) nela. */
    static <T> void emSegundoPlano(Supplier<T> tarefa, Consumer<T> aoTerminar, Consumer<String> aoFalhar) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() { return tarefa.get(); }
            @Override protected void done() {
                try {
                    aoTerminar.accept(get());
                } catch (ExecutionException e) {
                    aoFalhar.accept(e.getCause().getMessage());
                } catch (Exception e) {
                    aoFalhar.accept(e.getMessage());
                }
            }
        }.execute();
    }

    // ---------- Utilitários de UI ----------

    static JLabel rotulo(String texto, int tamanho, int estilo, Color cor) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font(FONTE, estilo, tamanho));
        l.setForeground(cor);
        return l;
    }

    private static JComponent alinhar(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    private static JComponent campoComRotulo(String titulo, CampoTexto campo) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(LEFT_ALIGNMENT);
        JLabel l = rotulo(titulo, 11, Font.BOLD, SUAVE);
        l.setAlignmentX(LEFT_ALIGNMENT);
        campo.setAlignmentX(LEFT_ALIGNMENT);
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        p.add(l);
        p.add(Box.createVerticalStrut(6));
        p.add(campo);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        return p;
    }

    private static Graphics2D suave(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        return g2;
    }

    static void aplicarTemaDialogos() {
        UIManager.put("OptionPane.background", SUPERFICIE);
        UIManager.put("Panel.background", SUPERFICIE);
        UIManager.put("OptionPane.messageForeground", TEXTO);
        UIManager.put("OptionPane.messageFont", new Font(FONTE, Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", new Font(FONTE, Font.BOLD, 13));
        UIManager.put("Button.background", CAMPO);
        UIManager.put("Button.foreground", TEXTO);
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        UIManager.put("Button.select", BORDA);
    }

    // ---------- Componentes personalizados ----------

    static class PainelArredondado extends JPanel {
        PainelArredondado() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = suave(g);
            g2.setColor(SUPERFICIE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g2.setColor(BORDA);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g2.dispose();
        }
    }

    static class Botao extends JButton {
        private final Color base, hover;
        private final boolean contorno;

        Botao(String texto, Color base, Color hover, Color textoCor, boolean contorno) {
            super(texto);
            this.base = base;
            this.hover = hover;
            this.contorno = contorno;
            setForeground(textoCor);
            setFont(new Font(FONTE, Font.BOLD, 14));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(120, 44));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = suave(g);
            boolean sobre = getModel().isRollover() && isEnabled();
            Composite original = g2.getComposite();
            if (!isEnabled()) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            if (contorno) {
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), sobre ? 40 : 18));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(base);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            } else {
                g2.setColor(sobre ? hover : base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(getForeground());
            String t = getText();
            g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.setComposite(original);
            g2.dispose();
        }
    }

    static class CampoTexto extends JTextField {
        private final String dica;

        CampoTexto(String dica) {
            this.dica = dica;
            setOpaque(false);
            setForeground(TEXTO);
            setCaretColor(TEXTO);
            setSelectionColor(SELECAO);
            setSelectedTextColor(TEXTO);
            setFont(new Font(FONTE, Font.PLAIN, 14));
            setBorder(new EmptyBorder(10, 14, 10, 14));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = suave(g);
            g2.setColor(CAMPO);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(hasFocus() ? DESTAQUE : BORDA);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            if (getText().isEmpty() && !hasFocus()) {
                g2.setColor(SUAVE.darker());
                g2.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(dica, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class Pilula extends JLabel {
        private Color cor;

        Pilula(String texto, Color cor) {
            setFont(new Font(FONTE, Font.BOLD, 12));
            setBorder(new EmptyBorder(6, 14, 6, 14));
            definir(texto, cor);
        }

        void definir(String texto, Color cor) {
            this.cor = cor;
            setText("●  " + texto);
            setForeground(cor);
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = suave(g);
            g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 28));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class RenderizadorCabecalho extends DefaultTableCellRenderer {
        private final boolean temCodigo;
        RenderizadorCabecalho(boolean temCodigo) { this.temCodigo = temCodigo; }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int l, int c) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, l, c);
            lbl.setText(v == null ? "" : v.toString().toUpperCase());
            lbl.setFont(new Font(FONTE, Font.BOLD, 11));
            lbl.setForeground(SUAVE);
            lbl.setBackground(CAMPO);
            lbl.setOpaque(true);
            lbl.setHorizontalAlignment(temCodigo && c == 0 ? CENTER : LEFT);
            lbl.setBorder(new EmptyBorder(0, 14, 0, 14));
            return lbl;
        }
    }

    static class RenderizadorCelula extends DefaultTableCellRenderer {
        private final boolean temCodigo;
        RenderizadorCelula(boolean temCodigo) { this.temCodigo = temCodigo; }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int l, int c) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, false, l, c);
            lbl.setBorder(new EmptyBorder(0, 14, 0, 14));
            lbl.setBackground(s ? SELECAO : (l % 2 == 0 ? SUPERFICIE : LINHA_ALT));
            int principal = temCodigo ? 1 : 0;
            if (temCodigo && c == 0) {
                lbl.setText(v == null ? "" : "#" + v);
                lbl.setForeground(SUAVE);
                lbl.setHorizontalAlignment(CENTER);
                lbl.setFont(new Font(FONTE, Font.PLAIN, 13));
            } else {
                lbl.setForeground(c == principal ? TEXTO : new Color(0xB8BECC));
                lbl.setHorizontalAlignment(LEFT);
                lbl.setFont(new Font(FONTE, c == principal ? Font.BOLD : Font.PLAIN, 14));
            }
            return lbl;
        }
    }

    static class BarraFina extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            thumbColor = BORDA;
            trackColor = SUPERFICIE;
        }
        @Override protected JButton createDecreaseButton(int o) { return botaoVazio(); }
        @Override protected JButton createIncreaseButton(int o) { return botaoVazio(); }
        private JButton botaoVazio() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D g2 = suave(g);
            g2.setColor(isThumbRollover() ? SUAVE : BORDA);
            g2.fillRoundRect(r.x + 1, r.y, r.width - 2, r.height, 6, 6);
            g2.dispose();
        }
    }

    public static void abrir() {
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            aplicarTemaDialogos();
            new TelaPrincipal().setVisible(true);
        });
    }
}
