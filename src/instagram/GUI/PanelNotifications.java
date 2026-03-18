package instagram.GUI;

import instagram.Utilities.ChatClient;
import instagram.Utilities.FollowManager;
import instagram.Utilities.Notification;
import instagram.Utilities.NotificationManager;
import instagram.Utilities.Post;
import instagram.Utilities.PostManager;
import instagram.Enums.TipoNotificacion;
import instagram.Utilities.User;
import instagram.Utilities.UserManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class PanelNotifications extends JPanel implements ChatClient.NotificationListener {

    private User               usuario;
    private NotificationManager notificationManager;
    private FollowManager      followManager;
    private UserManager        userManager;
    private ChatClient         chatClient;
    private MainFrame          mainFrame;

    private JPanel   panelLista;
    private JLabel   lblContador;
    private JLabel   lblFeedback;
    private TipoNotificacion filtro = null;

    public PanelNotifications(User usuario, NotificationManager notificationManager,
                              FollowManager followManager, ChatClient chatClient,
                              MainFrame mainFrame) {
        // userManager injected lazily from mainFrame
        this.userManager = mainFrame != null ? mainFrame.userManager : null;
        this.usuario             = usuario;
        this.notificationManager = notificationManager;
        this.followManager       = followManager;
        this.chatClient          = chatClient;
        this.mainFrame           = mainFrame;

        if (chatClient != null) chatClient.setNotificationListener(this);

        setLayout(new BorderLayout());
        setBackground(InstagramColors.FONDO_GRIS);
        construirUI();
    }

    private void construirUI() {
        // ── Sub-header (marcar leídas + filtros) ─────────────
        JPanel subHeader = new JPanel(null);
        subHeader.setPreferredSize(new Dimension(390, 48));
        subHeader.setBackground(Color.WHITE);
        subHeader.setBorder(BorderFactory.createMatteBorder(0,0,1,0,
            InstagramColors.BORDE_GRIS));

        lblContador = new JLabel("", SwingConstants.LEFT);
        lblContador.setBounds(14, 14, 180, 20);
        lblContador.setFont(new Font("Arial", Font.PLAIN, 12));
        lblContador.setForeground(InstagramColors.TEXTO_GRIS);
        subHeader.add(lblContador);

        JButton btnLeer = new JButton("Marcar leídas");
        btnLeer.setBounds(196, 10, 116, 28);
        btnLeer.setFont(new Font("Arial", Font.PLAIN, 11));
        btnLeer.setForeground(InstagramColors.INSTAGRAM_AZUL);
        btnLeer.setBackground(Color.WHITE);
        btnLeer.setBorderPainted(false);
        btnLeer.setFocusPainted(false);
        btnLeer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLeer.addActionListener(e -> marcarTodasLeidas());
        subHeader.add(btnLeer);

        JLabel btnFiltro = new JLabel("Filtrar ▾", SwingConstants.CENTER);
        btnFiltro.setBounds(300, 10, 76, 28);
        btnFiltro.setFont(new Font("Arial", Font.PLAIN, 12));
        btnFiltro.setForeground(InstagramColors.TEXTO_NEGRO);
        btnFiltro.setBackground(new Color(239,239,239));
        btnFiltro.setOpaque(true);
        btnFiltro.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnFiltro.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { mostrarMenuFiltros(btnFiltro); }
        });
        subHeader.add(btnFiltro);

        add(subHeader, BorderLayout.NORTH);

        // ── Lista ─────────────────────────────────────────────
        panelLista = new JPanel();
        panelLista.setLayout(new BoxLayout(panelLista, BoxLayout.Y_AXIS));
        panelLista.setBackground(InstagramColors.FONDO_GRIS);

        JScrollPane scroll = new JScrollPane(panelLista,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // ── Footer feedback ───────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout());
        footer.setPreferredSize(new Dimension(390, 32));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1,0,0,0,
            InstagramColors.BORDE_GRIS));
        lblFeedback = new JLabel("", SwingConstants.CENTER);
        lblFeedback.setFont(new Font("Arial", Font.PLAIN, 11));
        footer.add(lblFeedback, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    // ── CARGA ────────────────────────────────────────────────────

    public void refrescar() {
        if (chatClient != null && chatClient.isConnected()) {
            try { chatClient.solicitarNotificaciones(); return; }
            catch (Exception ignored) {}
        }
        cargarLocal();
    }

    private void cargarLocal() {
        new SwingWorker<ArrayList<Notification>, Void>() {
            @Override protected ArrayList<Notification> doInBackground() throws Exception {
                if (filtro != null)
                    return notificationManager.obtenerPorTipo(usuario.getUsername(), filtro);
                return notificationManager.obtenerNotificaciones(usuario.getUsername());
            }
            @Override protected void done() {
                try { renderLista(get()); actualizarContador(); }
                catch (Exception ignored) {}
            }
        }.execute();
    }

    private void renderLista(ArrayList<Notification> lista) {
        // Asegurar orden más reciente primero
        java.util.Collections.sort(lista);
        panelLista.removeAll();

        if (lista.isEmpty()) {
            JLabel vacio = new JLabel(
                filtro != null ? "No hay notificaciones de este tipo"
                               : "No tienes notificaciones",
                SwingConstants.CENTER);
            vacio.setFont(new Font("Arial", Font.PLAIN, 14));
            vacio.setForeground(InstagramColors.TEXTO_GRIS);
            vacio.setAlignmentX(Component.CENTER_ALIGNMENT);
            vacio.setBorder(BorderFactory.createEmptyBorder(70, 0, 0, 0));
            panelLista.add(vacio);
        } else {
            String grupoActual = null;
            for (Notification n : lista) {
                String g = grupoFecha(n);
                if (!g.equals(grupoActual)) {
                    panelLista.add(separadorGrupo(g));
                    grupoActual = g;
                }
                panelLista.add(itemNotif(n));
            }
        }

        panelLista.revalidate();
        panelLista.repaint();
    }

    // ── ITEMS ────────────────────────────────────────────────────

    private JLabel separadorGrupo(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        lbl.setForeground(InstagramColors.TEXTO_GRIS);
        lbl.setBackground(InstagramColors.FONDO_GRIS);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createEmptyBorder(8, 16, 6, 16));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setMaximumSize(new Dimension(390, 34));
        return lbl;
    }

    private void cargarFotoNotif(ComponenteCircular cc, String username) {
        if (username == null || username.isEmpty() || userManager == null) return;
        new javax.swing.SwingWorker<java.awt.image.BufferedImage, Void>() {
            @Override protected java.awt.image.BufferedImage doInBackground() {
                try {
                    User u = userManager.buscarUsuario(username);
                    if (u == null) return null;
                    String ruta = u.getRutaFotoPerfil();
                    if (ruta == null) return null;
                    ruta = ruta.replaceAll("[\u0000-\u001F]+", "").trim();
                    if (ruta.isEmpty() || ruta.equals("default.jpg")) return null;
                    java.io.File f = new java.io.File(ruta);
                    if (!f.exists() || f.length() == 0) return null;
                    return javax.imageio.ImageIO.read(f);
                } catch (Exception e) { return null; }
            }
            @Override protected void done() {
                try {
                    java.awt.image.BufferedImage bi = get();
                    if (bi != null) { cc.setImagen(bi); cc.repaint(); }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private JPanel itemNotif(Notification n) {
        boolean leida    = n.isLeida();
        boolean esRequest = n.getTipo() == TipoNotificacion.FOLLOW_REQUEST;

        // Alturas:
        //   normal   : foto(46) + padding = 72
        //   request  : foto(46) + emoji(20) + botones(32) + padding = 110
        int itemH = esRequest ? 110 : 72;

        JPanel p = new JPanel(null);
        p.setPreferredSize(new Dimension(390, itemH));
        p.setMaximumSize(new Dimension(390, itemH));
        p.setBackground(leida ? Color.WHITE : InstagramColors.NOTIF_NO_LEIDA);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, InstagramColors.BORDE_GRIS_CLARO));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Foto de perfil (izquierda, centrada verticalmente) ────
        int fotoY = (itemH - 46) / 2;
        ComponenteCircular fotoPerfil = new ComponenteCircular(46);
        fotoPerfil.setBounds(10, fotoY, 46, 46);
        p.add(fotoPerfil);
        cargarFotoNotif(fotoPerfil, n.getEmisor());

        // ── Texto: @emisor + descripción (derecha del avatar) ─────
        // El emoji va integrado en la misma línea de texto, al inicio
        String emoji = n.getTipo().getEmoji();
        String msgHtml = "<html><div style='width:245px'>"
            + emoji + " <b>@" + n.getEmisor() + "</b> "
            + n.getTipo().getDescripcion()
            + "</div></html>";
        JLabel msg = new JLabel(msgHtml);
        msg.setBounds(64, 10, 295, esRequest ? 38 : 34);
        msg.setFont(new Font("Arial", Font.PLAIN, 13));
        msg.setForeground(InstagramColors.TEXTO_NEGRO);
        p.add(msg);

        // ── Tiempo ────────────────────────────────────────────────
        JLabel tiempo = new JLabel(n.getTiempoTranscurrido());
        tiempo.setBounds(64, esRequest ? 50 : 46, 200, 14);
        tiempo.setFont(new Font("Arial", Font.PLAIN, 11));
        tiempo.setForeground(InstagramColors.TEXTO_GRIS);
        p.add(tiempo);

        // ── Punto azul si no leída (derecha, centrado vertical) ───
        if (!leida) {
            JLabel punto = new JLabel("●");
            punto.setBounds(370, (itemH - 12) / 2, 12, 12);
            punto.setFont(new Font("Arial", Font.BOLD, 10));
            punto.setForeground(InstagramColors.INSTAGRAM_AZUL);
            p.add(punto);
        }

        // ── Botones Aceptar/Rechazar (solo follow request) ────────
        if (esRequest) {
            JButton btnAcept = botonNotif("Aceptar", InstagramColors.INSTAGRAM_AZUL, Color.WHITE);
            btnAcept.setBounds(64, 70, 100, 28);
            btnAcept.addActionListener(e -> aceptarSolicitud(n, p));
            p.add(btnAcept);

            JButton btnRech = botonNotif("Rechazar", new Color(239, 239, 239), InstagramColors.TEXTO_NEGRO);
            btnRech.setBounds(170, 70, 100, 28);
            btnRech.addActionListener(e -> rechazarSolicitud(n, p));
            p.add(btnRech);
        }

        // Click para marcar como leída y navegar
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                marcarLeida(n);
                p.setBackground(Color.WHITE);
                // Navegar según tipo
                switch (n.getTipo()) {
                    case FOLLOW:
                    case FOLLOW_REQUEST:
                        mainFrame.verPerfilUsuario(n.getEmisor()); break;
                    case LIKE:
                    case COMMENT:
                    case MENTION:
                        // contenidoExtra = postId → abrir el post
                        if (n.getContenidoExtra() != null && !n.getContenidoExtra().isEmpty()) {
                            abrirPost(n.getContenidoExtra(), n.getEmisor());
                        } else {
                            mainFrame.verPerfilUsuario(n.getEmisor());
                        }
                        break;
                    case MESSAGE:
                        mainFrame.navegarA("INBOX"); break;
                    default: break;
                }
            }
            @Override public void mouseEntered(MouseEvent e) {
                if (leida) p.setBackground(new Color(248,248,248));
            }
            @Override public void mouseExited(MouseEvent e) {
                p.setBackground(leida ? Color.WHITE : InstagramColors.NOTIF_NO_LEIDA);
            }
        });

        return p;
    }

    private JButton botonNotif(String texto, Color bg, Color fg) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setMargin(new java.awt.Insets(0, 6, 0, 6));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── ACCIONES ─────────────────────────────────────────────────

    private void aceptarSolicitud(Notification n, JPanel itemPanel) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                followManager.aceptarSolicitud(
                    usuario.getUsername(), n.getEmisor());
                notificationManager.eliminarNotificacion(
                    usuario.getUsername(), n.getId());
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    panelLista.remove(itemPanel);
                    panelLista.revalidate();
                    panelLista.repaint();
                    mostrarFeedback("✓ Solicitud aceptada");
                    actualizarContador();
                    if (mainFrame != null) mainFrame.actualizarBadges();
                } catch (Exception ex) {
                    mostrarFeedback("❌ Error al aceptar");
                }
            }
        }.execute();
    }

    private void rechazarSolicitud(Notification n, JPanel itemPanel) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                followManager.rechazarSolicitud(
                    usuario.getUsername(), n.getEmisor());
                notificationManager.eliminarNotificacion(
                    usuario.getUsername(), n.getId());
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    panelLista.remove(itemPanel);
                    panelLista.revalidate();
                    panelLista.repaint();
                    mostrarFeedback("Solicitud rechazada");
                    actualizarContador();
                    if (mainFrame != null) mainFrame.actualizarBadges();
                } catch (Exception ex) {
                    mostrarFeedback("❌ Error al rechazar");
                }
            }
        }.execute();
    }

    private void marcarLeida(Notification n) {
        if (n.isLeida()) return;
        try {
            if (chatClient != null && chatClient.isConnected()) {
                chatClient.marcarComoLeida(n.getId());
            } else {
                notificationManager.marcarComoLeida(usuario.getUsername(), n.getId());
            }
            n.setLeida(true);
            actualizarContador();
            if (mainFrame != null) mainFrame.actualizarBadges();
        } catch (Exception ignored) {}
    }

    private void marcarTodasLeidas() {
        try {
            if (chatClient != null && chatClient.isConnected()) {
                chatClient.marcarTodasComoLeidas();
            } else {
                notificationManager.marcarTodasComoLeidas(usuario.getUsername());
            }
            mostrarFeedback("✓ Todas marcadas como leídas");
            cargarLocal();
            if (mainFrame != null) mainFrame.actualizarBadges();
        } catch (Exception ignored) {}
    }

    private void mostrarMenuFiltros(Component origen) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem todos = new JMenuItem("Todas");
        todos.addActionListener(e -> { filtro = null; refrescar(); });
        menu.add(todos);
        menu.addSeparator();

        for (TipoNotificacion tipo : TipoNotificacion.values()) {
            JMenuItem item = new JMenuItem(tipo.getEmoji() + " " +
                tipo.getDescripcion());
            item.addActionListener(e -> { filtro = tipo; refrescar(); });
            menu.add(item);
        }
        menu.show(origen, 0, origen.getHeight());
    }

    private void actualizarContador() {
        try {
            int c = notificationManager.contarNoLeidas(usuario.getUsername());
            lblContador.setText(c > 0 ? c + " sin leer" : "");
        } catch (Exception ignored) {}
    }

    private void mostrarFeedback(String txt) {
        lblFeedback.setText(txt);
        lblFeedback.setForeground(InstagramColors.TEXTO_GRIS);
        Timer t = new Timer(3000, e -> lblFeedback.setText(""));
        t.setRepeats(false); t.start();
    }

    private String grupoFecha(Notification n) {
        long dias = (new java.util.Date().getTime() - n.getFecha().getTime())
                    / (1000L * 60 * 60 * 24);
        if (dias == 0) return "Hoy";
        if (dias == 1) return "Ayer";
        if (dias < 7)  return "Esta semana";
        if (dias < 30) return "Este mes";
        return "Anteriores";
    }

    // ── LISTENER ─────────────────────────────────────────────────

    @Override public void onNotificationReceived(Notification notif) {
        SwingUtilities.invokeLater(() -> {
            cargarLocal();
            mostrarFeedback("🔔 " + notif.generarMensaje());
            if (mainFrame != null) mainFrame.actualizarBadges();
        });
    }

    @Override public void onNotificationListReceived(ArrayList<Notification> list) {
        SwingUtilities.invokeLater(() -> { renderLista(list); actualizarContador(); });
    }

    @Override public void onUnreadCountReceived(int count) {
        SwingUtilities.invokeLater(() ->
            lblContador.setText(count > 0 ? count + " sin leer" : ""));
    }
    private void abrirPost(String postId, String autorHint) {
        if (postId == null || postId.isEmpty()) {
            mainFrame.verPerfilUsuario(autorHint);
            return;
        }
        new SwingWorker<Post, Void>() {
            @Override protected Post doInBackground() throws Exception {
                PostManager pm = new PostManager();
                // Intentar con autorHint primero
                try {
                    Post p = pm.buscarPost(autorHint, postId);
                    if (p != null) return p;
                } catch (Exception ignored) {}
                // Extraer autor del postId (formato "username_timestamp")
                if (postId.contains("_")) {
                    String autor = postId.substring(0, postId.indexOf("_"));
                    try { return pm.buscarPost(autor, postId); }
                    catch (Exception ignored) {}
                }
                return null;
            }
            @Override protected void done() {
                try {
                    Post p = get();
                    if (p != null) mainFrame.verPost(p);
                    else mainFrame.verPerfilUsuario(autorHint);
                } catch (Exception ignored) {
                    mainFrame.verPerfilUsuario(autorHint);
                }
            }
        }.execute();
    }


}