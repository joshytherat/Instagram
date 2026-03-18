package instagram.GUI;

import instagram.Utilities.FollowManager;
import instagram.Utilities.Post;
import instagram.Utilities.PostManager;
import instagram.Utilities.User;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class PanelHome extends JPanel {

    private User          usuario;
    private PostManager   postManager;
    private FollowManager followManager;
    private MainFrame     mainFrame;

    private JPanel      panelContenido;
    private JScrollPane scroll;
    private JLabel      lblBanner;

    public PanelHome(User usuario, PostManager postManager,
                     FollowManager followManager, MainFrame mainFrame) {
        this.usuario       = usuario;
        this.postManager   = postManager;
        this.followManager = followManager;
        this.mainFrame     = mainFrame;

        setLayout(new BorderLayout());
        setBackground(new Color(250, 250, 250));
        construirUI();
    }

    private void construirUI() {
        lblBanner = new JLabel("✓ ¡Tu cuenta ha sido reactivada!", SwingConstants.CENTER);
        lblBanner.setFont(new Font("Arial", Font.BOLD, 12));
        lblBanner.setForeground(Color.WHITE);
        lblBanner.setBackground(InstagramColors.VERDE_EXITO);
        lblBanner.setOpaque(true);
        lblBanner.setPreferredSize(new Dimension(390, 34));
        lblBanner.setVisible(false);
        add(lblBanner, BorderLayout.NORTH);

        panelContenido = new JPanel();
        panelContenido.setLayout(new BoxLayout(panelContenido, BoxLayout.Y_AXIS));
        panelContenido.setBackground(new Color(250, 250, 250));

        scroll = new JScrollPane(panelContenido,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(22);
        add(scroll, BorderLayout.CENTER);
    }

    // ── Feed pre-cargado desde VentanaLogin ──────────────────────

    /**
     * Recibe el feed ya cargado (pre-fetched en VentanaLogin).
     * Se llama desde MainFrame justo al construir, antes del primer
     * refrescarFeed(), así el usuario ve contenido inmediatamente.
     */
    public void mostrarFeedPreCargado(ArrayList<Post> posts) {
        renderFeed(posts);
    }

    // ── Carga normal (llamada cuando el usuario navega a HOME) ────

    public void refrescarFeed() {
        panelContenido.removeAll();

        JLabel cargando = new JLabel("Cargando...", SwingConstants.CENTER);
        cargando.setFont(new Font("Arial", Font.PLAIN, 13));
        cargando.setForeground(InstagramColors.TEXTO_GRIS);
        cargando.setAlignmentX(Component.CENTER_ALIGNMENT);
        cargando.setBorder(BorderFactory.createEmptyBorder(60, 0, 0, 0));
        panelContenido.add(cargando);
        panelContenido.revalidate();

        new SwingWorker<ArrayList<Post>, Void>() {
            @Override protected ArrayList<Post> doInBackground() throws Exception {
                ArrayList<String> siguiendo =
                    followManager.obtenerFollowing(usuario.getUsername());
                ArrayList<Post> feed = new ArrayList<>();
                // Include own posts
                feed.addAll(postManager.obtenerPostsDeUsuario(usuario.getUsername()));
                for (String username : siguiendo)
                    feed.addAll(postManager.obtenerPostsDeUsuario(username));
                feed.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
                return feed;
            }
            @Override protected void done() {
                try {
                    renderFeed(get());
                } catch (Exception ex) {
                    panelContenido.removeAll();
                    JLabel err = new JLabel("Error cargando el feed", SwingConstants.CENTER);
                    err.setFont(new Font("Arial", Font.PLAIN, 13));
                    err.setForeground(InstagramColors.TEXTO_GRIS);
                    err.setAlignmentX(Component.CENTER_ALIGNMENT);
                    panelContenido.add(Box.createRigidArea(new Dimension(0, 80)));
                    panelContenido.add(err);
                    panelContenido.revalidate();
                }
            }
        }.execute();
    }

    // ── Render ────────────────────────────────────────────────────

    public void renderFeed(ArrayList<Post> posts) {
        panelContenido.removeAll();

        if (posts.isEmpty()) {
            JPanel panelVacio = new JPanel();
            panelVacio.setLayout(new BoxLayout(panelVacio, BoxLayout.Y_AXIS));
            panelVacio.setBackground(new Color(250, 250, 250));
            panelVacio.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel icono = new JLabel("📷", SwingConstants.CENTER);
            icono.setFont(new Font("Arial", Font.PLAIN, 48));
            icono.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel msg = new JLabel(
                "<html><center>Aún no hay publicaciones.<br>" +
                "Sigue a otras personas para ver su contenido.</center></html>",
                SwingConstants.CENTER);
            msg.setFont(new Font("Arial", Font.PLAIN, 14));
            msg.setForeground(InstagramColors.TEXTO_GRIS);
            msg.setAlignmentX(Component.CENTER_ALIGNMENT);
            msg.setBorder(BorderFactory.createEmptyBorder(12, 30, 0, 30));

            panelVacio.add(Box.createRigidArea(new Dimension(0, 80)));
            panelVacio.add(icono);
            panelVacio.add(msg);
            panelContenido.add(panelVacio);
        } else {
            for (Post p : posts) {
                ComponentePost cp = new ComponentePost(
                    p, usuario, postManager,
                    followManager, mainFrame.userManager, mainFrame);
                cp.setAlignmentX(Component.LEFT_ALIGNMENT);
                panelContenido.add(cp);
                panelContenido.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }

        panelContenido.revalidate();
        panelContenido.repaint();
        SwingUtilities.invokeLater(() ->
            scroll.getVerticalScrollBar().setValue(0));
    }

    public void mostrarBannerReactivado() {
        lblBanner.setVisible(true);
        Timer t = new Timer(4000, e -> lblBanner.setVisible(false));
        t.setRepeats(false);
        t.start();
    }
}