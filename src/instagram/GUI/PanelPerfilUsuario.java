package instagram.GUI;

import instagram.Utilities.FollowManager;
import instagram.Utilities.Post;
import instagram.Utilities.PostManager;
import instagram.Enums.TipoCuenta;
import instagram.Utilities.User;
import instagram.Utilities.UserManager;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

/**
 * Perfil de OTRO usuario.
 * Misma estructura visual que PanelProfile:
 *   foto + stats | nombre + bio | botones | separador | grid
 *
 * Botones:
 *   - NO_SIGUE / SOLICITADO: un botón ancho "Seguir" / "Solicitado"
 *   - SIGUIENDO: dos botones "Siguiendo" + "Mensaje"
 */
public class PanelPerfilUsuario extends JPanel {

    private String        usernameObjetivo;
    private User          usuarioActual;
    private UserManager   userManager;
    private FollowManager followManager;
    private PostManager   postManager;
    private MainFrame     mainFrame;

    private User perfilUsuario;

    // Componentes dinámicos
    private JLabel  lblNombre;
    private JLabel  lblBio;
    private JLabel  lblPosts;
    private JLabel  lblFollowers;
    private JLabel  lblFollowing;
    private JButton btnAccion;
    private JButton btnMensaje;
    private JLabel  lblFeedback;
    private JPanel  panelGrid;
    private JPanel  btnRow;
    private JPanel  panelConfirmUnfollow;

    private enum Estado { NO_SIGUE, SIGUIENDO, SOLICITADO }
    private Estado estado = Estado.NO_SIGUE;

    public PanelPerfilUsuario(String username, User usuarioActual,
                              UserManager userManager, FollowManager followManager,
                              PostManager postManager, MainFrame mainFrame) {
        this.usernameObjetivo = username;
        this.usuarioActual    = usuarioActual;
        this.userManager      = userManager;
        this.followManager    = followManager;
        this.postManager      = postManager;
        this.mainFrame        = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        cargarYConstruir();
    }

    private void cargarYConstruir() {
        // Si el usuario está intentando ver su propio perfil,
        // redirigir directamente al panel de perfil propio.
        if (usernameObjetivo.equalsIgnoreCase(usuarioActual.getUsername())) {
            SwingUtilities.invokeLater(() -> mainFrame.navegarPrincipal("PROFILE"));
            return;
        }

        new SwingWorker<User, Void>() {
            @Override protected User doInBackground() throws Exception {
                return userManager.buscarUsuario(usernameObjetivo);
            }
            @Override protected void done() {
                try {
                    perfilUsuario = get();
                    if (perfilUsuario == null) mostrarError("Usuario no encontrado");
                    else { construirUI(); cargarEstadisticas(); cargarEstadoRelacion(); cargarGrid(); }
                } catch (Exception ex) { mostrarError("Error cargando perfil"); }
            }
        }.execute();
    }

