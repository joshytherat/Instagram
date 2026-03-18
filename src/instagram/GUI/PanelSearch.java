package instagram.GUI;

import instagram.Enums.EstadoCuenta;
import instagram.Utilities.FollowManager;
import instagram.Utilities.Post;
import instagram.Utilities.PostManager;
import instagram.Enums.TipoCuenta;
import instagram.Utilities.User;
import instagram.Utilities.UserManager;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

public class PanelSearch extends JPanel {

    private User          usuario;
    private UserManager   userManager;
    private FollowManager followManager;
    private PostManager   postManager;
    private MainFrame     mainFrame;

    private JTextField txtBuscar;
    private JPanel     listPanel;

    // FIX: flag para evitar que el DocumentListener dispare búsqueda cuando
    // se escribe el texto de placeholder programáticamente en focusLost.
    private boolean settingPlaceholder = false;

    public PanelSearch(User usuario, UserManager userManager,
                       FollowManager followManager, PostManager postManager,
                       MainFrame mainFrame) {
        this.usuario       = usuario;
        this.userManager   = userManager;
        this.followManager = followManager;
        this.postManager   = postManager;
        this.mainFrame     = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        construir();
        mostrarSugerencias();
    }

    private void construir() {
        // ── Barra de búsqueda ─────────────────────────────────
        JPanel barra = new JPanel(new BorderLayout(8, 0));
        barra.setBackground(Color.WHITE);
        barra.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(219, 219, 219)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JPanel inputWrap = new JPanel(new BorderLayout(6, 0));
        inputWrap.setBackground(new Color(239, 239, 239));
        inputWrap.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(219, 219, 219), 1),
            BorderFactory.createEmptyBorder(0, 10, 0, 10)));

        JLabel lupa = new JLabel();
        ImageIcon lupaIcon = InstagramIcons.cargar("buscar.png", 16, 16);
        if (lupaIcon != null) lupa.setIcon(lupaIcon);
        else { lupa.setText("🔍"); lupa.setFont(new Font("SansSerif", Font.PLAIN, 14)); }
        lupa.setPreferredSize(new Dimension(22, 36));
        inputWrap.add(lupa, BorderLayout.WEST);

        txtBuscar = new JTextField();
        txtBuscar.setBorder(null);
        txtBuscar.setBackground(new Color(239, 239, 239));
        txtBuscar.setFont(new Font("SansSerif", Font.PLAIN, 14));
        txtBuscar.setForeground(new Color(142, 142, 142));
        txtBuscar.setText("Buscar");
        txtBuscar.setPreferredSize(new Dimension(0, 36));

        txtBuscar.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (txtBuscar.getText().equals("Buscar")) {
                    txtBuscar.setText("");
                    txtBuscar.setForeground(new Color(38, 38, 38));
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (txtBuscar.getText().isEmpty()) {
                    // FIX: activar flag para que el DocumentListener lo ignore
                    settingPlaceholder = true;
                    txtBuscar.setText("Buscar");
                    txtBuscar.setForeground(new Color(142, 142, 142));
                    settingPlaceholder = false;
                }
            }
        });

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { buscar(); }
            public void removeUpdate(DocumentEvent e)  { buscar(); }
            public void changedUpdate(DocumentEvent e) { buscar(); }
        });

        inputWrap.add(txtBuscar, BorderLayout.CENTER);
        barra.add(inputWrap, BorderLayout.CENTER);
        add(barra, BorderLayout.NORTH);

        // ── Panel de resultados ───────────────────────────────
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(listPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        // Scrollbar invisible pero funcional (no ocupa espacio)
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private void buscar() {
        if (settingPlaceholder) return;

        String t = txtBuscar.getText().trim();
        if (t.isEmpty() || t.equals("Buscar")) { mostrarSugerencias(); return; }

        if (t.startsWith("#")) {
            String tag = t.substring(1).trim();
            // No buscar si solo se escribió "#" sin nada más
            if (tag.isEmpty()) { mostrarSugerencias(); return; }
            buscarHashtag(tag);
        } else if (t.startsWith("@")) {
            // @ busca por username exacto
            String username = t.substring(1).trim();
            if (username.isEmpty()) { mostrarSugerencias(); return; }
            buscarUsuarios(username);
        } else {
            buscarUsuarios(t);
        }
    }

    // FIX: método público para que MainFrame lo llame al navegar a SEARCH
    public void refrescarSugerencias() {
        if (txtBuscar.getText().equals("Buscar") || txtBuscar.getText().isEmpty()) {
            mostrarSugerencias();
        }
    }

    /** Llamado desde hashtag clickeables: pone el tag en el campo y busca */
    public void navegarAHashtag(String tag) {
        settingPlaceholder = false;
        txtBuscar.setText("#" + tag);
        txtBuscar.setForeground(new Color(38, 38, 38));
        buscarHashtag(tag);
    }


    private void mostrarSugerencias() {
        new SwingWorker<ArrayList<User>, Void>() {
            @Override protected ArrayList<User> doInBackground() throws Exception {
                ArrayList<User> todos = userManager.obtenerTodosLosUsuarios();
                ArrayList<User> sug   = new ArrayList<>();
                for (User u : todos) {
                    if (u.getEstadoCuenta() != EstadoCuenta.ACTIVO) continue;
                    if (u.getUsername().equalsIgnoreCase(usuario.getUsername())) continue;
                    try {
                        if (!followManager.estaSiguiendo(usuario.getUsername(), u.getUsername()))
                            sug.add(u);
                    } catch (Exception ignored) { sug.add(u); }
                    if (sug.size() >= 30) break;
                }
                return sug;
            }
            @Override protected void done() {
                try {
                    listPanel.removeAll();
                    listPanel.add(tituloSeccion("Sugerencias para ti"));
                    for (User u : get()) listPanel.add(rowUsuario(u, true));
                    listPanel.revalidate(); listPanel.repaint();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void buscarUsuarios(String term) {
        new SwingWorker<ArrayList<User>, Void>() {
            @Override protected ArrayList<User> doInBackground() throws Exception {
                ArrayList<User> todos = userManager.obtenerTodosLosUsuarios();
                ArrayList<User> res   = new ArrayList<>();
                String t = term.toLowerCase();
                for (User u : todos) {
                    if (u.getEstadoCuenta() != EstadoCuenta.ACTIVO) continue;
                    if (u.getUsername().toLowerCase().contains(t)
                        || u.getNombreCompleto().toLowerCase().contains(t))
                        res.add(u);
                }
                return res;
            }
            @Override protected void done() {
                try {
                    listPanel.removeAll();
                    ArrayList<User> res = get();
                    if (res.isEmpty()) {
                        listPanel.add(labelVacio("No se encontraron usuarios"));
                    } else {
                        listPanel.add(tituloSeccion("Usuarios"));
                        for (User u : res) listPanel.add(rowUsuario(u, false));
                    }
                    listPanel.revalidate(); listPanel.repaint();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void buscarHashtag(String tag) {
        new SwingWorker<ArrayList<Post>, Void>() {
            @Override protected ArrayList<Post> doInBackground() throws Exception {
                ArrayList<Post> todos = postManager.obtenerFeedCompleto();
                ArrayList<Post> res   = new ArrayList<>();
                for (Post p : todos)
                    for (String h : p.getHashtags())
                        if (h.toLowerCase().contains(tag.toLowerCase())) { res.add(p); break; }
                return res;
            }
            @Override protected void done() {
                try {
                    listPanel.removeAll();
                    ArrayList<Post> res = get();
                    listPanel.add(tituloSeccion("#" + tag + "  —  " + res.size() + " publicaciones"));
                    if (res.isEmpty()) {
                        listPanel.add(labelVacio("No hay publicaciones con #" + tag));
                    } else {
                        // FlowLayout LEFT: cada thumbnail ocupa su tamaño real (no columna completa)
                        JPanel grid = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
                        grid.setBackground(new Color(239, 239, 239));
                        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
                        grid.setMaximumSize(new Dimension(390, Integer.MAX_VALUE));
                        for (Post p : res) grid.add(thumbPost(p));
                        listPanel.add(grid);
                    }
                    listPanel.revalidate(); listPanel.repaint();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // ── Componentes UI ────────────────────────────────────────────

    private JPanel rowUsuario(User u, boolean mostrarSeguir) {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(239, 239, 239)),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        p.setMaximumSize(new Dimension(390, 68));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        ComponenteCircular av = new ComponenteCircular(44);
        av.setPreferredSize(new Dimension(44, 44));
        cargarFotoCirculo(av, u);
        p.add(av, BorderLayout.WEST);

        JPanel texto = new JPanel();
        texto.setLayout(new BoxLayout(texto, BoxLayout.Y_AXIS));
        texto.setBackground(Color.WHITE);
        // Nombre completo arriba (prominente), @username abajo en bold
        JLabel lblN = new JLabel(u.getNombreCompleto());
        lblN.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblN.setForeground(new Color(38, 38, 38));
        JLabel lblU = new JLabel("@" + u.getUsername());
        lblU.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblU.setForeground(new Color(142, 142, 142));
        texto.add(lblN);
        texto.add(lblU);
        p.add(texto, BorderLayout.CENTER);

        if (mostrarSeguir && !u.getUsername().equalsIgnoreCase(usuario.getUsername())) {
            JButton btn = btnSeguirPequeno();
            btn.addActionListener(e -> {
                btn.setEnabled(false);
                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() throws Exception {
                        followManager.seguir(usuario.getUsername(), u.getUsername());
                        return null;
                    }
                    @Override protected void done() {
                        try {
                            get();
                            btn.setText(u.getTipoCuenta() == TipoCuenta.PRIVADA
                                ? "Solicitado" : "Siguiendo");
                            btn.setBackground(new Color(239, 239, 239));
                            btn.setForeground(new Color(38, 38, 38));
                        } catch (Exception ex) { btn.setEnabled(true); }
                    }
                }.execute();
            });
            p.add(btn, BorderLayout.EAST);
        }

        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                // Si el usuario es el mismo que está logueado → perfil propio
                if (u.getUsername().equalsIgnoreCase(usuario.getUsername())) {
                    mainFrame.navegarPrincipal("PROFILE");
                } else {
                    mainFrame.verPerfilUsuario(u.getUsername());
                }
            }
            @Override public void mouseEntered(MouseEvent e) {
                p.setBackground(new Color(250, 250, 250));
                texto.setBackground(new Color(250, 250, 250));
            }
            @Override public void mouseExited(MouseEvent e) {
                p.setBackground(Color.WHITE);
                texto.setBackground(Color.WHITE);
            }
        });
        return p;
    }

    private JPanel thumbPost(Post p) {
        // Ancho fijo = 128px (1/3 de 390 − gaps). Altura proporcional a la imagen real.
        final int W = 128;
        JPanel c = new JPanel(new BorderLayout());
        c.setBackground(new Color(239, 239, 239));
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (p.getRutaImagen() != null && !p.getRutaImagen().isEmpty()) {
            try {
                BufferedImage bi = ImageIO.read(new File(p.getRutaImagen()));
                if (bi != null) {
                    // Calcular altura proporcional: H = W * (srcH / srcW)
                    int srcW = bi.getWidth(), srcH = bi.getHeight();
                    int H = (srcW > 0) ? (int)(W * (double) srcH / srcW) : W;
                    H = Math.max(H, 40); // mínimo visible
                    Image scaled = bi.getScaledInstance(W, H, Image.SCALE_SMOOTH);
                    JLabel img = new JLabel(new ImageIcon(scaled));
                    img.setPreferredSize(new Dimension(W, H));
                    c.add(img, BorderLayout.CENTER);
                    c.setPreferredSize(new Dimension(W, H));
                }
            } catch (Exception ignored) {
                c.setPreferredSize(new Dimension(W, W));
            }
        } else {
            // Sin imagen: placeholder cuadrado con ícono
            JLabel ph = new JLabel("📷", SwingConstants.CENTER);
            ph.setFont(new Font("Arial", Font.PLAIN, 28));
            ph.setPreferredSize(new Dimension(W, W));
            c.add(ph, BorderLayout.CENTER);
            c.setPreferredSize(new Dimension(W, W));
        }

        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { mainFrame.verPost(p); }
        });
        return c;
    }

    private JLabel tituloSeccion(String txt) {
        JLabel lbl = new JLabel(txt);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(new Color(38, 38, 38));
        lbl.setBorder(BorderFactory.createEmptyBorder(12, 14, 6, 14));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setMaximumSize(new Dimension(390, 40));
        return lbl;
    }

    private JLabel labelVacio(String txt) {
        JLabel lbl = new JLabel(txt, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(new Color(142, 142, 142));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0));
        lbl.setMaximumSize(new Dimension(390, 80));
        return lbl;
    }

    private JButton btnSeguirPequeno() {
        JButton btn = new JButton("Seguir");
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(0, 149, 246));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(76, 30));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void cargarFotoCirculo(ComponenteCircular cc, User u) {
        new SwingWorker<java.awt.image.BufferedImage, Void>() {
            @Override protected java.awt.image.BufferedImage doInBackground() {
                try {
                    String ruta = u.getRutaFotoPerfil();
                    if (ruta == null) return null;
                    ruta = ruta.replaceAll("[\\u0000-\\u001F]+", "").trim();
                    if (ruta.isEmpty() || ruta.equals("default.jpg")) return null;
                    File f = new File(ruta);
                    if (!f.exists() || f.length() == 0) return null;
                    return ImageIO.read(f);
                } catch (Exception ignored) { return null; }
            }
            @Override protected void done() {
                try {
                    java.awt.image.BufferedImage bi = get();
                    if (bi != null) { cc.setImagen(bi); cc.repaint(); }
                } catch (Exception ignored) {}
            }
        }.execute();
    }
}