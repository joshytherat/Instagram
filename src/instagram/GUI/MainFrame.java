package instagram.GUI;

import instagram.Utilities.ChatClient;
import instagram.Utilities.FollowManager;
import instagram.GUI.BadgeLabel;
import instagram.GUI.InstagramColors;
import instagram.GUI.InstagramIcons;
import instagram.GUI.PanelPerfilUsuario;
import instagram.GUI.PanelInbox;
import instagram.GUI.PanelEditarPerfil;
import instagram.GUI.PanelCreatePost;
import instagram.GUI.PanelProfile;
import instagram.GUI.PanelNotifications;
import instagram.GUI.PanelSearch;
import instagram.GUI.PanelVerPost;
import instagram.GUI.PanelSettings;
import instagram.GUI.PanelAuth;
import instagram.GUI.PanelHome;
import instagram.Utilities.MessageManager;
import instagram.Utilities.NotificationManager;
import instagram.Utilities.Post;
import instagram.Utilities.PostManager;
import instagram.Utilities.User;
import instagram.Utilities.UserManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

/**
 * MainFrame — única ventana de toda la aplicación.
 *
 * Flujo:
 *   arranque → "AUTH" (login/registro via PanelAuth)
 *   login ok  → construye app panels y muestra "HOME"
 *
 * No se abre ninguna ventana extra (ni VentanaLogin ni nada).
 */
public class MainFrame extends JFrame {

    public static final int W         = 390;
    public static final int H         = 844;
    public static final int HEADER_H  = 56;
    public static final int NAV_H     = 56;
    public static final int CONTENT_H = H - HEADER_H - NAV_H;

    // ── Managers (nulos hasta que el usuario hace login) ──────────
    public UserManager         userManager;
    public PostManager         postManager;
    public MessageManager      messageManager;
    public NotificationManager notificationManager;
    public FollowManager       followManager;
    public ChatClient          chatClient;

    private User usuario;

    // ── Layout raíz ───────────────────────────────────────────────
    private final CardLayout rootLayout = new CardLayout();
    private final JPanel     rootPanel  = new JPanel(rootLayout);

    // ── Auth panel ────────────────────────────────────────────────
    private PanelAuth panelAuth;

    // ── App panels ────────────────────────────────────────────────
    private JPanel     appShell;        // header + contenedor + navbar
    private JPanel     wrapHeader;
    private JPanel     headerNormal;
    private JPanel     headerConBack;
    private JLabel     lblHeaderTitulo;
    private JLabel     btnHeaderBack;

    private JButton btnNavHome;
    private JButton btnNavSearch;
    private JButton btnNavCreate;
    private JButton btnNavProfile;

    private BadgeLabel badgeNotif;
    private BadgeLabel badgeInbox;
    private JButton btnNotif;

    private CardLayout cardLayout;
    private JPanel     contenedor;

    private PanelHome          panelHome;
    private PanelSearch        panelSearch;
    private PanelCreatePost    panelCreate;
    private PanelProfile       panelProfile;
    private PanelInbox         panelInbox;
    private PanelNotifications panelNotif;
    private PanelSettings      panelSettings;
    private PanelEditarPerfil  panelEditarPerfil;
    private PanelVerPost       panelVerPost;
    private PanelPerfilUsuario panelPerfilUsuario;
    private PanelSeguidores    panelSeguidores;

    private final Deque<String> historial = new ArrayDeque<>();
    private String  panelActual  = "HOME";
    private boolean inboxEnChat  = false;
    private boolean appBuilt     = false;

    // ─────────────────────────────────────────────────────────────
    // Constructor — se llama una sola vez al arrancar
    // ─────────────────────────────────────────────────────────────
    public MainFrame() {
        configurarVentana();
        userManager = new UserManager();          // necesario para auth
        panelAuth   = new PanelAuth(userManager, this);

        rootPanel.add(panelAuth, "AUTH");
        rootPanel.add(new JPanel(), "APP");       // placeholder vacío
        add(rootPanel);

        rootLayout.show(rootPanel, "AUTH");
        setVisible(true);
    }

