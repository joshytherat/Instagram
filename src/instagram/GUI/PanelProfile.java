package instagram.GUI;

import instagram.Utilities.FollowManager;
import instagram.Utilities.Post;
import instagram.Utilities.PostManager;
import instagram.Utilities.User;
import instagram.Utilities.UserManager;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

public class PanelProfile extends JPanel {

    private User          usuario;
    private UserManager   userManager;
    private PostManager   postManager;
    private FollowManager followManager;
    private MainFrame     mainFrame;

    private ComponenteCircular fotoCirculo;
    private JLabel  lblNombreCompleto;
    private JLabel  lblBio;
    private JLabel  lblPosts;
    private JLabel  lblFollowers;
    private JLabel  lblFollowing;
    private JPanel  gridPanel;

    public PanelProfile(User usuario, UserManager userManager, PostManager postManager,
                        FollowManager followManager, MainFrame mainFrame) {
        this.usuario       = usuario;
        this.userManager   = userManager;
        this.postManager   = postManager;
        this.followManager = followManager;
        this.mainFrame     = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        construir();
    }

    private void construir() {
        JPanel todo = new JPanel() {
            @Override public Dimension getPreferredSize() {
                return new Dimension(390, super.getPreferredSize().height);
            }
        };
        todo.setLayout(new BoxLayout(todo, BoxLayout.Y_AXIS));
        todo.setBackground(Color.WHITE);

        // ── Username ──────────────────────────────────────────
        JPanel rowUsername = new JPanel(new BorderLayout());
        rowUsername.setBackground(Color.WHITE);
        rowUsername.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 16));
        rowUsername.setMaximumSize(new Dimension(390, 44));
        rowUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblAt = new JLabel(usuario.getUsername());
        lblAt.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblAt.setForeground(new Color(38, 38, 38));
        rowUsername.add(lblAt, BorderLayout.WEST);
        todo.add(rowUsername);

        // ── Foto + estadísticas ───────────────────────────────
        JPanel rowInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 8));
        rowInfo.setBackground(Color.WHITE);
        rowInfo.setMaximumSize(new Dimension(390, 104));
        rowInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        fotoCirculo = new ComponenteCircular(80);
        fotoCirculo.setPreferredSize(new Dimension(80, 80));
        rowInfo.add(fotoCirculo);
        JPanel stats = new JPanel(new GridLayout(1, 3, 0, 0));
        stats.setBackground(Color.WHITE);
        stats.setPreferredSize(new Dimension(240, 60));
        lblPosts     = statCol("0", "publicaciones");
        lblFollowers = statCol("0", "seguidores");
        lblFollowing = statCol("0", "seguidos");

        // Seguidores y seguidos clickeables
        lblFollowers.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblFollowers.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                mainFrame.abrirSeguidores(true);
            }
        });
        lblFollowing.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblFollowing.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                mainFrame.abrirSeguidores(false);
            }
        });

        stats.add(lblPosts); stats.add(lblFollowers); stats.add(lblFollowing);
        rowInfo.add(stats);
        todo.add(rowInfo);

        // ── Nombre completo + bio ─────────────────────────────
        // Usamos null layout con posicionamiento explícito para garantizar
        // que el nombre esté exactamente a la izquierda sin ambigüedad de BoxLayout.
        JPanel rowBio = new JPanel(null);
        rowBio.setBackground(Color.WHITE);
        rowBio.setPreferredSize(new Dimension(390, 58));
        rowBio.setMaximumSize(new Dimension(390, 80));
        rowBio.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblNombreCompleto = new JLabel(usuario.getNombreCompleto());
        lblNombreCompleto.setBounds(16, 2, 358, 20);
        lblNombreCompleto.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblNombreCompleto.setHorizontalAlignment(SwingConstants.LEFT);
        rowBio.add(lblNombreCompleto);

        lblBio = new JLabel("<html><p style='width:355px'>"
            + (usuario.getBiografia() != null ? usuario.getBiografia() : "")
            + "</p></html>");
        lblBio.setBounds(16, 24, 358, 32);
        lblBio.setFont(new Font("SansSerif", Font.PLAIN, 13));
        rowBio.add(lblBio);
        todo.add(rowBio);

        // ── [  Editar perfil  ─────────────] [☰] ─────────────
        JPanel rowBtns = new JPanel(null);
        rowBtns.setBackground(Color.WHITE);
        rowBtns.setPreferredSize(new Dimension(390, 46));
        rowBtns.setMaximumSize(new Dimension(390, 46));
        rowBtns.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnEdit = new JButton("Editar perfil");
        btnEdit.setBounds(14, 8, 322, 30);
        btnEdit.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnEdit.setBackground(new Color(239, 239, 239));
        btnEdit.setForeground(new Color(38, 38, 38));
        btnEdit.setBorderPainted(false);
        btnEdit.setFocusPainted(false);
        btnEdit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEdit.addActionListener(e -> mainFrame.navegarA("EDITAR_PERFIL"));
        rowBtns.add(btnEdit);

        JLabel btnCfg = new JLabel("☰");
        btnCfg.setBounds(342, 8, 34, 30);
        btnCfg.setFont(new Font("SansSerif", Font.PLAIN, 20));
        btnCfg.setHorizontalAlignment(SwingConstants.CENTER);
        btnCfg.setBackground(new Color(239, 239, 239));
        btnCfg.setOpaque(true);
        btnCfg.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCfg.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { mainFrame.navegarA("SETTINGS"); }
        });
        rowBtns.add(btnCfg);
        todo.add(rowBtns);

        // ── Separador ─────────────────────────────────────────
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(219, 219, 219));
        sep.setMaximumSize(new Dimension(390, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        todo.add(sep);

        // ── Grid posts ────────────────────────────────────────
        gridPanel = new JPanel(new GridLayout(0, 3, 2, 2));
        gridPanel.setBackground(new Color(239, 239, 239));
        gridPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel wrapGrid = new JPanel(new BorderLayout());
        wrapGrid.setBackground(new Color(239, 239, 239));
        wrapGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapGrid.add(gridPanel, BorderLayout.NORTH);
        todo.add(wrapGrid);

        JScrollPane scroll = new JScrollPane(todo,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    public void refrescarPerfil() {
        lblNombreCompleto.setText(usuario.getNombreCompleto());
        lblBio.setText("<html><p style='width:355px'>"
            + (usuario.getBiografia() != null ? usuario.getBiografia() : "")
            + "</p></html>");
        cargarFoto(); cargarEstadisticas(); cargarGrid();
    }

    private void cargarFoto() {
        new SwingWorker<java.awt.image.BufferedImage, Void>() {
            @Override protected java.awt.image.BufferedImage doInBackground() {
                try {
                    String ruta = usuario.getRutaFotoPerfil();
                    if (ruta == null) return null;
                    ruta = ruta.replaceAll("[\\x00-\\x1F]", "").trim();
                    if (ruta.isEmpty() || ruta.equals("default.jpg")) return null;
                    File f = new File(ruta);
                    if (!f.exists() || f.length() == 0) return null;
                    return ImageIO.read(f);
                } catch (Exception e) { return null; }
            }
            @Override protected void done() {
                try {
                    java.awt.image.BufferedImage bi = get();
                    if (bi != null) { fotoCirculo.setImagen(bi); fotoCirculo.repaint(); }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void cargarEstadisticas() {
        new SwingWorker<int[], Void>() {
            @Override protected int[] doInBackground() throws Exception {
                return new int[]{
                    postManager.obtenerPostsDeUsuario(usuario.getUsername()).size(),
                    followManager.contarFollowers(usuario.getUsername()),
                    followManager.contarFollowing(usuario.getUsername())
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

    private void cargarGrid() {
        new SwingWorker<ArrayList<Post>, Void>() {
            @Override protected ArrayList<Post> doInBackground() throws Exception {
                return postManager.obtenerPostsDeUsuario(usuario.getUsername());
            }
            @Override protected void done() {
                try { pintarGrid(get()); } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void pintarGrid(ArrayList<Post> posts) {
        gridPanel.removeAll();
        if (posts.isEmpty()) {
            gridPanel.setLayout(new BorderLayout());
            JLabel lbl = new JLabel("<html><center>Aún no hay publicaciones</center></html>",
                SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
            lbl.setForeground(new Color(142, 142, 142));
            lbl.setPreferredSize(new Dimension(390, 160));
            gridPanel.add(lbl, BorderLayout.CENTER);
        } else {
            gridPanel.setLayout(new GridLayout(0, 3, 2, 2));
            for (Post p : posts) gridPanel.add(celdaGrid(p));
            int r = posts.size() % 3;
            if (r != 0) for (int i = 0; i < 3-r; i++) {
                JPanel v = new JPanel(); v.setBackground(new Color(239,239,239));
                v.setPreferredSize(new Dimension(128,128)); gridPanel.add(v);
            }
        }
        gridPanel.revalidate(); gridPanel.repaint();
    }

    private JPanel celdaGrid(Post p) {
        JPanel c = new JPanel(new BorderLayout());
        c.setPreferredSize(new Dimension(128, 128));
        c.setBackground(new Color(239, 239, 239));
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (p.getRutaImagen() != null && !p.getRutaImagen().isEmpty()) {
            try {
                BufferedImage bi = ImageIO.read(new File(p.getRutaImagen()));
                if (bi != null) c.add(new JLabel(new ImageIcon(
                    bi.getScaledInstance(128, 128, Image.SCALE_SMOOTH))));
            } catch (Exception ignored) {}
        }
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { mainFrame.verPost(p); }
        });
        return c;
    }

    private JLabel statCol(String num, String label) {
        JLabel lbl = new JLabel("<html><center><b>" + num + "</b><br>"
            + "<small style='color:gray'>" + label + "</small></center></html>",
            SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return lbl;
    }

    private void actualizarStatCol(JLabel lbl, int num, String label) {
        lbl.setText("<html><center><b>" + num + "</b><br>"
            + "<small style='color:gray'>" + label + "</small></center></html>");
    }
}