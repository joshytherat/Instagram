package instagram.GUI;

import instagram.Utilities.ChatClient;
import instagram.Enums.EstadoCuenta;
import instagram.Utilities.FollowManager;
import instagram.Abstracts.Message;
import instagram.Utilities.MessageManager;
import instagram.Utilities.StickerMessage;
import instagram.Utilities.TextMessage;
import instagram.Enums.TipoCuenta;
import instagram.Utilities.User;
import instagram.Utilities.UserManager;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PanelInbox — diseño profesional Instagram-like.
 *
 * Estructura visual:
 *   LISTA  → lista de conversaciones compacta con avatar + preview
 *   CHAT   → burbujas + barra de entrada + sticker drawer
 *
 * Reglas de negocio:
 *   - Cuentas DESACTIVADAS: no se puede enviar mensajes nuevos (historial visible)
 *   - Cuentas PRIVADAS: no se puede escribir hasta que se acepte el follow request
 */
public class PanelInbox extends JPanel implements ChatClient.MessageListener {

    // ── Colores del chat ──────────────────────────────────────────
    private static final Color BG           = new Color(250, 250, 250);
    private static final Color BURBUJA_MIO  = new Color(0,  149, 246);
    private static final Color BURBUJA_OTRO = new Color(210, 210, 210);
    private static final Color TEXTO_BURBUJA_MIO  = Color.WHITE;
    private static final Color TEXTO_BURBUJA_OTRO = new Color(38, 38, 38);
    private static final Color SEPARADOR    = new Color(219, 219, 219);
    private static final Color INPUT_BG     = new Color(245, 245, 245);

    // ── Dependencias ──────────────────────────────────────────────
    private final User          usuario;
    private final MessageManager messageManager;
    private final UserManager   userManager;
    private final FollowManager followManager;
    private final ChatClient    chatClient;
    private final MainFrame     mainFrame;

    // ── Estado ────────────────────────────────────────────────────
    private String  conversacionActiva; // username del otro usuario
    private boolean enChat = false;     // true = vista CHAT activa

    // ── Vistas ────────────────────────────────────────────────────
    private final CardLayout cards = new CardLayout();
    private final JPanel     raiz  = new JPanel(cards);

    // Vista LISTA
    private JPanel listaConvPanel;

    // Vista CHAT
    private JLabel      lblChatNombre;
    private JLabel      lblChatEstado;    // "en línea" / "última vez..."
    private ComponenteCircular avatarChatHeader;
    private JPanel      panelMensajes;
    private JScrollPane scrollMensajes;
    private JTextField  txtMensaje;
    private JPanel      stickerDrawer;
    private boolean     drawerVisible = false;

    private volatile int listaGeneracion = 0;
    private static final Map<String, BufferedImage> imgCache = new ConcurrentHashMap<>();
    // Timer para polling de estado "visto" cuando estamos en un chat
    private javax.swing.Timer timerVisto = null;

    // ── Sticker drawer ────────────────────────────────────────────
    // Códigos de stickers por defecto — PNG en resources/ con ese nombre
    // Tamaño en pantalla de los stickers (en burbujas y en el picker)
    private static final int STICKER_SIZE = 80;
    private static final String STICKERS_DIR_GLOBAL = "INSTA_RAIZ/stickers_globales";

    public PanelInbox(User usuario, MessageManager messageManager,
                      ChatClient chatClient, MainFrame mainFrame) {
        this(usuario, messageManager, chatClient, mainFrame, null, null);
    }

    public PanelInbox(User usuario, MessageManager messageManager,
                      ChatClient chatClient, MainFrame mainFrame,
                      UserManager userManager, FollowManager followManager) {
        this.usuario        = usuario;
        this.messageManager = messageManager;
        this.chatClient     = chatClient;
        this.mainFrame      = mainFrame;
        // Usar managers del mainFrame si no se pasaron explícitamente
        this.userManager   = (userManager   != null) ? userManager   : mainFrame.userManager;
        this.followManager = (followManager != null) ? followManager : mainFrame.followManager;

        if (chatClient != null) chatClient.setMessageListener(this);

        setLayout(new BorderLayout());
        setBackground(BG);

        raiz.setBackground(BG);
        add(raiz, BorderLayout.CENTER);

        construirVista();
        cargarConversaciones();
    }

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCCIÓN
    // ═══════════════════════════════════════════════════════════════

    private void construirVista() {
        raiz.add(construirLista(), "LISTA");
        raiz.add(construirChat(),  "CHAT");
        mostrarLista();
    }

    // ── Vista LISTA ───────────────────────────────────────────────