    private void construirUI() {
        // Panel que siempre reporta 390px de ancho (igual que PanelProfile)
        JPanel todo = new JPanel() {
            @Override public Dimension getPreferredSize() {
                return new Dimension(390, super.getPreferredSize().height);
            }
        };
        todo.setLayout(new BoxLayout(todo, BoxLayout.Y_AXIS));
        todo.setBackground(Color.WHITE);

        // ── Foto + estadísticas ───────────────────────────────
        JPanel rowInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 8));
        rowInfo.setBackground(Color.WHITE);
        rowInfo.setMaximumSize(new Dimension(390, 104));
        rowInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        ComponenteCircular fotoCirculo = new ComponenteCircular(80);
        fotoCirculo.setPreferredSize(new Dimension(80, 80));
        cargarFoto(fotoCirculo, perfilUsuario);
        rowInfo.add(fotoCirculo);

        JPanel stats = new JPanel(new GridLayout(1, 3, 0, 0));
        stats.setBackground(Color.WHITE);
        stats.setPreferredSize(new Dimension(240, 60));
        lblPosts     = statCol("0", "publicaciones");
        lblFollowers = statCol("0", "seguidores");
        lblFollowing = statCol("0", "seguidos");
        stats.add(lblPosts); stats.add(lblFollowers); stats.add(lblFollowing);
        rowInfo.add(stats);
        todo.add(rowInfo);

        // ── Nombre completo + bio ─────────────────────────────
        JPanel rowBio = new JPanel(null);
        rowBio.setBackground(Color.WHITE);
        rowBio.setPreferredSize(new Dimension(390, 58));
        rowBio.setMaximumSize(new Dimension(390, 80));
        rowBio.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblNombre = new JLabel(perfilUsuario.getNombreCompleto());
        lblNombre.setBounds(16, 2, 358, 20);
        lblNombre.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblNombre.setHorizontalAlignment(SwingConstants.LEFT);
        rowBio.add(lblNombre);

        String bio = perfilUsuario.getBiografia();
        lblBio = new JLabel("<html><p style='width:355px'>"
            + (bio != null && !bio.isEmpty() ? bio : "") + "</p></html>");
        lblBio.setBounds(16, 24, 358, 32);
        lblBio.setFont(new Font("SansSerif", Font.PLAIN, 13));
        rowBio.add(lblBio);
        todo.add(rowBio);

        // ── Fila de botones (null layout para control preciso) ─
        // Estado NO_SIGUE / SOLICITADO: un botón ancho (x=14 w=362)
        // Estado SIGUIENDO: dos botones (x=14 w=176) + (x=198 w=178)
        btnRow = new JPanel(null);
        btnRow.setBackground(Color.WHITE);
        btnRow.setPreferredSize(new Dimension(390, 46));
        btnRow.setMaximumSize(new Dimension(390, 46));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnAccion = new JButton("Seguir");
        btnAccion.setBounds(14, 6, 362, 34);   // empieza full-width
        estilizarBtnPrimario(btnAccion);
        btnAccion.addActionListener(e -> accionSeguir());
        btnRow.add(btnAccion);

        btnMensaje = new JButton("Mensaje");
        btnMensaje.setBounds(198, 6, 178, 34);
        estilizarBtnGris(btnMensaje);
        btnMensaje.setVisible(false);
        // Abre inbox y luego el chat directamente con este usuario
        btnMensaje.addActionListener(e -> {
            mainFrame.navegarA("INBOX");
            mainFrame.abrirChatCon(usernameObjetivo);
        });
        btnRow.add(btnMensaje);
        todo.add(btnRow);

        // ── Panel inline de confirmación unfollow ─────────────
        panelConfirmUnfollow = new JPanel(null);
        panelConfirmUnfollow.setPreferredSize(new Dimension(390, 50));
        panelConfirmUnfollow.setMaximumSize(new Dimension(390, 50));
        panelConfirmUnfollow.setBackground(new Color(255, 240, 240));
        panelConfirmUnfollow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelConfirmUnfollow.setVisible(false);

        JLabel lblPregunta = new JLabel("¿Dejar de seguir a @" + perfilUsuario.getUsername() + "?");
        lblPregunta.setBounds(14, 15, 230, 18);
        lblPregunta.setFont(new Font("Arial", Font.PLAIN, 12));
        panelConfirmUnfollow.add(lblPregunta);

        JButton btnSi = new JButton("Sí");
        btnSi.setBounds(252, 11, 56, 28);
        btnSi.setFont(new Font("Arial", Font.BOLD, 12));
        btnSi.setForeground(Color.WHITE);
        btnSi.setBackground(InstagramColors.ROJO_ERROR);
        btnSi.setBorderPainted(false); btnSi.setFocusPainted(false);
        btnSi.addActionListener(e -> confirmarUnfollow());
        panelConfirmUnfollow.add(btnSi);

        JButton btnNo = new JButton("Cancelar");
        btnNo.setBounds(316, 11, 68, 28);
        btnNo.setFont(new Font("Arial", Font.PLAIN, 11));
        btnNo.setBackground(new Color(239, 239, 239));
        btnNo.setBorderPainted(false); btnNo.setFocusPainted(false);
        btnNo.addActionListener(e -> panelConfirmUnfollow.setVisible(false));
        panelConfirmUnfollow.add(btnNo);
        todo.add(panelConfirmUnfollow);

        // ── Feedback ──────────────────────────────────────────
        lblFeedback = new JLabel("", SwingConstants.CENTER);
        lblFeedback.setFont(new Font("Arial", Font.PLAIN, 12));
        lblFeedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblFeedback.setMaximumSize(new Dimension(390, 22));
        lblFeedback.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        todo.add(lblFeedback);

        // ── Separador ─────────────────────────────────────────
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(390, 1));
        sep.setForeground(InstagramColors.BORDE_GRIS);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        todo.add(sep);

        // ── Grid de publicaciones ─────────────────────────────
        panelGrid = new JPanel(new GridLayout(0, 3, 2, 2));
        panelGrid.setBackground(new Color(239, 239, 239));
        panelGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel wrapGrid = new JPanel(new BorderLayout());
        wrapGrid.setBackground(new Color(239, 239, 239));
        wrapGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapGrid.add(panelGrid, BorderLayout.NORTH);
        todo.add(wrapGrid);

        JScrollPane scroll = new JScrollPane(todo,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        revalidate(); repaint();
    }

    // ── Actualización de botones ──────────────────────────────────

    /**
     * Ajusta posición y visibilidad de los botones según el estado.
     * NO_SIGUE / SOLICITADO → un botón ancho (362px)
     * SIGUIENDO             → dos botones (176px + 178px)
     */
    private void actualizarBotones(Estado nuevoEstado) {
        this.estado = nuevoEstado;
        switch (nuevoEstado) {
            case NO_SIGUE:
                btnAccion.setText("Seguir");
                estilizarBtnPrimario(btnAccion);
                btnAccion.setBounds(14, 6, 362, 34);
                btnMensaje.setVisible(false);
                break;
            case SIGUIENDO:
                btnAccion.setText("Siguiendo");
                estilizarBtnGris(btnAccion);
                btnAccion.setBounds(14, 6, 176, 34);
                btnMensaje.setVisible(true);
                break;
            case SOLICITADO:
                btnAccion.setText("Solicitado");
                estilizarBtnGris(btnAccion);
                btnAccion.setBounds(14, 6, 362, 34);
                btnMensaje.setVisible(false);
                break;
        }
        if (btnRow != null) { btnRow.revalidate(); btnRow.repaint(); }
    }

    // ── Carga de datos ────────────────────────────────────────────

    private void cargarEstadisticas() {
        new SwingWorker<int[], Void>() {
            @Override protected int[] doInBackground() throws Exception {
                return new int[]{
                    postManager.obtenerPostsDeUsuario(perfilUsuario.getUsername()).size(),
                    followManager.contarFollowers(perfilUsuario.getUsername()),
                    followManager.contarFollowing(perfilUsuario.getUsername())
                };
            }
            @Override protected void done() {
                try {
                    int[] s = get();
                    actualizarStatCol(lblPosts,     s[0], "publicaciones");
                    actualizarStatCol(lblFollowers, s[1], "seguidores");
                    actualizarStatCol(lblFollowing, s[2], "seguidos");
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void cargarEstadoRelacion() {
        new SwingWorker<Estado, Void>() {
            @Override protected Estado doInBackground() throws Exception {
                boolean sigo      = followManager.estaSiguiendo(
                    usuarioActual.getUsername(), perfilUsuario.getUsername());
                boolean pendiente = followManager.tieneSolicitudPendiente(
                    usuarioActual.getUsername(), perfilUsuario.getUsername());
                if (sigo)      return Estado.SIGUIENDO;
                if (pendiente) return Estado.SOLICITADO;
                return Estado.NO_SIGUE;
            }
            @Override protected void done() {
                try { actualizarBotones(get()); } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void cargarGrid() {
        new SwingWorker<ArrayList<Post>, Void>() {
            @Override protected ArrayList<Post> doInBackground() throws Exception {
                if (perfilUsuario.getTipoCuenta() == TipoCuenta.PRIVADA) {
                    boolean sigo = followManager.estaSiguiendo(
                        usuarioActual.getUsername(), perfilUsuario.getUsername());
                    if (!sigo) return new ArrayList<>();
                }
                return postManager.obtenerPostsDeUsuario(perfilUsuario.getUsername());
            }
            @Override protected void done() {
                try { renderGrid(get()); } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void renderGrid(ArrayList<Post> posts) {
        if (panelGrid == null) return;
        panelGrid.removeAll();
        if (posts.isEmpty()) {
            boolean esPrivada = perfilUsuario.getTipoCuenta() == TipoCuenta.PRIVADA;
            String html = (esPrivada && estado != Estado.SIGUIENDO)
                ? "<html><center>🔒<br>Esta cuenta es privada</center></html>"
                : "<html><center>📷<br>Sin publicaciones aún</center></html>";
            JLabel lbl = new JLabel(html, SwingConstants.CENTER);
            lbl.setFont(new Font("Arial", Font.PLAIN, 13));
            lbl.setForeground(InstagramColors.TEXTO_GRIS);
            lbl.setPreferredSize(new Dimension(390, 140));
            panelGrid.setLayout(new BorderLayout());
            panelGrid.add(lbl, BorderLayout.CENTER);
        } else {
            panelGrid.setLayout(new GridLayout(0, 3, 2, 2));
            for (Post p : posts) {
                JPanel cell = new JPanel(new BorderLayout());
                cell.setPreferredSize(new Dimension(128, 128));
                cell.setBackground(new Color(239, 239, 239));
                cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                if (p.getRutaImagen() != null && !p.getRutaImagen().isEmpty()) {
                    try {
                        BufferedImage bi = ImageIO.read(new File(p.getRutaImagen()));
                        if (bi != null) cell.add(new JLabel(new ImageIcon(
                            bi.getScaledInstance(128, 128, Image.SCALE_SMOOTH))));
                    } catch (Exception ignored) {}
                }
                cell.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { mainFrame.verPost(p); }
                });
                panelGrid.add(cell);
            }
            int mod = posts.size() % 3;
            if (mod != 0) for (int i = 0; i < 3-mod; i++) {
                JPanel v = new JPanel(); v.setBackground(new Color(239,239,239)); panelGrid.add(v);
            }
        }
        panelGrid.revalidate(); panelGrid.repaint();
    }

    // ── Acciones ──────────────────────────────────────────────────

    private void accionSeguir() {
        switch (estado) {
            case NO_SIGUE:   ejecutarFollow();          break;
            case SIGUIENDO:  pedirConfirmUnfollow();    break;
            case SOLICITADO: cancelarSolicitud();       break;
        }
    }

    private void ejecutarFollow() {
        btnAccion.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                followManager.seguir(usuarioActual.getUsername(), perfilUsuario.getUsername());
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    if (perfilUsuario.getTipoCuenta() == TipoCuenta.PRIVADA) {
                        actualizarBotones(Estado.SOLICITADO);
                        mostrarFeedback("Solicitud enviada", InstagramColors.TEXTO_GRIS);
                    } else {
                        actualizarBotones(Estado.SIGUIENDO);
                        mostrarFeedback("Ahora sigues a @" + perfilUsuario.getUsername(),
                            InstagramColors.VERDE_EXITO);
                    }
                    cargarEstadisticas();
                } catch (Exception ex) {
                    mostrarFeedback("Error al seguir", InstagramColors.ROJO_ERROR);
                }
                btnAccion.setEnabled(true);
            }
        }.execute();
    }

    private void pedirConfirmUnfollow() {
        if (panelConfirmUnfollow != null) panelConfirmUnfollow.setVisible(true);
    }

    private void confirmarUnfollow() {
        panelConfirmUnfollow.setVisible(false);
        btnAccion.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                followManager.dejarDeSeguir(usuarioActual.getUsername(), perfilUsuario.getUsername());
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    actualizarBotones(Estado.NO_SIGUE);
                    cargarEstadisticas();
                    if (perfilUsuario.getTipoCuenta() == TipoCuenta.PRIVADA)
                        renderGrid(new ArrayList<>());
                } catch (Exception ex) {
                    mostrarFeedback("Error", InstagramColors.ROJO_ERROR);
                }
                btnAccion.setEnabled(true);
            }
        }.execute();
    }

    private void cancelarSolicitud() {
        btnAccion.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                followManager.cancelarSolicitud(usuarioActual.getUsername(), perfilUsuario.getUsername());
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    actualizarBotones(Estado.NO_SIGUE);
                    mostrarFeedback("Solicitud cancelada", InstagramColors.TEXTO_GRIS);
                } catch (Exception ex) {
                    mostrarFeedback("Error", InstagramColors.ROJO_ERROR);
                }
                btnAccion.setEnabled(true);
            }
        }.execute();
    }

    // ── Helpers ───────────────────────────────────────────────────

    public String getUsername() { return usernameObjetivo; }

    private void mostrarError(String msg) {
        JLabel lbl = new JLabel(msg, SwingConstants.CENTER);
        lbl.setFont(new Font("Arial", Font.PLAIN, 14));
        lbl.setForeground(InstagramColors.TEXTO_GRIS);
        add(lbl, BorderLayout.CENTER); revalidate();
    }

    private void mostrarFeedback(String txt, Color color) {
        if (lblFeedback == null) return;
        lblFeedback.setText(txt); lblFeedback.setForeground(color);
        Timer t = new Timer(3000, e -> lblFeedback.setText(""));
        t.setRepeats(false); t.start();
    }

    private void cargarFoto(ComponenteCircular cc, User u) {
        try {
            String ruta = u.getRutaFotoPerfil();
            if (ruta != null && !ruta.isEmpty() && !ruta.equals("default.jpg")) {
                File f = new File(ruta);
                if (f.exists()) cc.setImagen(ImageIO.read(f));
            }
        } catch (Exception ignored) {}
    }

    private JLabel statCol(String num, String label) {
        JLabel lbl = new JLabel("<html><center><b>" + num + "</b><br>"
            + "<small style='color:gray'>" + label + "</small></center></html>",
            SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return lbl;
    }

    private void actualizarStatCol(JLabel lbl, int num, String label) {
        if (lbl == null) return;
        lbl.setText("<html><center><b>" + num + "</b><br>"
            + "<small style='color:gray'>" + label + "</small></center></html>");
    }

    private void estilizarBtnPrimario(JButton btn) {
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(InstagramColors.INSTAGRAM_AZUL);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void estilizarBtnGris(JButton btn) {
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setForeground(InstagramColors.TEXTO_NEGRO);
        btn.setBackground(new Color(239, 239, 239));
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}