package instagram.GUI;

import instagram.Utilities.FollowManager;
import instagram.Utilities.User;
import instagram.Utilities.UserManager;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

/**
 * Panel que muestra la lista de seguidores o seguidos del usuario.
 * Se abre desde PanelProfile al tocar las estadísticas de seguidores/seguidos.
 */
public class PanelSeguidores extends JPanel {

    public enum Modo { SEGUIDORES, SEGUIDOS }

    private final User          usuario;
    private final UserManager   userManager;
    private final FollowManager followManager;
    private final MainFrame     mainFrame;

    private Modo modoActual;

    // Tabs
    private JLabel tabSeguidores;
    private JLabel tabSeguidos;
    private JSeparator lineaActiva;

    // Búsqueda
    private JTextField txtBuscar;
    private boolean settingPlaceholder = false;

    // Lista
    private JPanel listPanel;

    // Datos cargados
    private ArrayList<User> listaSeguidores = new ArrayList<>();
    private ArrayList<User> listaSeguidos   = new ArrayList<>();

    public PanelSeguidores(User usuario, UserManager userManager,
                           FollowManager followManager, MainFrame mainFrame,
                           Modo modoInicial) {
        this.usuario       = usuario;
        this.userManager   = userManager;
        this.followManager = followManager;
        this.mainFrame     = mainFrame;
        this.modoActual    = modoInicial;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        construirUI();
        cargarDatos();
    }

