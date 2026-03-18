package instagram.GUI;

import instagram.Enums.TipoNotificacion;
import instagram.Utilities.ChatClient;
import instagram.Utilities.Comment;
import instagram.Utilities.FollowManager;
import instagram.Utilities.NotificationManager;
import instagram.Utilities.Post;
import instagram.Utilities.PostManager;
import instagram.Utilities.User;
import instagram.Utilities.UserManager;
import instagram.Utilities.ValidadorContenido;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Vista detallada de un post: imagen + comentarios scrollables */
public class PanelVerPost extends JPanel {

    private final Post          post;
    private final User          usuario;
    private final PostManager   postManager;
    private final FollowManager followManager;
    private final UserManager   userManager;
    private final ChatClient    chatClient;
    private final MainFrame     mainFrame;

    // Cache compartido con PanelInbox para no releer archivos ya cargados
    private static final Map<String, BufferedImage> imgCache =
        new ConcurrentHashMap<>();

    // AtomicBoolean: cuando el usuario hace back se pone false.
    // Todos los SwingWorker lo verifican antes de tocar la UI.
    private final AtomicBoolean activo = new AtomicBoolean(true);

    private JPanel     panelComentarios;
    private JTextField txtComentario;
    private JLabel     lblLikesCount;
    private JButton    btnLike;
    private boolean    tienelike;

    public PanelVerPost(Post post, User usuario, PostManager postManager,
                        FollowManager followManager, UserManager userManager,
                        ChatClient chatClient, MainFrame mainFrame) {
        this.post          = post;
        this.usuario       = usuario;
        this.postManager   = postManager;
        this.followManager = followManager;
        this.userManager   = userManager;
        this.chatClient    = chatClient;
        this.mainFrame     = mainFrame;
        this.tienelike     = post.tienelike(usuario.getUsername());

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        construirUI();
    }

    /** MainFrame llama esto al hacer back — cancela todos los workers de fotos */
    public void destroy() {
        activo.set(false);
    }

    // ─────────────────────────────────────────────────────────────
    // Construcción UI
    // ─────────────────────────────────────────────────────────────

    private void construirUI() {
        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBackground(Color.WHITE);

        contenido.add(crearHeaderPost());

        if (post.getRutaImagen() != null && !post.getRutaImagen().isEmpty()) {
            int h = calcularAlturaImg();
            JLabel img = cargarImagenPost(post.getRutaImagen(), 390, h);
            img.setMaximumSize(new Dimension(390, h));
            img.setPreferredSize(new Dimension(390, h));
            img.setAlignmentX(Component.LEFT_ALIGNMENT);
            contenido.add(img);
        }

        contenido.add(crearPanelBotones());

        lblLikesCount = new JLabel(formatLikes(post.contarLikes()));
        lblLikesCount.setFont(new Font("Arial", Font.BOLD, 13));
        lblLikesCount.setForeground(InstagramColors.TEXTO_NEGRO);
        lblLikesCount.setBorder(BorderFactory.createEmptyBorder(2, 14, 6, 14));
        lblLikesCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        contenido.add(lblLikesCount);

        // Descripción — hashtags y menciones ya vienen dentro del editor
        if (post.getContenido() != null && !post.getContenido().isEmpty()) {
            JEditorPane desc = construirEditor(post.getUsername(), post.getContenido());
            desc.setBorder(BorderFactory.createEmptyBorder(2, 14, 6, 14));
            desc.setAlignmentX(Component.LEFT_ALIGNMENT);
            contenido.add(desc);
        }

        JLabel lblFecha = new JLabel(
            ValidadorContenido.formatearFechaRelativa(post.getFecha()).toUpperCase());
        lblFecha.setFont(new Font("Arial", Font.PLAIN, 10));
        lblFecha.setForeground(InstagramColors.TEXTO_GRIS);
        lblFecha.setBorder(BorderFactory.createEmptyBorder(0, 14, 10, 14));
        lblFecha.setAlignmentX(Component.LEFT_ALIGNMENT);
        contenido.add(lblFecha);

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(390, 1));
        sep.setForeground(InstagramColors.BORDE_GRIS_CLARO);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        contenido.add(sep);
        contenido.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel lblTit = new JLabel("Comentarios");
        lblTit.setFont(new Font("Arial", Font.BOLD, 14));
        lblTit.setForeground(InstagramColors.TEXTO_NEGRO);
        lblTit.setBorder(BorderFactory.createEmptyBorder(0, 14, 8, 14));
        lblTit.setAlignmentX(Component.LEFT_ALIGNMENT);
        contenido.add(lblTit);