    /**
     * Llamado por PanelAuth cuando el login/registro termina con éxito.
     * Construye la shell de la app y hace la transición sin abrir nueva ventana.
     */
    public void onLoginExitoso(User usuario, ArrayList<Post> feedPreCargado,
                               boolean fueReactivado) {
        this.usuario = usuario;
        inicializarManagers();
        construirAppShell();

        // Reemplazar el placeholder en el rootPanel
        rootPanel.remove(1);
        rootPanel.add(appShell, "APP");
        rootLayout.show(rootPanel, "APP");

        if (feedPreCargado != null && !feedPreCargado.isEmpty()) {
            panelHome.mostrarFeedPreCargado(feedPreCargado);
        } else {
            panelHome.refrescarFeed();
        }
        cardLayout.show(contenedor, "HOME");
        mostrarHeaderNormal();
        actualizarNavBar("HOME");

        if (fueReactivado) {
            panelHome.mostrarBannerReactivado();
        }

        iniciarActualizaciones();
    }

    // ─────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────

    private void configurarVentana() {
        setTitle("Instagram");
        setSize(W, H);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (chatClient != null) chatClient.desconectar();
            }
        });
    }

    private void inicializarManagers() {
        postManager         = new PostManager();
        messageManager      = new MessageManager();
        notificationManager = new NotificationManager();
        followManager       = new FollowManager(userManager);
        chatClient          = new ChatClient(usuario.getUsername());

        // Escuchar errores del servidor — especialmente DOBLE_SESION
        chatClient.setMessageListener(new ChatClient.MessageListener() {
            @Override public void onMessageReceived(instagram.Abstracts.Message m) {}
            @Override public void onConversationsReceived(java.util.ArrayList<String> c) {}
            @Override public void onConnectionStatusChanged(boolean connected) {}
            @Override public void onNotificationReceived(String tipo, Object datos) {}
            @Override public void onError(String error) {
                if (error != null && error.startsWith("DOBLE_SESION:")) {
                    String msg = error.substring("DOBLE_SESION:".length());
                    SwingUtilities.invokeLater(() -> manejarDobleSesion(msg));
                }
            }
        });

        new Thread(() -> {
            try { chatClient.conectar(); } catch (Exception ignored) {}
        }, "chat-connect").start();
    }

    private void construirAppShell() {
        appShell = new JPanel(new BorderLayout());
        appShell.setBackground(Color.WHITE);

        // ── Header ────────────────────────────────────────────────
        wrapHeader = new JPanel(new CardLayout());
        wrapHeader.setPreferredSize(new Dimension(W, HEADER_H));
        headerNormal  = crearHeaderNormal();
        headerConBack = crearHeaderConBack();
        wrapHeader.add(headerNormal,  "NORMAL");
        wrapHeader.add(headerConBack, "BACK");
        appShell.add(wrapHeader, BorderLayout.NORTH);

        // ── Contenedor de paneles ─────────────────────────────────
        cardLayout = new CardLayout();
        contenedor = new JPanel(cardLayout);
        contenedor.setBackground(Color.WHITE);

        panelHome         = new PanelHome(usuario, postManager, followManager, this);
        panelSearch       = new PanelSearch(usuario, userManager, followManager, postManager, this);
        panelCreate       = new PanelCreatePost(usuario, postManager, this);
        panelProfile      = new PanelProfile(usuario, userManager, postManager, followManager, this);
        panelInbox        = new PanelInbox(usuario, messageManager, chatClient, this, userManager, followManager);
        panelNotif        = new PanelNotifications(usuario, notificationManager, followManager, chatClient, this);
        panelSettings     = new PanelSettings(usuario, userManager, postManager, this);
        panelEditarPerfil = new PanelEditarPerfil(usuario, userManager, this);

        contenedor.add(panelHome,         "HOME");
        contenedor.add(panelSearch,       "SEARCH");
        contenedor.add(panelCreate,       "CREATE");
        contenedor.add(panelProfile,      "PROFILE");
        contenedor.add(panelInbox,        "INBOX");
        contenedor.add(panelNotif,        "NOTIFICATIONS");
        contenedor.add(panelSettings,     "SETTINGS");
        contenedor.add(panelEditarPerfil, "EDITAR_PERFIL");

        // Panel de carga genérico
        contenedor.add(crearPanelCargando(), "CARGANDO");

        appShell.add(contenedor, BorderLayout.CENTER);
        appShell.add(crearNavBar(), BorderLayout.SOUTH);

        appBuilt = true;
    }

    /** Panel de "Cargando..." reutilizable */
    private JPanel crearPanelCargando() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        return p;
    }

    /**
     * Muestra un overlay de "Cargando <mensaje>..." en el contenedor
     * mientras se ejecuta el worker, luego muestra <targetPanel>.
     */
    public void navegarConCarga(String targetPanel, String mensaje,
                                Runnable trabajoEnBackground,
                                Runnable alTerminar) {

        // Mostrar panel de carga inmediatamente
        JPanel loading = new JPanel(new GridBagLayout());
        loading.setBackground(Color.WHITE);

        JLabel lbl = new JLabel(mensaje);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lbl.setForeground(InstagramColors.TEXTO_GRIS);
        loading.add(lbl);

        // Añadir (o reemplazar) el panel CARGANDO
        for (Component c : contenedor.getComponents()) {
            if ("CARGANDO".equals(((JPanel) contenedor).getLayout() instanceof CardLayout
                    ? null : null)) break;
        }
        // Quitar el placeholder viejo y poner uno nuevo con el mensaje
        for (int i = 0; i < contenedor.getComponentCount(); i++) {
            Component c = contenedor.getComponent(i);
            if (c.getName() != null && c.getName().equals("CARGANDO")) {
                contenedor.remove(c);
                break;
            }
        }
        loading.setName("CARGANDO");
        contenedor.add(loading, "CARGANDO");
        cardLayout.show(contenedor, "CARGANDO");
        contenedor.revalidate();

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                if (trabajoEnBackground != null) trabajoEnBackground.run();
                return null;
            }
            @Override protected void done() {
                if (alTerminar != null) alTerminar.run();
                cardLayout.show(contenedor, targetPanel);
                contenedor.revalidate();
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────
    // Navegación pública
    // ─────────────────────────────────────────────────────────────

    public void navegarPrincipal(String panel) {
        historial.clear();
        panelActual = panel;
        cardLayout.show(contenedor, panel);
        mostrarHeaderNormal();
        actualizarNavBar(panel);
        refrescarPanelActual(panel);
    }

    public void navegarA(String panel) {
        historial.push(panelActual);
        panelActual = panel;

        if (esSubPanel(panel)) {
            mostrarHeaderConBack(obtenerTituloPanel(panel));
        } else {
            mostrarHeaderNormal();
        }
        actualizarNavBar(panel);
        refrescarPanelActual(panel);
        cardLayout.show(contenedor, panel);
    }

    public void verPerfilUsuario(String username) {
        if (username.equalsIgnoreCase(usuario.getUsername())) {
            navegarPrincipal("PROFILE"); return;
        }
        historial.push(panelActual);
        panelActual = "PERFIL_USUARIO";
        mostrarHeaderConBack("@" + username);
        panelPerfilUsuario = new PanelPerfilUsuario(
                username, usuario, userManager, followManager, postManager, this);
        for (Component comp : contenedor.getComponents())
            if (comp instanceof PanelPerfilUsuario) { contenedor.remove(comp); break; }
        contenedor.add(panelPerfilUsuario, "PERFIL_USUARIO");
        cardLayout.show(contenedor, "PERFIL_USUARIO");
    }

    public void verPost(Post post) {
        historial.push(panelActual);
        panelActual = "VER_POST";
        mostrarHeaderConBack("");
        panelVerPost = new PanelVerPost(post, usuario, postManager,
                followManager, userManager, chatClient, this);
        for (Component comp : contenedor.getComponents())
            if (comp instanceof PanelVerPost) { contenedor.remove(comp); break; }
        contenedor.add(panelVerPost, "VER_POST");
        cardLayout.show(contenedor, "VER_POST");
    }

    public void navegarAHashtag(String tag) {
        navegarPrincipal("SEARCH");
        SwingUtilities.invokeLater(() -> panelSearch.navegarAHashtag(tag));
    }

    public void volver() {
        if (inboxEnChat && "INBOX".equals(panelActual)) {
            inboxEnChat = false;
            mostrarHeaderConBack("Mensajes");
            panelInbox.volverALista();
            return;
        }
        if (historial.isEmpty()) return;
        if ("VER_POST".equals(panelActual) && panelVerPost != null) panelVerPost.destroy();
        panelActual = historial.pop();
        cardLayout.show(contenedor, panelActual);
        if (esSubPanel(panelActual)) mostrarHeaderConBack(obtenerTituloPanel(panelActual));
        else                        mostrarHeaderNormal();
        actualizarNavBar(panelActual);
        refrescarPanelActual(panelActual);
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private boolean esSubPanel(String panel) {
        return panel.equals("SETTINGS") || panel.equals("EDITAR_PERFIL") ||
               panel.equals("PERFIL_USUARIO") || panel.equals("VER_POST") ||
               panel.equals("INBOX") || panel.equals("NOTIFICATIONS") || panel.equals("SEGUIDORES");
    }

    private String obtenerTituloPanel(String panel) {
        switch (panel) {
            case "SETTINGS":       return "Configuración";
            case "EDITAR_PERFIL":  return "Editar perfil";
            case "PERFIL_USUARIO": return panelPerfilUsuario != null
                                        ? "@" + panelPerfilUsuario.getUsername() : "Perfil";
            case "VER_POST":       return "";
            case "INBOX":          return "Mensajes";
            case "NOTIFICATIONS":  return "Notificaciones";
            case "SEGUIDORES": return panelSeguidores != null ? panelSeguidores.getTituloModo() : "Seguidores";
            default:               return panel;
        }
    }

    private void mostrarHeaderNormal() {
        CardLayout cl = (CardLayout) wrapHeader.getLayout();
        cl.show(wrapHeader, "NORMAL");
    }

    private void mostrarHeaderConBack(String titulo) {
        lblHeaderTitulo.setText(titulo);
        CardLayout cl = (CardLayout) wrapHeader.getLayout();
        cl.show(wrapHeader, "BACK");
    }

    private void actualizarNavBar(String panel) {
        Color activo   = InstagramColors.TEXTO_NEGRO;
        Color inactivo = InstagramColors.TEXTO_GRIS;
        btnNavHome   .setForeground(panel.equals("HOME")    ? activo : inactivo);
        btnNavSearch .setForeground(panel.equals("SEARCH")  ? activo : inactivo);
        btnNavCreate .setForeground(panel.equals("CREATE")  ? activo : inactivo);
        btnNavProfile.setForeground(panel.equals("PROFILE") ? activo : inactivo);
    }

    private void refrescarPanelActual(String panel) {
        switch (panel) {
            case "HOME":          panelHome.refrescarFeed();         break;
            case "PROFILE":       panelProfile.refrescarPerfil();    break;
            case "NOTIFICATIONS": panelNotif.refrescar();            break;
            case "SEARCH":        panelSearch.refrescarSugerencias(); break;
            case "CREATE":        panelCreate.resetear();             break;
            case "INBOX":         /* PanelInbox carga internamente */ break;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Header normal
    // ─────────────────────────────────────────────────────────────

    private JPanel crearHeaderNormal() {
        JPanel h = new JPanel(null);
        h.setPreferredSize(new Dimension(W, HEADER_H));
        h.setBackground(Color.WHITE);
        h.setBorder(BorderFactory.createMatteBorder(0,0,1,0,InstagramColors.BORDE_GRIS));

        ImageIcon logoIcon = InstagramIcons.cargar("instagramlogo.jpeg", 32, 32);
        if (logoIcon != null) {
            JLabel lblLogo = new JLabel(logoIcon);
            lblLogo.setBounds(12, 12, 32, 32);
            h.add(lblLogo);
        }
        JLabel lblTitle = new JLabel("Instagram", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblTitle.setForeground(InstagramColors.TEXTO_NEGRO);
        lblTitle.setBounds(60, 10, 230, 36);
        h.add(lblTitle);

        // Notificaciones
        JPanel panelNotifBtn = new JPanel(null);
        panelNotifBtn.setBounds(298, 11, 36, 36);
        panelNotifBtn.setOpaque(false);
        panelNotifBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panelNotifBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { navegarA("NOTIFICATIONS"); }
        });
        JLabel lblNotifIcon = new JLabel();
        lblNotifIcon.setBounds(0, 0, 36, 36);
        lblNotifIcon.setHorizontalAlignment(SwingConstants.CENTER);
        ImageIcon notifIcon = InstagramIcons.cargar("notificacion.png", 22, 22);
        if (notifIcon == null) notifIcon = InstagramIcons.cargar("like.png", 22, 22);
        if (notifIcon != null) { lblNotifIcon.setIcon(notifIcon); }
        else { lblNotifIcon.setText("♥"); lblNotifIcon.setFont(new Font("Arial", Font.PLAIN, 22)); }
        panelNotifBtn.add(lblNotifIcon);
        badgeNotif = new BadgeLabel();
        badgeNotif.setBounds(18, 0, 18, 14);
        panelNotifBtn.add(badgeNotif);
        h.add(panelNotifBtn);

        // Inbox
        JPanel panelInboxBtn = new JPanel(null);
        panelInboxBtn.setBounds(343, 11, 36, 36);
        panelInboxBtn.setOpaque(false);
        panelInboxBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panelInboxBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { navegarA("INBOX"); }
        });
        JLabel lblInboxIcon = new JLabel();
        lblInboxIcon.setBounds(0, 0, 36, 36);
        lblInboxIcon.setHorizontalAlignment(SwingConstants.CENTER);
        ImageIcon inboxIcon = InstagramIcons.cargar("mensaje.png", 22, 22);
        if (inboxIcon != null) { lblInboxIcon.setIcon(inboxIcon); }
        else { lblInboxIcon.setText("💬"); lblInboxIcon.setFont(new Font("Arial", Font.PLAIN, 20)); }
        panelInboxBtn.add(lblInboxIcon);
        badgeInbox = new BadgeLabel();
        badgeInbox.setBounds(18, 0, 18, 14);
        panelInboxBtn.add(badgeInbox);
        h.add(panelInboxBtn);

        return h;
    }

    // ─────────────────────────────────────────────────────────────
    // Header con back
    // ─────────────────────────────────────────────────────────────

    private JPanel crearHeaderConBack() {
        JPanel h = new JPanel(null);
        h.setPreferredSize(new Dimension(W, HEADER_H));
        h.setBackground(Color.WHITE);
        h.setBorder(BorderFactory.createMatteBorder(0,0,1,0,InstagramColors.BORDE_GRIS));

        btnHeaderBack = new JLabel("←");
        btnHeaderBack.setBounds(10, 14, 36, 28);
        btnHeaderBack.setFont(new Font("Arial", Font.PLAIN, 22));
        btnHeaderBack.setHorizontalAlignment(SwingConstants.CENTER);
        btnHeaderBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnHeaderBack.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { volver(); }
        });
        h.add(btnHeaderBack);

        lblHeaderTitulo = new JLabel("", SwingConstants.CENTER);
        lblHeaderTitulo.setBounds(50, 14, 290, 28);
        lblHeaderTitulo.setFont(new Font("Arial", Font.BOLD, 16));
        lblHeaderTitulo.setForeground(InstagramColors.TEXTO_NEGRO);
        h.add(lblHeaderTitulo);

        return h;
    }

    // ─────────────────────────────────────────────────────────────
    // NavBar
    // ─────────────────────────────────────────────────────────────

    private JPanel crearNavBar() {
        JPanel nav = new JPanel(null);
        nav.setPreferredSize(new Dimension(W, NAV_H));
        nav.setBackground(Color.WHITE);
        nav.setBorder(BorderFactory.createMatteBorder(1,0,0,0,InstagramColors.BORDE_GRIS));

        int paso = W / 4, y = 8, iconSize = 26;
        btnNavHome    = crearNavBtn("home.png",    iconSize, "🏠", 0 * paso, y);
        btnNavSearch  = crearNavBtn("buscar.png",  iconSize, "🔍", 1 * paso, y);
        btnNavCreate  = crearNavBtn("nueva.png",   iconSize, "➕", 2 * paso, y);
        btnNavProfile = crearNavBtn("perfil.png",  iconSize, "👤", 3 * paso, y);

        btnNavHome   .addActionListener(e -> navegarPrincipal("HOME"));
        btnNavSearch .addActionListener(e -> navegarPrincipal("SEARCH"));
        btnNavCreate .addActionListener(e -> navegarPrincipal("CREATE"));
        btnNavProfile.addActionListener(e -> navegarPrincipal("PROFILE"));

        nav.add(btnNavHome); nav.add(btnNavSearch);
        nav.add(btnNavCreate); nav.add(btnNavProfile);
        return nav;
    }

    private JButton crearNavBtn(String img, int size, String fallback, int x, int y) {
        JButton btn = InstagramIcons.boton(img, size, fallback);
        btn.setBounds(x + (W/4 - 40)/2, y, 40, 40);
        return btn;
    }

    // ─────────────────────────────────────────────────────────────
    // Badges & actualizaciones periódicas
    // ─────────────────────────────────────────────────────────────

    public void actualizarBadges() {
        new SwingWorker<int[], Void>() {
            @Override protected int[] doInBackground() throws Exception {
                int notifs = notificationManager.contarNoLeidas(usuario.getUsername());
                return new int[]{ notifs, 0 };
            }
            @Override protected void done() {
                try {
                    int[] c = get();
                    badgeNotif.setCount(c[0]);
                    badgeInbox.setCount(c[1]);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void iniciarActualizaciones() {
        Timer t = new Timer(15000, e -> actualizarBadges());
        t.start();
        actualizarBadges();
    }

    // ─────────────────────────────────────────────────────────────
    // Métodos públicos usados por los paneles
    // ─────────────────────────────────────────────────────────────

    public User getUsuario() { return usuario; }

    public void onInboxEstado(boolean enChat, String titulo) {
        inboxEnChat = enChat;
        mostrarHeaderConBack(enChat && titulo != null ? titulo : "Mensajes");
    }

    public void abrirSeguidores(boolean verSeguidores) {
        PanelSeguidores.Modo modo = verSeguidores ? PanelSeguidores.Modo.SEGUIDORES : PanelSeguidores.Modo.SEGUIDOS;
        historial.push(panelActual); panelActual = "SEGUIDORES";
        mostrarHeaderConBack(verSeguidores ? "Seguidores" : "Seguidos");
        panelSeguidores = new PanelSeguidores(usuario, userManager, followManager, this, modo);
        for (Component c : contenedor.getComponents()) if (c instanceof PanelSeguidores) { contenedor.remove(c); break; }
        contenedor.add(panelSeguidores, "SEGUIDORES");
        cardLayout.show(contenedor, "SEGUIDORES");
    }

    public void abrirChatCon(String username) {
        javax.swing.Timer t = new javax.swing.Timer(120,
                e -> panelInbox.abrirChatCon(username));
        t.setRepeats(false);
        t.start();
    }

    public void mostrarBannerReactivado() {
        if (panelHome != null) panelHome.mostrarBannerReactivado();
        navegarPrincipal("HOME");
    }

    /**
     * Cierra sesión y vuelve a la pantalla de login — sin abrir ninguna ventana nueva.
     * Destruye la shell de la app y reconstruye PanelAuth en la misma ventana.
     */
    /**
     * Llamado cuando el servidor rechaza el login por sesión duplicada.
     * Vuelve al panel de auth y muestra un aviso — sin cerrar ninguna otra sesión.
     */
    private void manejarDobleSesion(String mensaje) {
        // Detener chatClient (la conexión fue rechazada de todas formas)
        if (chatClient != null) {
            try { chatClient.desconectar(); } catch (Exception ignored) {}
            chatClient = null;
        }

        // Volver al panel AUTH
        usuario  = null;
        appBuilt = false;
        historial.clear();

        userManager = new UserManager();
        panelAuth   = new PanelAuth(userManager, this);

        rootPanel.removeAll();
        rootPanel.add(panelAuth, "AUTH");
        rootPanel.add(new JPanel(), "APP");
        rootLayout.show(rootPanel, "AUTH");
        rootPanel.revalidate();
        rootPanel.repaint();

        // Mostrar el mensaje de sesión duplicada en PanelAuth
        panelAuth.mostrarErrorSesion(mensaje);
    }

    public void logout() {
        // Desconectar chat
        if (chatClient != null) {
            try { chatClient.desconectar(); } catch (Exception ignored) {}
            chatClient = null;
        }

        // Limpiar estado
        usuario    = null;
        appBuilt   = false;
        historial.clear();

        // Construir nuevo panel de auth fresco
        userManager = new UserManager();
        panelAuth   = new PanelAuth(userManager, this);

        // Limpiar rootPanel y poner sólo auth
        rootPanel.removeAll();
        rootPanel.add(panelAuth, "AUTH");
        rootPanel.add(new JPanel(), "APP");
        rootLayout.show(rootPanel, "AUTH");

        rootPanel.revalidate();
        rootPanel.repaint();
    }
}