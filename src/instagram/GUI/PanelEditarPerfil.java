package instagram.GUI;

import instagram.Utilities.Instagram;
import instagram.Utilities.User;
import instagram.Utilities.UserManager;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;

public class PanelEditarPerfil extends JPanel {

    private User        usuario;
    private UserManager userManager;
    private MainFrame   mainFrame;

    private JTextField txtNombre;
    private JTextArea  txtBio;
    private JLabel     lblContador;
    private JLabel     lblFeedback;
    private JButton    btnGuardar;
    private ComponenteCircular fotoCircular;

    public PanelEditarPerfil(User usuario, UserManager userManager, MainFrame mainFrame) {
        this.usuario     = usuario;
        this.userManager = userManager;
        this.mainFrame   = mainFrame;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        construirUI();
    }

    private void construirUI() {
        JPanel contenido = new JPanel(null);
        contenido.setBackground(Color.WHITE);
        contenido.setPreferredSize(new Dimension(390, 600));

        int y = 16;

        // ── Foto de perfil ────────────────────────────────────
        fotoCircular = new ComponenteCircular(80);
        fotoCircular.setBounds(155, y, 80, 80);
        contenido.add(fotoCircular);

        JLabel lblCambiarFoto = new JLabel("Cambiar foto de perfil", SwingConstants.CENTER);
        lblCambiarFoto.setBounds(0, y + 86, 390, 18);
        lblCambiarFoto.setFont(new Font("Arial", Font.BOLD, 13));
        lblCambiarFoto.setForeground(InstagramColors.INSTAGRAM_AZUL);
        lblCambiarFoto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblCambiarFoto.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { cambiarFoto(); }
        });
        contenido.add(lblCambiarFoto);
        y += 114;

        // ── Nombre ────────────────────────────────────────────
        contenido.add(labelCampo("Nombre", 20, y)); y += 22;
        txtNombre = inputField();
        txtNombre.setBounds(20, y, 350, 44);
        txtNombre.setText(usuario.getNombreCompleto());
        contenido.add(txtNombre);
        y += 52;

        // ── Nombre de usuario (solo lectura) ──────────────────
        contenido.add(labelCampo("Nombre de usuario", 20, y)); y += 22;
        JTextField txtUser = inputField();
        txtUser.setBounds(20, y, 350, 44);
        txtUser.setText(usuario.getUsername());
        txtUser.setEditable(false);
        txtUser.setBackground(new Color(245,245,245));
        txtUser.setForeground(InstagramColors.TEXTO_GRIS);
        contenido.add(txtUser);
        y += 52;

        // ── Biografía ─────────────────────────────────────────
        contenido.add(labelCampo("Biografía", 20, y));

        lblContador = new JLabel("0/220", SwingConstants.RIGHT);
        lblContador.setBounds(280, y, 90, 18);
        lblContador.setFont(new Font("Arial", Font.PLAIN, 11));
        lblContador.setForeground(InstagramColors.TEXTO_GRIS);
        contenido.add(lblContador);
        y += 22;

        txtBio = new JTextArea();
        txtBio.setFont(new Font("Arial", Font.PLAIN, 13));
        txtBio.setLineWrap(true);
        txtBio.setWrapStyleWord(true);
        txtBio.setText(usuario.getBiografia() != null ? usuario.getBiografia() : "");
        txtBio.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(InstagramColors.BORDE_GRIS, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        txtBio.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { actualizarContador(); }
            public void removeUpdate(DocumentEvent e)  { actualizarContador(); }
            public void changedUpdate(DocumentEvent e) { actualizarContador(); }
        });
        JScrollPane scrollBio = new JScrollPane(txtBio);
        scrollBio.setBounds(20, y, 350, 100);
        scrollBio.setBorder(BorderFactory.createLineBorder(InstagramColors.BORDE_GRIS, 1));
        contenido.add(scrollBio);
        y += 112;

        // ── Feedback ──────────────────────────────────────────
        lblFeedback = new JLabel("", SwingConstants.CENTER);
        lblFeedback.setBounds(20, y, 350, 20);
        lblFeedback.setFont(new Font("Arial", Font.PLAIN, 12));
        contenido.add(lblFeedback);
        y += 28;

        // ── Guardar ───────────────────────────────────────────
        btnGuardar = new JButton("Guardar");
        btnGuardar.setBounds(20, y, 350, 44);
        btnGuardar.setFont(new Font("Arial", Font.BOLD, 14));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setBackground(InstagramColors.INSTAGRAM_AZUL);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnGuardar.addActionListener(e -> guardar());
        contenido.add(btnGuardar);

        actualizarContador();

        JScrollPane scroll = new JScrollPane(contenido,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
    }

    private void cambiarFoto() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Imágenes", "jpg","jpeg","png","gif"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                String dir  = "INSTA_RAIZ/" + usuario.getUsername() + "/imagenes";
                Instagram.crearDir(dir);
                String ext  = f.getName().replaceAll(".*\\.", "");
                String dest = dir + "/perfil_" + System.currentTimeMillis() + "." + ext;
                Files.copy(f.toPath(), new File(dest).toPath(), StandardCopyOption.REPLACE_EXISTING);
                usuario.setRutaFotoPerfil(dest);
                // Actualizar círculo
                java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(new File(dest));
                fotoCircular.setImagen(bi);
                mostrarFeedback("✓ Foto actualizada", InstagramColors.VERDE_EXITO);
            } catch (Exception ex) {
                mostrarFeedback("❌ Error al cargar imagen", InstagramColors.ROJO_ERROR);
            }
        }
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String bio    = txtBio.getText().trim();

        if (nombre.isEmpty()) { mostrarFeedback("❌ El nombre no puede estar vacío", InstagramColors.ROJO_ERROR); return; }
        if (bio.length() > 220) { mostrarFeedback("❌ Biografía excede 220 caracteres", InstagramColors.ROJO_ERROR); return; }

        try {
            usuario.setNombreCompleto(nombre);
            usuario.setBiografia(bio);
            userManager.actualizarUsuario(usuario.getUsername(), usuario);
            mostrarFeedback("✓ Perfil actualizado", InstagramColors.VERDE_EXITO);
            Timer t = new Timer(1200, e -> mainFrame.volver());
            t.setRepeats(false); t.start();
        } catch (Exception ex) {
            mostrarFeedback("❌ Error al guardar", InstagramColors.ROJO_ERROR);
        }
    }

    private void actualizarContador() {
        int len = txtBio.getText().length();
        lblContador.setText(len + "/220");
        if (len > 220) {
            lblContador.setForeground(InstagramColors.ROJO_ERROR);
            btnGuardar.setEnabled(false);
        } else {
            lblContador.setForeground(InstagramColors.TEXTO_GRIS);
            btnGuardar.setEnabled(true);
        }
    }

    private void mostrarFeedback(String msg, Color c) {
        lblFeedback.setText(msg); lblFeedback.setForeground(c);
        Timer t = new Timer(3000, e -> lblFeedback.setText(""));
        t.setRepeats(false); t.start();
    }

    private JTextField inputField() {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Arial", Font.PLAIN, 14));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(InstagramColors.BORDE_GRIS, 1),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        return tf;
    }

    private JLabel labelCampo(String texto, int x, int y) {
        JLabel lbl = new JLabel(texto);
        lbl.setBounds(x, y, 200, 18);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        lbl.setForeground(InstagramColors.TEXTO_NEGRO);
        return lbl;
    }
}