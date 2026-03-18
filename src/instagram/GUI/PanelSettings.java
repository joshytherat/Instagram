package instagram.GUI;

import instagram.Enums.EstadoCuenta;
import instagram.Utilities.Post;
import instagram.Utilities.PostManager;
import instagram.Utilities.User;
import instagram.Utilities.UserManager;
import instagram.Utilities.ValidadorPassword;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

public class PanelSettings extends JPanel {

    private User        usuario;
    private UserManager userManager;
    private PostManager postManager;
    private MainFrame   mainFrame;

    private JPasswordField fldPassActual, fldPassNueva, fldPassConf;
    private JLabel         lblFeedPass;
    private JPanel         panelConfirmDesact;
    private JLabel         lblFeedDeact;

    private static final int INNER_W = 362;

    public PanelSettings(User usuario, UserManager userManager,
                         PostManager postManager, MainFrame mainFrame) {
        this.usuario     = usuario;
        this.userManager = userManager;
        this.postManager = postManager;
        this.mainFrame   = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        construir();
    }

    private void construir() {
        JPanel todo = new JPanel() {
            @Override public Dimension getPreferredSize() {
                return new Dimension(390, super.getPreferredSize().height);
            }
        };
        todo.setLayout(new BoxLayout(todo, BoxLayout.Y_AXIS));
        todo.setBackground(new Color(245, 245, 245));

        todo.add(espacio(12));
        todo.add(tarjeta("Cambiar contraseña",            panelPassword()));
        todo.add(espacio(10));
        todo.add(tarjeta("Publicaciones que te gustaron", panelLiked()));
        todo.add(espacio(10));
        todo.add(tarjeta("Cuenta",                        panelCuenta()));
        todo.add(espacio(20));

        JButton btnOut = new JButton("Cerrar sesión");
        btnOut.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnOut.setForeground(new Color(237, 73, 86));
        btnOut.setBackground(new Color(245, 245, 245));
        btnOut.setBorderPainted(false);
        btnOut.setFocusPainted(false);
        btnOut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnOut.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnOut.setMaximumSize(new Dimension(390, 40));
        btnOut.setPreferredSize(new Dimension(390, 40));
        btnOut.setHorizontalAlignment(SwingConstants.CENTER);
        btnOut.addActionListener(e -> logout());
        todo.add(btnOut);
        todo.add(espacio(30));

        JScrollPane scroll = new JScrollPane(todo,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(new Color(245, 245, 245));
        add(scroll, BorderLayout.CENTER);
    }

    // ── Contraseña ────────────────────────────────────────────────

    private JPanel panelPassword() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(6, 14, 14, 14));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        fldPassActual = campoPass();
        fldPassNueva  = campoPass();
        fldPassConf   = campoPass();

        p.add(label12("Contraseña actual"));            p.add(espacio(4));
        p.add(fldPassActual);                            p.add(espacio(10));
        p.add(label12("Nueva contraseña"));              p.add(espacio(4));
        p.add(fldPassNueva);                             p.add(espacio(10));
        p.add(label12("Confirmar nueva contraseña"));    p.add(espacio(4));
        p.add(fldPassConf);                              p.add(espacio(10));

        lblFeedPass = new JLabel(" ");
        lblFeedPass.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblFeedPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblFeedPass.setMaximumSize(new Dimension(INNER_W, 20));
        p.add(lblFeedPass);
        p.add(espacio(6));