        panelComentarios = new JPanel();
        panelComentarios.setLayout(new BoxLayout(panelComentarios, BoxLayout.Y_AXIS));
        panelComentarios.setBackground(Color.WHITE);
        panelComentarios.setAlignmentX(Component.LEFT_ALIGNMENT);
        contenido.add(panelComentarios);

        JScrollPane scroll = new JScrollPane(contenido,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        add(crearCajaComentario(), BorderLayout.SOUTH);
        cargarComentarios();
    }

    // ─────────────────────────────────────────────────────────────
    // Header
    // ─────────────────────────────────────────────────────────────

    private JPanel crearHeaderPost() {
        JPanel h = new JPanel(null);
        h.setPreferredSize(new Dimension(390, 56));
        h.setMaximumSize(new Dimension(390, 56));
        h.setBackground(Color.WHITE);

        ComponenteCircular fp = new ComponenteCircular(36);
        fp.setBounds(12, 10, 36, 36);
        fp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fp.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                mainFrame.verPerfilUsuario(post.getUsername());
            }
        });
        h.add(fp);
        cargarFoto(fp, post.getUsername());

        JLabel lblU = new JLabel(post.getUsername());
        lblU.setBounds(58, 10, 280, 18);
        lblU.setFont(new Font("Arial", Font.BOLD, 14));
        lblU.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblU.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                mainFrame.verPerfilUsuario(post.getUsername());
            }
        });
        h.add(lblU);

        JLabel lblH = new JLabel(post.getTiempoTranscurrido());
        lblH.setBounds(58, 30, 200, 14);
        lblH.setFont(new Font("Arial", Font.PLAIN, 11));
        lblH.setForeground(InstagramColors.TEXTO_GRIS);
        h.add(lblH);

        return h;
    }

    // ─────────────────────────────────────────────────────────────
    // Botones
    // ─────────────────────────────────────────────────────────────

    private JPanel crearPanelBotones() {
        JPanel p = new JPanel(null);
        p.setPreferredSize(new Dimension(390, 44));
        p.setMaximumSize(new Dimension(390, 44));
        p.setBackground(Color.WHITE);

        btnLike = InstagramIcons.boton("like.png", 26, tienelike ? "❤️" : "🤍");
        btnLike.setBounds(12, 9, 34, 34);
        if (tienelike) colorearLike();
        btnLike.addActionListener(e -> toggleLike());
        p.add(btnLike);

        JButton btnComent = InstagramIcons.boton("comentar.png", 26, "💬");
        btnComent.setBounds(54, 9, 34, 34);
        btnComent.addActionListener(e -> txtComentario.requestFocus());
        p.add(btnComent);

        return p;
    }

    // ─────────────────────────────────────────────────────────────
    // Caja comentario
    // ─────────────────────────────────────────────────────────────

    private JPanel crearCajaComentario() {
        JPanel p = new JPanel(null);
        p.setPreferredSize(new Dimension(390, 60));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, InstagramColors.BORDE_GRIS));

        ComponenteCircular av = new ComponenteCircular(32);
        av.setBounds(10, 14, 32, 32);
        p.add(av);
        cargarFoto(av, usuario.getUsername());

        txtComentario = new JTextField("Agrega un comentario...");
        txtComentario.setBounds(52, 14, 278, 34);
        txtComentario.setFont(new Font("Arial", Font.PLAIN, 13));
        txtComentario.setForeground(InstagramColors.TEXTO_GRIS);
        txtComentario.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        txtComentario.setBackground(Color.WHITE);
        txtComentario.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (txtComentario.getText().equals("Agrega un comentario...")) {
                    txtComentario.setText("");
                    txtComentario.setForeground(InstagramColors.TEXTO_NEGRO);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (txtComentario.getText().isEmpty()) {
                    txtComentario.setText("Agrega un comentario...");
                    txtComentario.setForeground(InstagramColors.TEXTO_GRIS);
                }
            }
        });
        txtComentario.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) publicarComentario();
            }
        });
        txtComentario.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (txtComentario.getText().equals("Agrega un comentario...")) {
                    txtComentario.setText("");
                    txtComentario.setForeground(InstagramColors.TEXTO_NEGRO);
                }
                txtComentario.requestFocusInWindow();
            }
        });
        p.add(txtComentario);

        JButton btnPublicar = new JButton("Publicar");
        btnPublicar.setBounds(338, 18, 44, 26);
        btnPublicar.setFont(new Font("Arial", Font.BOLD, 12));
        btnPublicar.setForeground(InstagramColors.INSTAGRAM_AZUL);
        btnPublicar.setBorderPainted(false);
        btnPublicar.setContentAreaFilled(false);
        btnPublicar.setFocusPainted(false);
        btnPublicar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPublicar.addActionListener(e -> publicarComentario());
        p.add(btnPublicar);

        return p;
    }

    // ─────────────────────────────────────────────────────────────
    // Acciones
    // ─────────────────────────────────────────────────────────────

    private void toggleLike() {
        try {
            if (tienelike) {
                postManager.quitarLike(post.getId(), usuario.getUsername(), post.getUsername());
                post.quitarLike(usuario.getUsername());
                tienelike = false;
                ImageIcon icon = InstagramIcons.cargar("like.png", 26, 26);
                if (icon != null) btnLike.setIcon(icon); else btnLike.setText("🤍");
            } else {
                postManager.darLike(post.getId(), usuario.getUsername(), post.getUsername());
                post.darLike(usuario.getUsername());
                tienelike = true;
                colorearLike();
            }
            lblLikesCount.setText(formatLikes(post.contarLikes()));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void colorearLike() {
        ImageIcon likedImg = InstagramIcons.cargar("liked.png", 26, 26);
        if (likedImg != null) { btnLike.setIcon(likedImg); btnLike.setText(""); return; }
        BufferedImage bi = new BufferedImage(26, 26, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ImageIcon orig = InstagramIcons.cargar("like.png", 26, 26);
        if (orig != null) {
            g2.drawImage(orig.getImage(), 0, 0, 26, 26, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN));
            g2.setColor(new Color(237, 73, 86));
            g2.fillRect(0, 0, 26, 26);
        } else {
            g2.setColor(new Color(237, 73, 86));
            g2.fillOval(3, 5, 9, 9); g2.fillOval(14, 5, 9, 9);
            int[] xp = {3, 13, 23}; int[] yp = {10, 24, 10};
            g2.fillPolygon(xp, yp, 3);
        }
        g2.dispose();
        btnLike.setIcon(new ImageIcon(bi)); btnLike.setText("");
    }

    private void publicarComentario() {
        String texto = txtComentario.getText().trim();
        if (texto.isEmpty() || texto.equals("Agrega un comentario...")) return;
        if (texto.length() > 220) return;
        try {
            Comment c = new Comment(post.getId(), usuario.getUsername(), texto);
            postManager.agregarComentario(c);
            post.agregarComentario(c);
            txtComentario.setText("Agrega un comentario...");
            txtComentario.setForeground(InstagramColors.TEXTO_GRIS);
            txtComentario.transferFocus();
            agregarComentarioUI(c);

            // Notificar @menciones en el comentario
            final String textoFinal = texto;
            final String postId = post.getId();
            final String comentador = usuario.getUsername();
            new Thread(() -> {
                try {
                    NotificationManager nm = new NotificationManager();
                    ArrayList<String> menciones = ValidadorContenido.extraerMenciones(textoFinal);
                    for (String m : menciones)
                        if (!m.equalsIgnoreCase(comentador))
                            nm.crearNotificacion(comentador, m, TipoNotificacion.MENTION, postId);
                } catch (Exception ignored) {}
            }).start();

        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ─────────────────────────────────────────────────────────────
    // Comentarios UI
    // ─────────────────────────────────────────────────────────────

    private void cargarComentarios() {
        ArrayList<Comment> lista = post.obtenerComentarios().toArrayList();
        panelComentarios.removeAll();
        for (Comment c : lista) agregarComentarioUI(c);
        panelComentarios.revalidate();
        panelComentarios.repaint();
    }

    private void agregarComentarioUI(Comment c) {
        JPanel item = new JPanel(new BorderLayout(8, 0));
        item.setBackground(Color.WHITE);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        ComponenteCircular av = new ComponenteCircular(28);
        av.setPreferredSize(new Dimension(28, 28));
        cargarFoto(av, c.getUsername());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.add(av);
        item.add(leftPanel, BorderLayout.WEST);

        JEditorPane txtComent = construirEditor(c.getUsername(), c.getContenido());
        item.add(txtComent, BorderLayout.CENTER);

        JLabel likes = new JLabel(c.contarLikes() > 0 ? c.contarLikes() + " ❤" : "");
        likes.setFont(new Font("Arial", Font.PLAIN, 11));
        likes.setForeground(InstagramColors.TEXTO_GRIS);
        likes.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        item.add(likes, BorderLayout.EAST);

        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    c.darLike(usuario.getUsername());
                    likes.setText(c.contarLikes() + " ❤");
                }
            }
        });

        panelComentarios.add(item);
        panelComentarios.revalidate();
        panelComentarios.repaint();
    }

    // ─────────────────────────────────────────────────────────────
    // Carga de foto — método ÚNICO, con AtomicBoolean guard
    // ─────────────────────────────────────────────────────────────

    private void cargarFoto(ComponenteCircular cc, String username) {
        if (username == null || username.isEmpty() || userManager == null) return;
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() {
                if (!activo.get()) return null;
                try {
                    User u = userManager.buscarUsuario(username);
                    if (!activo.get()) return null; // doble check post-busqueda
                    if (u == null) return null;
                    String ruta = u.getRutaFotoPerfil();
                    if (ruta == null) return null;
                    ruta = ruta.replaceAll("[\\x00-\\x1F]", "").trim();
                    if (ruta.isEmpty() || ruta.equals("default.jpg")) return null;
                    // Cache: no releer si ya tenemos la imagen
                    BufferedImage cached = imgCache.get(ruta);
                    if (cached != null) return cached;
                    File f = new File(ruta);
                    if (!f.exists() || !f.isFile() || f.length() == 0) return null;
                    if (!activo.get()) return null;
                    BufferedImage bi = ImageIO.read(f);
                    if (bi != null) imgCache.put(ruta, bi);
                    return bi;
                } catch (Exception e) { return null; }
            }

            @Override
            protected void done() {
                if (!activo.get()) return;
                try {
                    BufferedImage bi = get();
                    if (bi == null) return;
                    SwingUtilities.invokeLater(() -> {
                        if (!activo.get()) return;
                        if (!cc.isShowing()) return; // componente ya no visible
                        cc.setImagen(bi);
                        cc.repaint();
                    });
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private JEditorPane construirEditor(String username, String texto) {
        StringBuilder sb = new StringBuilder(
            "<html><body style='font-family:Arial;font-size:10pt;width:300px'>"
            + "<b>" + username + "</b> ");
        for (String tok : texto.split("\\s+")) {
            if (tok.startsWith("@") && tok.length() > 1) {
                String val = tok.substring(1).replaceAll("[^a-zA-Z0-9_]", "");
                sb.append("<a href='user:").append(val)
                  .append("' style='color:#0095F6;text-decoration:none'>")
                  .append(tok).append("</a> ");
            } else if (tok.startsWith("#") && tok.length() > 1) {
                String val = tok.substring(1).replaceAll("[^a-zA-Z0-9_]", "");
                sb.append("<a href='tag:").append(val)
                  .append("' style='color:#0095F6;text-decoration:none'>")
                  .append(tok).append("</a> ");
            } else {
                sb.append(tok).append(" ");
            }
        }
        sb.append("</body></html>");
        JEditorPane ep = new JEditorPane("text/html", sb.toString());
        ep.setEditable(false);
        ep.setOpaque(false);
        ep.setBackground(Color.WHITE);
        ep.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        ep.setAlignmentX(Component.LEFT_ALIGNMENT);
        ep.addHyperlinkListener(ev -> {
            if (ev.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                String href = ev.getDescription();
                if (href != null && href.startsWith("user:"))
                    mainFrame.verPerfilUsuario(href.substring(5));
                else if (href != null && href.startsWith("tag:"))
                    mainFrame.navegarAHashtag(href.substring(4));
            }
        });
        return ep;
    }

    private JLabel cargarImagenPost(String ruta, int w, int h) {
        try {
            File f = new File(ruta);
            if (f.exists()) {
                BufferedImage bi = ImageIO.read(f);
                if (bi != null)
                    return new JLabel(new ImageIcon(bi.getScaledInstance(w, h, Image.SCALE_SMOOTH)));
            }
        } catch (Exception ignored) {}
        JLabel ph = new JLabel("📷", SwingConstants.CENTER);
        ph.setOpaque(true);
        ph.setBackground(new Color(239, 239, 239));
        ph.setFont(new Font("Arial", Font.PLAIN, 60));
        return ph;
    }

    private int calcularAlturaImg() {
        switch (post.getTipoMultimedia()) {
            case IMAGEN_VERTICAL:   return (int)(390 * 1350.0 / 1080);
            case IMAGEN_HORIZONTAL: return (int)(390 * 566.0  / 1080);
            default:                return 390;
        }
    }

    private String formatLikes(int n) {
        return n == 1 ? "1 Me gusta" : n + " Me gusta";
    }
}