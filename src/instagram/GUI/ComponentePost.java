package instagram.GUI;

import instagram.Utilities.FollowManager;
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

/**
 * Post del feed — layout lineal con BoxLayout, imágenes reales.
 */
public class ComponentePost extends JPanel {

    private final Post        post;
    private final User        usuario;
    private final PostManager postManager;
    private final FollowManager followManager;
    private final UserManager userManager;
    private final MainFrame   mainFrame;

    private JLabel  lblLikes;
    private JButton btnLike;
    private boolean liked;

    public ComponentePost(Post post, User usuario, PostManager postManager,
                          FollowManager followManager, UserManager userManager,
                          MainFrame mainFrame) {
        this.post          = post;
        this.usuario       = usuario;
        this.postManager   = postManager;
        this.followManager = followManager;
        this.userManager   = userManager;
        this.mainFrame     = mainFrame;
        this.liked         = post.tienelike(usuario.getUsername());

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(390, Integer.MAX_VALUE));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                new Color(239, 239, 239)));
        construir();
    }

    private void construir() {
        add(panelHeader());
        if (post.getRutaImagen() != null && !post.getRutaImagen().isEmpty())
            add(panelImagen());
        add(panelAcciones());
        add(panelTexto());
    }

    // ── Header: avatar + @username + tiempo ─────────────────────

    private JPanel panelHeader() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.setBackground(Color.WHITE);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(390, 58));

        ComponenteCircular av = new ComponenteCircular(36);
        cargarFoto(av, post.getUsername());
        av.setPreferredSize(new Dimension(36, 36));
        av.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        av.addMouseListener(click(e -> mainFrame.verPerfilUsuario(post.getUsername())));
        p.add(av);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(Color.WHITE);

        JLabel lblU = new JLabel(post.getUsername());
        lblU.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblU.setForeground(new Color(38, 38, 38));
        lblU.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblU.addMouseListener(click(e -> mainFrame.verPerfilUsuario(post.getUsername())));
        info.add(lblU);

        JLabel lblT = new JLabel(post.getTiempoTranscurrido());
        lblT.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblT.setForeground(new Color(142, 142, 142));
        info.add(lblT);

        p.add(info);
        return p;
    }

    // ── Imagen del post ─────────────────────────────────────────

    private JLabel panelImagen() {
        int imgH = alturaImagen();
        JLabel lbl = new JLabel();
        lbl.setPreferredSize(new Dimension(390, imgH));
        lbl.setMaximumSize(new Dimension(390, imgH));
        lbl.setMinimumSize(new Dimension(390, imgH));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setOpaque(true);
        lbl.setBackground(new Color(245, 245, 245));

        BufferedImage bi = leerImagen(post.getRutaImagen());
        if (bi != null) {
            // Escalar manteniendo relación de aspecto y rellenando el ancho
            int srcW = bi.getWidth(), srcH = bi.getHeight();
            double ratio = (double) srcW / srcH;
            int dstW = 390, dstH = (int)(390 / ratio);
            if (dstH < imgH) dstH = imgH; // no dejar espacios
            Image img = bi.getScaledInstance(dstW, dstH, Image.SCALE_SMOOTH);
            lbl.setIcon(new ImageIcon(img));
        } else {
            lbl.setText("Sin imagen");
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
            lbl.setForeground(new Color(142, 142, 142));
        }

        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.addMouseListener(click(e -> mainFrame.verPost(post)));
        return lbl;
    }

    // ── Botones like + comentar ──────────────────────────────────

    private JPanel panelAcciones() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        p.setBackground(Color.WHITE);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(390, 46));

        // Like
        btnLike = InstagramIcons.botonIcono("like.png", 24, "♡");
        if (liked) setLikedIcon(btnLike);
        btnLike.setPreferredSize(new Dimension(32, 32));
        btnLike.addActionListener(e -> toggleLike());
        p.add(btnLike);

        // Comentar
        JButton btnC = InstagramIcons.botonIcono("comentar.png", 24, "💬");
        btnC.setPreferredSize(new Dimension(32, 32));
        btnC.addActionListener(e -> mainFrame.verPost(post));
        p.add(btnC);

        return p;
    }

    // ── Texto: likes, descripción, hashtags, fecha ───────────────

    private JPanel panelTexto() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));

        // Likes
        lblLikes = new JLabel(formatLikes(post.contarLikes()));
        lblLikes.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblLikes.setForeground(new Color(38, 38, 38));
        lblLikes.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lblLikes);

        // Descripción — JEditorPane para que #hashtags y @menciones sean clickeables
        String desc = post.getContenido();
        if (desc != null && !desc.isEmpty()) {
            JEditorPane epDesc = new JEditorPane("text/html", htmlContenido(post.getUsername(), desc));
            epDesc.setEditable(false);
            epDesc.setOpaque(false);
            epDesc.setBackground(Color.WHITE);
            epDesc.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            epDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
            epDesc.setMaximumSize(new Dimension(366, Integer.MAX_VALUE));
            epDesc.addHyperlinkListener(ev -> {
                if (ev.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                    String href = ev.getDescription();
                    if (href != null && href.startsWith("tag:")) {
                        mainFrame.navegarAHashtag(href.substring(4));
                    } else if (href != null && href.startsWith("user:")) {
                        mainFrame.verPerfilUsuario(href.substring(5));
                    }
                }
            });
            p.add(Box.createVerticalStrut(2));
            p.add(epDesc);
        }

        // Fecha
        JLabel lblFecha = new JLabel(
                ValidadorContenido.formatearFechaRelativa(post.getFecha()).toUpperCase());
        lblFecha.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblFecha.setForeground(new Color(142, 142, 142));
        lblFecha.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lblFecha);

        return p;
    }

    // ── Acciones ─────────────────────────────────────────────────

    private void toggleLike() {
        try {
            if (liked) {
                postManager.quitarLike(post.getId(), usuario.getUsername(), post.getUsername());
                post.quitarLike(usuario.getUsername());
                liked = false;
                // Quitar like → volver a like.png (vacío)
                ImageIcon ic = InstagramIcons.cargar("like.png", 24, 24);
                if (ic != null) btnLike.setIcon(ic); else btnLike.setText("♡");
            } else {
                postManager.darLike(post.getId(), usuario.getUsername(), post.getUsername());
                post.darLike(usuario.getUsername());
                liked = true;
                // Dar like → usar liked.png con fallback ❤
                setLikedIcon(btnLike);
            }
            lblLikes.setText(formatLikes(post.contarLikes()));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    /** Cambia el ícono al estado liked.
     *  1) Intenta liked.png  2) Intenta like.png y lo tine rojo  3) Fallback ❤ texto */
    private void setLikedIcon(JButton btn) {
        // 1) liked.png directo (corazón lleno del usuario)
        ImageIcon likedImg = InstagramIcons.cargar("liked.png", 24, 24);
        if (likedImg != null) { btn.setIcon(likedImg); btn.setText(""); return; }

        // 2) Generar corazón rojo programáticamente — siempre funciona
        BufferedImage bi = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Si hay like.png, cargarlo y teñirlo; si no, dibujar corazón geométrico
        ImageIcon orig = InstagramIcons.cargar("like.png", 24, 24);
        if (orig != null) {
            g2.drawImage(orig.getImage(), 0, 0, 24, 24, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN));
            g2.setColor(new Color(237, 73, 86));
            g2.fillRect(0, 0, 24, 24);
        } else {
            // Corazón geométrico simple
            g2.setColor(new Color(237, 73, 86));
            g2.fillOval(3, 4, 8, 8);
            g2.fillOval(13, 4, 8, 8);
            int[] xp = {3, 12, 21};
            int[] yp = {9, 22, 9};
            g2.fillPolygon(xp, yp, 3);
        }
        g2.dispose();
        btn.setIcon(new ImageIcon(bi));
        btn.setText("");
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String htmlContenido(String uname, String texto) {
        StringBuilder sb = new StringBuilder("<html><body><b>")
                .append(uname).append("</b> ");
        for (String tok : texto.split("\\s+")) {
            if (tok.startsWith("#") || tok.startsWith("@")) {
                // hyperlink real: href="tag:VALOR" o href="user:VALOR"
                String tipo = tok.startsWith("#") ? "tag" : "user";
                String valor = tok.substring(1).replaceAll("[^a-zA-Z0-9_]", "");
                sb.append("<a href='").append(tipo).append(":").append(valor)
                  .append("' style='color:#0095F6;text-decoration:none'>")
                  .append(tok).append("</a> ");
            } else {
                sb.append(tok).append(" ");
            }
        }
        return sb.append("</body></html>").toString();
    }

    private BufferedImage leerImagen(String ruta) {
        try {
            File f = new File(ruta);
            if (f.exists()) return ImageIO.read(f);
        } catch (Exception ignored) {}
        return null;
    }

    private void cargarFoto(ComponenteCircular cc, String uname) {
        try {
            User u = userManager != null ? userManager.buscarUsuario(uname) : null;
            if (u != null && u.getRutaFotoPerfil() != null
                    && !u.getRutaFotoPerfil().isEmpty()
                    && !u.getRutaFotoPerfil().equals("default.jpg")) {
                File f = new File(u.getRutaFotoPerfil());
                if (f.exists()) cc.setImagen(ImageIO.read(f));
            }
        } catch (Exception ignored) {}
    }

    private int alturaImagen() {
        if (post.getTipoMultimedia() == null) return 390;
        switch (post.getTipoMultimedia()) {
            case IMAGEN_VERTICAL:   return (int)(390 * 1350.0 / 1080);
            case IMAGEN_HORIZONTAL: return (int)(390 * 566.0  / 1080);
            default:                return 390;
        }
    }

    private String formatLikes(int n) {
        return n == 1 ? "1 Me gusta" : n + " Me gusta";
    }

    private MouseAdapter click(java.util.function.Consumer<MouseEvent> fn) {
        return new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { fn.accept(e); }
        };
    }
}