    private void construirUI() {
        // ── Tabs ──────────────────────────────────────────────
        JPanel tabs = new JPanel(null);
        tabs.setPreferredSize(new Dimension(390, 44));
        tabs.setBackground(Color.WHITE);
        tabs.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, InstagramColors.BORDE_GRIS));

        tabSeguidores = crearTab("Seguidores", 0, 195);
        tabSeguidos   = crearTab("Seguidos",   195, 195);

        tabSeguidores.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { cambiarModo(Modo.SEGUIDORES); }
        });
        tabSeguidos.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { cambiarModo(Modo.SEGUIDOS); }
        });

        tabs.add(tabSeguidores);
        tabs.add(tabSeguidos);

        // Línea indicadora activa
        lineaActiva = new JSeparator();
        lineaActiva.setForeground(InstagramColors.TEXTO_NEGRO);
        lineaActiva.setBackground(InstagramColors.TEXTO_NEGRO);
        lineaActiva.setOpaque(true);
        lineaActiva.setBounds(modoActual == Modo.SEGUIDORES ? 0 : 195, 41, 195, 2);
        tabs.add(lineaActiva);

        add(tabs, BorderLayout.NORTH);

        // ── Búsqueda ──────────────────────────────────────────
        JPanel barraBusqueda = new JPanel(new BorderLayout(0, 0));
        barraBusqueda.setBackground(Color.WHITE);
        barraBusqueda.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JPanel inputWrap = new JPanel(new BorderLayout(6, 0));
        inputWrap.setBackground(new Color(239, 239, 239));
        inputWrap.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(InstagramColors.BORDE_GRIS, 1),
            BorderFactory.createEmptyBorder(0, 10, 0, 10)));

        JLabel lupa = new JLabel("🔍");
        lupa.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lupa.setPreferredSize(new Dimension(20, 34));
        inputWrap.add(lupa, BorderLayout.WEST);

        txtBuscar = new JTextField();
        txtBuscar.setBorder(null);
        txtBuscar.setBackground(new Color(239, 239, 239));
        txtBuscar.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtBuscar.setForeground(new Color(142, 142, 142));
        txtBuscar.setText("Buscar");
        txtBuscar.setPreferredSize(new Dimension(0, 34));

        txtBuscar.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (txtBuscar.getText().equals("Buscar")) {
                    txtBuscar.setText("");
                    txtBuscar.setForeground(new Color(38, 38, 38));
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (txtBuscar.getText().isEmpty()) {
                    settingPlaceholder = true;
                    txtBuscar.setText("Buscar");
                    txtBuscar.setForeground(new Color(142, 142, 142));
                    settingPlaceholder = false;
                    filtrar("");
                }
            }
        });

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { filtrar(termino()); }
            public void removeUpdate(DocumentEvent e)  { filtrar(termino()); }
            public void changedUpdate(DocumentEvent e) { filtrar(termino()); }
        });

        inputWrap.add(txtBuscar, BorderLayout.CENTER);
        barraBusqueda.add(inputWrap, BorderLayout.CENTER);

        // ── Panel central: búsqueda + lista ──────────────────
        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Color.WHITE);
        centro.add(barraBusqueda, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(listPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        centro.add(scroll, BorderLayout.CENTER);

        add(centro, BorderLayout.CENTER);

        actualizarEstiloTabs();
    }

    private JLabel crearTab(String texto, int x, int w) {
        JLabel lbl = new JLabel(texto, SwingConstants.CENTER);
        lbl.setBounds(x, 0, w, 42);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(InstagramColors.TEXTO_NEGRO);
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return lbl;
    }

    // ── Datos ────────────────────────────────────────────────────

    private void cargarDatos() {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                // Seguidores
                ArrayList<String> usersFollowers = followManager.obtenerFollowers(usuario.getUsername());
                listaSeguidores = new ArrayList<>();
                for (String uname : usersFollowers) {
                    User u = userManager.buscarUsuario(uname);
                    if (u != null) listaSeguidores.add(u);
                }
                // Seguidos
                ArrayList<String> usersFollowing = followManager.obtenerFollowing(usuario.getUsername());
                listaSeguidos = new ArrayList<>();
                for (String uname : usersFollowing) {
                    User u = userManager.buscarUsuario(uname);
                    if (u != null) listaSeguidos.add(u);
                }
                return null;
            }
            @Override protected void done() {
                try { get(); renderLista(listaActual(), ""); }
                catch (Exception ignored) {}
            }
        }.execute();
    }

    // ── Navegación de tabs ───────────────────────────────────────

    private void cambiarModo(Modo modo) {
        modoActual = modo;
        actualizarEstiloTabs();
        // Mover línea indicadora
        lineaActiva.setBounds(modo == Modo.SEGUIDORES ? 0 : 195, 41, 195, 2);
        lineaActiva.getParent().repaint();
        // Limpiar búsqueda
        settingPlaceholder = true;
        txtBuscar.setText("Buscar");
        txtBuscar.setForeground(new Color(142, 142, 142));
        settingPlaceholder = false;
        renderLista(listaActual(), "");
    }

    private void actualizarEstiloTabs() {
        Color activo   = InstagramColors.TEXTO_NEGRO;
        Color inactivo = InstagramColors.TEXTO_GRIS;
        tabSeguidores.setForeground(modoActual == Modo.SEGUIDORES ? activo : inactivo);
        tabSeguidos  .setForeground(modoActual == Modo.SEGUIDOS   ? activo : inactivo);
    }

    private ArrayList<User> listaActual() {
        return modoActual == Modo.SEGUIDORES ? listaSeguidores : listaSeguidos;
    }

    // ── Filtro ───────────────────────────────────────────────────

    private String termino() {
        if (settingPlaceholder) return "";
        String t = txtBuscar.getText().trim();
        return t.equals("Buscar") ? "" : t.toLowerCase();
    }

    private void filtrar(String termino) {
        if (settingPlaceholder) return;
        renderLista(listaActual(), termino);
    }

    // ── Render ───────────────────────────────────────────────────

    private void renderLista(ArrayList<User> lista, String filtro) {
        listPanel.removeAll();

        ArrayList<User> filtrada = new ArrayList<>();
        for (User u : lista) {
            if (filtro.isEmpty()
                || u.getUsername().toLowerCase().contains(filtro)
                || u.getNombreCompleto().toLowerCase().contains(filtro)) {
                filtrada.add(u);
            }
        }

        if (filtrada.isEmpty()) {
            JLabel vacio = new JLabel(
                lista.isEmpty() ? (modoActual == Modo.SEGUIDORES
                    ? "Aún no tienes seguidores" : "Aún no sigues a nadie")
                    : "No se encontraron resultados",
                SwingConstants.CENTER);
            vacio.setFont(new Font("SansSerif", Font.PLAIN, 13));
            vacio.setForeground(InstagramColors.TEXTO_GRIS);
            vacio.setAlignmentX(Component.CENTER_ALIGNMENT);
            vacio.setBorder(BorderFactory.createEmptyBorder(60, 0, 0, 0));
            listPanel.add(vacio);
        } else {
            for (User u : filtrada) listPanel.add(rowUsuario(u));
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    // ── Row de usuario ───────────────────────────────────────────

    private JPanel rowUsuario(User u) {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(239, 239, 239)),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        p.setMaximumSize(new Dimension(390, 70));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Avatar
        ComponenteCircular av = new ComponenteCircular(46);
        av.setPreferredSize(new Dimension(46, 46));
        cargarFoto(av, u);
        p.add(av, BorderLayout.WEST);

        // Texto: nombre completo arriba, @username abajo en bold gris
        JPanel texto = new JPanel();
        texto.setLayout(new BoxLayout(texto, BoxLayout.Y_AXIS));
        texto.setBackground(Color.WHITE);

        JLabel lblNombre = new JLabel(u.getNombreCompleto());
        lblNombre.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblNombre.setForeground(InstagramColors.TEXTO_NEGRO);

        JLabel lblUser = new JLabel("@" + u.getUsername());
        lblUser.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblUser.setForeground(InstagramColors.TEXTO_GRIS);

        texto.add(lblNombre);
        texto.add(lblUser);
        p.add(texto, BorderLayout.CENTER);

        // Botón acción derecha
        boolean esMiUsuario = u.getUsername().equalsIgnoreCase(usuario.getUsername());
        if (!esMiUsuario) {
            JButton btnAccion = crearBotonAccion(u);
            p.add(btnAccion, BorderLayout.EAST);
        }

        // Click → ver perfil
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (u.getUsername().equalsIgnoreCase(usuario.getUsername()))
                    mainFrame.navegarPrincipal("PROFILE");
                else
                    mainFrame.verPerfilUsuario(u.getUsername());
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

    private JButton crearBotonAccion(User u) {
        // Determinar estado actual
        boolean yoSiguo = false;
        try { yoSiguo = followManager.estaSiguiendo(usuario.getUsername(), u.getUsername()); }
        catch (Exception ignored) {}

        JButton btn;
        if (yoSiguo) {
            btn = boton("Siguiendo", new Color(239, 239, 239), InstagramColors.TEXTO_NEGRO);
        } else {
            btn = boton("Seguir", InstagramColors.INSTAGRAM_AZUL, Color.WHITE);
        }

        final boolean[] siguiendo = {yoSiguo};
        btn.addActionListener(e -> {
            btn.setEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    if (siguiendo[0]) {
                        followManager.dejarDeSeguir(usuario.getUsername(), u.getUsername());
                    } else {
                        followManager.seguir(usuario.getUsername(), u.getUsername());
                    }
                    return null;
                }
                @Override protected void done() {
                    try {
                        get();
                        siguiendo[0] = !siguiendo[0];
                        if (siguiendo[0]) {
                            btn.setText("Siguiendo");
                            btn.setBackground(new Color(239, 239, 239));
                            btn.setForeground(InstagramColors.TEXTO_NEGRO);
                        } else {
                            btn.setText("Seguir");
                            btn.setBackground(InstagramColors.INSTAGRAM_AZUL);
                            btn.setForeground(Color.WHITE);
                        }
                        btn.setEnabled(true);
                    } catch (Exception ex) { btn.setEnabled(true); }
                }
            }.execute();
        });
        return btn;
    }

    private JButton boton(String texto, Color bg, Color fg) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(90, 30));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Foto async ───────────────────────────────────────────────

    private void cargarFoto(ComponenteCircular cc, User u) {
        new SwingWorker<BufferedImage, Void>() {
            @Override protected BufferedImage doInBackground() {
                try {
                    String ruta = u.getRutaFotoPerfil();
                    if (ruta == null) return null;
                    ruta = ruta.replaceAll("[\\u0000-\\u001F]+", "").trim();
                    if (ruta.isEmpty() || ruta.equals("default.jpg")) return null;
                    File f = new File(ruta);
                    if (!f.exists() || f.length() == 0) return null;
                    return ImageIO.read(f);
                } catch (Exception e) { return null; }
            }
            @Override protected void done() {
                try {
                    BufferedImage bi = get();
                    if (bi != null) { cc.setImagen(bi); cc.repaint(); }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public Modo getModoActual() { return modoActual; }
    public String getTituloModo() {
        return modoActual == Modo.SEGUIDORES ? "Seguidores" : "Seguidos";
    }
}