    private JPanel construirLista() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);

        // Contenedor de items
        listaConvPanel = new JPanel();
        listaConvPanel.setLayout(new BoxLayout(listaConvPanel, BoxLayout.Y_AXIS));
        listaConvPanel.setBackground(Color.WHITE);

        JScrollPane sc = new JScrollPane(listaConvPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(null);
        sc.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        sc.getVerticalScrollBar().setUnitIncrement(20);
        p.add(sc, BorderLayout.CENTER);

        return p;
    }

    // ── Vista CHAT ────────────────────────────────────────────────

    private JPanel construirChat() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);

        // ── Header real del chat: avatar + nombre → lleva al perfil ──
        JPanel chatHeader = new JPanel(null);
        chatHeader.setPreferredSize(new Dimension(390, 56));
        chatHeader.setBackground(Color.WHITE);
        chatHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SEPARADOR));

        avatarChatHeader = new ComponenteCircular(38);
        avatarChatHeader.setBounds(12, 9, 38, 38);
        avatarChatHeader.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatarChatHeader.addMouseListener(click(e -> {
            if (conversacionActiva != null) mainFrame.verPerfilUsuario(conversacionActiva);
        }));
        chatHeader.add(avatarChatHeader);

        lblChatNombre = new JLabel();
        lblChatNombre.setBounds(60, 10, 270, 20);
        lblChatNombre.setFont(new Font("Arial", Font.BOLD, 15));
        lblChatNombre.setForeground(new Color(38, 38, 38));
        lblChatNombre.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblChatNombre.addMouseListener(click(e -> {
            if (conversacionActiva != null) mainFrame.verPerfilUsuario(conversacionActiva);
        }));
        chatHeader.add(lblChatNombre);

        lblChatEstado = new JLabel();
        lblChatEstado.setBounds(60, 30, 270, 16);
        lblChatEstado.setFont(new Font("Arial", Font.PLAIN, 11));
        lblChatEstado.setForeground(new Color(142, 142, 142));
        chatHeader.add(lblChatEstado);

        p.add(chatHeader, BorderLayout.NORTH);

        // ── Área de mensajes ──────────────────────────────────────
        panelMensajes = new JPanel();
        panelMensajes = new JPanel();
        panelMensajes.setLayout(new BoxLayout(panelMensajes, BoxLayout.Y_AXIS));
        panelMensajes.setBackground(new Color(250, 250, 250));
        panelMensajes.setBorder(new EmptyBorder(6, 0, 6, 0));

        scrollMensajes = new JScrollPane(panelMensajes,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMensajes.setBorder(null);
        scrollMensajes.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollMensajes.getVerticalScrollBar().setUnitIncrement(24);
        scrollMensajes.setBackground(new Color(250, 250, 250));
        p.add(scrollMensajes, BorderLayout.CENTER);

        // ── Panel sur: drawer (oculto) + barra de entrada ───────────
        // NUNCA usar AFTER_LAST_LINE — no existe en BorderLayout.
        // Sticker drawer y barra van en un BoxLayout Y_AXIS en SOUTH.
        panelSur = new JPanel();
        panelSur.setLayout(new BoxLayout(panelSur, BoxLayout.Y_AXIS));
        panelSur.setBackground(Color.WHITE);

        stickerDrawer = construirStickerDrawer();
        stickerDrawer.setVisible(false);
        stickerDrawer.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelSur.add(stickerDrawer);

        JPanel barra = construirBarraEntrada();
        barra.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelSur.add(barra);

        p.add(panelSur, BorderLayout.SOUTH);
        return p;
    }

    // ── Barra de entrada ──────────────────────────────────────────

    private JPanel construirBarraEntrada() {
        JPanel barra = new JPanel(null);
        barra.setPreferredSize(new Dimension(390, 60));
        barra.setBackground(Color.WHITE);
        barra.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SEPARADOR));

        // Botón sticker (😊)
        JLabel btnStk = new JLabel("😊");
        btnStk.setBounds(10, 14, 32, 32);
        btnStk.setFont(new Font("Arial", Font.PLAIN, 20));
        btnStk.setHorizontalAlignment(SwingConstants.CENTER);
        btnStk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnStk.addMouseListener(click(e -> toggleStickerDrawer()));
        barra.add(btnStk);

        // Campo de texto
        txtMensaje = new JTextField();
        txtMensaje.setBounds(50, 14, 282, 32);
        txtMensaje.setFont(new Font("Arial", Font.PLAIN, 14));
        txtMensaje.setBackground(INPUT_BG);
        txtMensaje.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SEPARADOR, 1, true),
            new EmptyBorder(0, 12, 0, 12)));
        txtMensaje.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) enviarTexto();
            }
        });
        barra.add(txtMensaje);

        // Botón enviar
        JLabel btnEnviar = new JLabel("➤");
        btnEnviar.setBounds(340, 14, 40, 32);
        btnEnviar.setFont(new Font("Arial", Font.BOLD, 18));
        btnEnviar.setForeground(InstagramColors.INSTAGRAM_AZUL);
        btnEnviar.setHorizontalAlignment(SwingConstants.CENTER);
        btnEnviar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEnviar.addMouseListener(click(e -> enviarTexto()));
        barra.add(btnEnviar);

        return barra;
    }

    // ── Sticker drawer ────────────────────────────────────────────

    // Grid de stickers unificado (globales + personales)
    private JPanel stickerGrid;
    private JPanel panelSur;

    private JPanel construirStickerDrawer() {
        JPanel drawer = new JPanel(new BorderLayout());
        drawer.setBackground(Color.WHITE);
        drawer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SEPARADOR));
        drawer.setPreferredSize(new Dimension(390, 160));

        // Grid único con todos los stickers — scroll vertical
        stickerGrid = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 6));
        stickerGrid.setBackground(Color.WHITE);
        cargarStickersEnGrid(stickerGrid);

        JScrollPane sc = new JScrollPane(stickerGrid,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(null);
        sc.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        sc.getVerticalScrollBar().setUnitIncrement(16);
        sc.setBackground(Color.WHITE);
        drawer.add(sc, BorderLayout.CENTER);

        // Botón "+" para importar sticker personal
        JLabel btnAgregar = new JLabel("＋");
        btnAgregar.setFont(new Font("Arial", Font.BOLD, 18));
        btnAgregar.setForeground(InstagramColors.INSTAGRAM_AZUL);
        btnAgregar.setPreferredSize(new Dimension(40, 130));
        btnAgregar.setHorizontalAlignment(SwingConstants.CENTER);
        btnAgregar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAgregar.setToolTipText("Importar sticker");
        btnAgregar.addMouseListener(click(e -> agregarStickerCustom(stickerGrid)));
        drawer.add(btnAgregar, BorderLayout.EAST);

        return drawer;
    }

    /** Crea un JLabel con la imagen del sticker escalada. */
    private JLabel crearLabelSticker(String code, int size, String usr) {
        java.awt.image.BufferedImage bi = StickerMessage.cargarImagen(code, usr);
        if (bi == null) {
            JLabel lbl = new JLabel(code, SwingConstants.CENTER);
            lbl.setFont(new Font("Arial", Font.PLAIN, 9));
            lbl.setPreferredSize(new Dimension(size, size));
            lbl.setBorder(BorderFactory.createLineBorder(new Color(210,210,210)));
            return lbl;
        }
        Image scaled = bi.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        JLabel lbl = new JLabel(new ImageIcon(scaled));
        lbl.setPreferredSize(new Dimension(size, size));
        lbl.setToolTipText(code);
        return lbl;
    }

    private void cargarStickersEnGrid(JPanel grid) {
        grid.removeAll();
        agregarStickersDeDir(grid, new File(STICKERS_DIR_GLOBAL), false);
        agregarStickersDeDir(grid,
            new File("INSTA_RAIZ/" + usuario.getUsername() + "/stickers_personales"), true);
        grid.revalidate();
        grid.repaint();
    }

    private void agregarStickersDeDir(JPanel grid, File dir, boolean personal) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles(f -> f.isFile() && (
            f.getName().endsWith(".png") || f.getName().endsWith(".jpg")
            || f.getName().endsWith(".jpeg") || f.getName().endsWith(".gif")));
        if (files == null) return;
        for (File f : files) {
            try {
                BufferedImage bi = ImageIO.read(f);
                if (bi == null) continue;
                JLabel lbl = new JLabel(new ImageIcon(
                    bi.getScaledInstance(56, 56, Image.SCALE_SMOOTH)));
                lbl.setPreferredSize(new Dimension(60, 60));
                lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                lbl.setToolTipText(f.getName().replaceFirst("[.][^.]+$", ""));
                final String ruta = f.getAbsolutePath();
                final String code = f.getName().replaceFirst("[.][^.]+$", "");
                final BufferedImage img = bi;
                lbl.addMouseListener(click(e -> enviarImagenSticker(ruta)));
                if (personal) {
                    // Long-press / right-click → eliminar
                    lbl.addMouseListener(new MouseAdapter() {
                        @Override public void mouseClicked(MouseEvent e) {
                            if (SwingUtilities.isRightMouseButton(e)) {
                                new File(ruta).delete();
                                cargarStickersEnGrid(grid);
                            }
                        }
                    });
                }
                grid.add(lbl);
            } catch (Exception ignored) {}
        }
    }

    private void agregarStickerCustom(JPanel grid) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar imagen para sticker");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Imágenes", "png", "jpg", "jpeg", "gif"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File src = fc.getSelectedFile();
        try {
            String dir = "INSTA_RAIZ/" + usuario.getUsername() + "/stickers_personales";
            new File(dir).mkdirs();
            File dest = new File(dir + "/" + src.getName());
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            cargarStickersEnGrid(grid);
        } catch (Exception ex) {
            mostrarFeedback("❌ Error al guardar el sticker", new Color(220, 50, 50));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // NAVEGACIÓN INTERNA
    // ═══════════════════════════════════════════════════════════════

    private void mostrarLista() {
        enChat = false;
        conversacionActiva = null;
        cerrarDrawer();
        detenerTimerVisto();
        cards.show(raiz, "LISTA");
        mainFrame.onInboxEstado(false, null);
    }

    public void mostrarChat(String otroUsername) {
        enChat = true;
        conversacionActiva = otroUsername;
        panelMensajes.removeAll();
        JLabel lblCargando = new JLabel("Cargando mensajes...", SwingConstants.CENTER);
        lblCargando.setFont(new Font("Arial", Font.PLAIN, 13));
        lblCargando.setForeground(new Color(142, 142, 142));
        lblCargando.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelMensajes.add(Box.createVerticalGlue());
        panelMensajes.add(lblCargando);
        panelMensajes.add(Box.createVerticalGlue());
        panelMensajes.revalidate(); panelMensajes.repaint();
        cargarMensajes(otroUsername);
        cards.show(raiz, "CHAT");
        mainFrame.onInboxEstado(true, "Mensajes");
        SwingUtilities.invokeLater(() -> {
            actualizarHeaderChat(otroUsername);
            txtMensaje.requestFocus();
        });
        // Iniciar polling para actualizar ✓✓ (visto) en tiempo real
        iniciarTimerVisto(otroUsername);
    }

    public void abrirChatCon(String otroUsername) {
        // Verificar permisos antes de abrir el chat
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return verificarPermiso(otroUsername);
            }
            @Override protected void done() {
                try {
                    String bloqueo = get();
                    if (bloqueo != null) {
                        mostrarAvisoBloqueo(bloqueo);
                    } else {
                        mostrarChat(otroUsername);
                    }
                } catch (Exception ignored) { mostrarChat(otroUsername); }
            }
        }.execute();
    }

    public void volverALista() {
        cerrarDrawer();
        detenerTimerVisto();
        avatarChatHeader.setImagen(null);
        avatarChatHeader.revalidate();
        avatarChatHeader.repaint();
        mostrarLista();
        new SwingWorker<ArrayList<String>, Void>() {
            @Override protected ArrayList<String> doInBackground() throws Exception {
                ArrayList<String> convs = messageManager.obtenerConversaciones(usuario.getUsername());
                convs.sort((a, b) -> {
                    try {
                        ArrayList<Message> msgsA = messageManager.obtenerConversacion(usuario.getUsername(), a);
                        ArrayList<Message> msgsB = messageManager.obtenerConversacion(usuario.getUsername(), b);
                        long fA = msgsA.isEmpty() ? 0 : msgsA.get(msgsA.size()-1).getFecha().getTime();
                        long fB = msgsB.isEmpty() ? 0 : msgsB.get(msgsB.size()-1).getFecha().getTime();
                        return Long.compare(fB, fA);
                    } catch (Exception e) { return 0; }
                });
                return convs;
            }
            @Override protected void done() {
                try { renderConversaciones(get()); } catch (Exception ignored) {}
            }
        }.execute();
    }


    // ── Permiso para escribir ─────────────────────────────────────

    /**
     * @return null si puede escribir, String con el motivo si no puede
     */
    /**
     * Verifica permiso según spec:
     *  - Cuenta DESACTIVADA         → bloqueado siempre
     *  - Perfil PÚBLICO             → permitido
     *  - Perfil PRIVADO + amigos    → permitido
     *  - Perfil PRIVADO sin amistad → bloqueado
     */
    private String verificarPermiso(String otroUsername) {
        if (userManager == null || followManager == null) return null; // sin managers: permitir
        try {
            User otro = userManager.buscarUsuario(otroUsername);
            if (otro == null) return null; // no encontrado → no bloquear
            if (otro.getEstadoCuenta() == EstadoCuenta.DESACTIVADO) return null; // ver historial OK
            if (otro.getTipoCuenta() == TipoCuenta.PUBLICA)
                return null; // cuenta pública → siempre permitido
            // Cuenta privada: solo si son amigos mutuos
            boolean amigos = followManager.sonAmigos(
                usuario.getUsername(), otroUsername);
            if (!amigos)
                return "@" + otroUsername + " tiene cuenta privada.Solo puedes enviarle mensajes cuando sean amigos mutuos.";
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Verifica si se puede enviar (cuenta no desactivada).
     * Historial sigue visible aunque la cuenta esté desactivada.
     */
    private boolean puedeEnviarMensaje() {
        if (conversacionActiva == null) return false;
        if (userManager == null || followManager == null) return true;
        try {
            User otro = userManager.buscarUsuario(conversacionActiva);
            if (otro == null) return true;
            if (otro.getEstadoCuenta() == EstadoCuenta.DESACTIVADO) return false;
            if (otro.getTipoCuenta() == TipoCuenta.PUBLICA) return true;
            return followManager.sonAmigos(usuario.getUsername(), conversacionActiva);
        } catch (Exception e) { return true; }
    }

    // ═══════════════════════════════════════════════════════════════
    // CARGA DE DATOS
    // ═══════════════════════════════════════════════════════════════

    public void cargarConversaciones() {
        // SIEMPRE cargar desde disco — rápido, sin depender de red.
        // Ordenar por fecha del último mensaje (más reciente arriba).
        new SwingWorker<ArrayList<String>, Void>() {
            @Override protected ArrayList<String> doInBackground() throws Exception {
                ArrayList<String> convs = messageManager.obtenerConversaciones(usuario.getUsername());
                // Ordenar: el que tiene el mensaje más reciente va primero
                convs.sort((a, b) -> {
                    try {
                        ArrayList<instagram.Abstracts.Message> msgsA =
                            messageManager.obtenerConversacion(usuario.getUsername(), a);
                        ArrayList<instagram.Abstracts.Message> msgsB =
                            messageManager.obtenerConversacion(usuario.getUsername(), b);
                        long fechaA = msgsA.isEmpty() ? 0 : msgsA.get(msgsA.size()-1).getFecha().getTime();
                        long fechaB = msgsB.isEmpty() ? 0 : msgsB.get(msgsB.size()-1).getFecha().getTime();
                        return Long.compare(fechaB, fechaA); // más reciente primero
                    } catch (Exception e) { return 0; }
                });
                return convs;
            }
            @Override protected void done() {
                try { renderConversaciones(get()); } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void renderConversaciones(ArrayList<String> convs) {
        listaGeneracion++;
        listaConvPanel.removeAll();
        if (convs.isEmpty()) {
            JLabel lbl = new JLabel(
                "<html><center>Aún no tienes mensajes.<br>Ve al perfil de un usuario para iniciar.</center></html>",
                SwingConstants.CENTER);
            lbl.setFont(new Font("Arial", Font.PLAIN, 14));
            lbl.setForeground(new Color(142,142,142));
            lbl.setBorder(new EmptyBorder(60, 30, 0, 30));
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            listaConvPanel.add(lbl);
        } else {
            final int gen = listaGeneracion;
            for (String otro : convs) listaConvPanel.add(crearItemConversacion(otro, gen));
        }
        listaConvPanel.revalidate();
        listaConvPanel.repaint();
    }

    private JPanel crearItemConversacion(String otro, int gen) {
        JPanel item = new JPanel(null);
        item.setPreferredSize(new Dimension(390, 72));
        item.setMaximumSize(new Dimension(390, 72));
        item.setBackground(Color.WHITE);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240,240,240)));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Avatar
        ComponenteCircular av = new ComponenteCircular(48);
        av.setBounds(14, 12, 48, 48);
        av.setImagen(null);
        item.add(av);
        cargarFotoPerfil(av, otro, gen);

        // Nombre
        JLabel lblNombre = new JLabel("@" + otro);
        lblNombre.setBounds(74, 14, 240, 20);
        lblNombre.setFont(new Font("Arial", Font.BOLD, 14));
        lblNombre.setForeground(new Color(38, 38, 38));
        item.add(lblNombre);

        // Preview último mensaje (async)
        JLabel lblPreview = new JLabel("Toca para chatear");
        lblPreview.setBounds(74, 36, 240, 16);
        lblPreview.setFont(new Font("Arial", Font.PLAIN, 12));
        lblPreview.setForeground(new Color(142, 142, 142));
        item.add(lblPreview);
        cargarPreviewUltimoMensaje(lblPreview, otro);

        // Flecha derecha
        JLabel arrow = new JLabel("›");
        arrow.setBounds(358, 25, 18, 22);
        arrow.setFont(new Font("Arial", Font.PLAIN, 22));
        arrow.setForeground(new Color(199, 199, 199));
        item.add(arrow);

        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { mostrarChat(otro); }
            @Override public void mouseEntered(MouseEvent e) { item.setBackground(new Color(248,248,248)); }
            @Override public void mouseExited(MouseEvent e)  { item.setBackground(Color.WHITE); }
        });
        return item;
    }

    private void cargarPreviewUltimoMensaje(JLabel lbl, String otro) {
        new SwingWorker<String[], Void>() {
            @Override protected String[] doInBackground() throws Exception {
                ArrayList<Message> msgs = messageManager.obtenerConversacion(
                    usuario.getUsername(), otro);
                if (msgs.isEmpty()) return null;
                Message last = msgs.get(msgs.size() - 1);
                boolean esMio = last.getEmisor().equals(usuario.getUsername());
                String preview = esMio ? "Tú: " + last.getContenido() : last.getContenido();
                preview = preview.length() > 34 ? preview.substring(0, 34) + "…" : preview;
                // Estado "Visto" solo para mis mensajes
                String estado = "";
                if (esMio) {
                    estado = last.getEstado() == Message.EstadoMensaje.LEIDO ? "Visto" : "";
                }
                return new String[]{ preview, estado };
            }
            @Override protected void done() {
                try {
                    String[] res = get();
                    if (res == null) return;
                    if (!res[1].isEmpty()) {
                        // Mostrar "Visto" en azul junto al preview
                        lbl.setText("<html>" + res[0] + " <font color='#0095F6'>· Visto</font></html>");
                    } else {
                        lbl.setText(res[0]);
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void actualizarHeaderChat(String otro) {
        lblChatNombre.setText("@" + otro);
        lblChatEstado.setText("");
        avatarChatHeader.setImagen(null);
        avatarChatHeader.repaint();

        // Un único SwingWorker: buscar usuario UNA sola vez,
        // luego cargar foto y estado con el mismo objeto User.
        // Evita dos llamadas concurrentes a buscarUsuario sobre el mismo RAF.
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() throws Exception {
                if (userManager == null) return new Object[]{null, ""};
                User u = userManager.buscarUsuario(otro);
                if (u == null) return new Object[]{null, ""};

                // Cargar imagen
                BufferedImage foto = null;
                String ruta = u.getRutaFotoPerfil();
                if (ruta != null) {
                    ruta = ruta.replaceAll("[ -]+", "").trim();
                    if (!ruta.isEmpty() && !ruta.equals("default.jpg")) {
                        File f = new File(ruta);
                        if (f.exists() && f.isFile() && f.length() > 0) {
                            try {
                                String ext = ruta.toLowerCase();
                                if (ext.endsWith(".png")) {
                                    try (javax.imageio.stream.ImageInputStream iis =
                                            javax.imageio.ImageIO.createImageInputStream(f)) {
                                        java.util.Iterator<javax.imageio.ImageReader> rs =
                                            javax.imageio.ImageIO.getImageReadersByFormatName("png");
                                        if (rs.hasNext()) {
                                            javax.imageio.ImageReader r = rs.next();
                                            r.setInput(iis); foto = r.read(0); r.dispose();
                                        }
                                    }
                                } else if (ext.endsWith(".jpg") || ext.endsWith(".jpeg")) {
                                    try (javax.imageio.stream.ImageInputStream iis =
                                            javax.imageio.ImageIO.createImageInputStream(f)) {
                                        java.util.Iterator<javax.imageio.ImageReader> rs =
                                            javax.imageio.ImageIO.getImageReadersByFormatName("jpeg");
                                        if (rs.hasNext()) {
                                            javax.imageio.ImageReader r = rs.next();
                                            r.setInput(iis); foto = r.read(0); r.dispose();
                                        }
                                    }
                                } else {
                                    foto = ImageIO.read(f);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }

                // Estado de la cuenta
                String estado = "";
                if (u.getEstadoCuenta() == EstadoCuenta.DESACTIVADO) {
                    estado = "Cuenta desactivada";
                } else if (chatClient != null && chatClient.isConnected()) {
                    estado = "en línea";
                }

                return new Object[]{foto, estado};
            }

            @Override protected void done() {
                try {
                    Object[] res = get();
                    BufferedImage foto  = (BufferedImage) res[0];
                    String      estado = (String)        res[1];
                    SwingUtilities.invokeLater(() -> {
                        if (foto != null) {
                            avatarChatHeader.setImagen(foto);
                            avatarChatHeader.repaint();
                            if (avatarChatHeader.getParent() != null)
                                avatarChatHeader.getParent().repaint();
                        }
                        lblChatEstado.setText(estado);
                    });
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void cargarMensajes(String otro) {
        new SwingWorker<ArrayList<Message>, Void>() {
            @Override protected ArrayList<Message> doInBackground() throws Exception {
                ArrayList<Message> msgs = messageManager.obtenerConversacion(
                    usuario.getUsername(), otro);
                // Marcar como leídos todos los mensajes recibidos del otro usuario
                // Funciona tanto online como offline — actualizarEstadoMensaje reescribe el archivo
                for (Message m : msgs) {
                    if (m.getReceptor().equals(usuario.getUsername())
                            && m.getEstado() != Message.EstadoMensaje.LEIDO) {
                        m.setEstado(Message.EstadoMensaje.LEIDO);
                        try { messageManager.actualizarEstadoMensaje(m); } catch (Exception ignored) {}
                        if (chatClient != null && chatClient.isConnected()) {
                            try { chatClient.marcarComoLeido(m.getId()); } catch (Exception ignored) {}
                        }
                    }
                }
                return msgs;
            }
            @Override protected void done() {
                try {
                    panelMensajes.removeAll();
                    ArrayList<Message> msgs = get();
                    String fechaAnterior = "";
                    for (Message m : msgs) {
                        String fechaHoy = new SimpleDateFormat("dd/MM/yyyy").format(m.getFecha());
                        if (!fechaHoy.equals(fechaAnterior)) {
                            panelMensajes.add(crearSeparadorFecha(fechaHoy));
                            fechaAnterior = fechaHoy;
                        }
                        panelMensajes.add(crearBurbuja(m));
                    }
                    panelMensajes.revalidate();
                    panelMensajes.repaint();
                    scrollAlFinal();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // ═══════════════════════════════════════════════════════════════
    // BURBUJAS DE MENSAJES
    // ═══════════════════════════════════════════════════════════════

    private JPanel crearBurbuja(Message m) {
        boolean mio = m.getEmisor().equals(usuario.getUsername());
        boolean esImgSticker = m.getTipo() == Message.TipoMensaje.STICKER
            && m.getContenido() != null && m.getContenido().startsWith("imgsticker:");
        boolean esEmojiSticker = m.getTipo() == Message.TipoMensaje.STICKER && !esImgSticker;

        // Fila exterior — empuja la burbuja hacia el lado correcto
        JPanel fila = new JPanel();
        fila.setLayout(new BoxLayout(fila, BoxLayout.X_AXIS));
        fila.setBackground(new Color(250, 250, 250));
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.setMaximumSize(new Dimension(390, Integer.MAX_VALUE));
        fila.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 8));

        // Avatar del emisor (solo mensajes recibidos)
        if (!mio) {
            ComponenteCircular av = new ComponenteCircular(28);
            av.setPreferredSize(new Dimension(28, 28));
            av.setMinimumSize(new Dimension(28, 28));
            av.setMaximumSize(new Dimension(28, 28));
            cargarFotoPerfil(av, m.getEmisor());
            fila.add(av);
            fila.add(Box.createHorizontalStrut(6));
        }

        // Contenido de la burbuja
        JComponent contenidoBurbuja;

        if (esImgSticker) {
            // Sticker de imagen — cargar desde la ruta guardada en el contenido
            String ruta = m.getContenido().substring("imgsticker:".length());
            JLabel imgLbl = new JLabel();
            imgLbl.setPreferredSize(new Dimension(100, 100));
            java.awt.image.BufferedImage imgStickerBi = null;
            try {
                File imgFile = new File(ruta);
                if (imgFile.exists()) imgStickerBi = ImageIO.read(imgFile);
            } catch (Exception ignored) {}
            if (imgStickerBi != null)
                imgLbl.setIcon(new ImageIcon(
                    imgStickerBi.getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
            // Click en sticker recibido → guardar
            if (!mio) {
                final java.awt.image.BufferedImage finalBi = imgStickerBi;
                final String finalCode = new File(ruta).getName()
                    .replaceFirst("[.][^.]+$", "");
                imgLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                imgLbl.setToolTipText("Toca para guardar este sticker");
                imgLbl.addMouseListener(click(ev -> {
                    if (finalBi != null) {
                        ofrecerGuardarSticker(finalCode, finalBi);
                    } else {
                        mostrarFeedback("Sticker no disponible",
                            new java.awt.Color(142, 142, 142));
                    }
                }));
            }
            contenidoBurbuja = imgLbl;
        } else if (esEmojiSticker) {
            // Sticker PNG — cargar desde resources o carpetas del usuario
            String code = m.formatearVisualizacion();
            java.awt.image.BufferedImage stkImg =
                StickerMessage.cargarImagen(code, m.getEmisor());
            JLabel stickerLbl;
            if (stkImg != null) {
                Image scaled = stkImg.getScaledInstance(STICKER_SIZE, STICKER_SIZE, Image.SCALE_SMOOTH);
                stickerLbl = new JLabel(new ImageIcon(scaled));
            } else {
                // Fallback: mostrar nombre del sticker
                stickerLbl = new JLabel(code, SwingConstants.CENTER);
                stickerLbl.setFont(new Font("Arial", Font.PLAIN, 11));
                stickerLbl.setBackground(new Color(239,239,239));
                stickerLbl.setOpaque(true);
            }
            stickerLbl.setPreferredSize(new Dimension(STICKER_SIZE, STICKER_SIZE));
            stickerLbl.setMaximumSize(new Dimension(STICKER_SIZE + 4, STICKER_SIZE + 4));
            stickerLbl.setToolTipText(!mio ? "Toca para guardar este sticker" : null);
            stickerLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Click en cualquier sticker recibido → toast inline para guardar
            if (!mio) {
                final String finalCode = code;
                // Cargar imagen desde perspectiva del receptor si no cargó desde el emisor
                java.awt.image.BufferedImage imgParaGuardar = stkImg;
                if (imgParaGuardar == null)
                    imgParaGuardar = StickerMessage.cargarImagen(code, usuario.getUsername());
                if (imgParaGuardar == null) {
                    // Fallback: intentar el code como ruta de archivo directa
                    try {
                        File cf = new File(code);
                        if (cf.exists()) imgParaGuardar = ImageIO.read(cf);
                    } catch (Exception ignored) {}
                }
                final java.awt.image.BufferedImage finalImg = imgParaGuardar;
                stickerLbl.addMouseListener(click(ev -> {
                    if (finalImg != null) {
                        ofrecerGuardarSticker(finalCode, finalImg);
                    } else {
                        mostrarFeedback("No se puede guardar este sticker",
                            new java.awt.Color(142, 142, 142));
                    }
                }));
            }
            contenidoBurbuja = stickerLbl;
        } else {
            // Burbuja de texto dinámica:
            // 1) Medimos el texto con FontMetrics para saber el ancho real
            // 2) Si cabe en una línea → burbuja angosta; si no → wrap a 220px
            String texto = m.formatearVisualizacion();
            final Color bubbleColor = mio ? BURBUJA_MIO : BURBUJA_OTRO;
            final Color textColor   = mio ? TEXTO_BURBUJA_MIO : TEXTO_BURBUJA_OTRO;
            final Font  txFont      = new Font("Arial", Font.PLAIN, 14);
            final int   PAD_X = 12, PAD_Y = 8;
            final int   MAX_W = 230; // ancho máximo del área de texto

            // Medir con un JLabel temporal de ancho fijo para forzar wrap
            JLabel medidor = new JLabel(
                "<html><body style='width:" + MAX_W + "px'>" + texto + "</body></html>");
            medidor.setFont(txFont);
            int textH = medidor.getPreferredSize().height;

            // Medir ancho de una sola línea (sin wrap)
            java.awt.FontMetrics fm =
                medidor.getFontMetrics(txFont);
            int singleLineW = fm.stringWidth(texto);
            int bubbleTextW = Math.min(singleLineW, MAX_W);

            JLabel lblText = new JLabel(
                singleLineW <= MAX_W
                    ? "<html>" + texto + "</html>"
                    : "<html><body style='width:" + MAX_W + "px'>" + texto + "</body></html>");
            lblText.setFont(txFont);
            lblText.setForeground(textColor);
            lblText.setPreferredSize(new Dimension(bubbleTextW, textH));

            final int bW = bubbleTextW + PAD_X * 2;
            final int bH = textH + PAD_Y * 2;

            JPanel bubble = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bubbleColor);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                    g2.dispose();
                }
            };
            bubble.setOpaque(false);
            bubble.setBorder(BorderFactory.createEmptyBorder(PAD_Y, PAD_X, PAD_Y, PAD_X));
            bubble.setPreferredSize(new Dimension(bW, bH));
            bubble.setMaximumSize(new Dimension(bW + 4, bH + 4));
            bubble.add(lblText, BorderLayout.CENTER);
            contenidoBurbuja = bubble;
        }

        // Hora debajo de la burbuja
        JLabel lblHora = new JLabel(new java.text.SimpleDateFormat("HH:mm").format(m.getFecha()));
        lblHora.setFont(new Font("Arial", Font.PLAIN, 10));
        lblHora.setForeground(new Color(142, 142, 142));

        // ── Fila de metadatos: hora + check de estado (solo mensajes propios) ──
        JPanel metaFila = new JPanel();
        metaFila.setLayout(new BoxLayout(metaFila, BoxLayout.X_AXIS));
        metaFila.setBackground(new Color(250, 250, 250));
        metaFila.setOpaque(false);
        metaFila.setAlignmentX(mio ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        if (mio) {
            metaFila.add(Box.createHorizontalGlue());
            metaFila.add(lblHora);
            metaFila.add(Box.createHorizontalStrut(3));
            JLabel check = new JLabel(m.getEstado() == Message.EstadoMensaje.LEIDO ? "✓✓" : "✓");
            check.setFont(new Font("Arial", Font.PLAIN, 10));
            check.setForeground(m.getEstado() == Message.EstadoMensaje.LEIDO
                ? new Color(0, 149, 246)        // azul Instagram = leído
                : new Color(180, 180, 180));    // gris = enviado/entregado
            metaFila.add(check);
        } else {
            metaFila.add(lblHora);
            metaFila.add(Box.createHorizontalGlue());
        }

        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setBackground(new Color(250, 250, 250));
        wrap.setOpaque(false);
        contenidoBurbuja.setAlignmentX(mio ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        wrap.add(contenidoBurbuja);
        wrap.add(metaFila);

        if (mio) {
            fila.add(Box.createHorizontalGlue());
            fila.add(wrap);
        } else {
            fila.add(wrap);
            fila.add(Box.createHorizontalGlue());
        }
        return fila;
    }



    private JPanel crearSeparadorFecha(String fecha) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(new Color(250, 250, 250));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(new EmptyBorder(8, 20, 8, 20));
        p.setMaximumSize(new Dimension(390, 30));

        JSeparator l = new JSeparator(); l.setForeground(new Color(220,220,220));
        JLabel lbl = new JLabel(fecha, SwingConstants.CENTER);
        lbl.setFont(new Font("Arial", Font.PLAIN, 11));
        lbl.setForeground(new Color(142,142,142));
        lbl.setPreferredSize(new Dimension(100, 16));
        JSeparator r = new JSeparator(); r.setForeground(new Color(220,220,220));

        p.add(l, BorderLayout.WEST);
        p.add(lbl, BorderLayout.CENTER);
        p.add(r, BorderLayout.EAST);
        return p;
    }

    // ═══════════════════════════════════════════════════════════════
    // ENVÍO
    // ═══════════════════════════════════════════════════════════════

    private void enviarTexto() {
        String txt = txtMensaje.getText().trim();
        if (txt.isEmpty() || conversacionActiva == null) return;
        if (!puedeEnviarMensaje()) {
            mostrarBloqueoCuenta();
            return;
        }
        TextMessage m = new TextMessage(usuario.getUsername(), conversacionActiva, txt);
        txtMensaje.setText("");
        enviarMensajeInterno(m);
    }

    private void enviarStickerPNG(String code) {
        if (conversacionActiva == null || !puedeEnviarMensaje()) return;
        StickerMessage m = new StickerMessage(usuario.getUsername(), conversacionActiva, code);
        enviarMensajeInterno(m);
        cerrarDrawer();
    }

    private void enviarImagenSticker(String rutaImagen) {
        if (conversacionActiva == null || !puedeEnviarMensaje()) return;
        // Usamos un código especial para indicar sticker de imagen
        TextMessage m = new TextMessage(usuario.getUsername(), conversacionActiva,
            "imgsticker:" + rutaImagen);
        m.setTipo(Message.TipoMensaje.STICKER);
        enviarMensajeInterno(m);
        cerrarDrawer();
    }

    private void enviarMensajeInterno(Message m) {
        boolean online = chatClient != null && chatClient.isConnected();

        if (online) {
            // Modo online: el servidor guarda en ambos inboxes.
            // El cliente NO guarda localmente para evitar duplicados.
            panelMensajes.add(crearBurbuja(m));
            panelMensajes.revalidate();
            panelMensajes.repaint();
            scrollAlFinal();
            new Thread(() -> {
                try { chatClient.enviarMensaje(m); }
                catch (Exception ex) {
                    // Si falla el envío, guardar localmente como fallback
                    try { messageManager.guardarMensaje(m); } catch (Exception ignored) {}
                    SwingUtilities.invokeLater(() ->
                        mostrarFeedback("⚠ Sin conexión", new Color(255, 150, 0)));
                }
            }, "msg-send").start();
        } else {
            // Modo offline: guardar localmente
            try { messageManager.guardarMensaje(m); }
            catch (Exception ex) { return; }
            panelMensajes.add(crearBurbuja(m));
            panelMensajes.revalidate();
            panelMensajes.repaint();
            scrollAlFinal();
        }
    }

    private void mostrarBloqueoCuenta() {
        mostrarAvisoBloqueo("No puedes enviar mensajes a esta cuenta.");
    }

    /**
     * Muestra un aviso de bloqueo inline en el área de mensajes,
     * centrado y en gris, sin ninguna ventana emergente.
     */
    private void mostrarAvisoBloqueo(String mensaje) {
        // Si estamos en la lista, solo mostrar feedback
        if (!enChat) { mostrarFeedback("⚠ " + mensaje, new Color(142, 142, 142)); return; }
        // Si estamos en el chat, insertar un label informativo en el área de mensajes
        JPanel aviso = new JPanel();
        aviso.setLayout(new BoxLayout(aviso, BoxLayout.Y_AXIS));
        aviso.setBackground(new Color(250, 250, 250));
        aviso.setAlignmentX(Component.LEFT_ALIGNMENT);
        aviso.setMaximumSize(new Dimension(390, 60));
        aviso.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        JLabel lbl = new JLabel("<html><center>" + mensaje + "</center></html>",
            SwingConstants.CENTER);
        lbl.setFont(new Font("Arial", Font.PLAIN, 12));
        lbl.setForeground(new Color(142, 142, 142));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        aviso.add(lbl);

        panelMensajes.add(aviso);
        panelMensajes.revalidate();
        panelMensajes.repaint();
        scrollAlFinal();
    }


    // ═══════════════════════════════════════════════════════════════
    // STICKER DRAWER
    // ═══════════════════════════════════════════════════════════════

    private void toggleStickerDrawer() {
        drawerVisible = !drawerVisible;
        stickerDrawer.setVisible(drawerVisible);
        revalidate(); repaint();
    }

    private void cerrarDrawer() {
        drawerVisible = false;
        if (stickerDrawer != null) stickerDrawer.setVisible(false);
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    private void scrollAlFinal() {
        SwingUtilities.invokeLater(() ->
            scrollMensajes.getVerticalScrollBar().setValue(
                scrollMensajes.getVerticalScrollBar().getMaximum()));
    }

    private void cargarFotoPerfil(ComponenteCircular cc, String username, int gen) {
        if (cc == null || username == null || username.isEmpty() || userManager == null) return;
        new SwingWorker<BufferedImage, Void>() {
            @Override protected BufferedImage doInBackground() {
                if (gen != listaGeneracion) return null;
                try {
                    User u = userManager.buscarUsuario(username);
                    if (gen != listaGeneracion) return null;
                    if (u == null) return null;
                    String ruta = u.getRutaFotoPerfil();
                    if (ruta == null) return null;
                    ruta = ruta.replaceAll("[\\x00-\\x1F]", "").trim();
                    if (ruta.isEmpty() || ruta.equals("default.jpg")) return null;
                    BufferedImage cached = imgCache.get(ruta);
                    if (cached != null) return cached;
                    File f = new File(ruta);
                    if (!f.exists() || !f.isFile() || f.length() == 0) return null;
                    if (gen != listaGeneracion) return null;
                    BufferedImage bi = ImageIO.read(f);
                    if (bi != null) imgCache.put(ruta, bi);
                    return bi;
                } catch (Exception e) { return null; }
            }
            @Override protected void done() {
                if (gen != listaGeneracion) return;
                try {
                    BufferedImage bi = get(); if (bi == null) return;
                    SwingUtilities.invokeLater(() -> {
                        if (gen != listaGeneracion || !cc.isShowing()) return;
                        cc.setImagen(bi); cc.repaint();
                    });
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void cargarFotoPerfil(ComponenteCircular cc, String username) {
        if (cc == null || username == null || username.isEmpty() || userManager == null) return;
        new SwingWorker<BufferedImage, Void>() {
            @Override protected BufferedImage doInBackground() {
                try {
                    User u = userManager.buscarUsuario(username);
                    if (u == null) return null;
                    String ruta = u.getRutaFotoPerfil();
                    if (ruta == null) return null;
                    ruta = ruta.replaceAll("[\\x00-\\x1F]", "").trim();
                    if (ruta.isEmpty() || ruta.equals("default.jpg")) return null;
                    BufferedImage cached = imgCache.get(ruta);
                    if (cached != null) return cached;
                    File f = new File(ruta);
                    if (!f.exists() || !f.isFile() || f.length() == 0) return null;
                    BufferedImage bi = ImageIO.read(f);
                    if (bi != null) imgCache.put(ruta, bi);
                    return bi;
                } catch (Exception e) { return null; }
            }
            @Override protected void done() {
                try {
                    BufferedImage bi = get(); if (bi == null) return;
                    SwingUtilities.invokeLater(() -> { cc.setImagen(bi); cc.repaint(); });
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    /** Inicia polling cada 3s para detectar cuando el otro lee los mensajes */
    private void iniciarTimerVisto(String otroUsername) {
        detenerTimerVisto();
        timerVisto = new javax.swing.Timer(3000, e -> {
            if (!enChat || !otroUsername.equals(conversacionActiva)) {
                detenerTimerVisto(); return;
            }
            // Releer mensajes del chat y actualizar checkmarks si cambió el estado
            new SwingWorker<ArrayList<Message>, Void>() {
                @Override protected ArrayList<Message> doInBackground() throws Exception {
                    return messageManager.obtenerConversacion(usuario.getUsername(), otroUsername);
                }
                @Override protected void done() {
                    try {
                        ArrayList<Message> msgs = get();
                        boolean hayLeido = msgs.stream().anyMatch(m ->
                            m.getEmisor().equals(usuario.getUsername()) &&
                            m.getEstado() == Message.EstadoMensaje.LEIDO);
                        if (hayLeido) actualizarCheckmarksEnPantalla();
                    } catch (Exception ignored) {}
                }
            }.execute();
        });
        timerVisto.start();
    }

    private void detenerTimerVisto() {
        if (timerVisto != null) { timerVisto.stop(); timerVisto = null; }
    }

    /** Actualiza todos los ✓/✓✓ en pantalla según el estado guardado */
    private void actualizarCheckmarksEnPantalla() {
        // Recargar mensajes del chat para refrescar los checks
        if (enChat && conversacionActiva != null) {
            cargarMensajes(conversacionActiva);
        }
    }

    private MouseAdapter click(java.util.function.Consumer<MouseEvent> fn) {
        return new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { fn.accept(e); }
        };
    }

    // ═══════════════════════════════════════════════════════════════
    // CHAT CLIENT LISTENER
    // ═══════════════════════════════════════════════════════════════

    @Override public void onMessageReceived(Message m) {
        SwingUtilities.invokeLater(() -> {
            if (enChat && m.getEmisor().equals(conversacionActiva)) {
                panelMensajes.add(crearBurbuja(m));
                panelMensajes.revalidate();
                panelMensajes.repaint();
                scrollAlFinal();
            }
            cargarConversaciones();
        });
    }

    @Override public void onConversationsReceived(ArrayList<String> convs) {
        SwingUtilities.invokeLater(() -> renderConversaciones(convs));
    }

    @Override public void onConnectionStatusChanged(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            if (connected) {
                cargarConversaciones();
                // Re-evaluar estado online del chat activo
                if (enChat && conversacionActiva != null) {
                    lblChatEstado.setText(connected ? "en línea" : "");
                }
            } else {
                if (enChat) lblChatEstado.setText("");
            }
        });
    }

    @Override public void onNotificationReceived(String tipo, Object datos) {
        if ("USER_ONLINE".equals(tipo) || "USER_OFFLINE".equals(tipo)) {
            String user = (String) datos;
            // Si estamos en el chat con ese usuario, actualizar el estado
            if (enChat && user != null && user.equals(conversacionActiva)) {
                SwingUtilities.invokeLater(() ->
                    lblChatEstado.setText("USER_ONLINE".equals(tipo) ? "en línea" : ""));
            }
        }
    }
    @Override public void onError(String error) {}
    // ── mostrarFeedback — muestra texto temporal en el footer del chat ──

    private javax.swing.Timer feedbackTimer;

    private void mostrarFeedback(String texto, java.awt.Color color) {
        // Insertar un label de feedback inline al final del área de mensajes
        JPanel fb = new JPanel(new BorderLayout());
        fb.setBackground(new java.awt.Color(250, 250, 250));
        fb.setAlignmentX(Component.LEFT_ALIGNMENT);
        fb.setMaximumSize(new Dimension(390, 28));
        fb.setPreferredSize(new Dimension(390, 28));
        fb.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16));

        JLabel lbl = new JLabel(texto, SwingConstants.CENTER);
        lbl.setFont(new Font("Arial", Font.PLAIN, 12));
        lbl.setForeground(color);
        fb.add(lbl, BorderLayout.CENTER);

        panelMensajes.add(fb);
        panelMensajes.revalidate();
        panelMensajes.repaint();
        scrollAlFinal();

        // Auto-eliminar después de 3 segundos
        if (feedbackTimer != null) feedbackTimer.stop();
        feedbackTimer = new javax.swing.Timer(3000, e -> {
            panelMensajes.remove(fb);
            panelMensajes.revalidate();
            panelMensajes.repaint();
        });
        feedbackTimer.setRepeats(false);
        feedbackTimer.start();
    }

    // ─────────────────────────────────────────────────────────────────
    // ofrecerGuardarSticker
    // Muestra un panel tipo drawer (igual que el picker de stickers)
    // con preview de la imagen, botón Guardar y Cancelar.
    // Verifica duplicados antes de guardar.
    // ─────────────────────────────────────────────────────────────────

    private JPanel saveDrawer = null; // referencia al drawer activo

    private void ofrecerGuardarSticker(String code, java.awt.image.BufferedImage img) {
        if (img == null) {
            mostrarFeedback("Sticker no disponible", new java.awt.Color(142, 142, 142));
            return;
        }
        // Cerrar cualquier drawer de guardado previo
        if (saveDrawer != null) {
            panelSur.remove(saveDrawer);
            saveDrawer = null;
            panelSur.revalidate();
            panelSur.repaint();
        }

        // ── Drawer de confirmación ─────────────────────────────
        JPanel drawer = new JPanel(null);
        drawer.setBackground(Color.WHITE);
        drawer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SEPARADOR));
        drawer.setPreferredSize(new Dimension(390, 100));
        drawer.setMaximumSize(new Dimension(390, 100));
        drawer.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveDrawer = drawer;

        // Preview del sticker
        Image preview = img.getScaledInstance(72, 72, Image.SCALE_SMOOTH);
        JLabel lblPreview = new JLabel(new ImageIcon(preview));
        lblPreview.setBounds(14, 14, 72, 72);
        drawer.add(lblPreview);

        // Texto informativo
        String sanitized = code.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (sanitized.isEmpty()) sanitized = "sticker";
        String dir = "INSTA_RAIZ/" + usuario.getUsername() + "/stickers_personales";
        boolean yaExiste = new File(dir + "/" + sanitized + ".png").exists();

        JLabel lblTitulo = new JLabel(yaExiste ? "Ya tienes este sticker" : "Guardar en mis stickers");
        lblTitulo.setBounds(100, 18, 200, 18);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 13));
        lblTitulo.setForeground(new java.awt.Color(38, 38, 38));
        drawer.add(lblTitulo);

        JLabel lblNombre = new JLabel(sanitized + ".png");
        lblNombre.setBounds(100, 38, 200, 15);
        lblNombre.setFont(new Font("Arial", Font.PLAIN, 11));
        lblNombre.setForeground(new java.awt.Color(142, 142, 142));
        drawer.add(lblNombre);

        // Botón Guardar (solo si no es duplicado)
        JLabel btnGuardar = new JLabel(yaExiste ? "Ya guardado" : "Guardar");
        btnGuardar.setBounds(100, 62, 90, 22);
        btnGuardar.setFont(new Font("Arial", Font.BOLD, 13));
        btnGuardar.setForeground(yaExiste
            ? new java.awt.Color(142, 142, 142)
            : InstagramColors.INSTAGRAM_AZUL);
        if (!yaExiste) {
            btnGuardar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            final String finalNombre = sanitized;
            btnGuardar.addMouseListener(click(e -> {
                panelSur.remove(saveDrawer);
                saveDrawer = null;
                panelSur.revalidate();
                panelSur.repaint();
                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() throws Exception {
                        new File(dir).mkdirs();
                        File dest = new File(dir + "/" + finalNombre + ".png");
                        // Verificación final de duplicado (thread-safe)
                        if (dest.exists())
                            throw new Exception("Ya tienes este sticker");
                        boolean ok = javax.imageio.ImageIO.write(img, "PNG", dest);
                        if (!ok) throw new Exception("No se pudo guardar la imagen");
                        return null;
                    }
                    @Override protected void done() {
                        try {
                            get();
                            if (stickerGrid != null) cargarStickersEnGrid(stickerGrid);
                            mostrarFeedback("Sticker guardado ✓",
                                new java.awt.Color(34, 197, 94));
                        } catch (Exception ex) {
                            mostrarFeedback(ex.getMessage().contains("Ya tienes")
                                ? "Ya tienes este sticker"
                                : "Error al guardar",
                                new java.awt.Color(220, 50, 50));
                        }
                    }
                }.execute();
            }));
        }
        drawer.add(btnGuardar);

        // Botón Cancelar
        JLabel btnCancelar = new JLabel("Cancelar");
        btnCancelar.setBounds(200, 62, 70, 22);
        btnCancelar.setFont(new Font("Arial", Font.PLAIN, 13));
        btnCancelar.setForeground(new java.awt.Color(142, 142, 142));
        btnCancelar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancelar.addMouseListener(click(e -> {
            panelSur.remove(saveDrawer);
            saveDrawer = null;
            panelSur.revalidate();
            panelSur.repaint();
        }));
        drawer.add(btnCancelar);

        // Insertar en panelSur ENCIMA de la barra de entrada (primer posición)
        panelSur.add(drawer, 0);
        panelSur.revalidate();
        panelSur.repaint();
    }


    /**
     * WrapLayout — FlowLayout que calcula su preferred height considerando
     * el ancho del contenedor, permitiendo scroll vertical correcto.
     */
    private static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }
        @Override public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }
        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);
                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0, rowHeight = 0;
                int nmembers = target.getComponentCount();
                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        if (rowWidth + d.width > maxWidth) {
                            dim.width = Math.max(dim.width, rowWidth);
                            dim.height += rowHeight + vgap;
                            rowWidth = 0; rowHeight = 0;
                        }
                        if (rowWidth != 0) rowWidth += hgap;
                        rowWidth += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }
                dim.width  = Math.max(dim.width, rowWidth);
                dim.height += rowHeight + insets.top + insets.bottom + vgap * 2;
                return dim;
            }
        }
    }

}