        JButton btn = btnAzul("Cambiar contraseña");
        btn.addActionListener(e -> cambiarPass());
        p.add(btn);
        return p;
    }

    private void cambiarPass() {
        String actual = new String(fldPassActual.getPassword());
        String nueva  = new String(fldPassNueva.getPassword());
        String conf   = new String(fldPassConf.getPassword());
        if (!usuario.getPassword().equals(actual)) {
            fb(lblFeedPass, "❌ Contraseña actual incorrecta", new Color(237,73,86)); return;
        }
        if (!nueva.equals(conf)) {
            fb(lblFeedPass, "❌ Las contraseñas no coinciden", new Color(237,73,86)); return;
        }
        ValidadorPassword v = new ValidadorPassword();
        v.validar(nueva);
        if (!v.esValida()) {
            fb(lblFeedPass, "❌ " + v.obtenerMensajeError().replace("\n","  "),
                new Color(237,73,86)); return;
        }
        try {
            usuario.setPassword(nueva);
            userManager.actualizarUsuario(usuario.getUsername(), usuario);
            fb(lblFeedPass, "✓ Contraseña actualizada", new Color(0,168,107));
            fldPassActual.setText(""); fldPassNueva.setText(""); fldPassConf.setText("");
        } catch (Exception ex) {
            fb(lblFeedPass, "❌ Error al guardar", new Color(237,73,86));
        }
    }

    // ── Liked posts ───────────────────────────────────────────────

    private JPanel panelLiked() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Color.WHITE);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setPreferredSize(new Dimension(390, 148));
        wrap.setMaximumSize(new Dimension(390, 148));
        wrap.setBorder(BorderFactory.createEmptyBorder(4, 14, 10, 14));

        JLabel cargando = new JLabel("Cargando...");
        cargando.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cargando.setForeground(new Color(142,142,142));
        cargando.setHorizontalAlignment(SwingConstants.CENTER);
        wrap.add(cargando, BorderLayout.CENTER);

        new SwingWorker<ArrayList<Post>, Void>() {
            @Override protected ArrayList<Post> doInBackground() throws Exception {
                ArrayList<Post> all = postManager.obtenerFeedCompleto();
                ArrayList<Post> liked = new ArrayList<>();
                for (Post p : all) if (p.tienelike(usuario.getUsername())) liked.add(p);
                return liked;
            }
            @Override protected void done() {
                try {
                    wrap.remove(cargando);
                    ArrayList<Post> liked = get();
                    if (liked.isEmpty()) {
                        JLabel v = new JLabel("Aún no hay publicaciones marcadas", SwingConstants.CENTER);
                        v.setFont(new Font("SansSerif", Font.PLAIN, 12));
                        v.setForeground(new Color(142,142,142));
                        wrap.add(v, BorderLayout.CENTER);
                    } else {
                        JPanel grid = new JPanel(new GridLayout(2, 4, 3, 3));
                        grid.setBackground(new Color(239,239,239));
                        int max = Math.min(liked.size(), 8);
                        for (int i = 0; i < max; i++) {
                            final Post p = liked.get(i);
                            JPanel cell = new JPanel(new BorderLayout());
                            cell.setBackground(new Color(220,220,220));
                            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            if (p.getRutaImagen()!=null && !p.getRutaImagen().isEmpty()) {
                                try {
                                    BufferedImage bi = ImageIO.read(new File(p.getRutaImagen()));
                                    if (bi!=null) cell.add(new JLabel(new ImageIcon(
                                        bi.getScaledInstance(80,60,Image.SCALE_SMOOTH))));
                                } catch (Exception ignored) {}
                            }
                            cell.addMouseListener(new MouseAdapter(){
                                @Override public void mouseClicked(MouseEvent e){ mainFrame.verPost(p); }
                            });
                            grid.add(cell);
                        }
                        for (int i=max;i<8;i++){JPanel v=new JPanel();v.setBackground(new Color(239,239,239));grid.add(v);}
                        wrap.add(grid, BorderLayout.CENTER);
                    }
                    wrap.revalidate(); wrap.repaint();
                } catch (Exception ignored) {}
            }
        }.execute();
        return wrap;
    }

    // ── Cuenta ────────────────────────────────────────────────────

    private JPanel panelCuenta() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(6, 14, 14, 14));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblFeedDeact = new JLabel(" ");
        lblFeedDeact.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblFeedDeact.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblFeedDeact.setMaximumSize(new Dimension(INNER_W, 20));
        p.add(lblFeedDeact);
        p.add(espacio(6));

        // Panel de confirmación con layout null para posicionamiento preciso
        panelConfirmDesact = new JPanel(null);
        panelConfirmDesact.setBackground(new Color(255,243,243));
        panelConfirmDesact.setBorder(BorderFactory.createLineBorder(new Color(237,73,86),1));
        panelConfirmDesact.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelConfirmDesact.setMaximumSize(new Dimension(INNER_W, 72));
        panelConfirmDesact.setPreferredSize(new Dimension(INNER_W, 72));
        panelConfirmDesact.setMinimumSize(new Dimension(INNER_W, 72));
        panelConfirmDesact.setVisible(false);

        JLabel pregunta = new JLabel("¿Seguro que quieres desactivar tu cuenta?", SwingConstants.CENTER);
        pregunta.setBounds(0, 6, INNER_W, 20);
        pregunta.setFont(new Font("SansSerif", Font.BOLD, 12));
        pregunta.setForeground(new Color(80, 0, 0));
        panelConfirmDesact.add(pregunta);

        // JLabel como botón — nunca truncan con "..."
        JLabel btnSi = new JLabel("Sí, desactivar", SwingConstants.CENTER);
        btnSi.setBounds(10, 34, (INNER_W - 30) / 2, 28);
        btnSi.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnSi.setForeground(Color.WHITE);
        btnSi.setBackground(new Color(237,73,86));
        btnSi.setOpaque(true);
        btnSi.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSi.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { desactivarCuenta(); }
        });
        panelConfirmDesact.add(btnSi);

        JLabel btnNo = new JLabel("Cancelar", SwingConstants.CENTER);
        btnNo.setBounds(20 + (INNER_W - 30) / 2, 34, (INNER_W - 30) / 2, 28);
        btnNo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnNo.setForeground(new Color(38,38,38));
        btnNo.setBackground(new Color(239,239,239));
        btnNo.setOpaque(true);
        btnNo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNo.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { panelConfirmDesact.setVisible(false); }
        });
        panelConfirmDesact.add(btnNo);
        p.add(panelConfirmDesact);
        p.add(espacio(10));

        // JLabel como botón — se centra correctamente con BoxLayout
        JLabel btnDesact = new JLabel("Desactivar cuenta temporalmente", SwingConstants.CENTER);
        btnDesact.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnDesact.setForeground(new Color(237, 73, 86));
        btnDesact.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDesact.setMaximumSize(new Dimension(INNER_W, 36));
        btnDesact.setPreferredSize(new Dimension(INNER_W, 36));
        btnDesact.setMinimumSize(new Dimension(INNER_W, 36));
        btnDesact.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDesact.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                panelConfirmDesact.setVisible(true);
            }
        });
        p.add(btnDesact);
        return p;
    }

    private void desactivarCuenta() {
        try {
            usuario.setEstadoCuenta(EstadoCuenta.DESACTIVADO);
            userManager.actualizarUsuario(usuario.getUsername(), usuario);
            panelConfirmDesact.setVisible(false);
            fb(lblFeedDeact, "Cuenta desactivada. Cerrando sesión...", new Color(142,142,142));
            Timer t = new Timer(1800, e -> logout());
            t.setRepeats(false); t.start();
        } catch (Exception ex) {
            fb(lblFeedDeact, "❌ Error al desactivar", new Color(237,73,86));
        }
    }

    /**
     * Cierra sesión volviendo a la pantalla de login dentro de la misma ventana.
     * No abre ninguna ventana nueva.
     */
    private void logout() {
        mainFrame.logout();
    }

    // ── Helpers ───────────────────────────────────────────────────

    private JPanel tarjeta(String titulo, JPanel cuerpo) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(390, 2000));
        card.setMinimumSize(new Dimension(390, 10));
        card.setBorder(BorderFactory.createMatteBorder(1,0,1,0,new Color(219,219,219)));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTitulo.setForeground(new Color(38,38,38));
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(12,14,4,14));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblTitulo.setMaximumSize(new Dimension(390, 36));
        card.add(lblTitulo);

        cuerpo.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(cuerpo);
        return card;
    }

    private JLabel label12(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(new Color(100,100,100));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setMaximumSize(new Dimension(INNER_W, 18));
        return lbl;
    }

    private JPasswordField campoPass() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(new Font("SansSerif", Font.PLAIN, 14));
        pf.setEchoChar('•');
        pf.setBackground(new Color(250,250,250));
        pf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(219,219,219),1),
            BorderFactory.createEmptyBorder(0,10,0,10)));
        pf.setAlignmentX(Component.LEFT_ALIGNMENT);
        pf.setMaximumSize(new Dimension(INNER_W, 42));
        pf.setPreferredSize(new Dimension(INNER_W, 42));
        return pf;
    }

    private JButton btnAzul(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(0,149,246));
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(INNER_W, 42));
        btn.setPreferredSize(new Dimension(INNER_W, 42));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private Component espacio(int h) {
        JPanel sp = new JPanel();
        sp.setOpaque(false);
        sp.setPreferredSize(new Dimension(390, h));
        sp.setMaximumSize(new Dimension(390, h));
        sp.setMinimumSize(new Dimension(390, h));
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sp;
    }

    private void fb(JLabel lbl, String txt, Color color) {
        lbl.setText(txt); lbl.setForeground(color);
        Timer t = new Timer(3000, e -> lbl.setText(" "));
        t.setRepeats(false); t.start();
    }
}