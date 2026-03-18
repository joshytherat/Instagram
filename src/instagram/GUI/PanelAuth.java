package instagram.GUI;

import instagram.Exceptions.CredencialesInvalidasException;
import instagram.Exceptions.UserException;
import instagram.Enums.EstadoCuenta;
import instagram.Enums.TipoCuenta;
import instagram.Enums.Genero;
import instagram.Utilities.FollowManager;
import instagram.Utilities.Instagram;
import instagram.Utilities.Post;
import instagram.Utilities.PostManager;
import instagram.Utilities.User;
import instagram.Utilities.UserManager;
import instagram.Utilities.ValidadorPassword;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

/**
 * PanelAuth — login y registro en un solo panel con CardLayout.
 * No crea ninguna ventana extra. Cuando el login/registro es exitoso,
 * llama a mainFrame.onLoginExitoso(...).
 */
public class PanelAuth extends JPanel {

    private static final Color AZUL  = new Color(0, 149, 246);
    private static final Color GRIS  = new Color(142, 142, 142);
    private static final Color NEGRO = new Color(38, 38, 38);
    private static final Color VERDE = new Color(0, 168, 107);
    private static final Color ROJO  = new Color(237, 73, 86);
    private static final Color BORDE = new Color(219, 219, 219);
    private static final Color BG    = Color.WHITE;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     panelRaiz  = new JPanel(cardLayout);

    // ── Login fields ──────────────────────────────────────────────
    private JTextField     txtUser;
    private JPasswordField txtPass;
    private JLabel         lblErrLogin;
    private JButton        btnLogin;

    // ── Registro fields ───────────────────────────────────────────
    private JTextField     txtNombre;
    private JTextField     txtUserReg;
    private JPasswordField txtPassReg;
    private JPasswordField txtPassConfirm;

    private ComponenteCircular fotoRegCirculo;
    private JLabel             lblFotoRegHint;
    private java.io.File       fotoRegArchivo;

    private JSpinner              spnEdad;
    private JComboBox<Genero>     cmbGenero;
    private JComboBox<TipoCuenta> cmbTipo;

    private JLabel lblChkLen;
    private JLabel lblChkMay;
    private JLabel lblChkSim;
    private JLabel lblChkMatch;
    private JLabel lblErrReg;

    // ── Dependencias ──────────────────────────────────────────────
    private final UserManager  userManager;
    private final ValidadorPassword validador;
    private final MainFrame    mainFrame;

    public PanelAuth(UserManager userManager, MainFrame mainFrame) {
        this.userManager = userManager;
        this.validador   = userManager.getValidadorPassword();
        this.mainFrame   = mainFrame;

        setLayout(new BorderLayout());
        setBackground(BG);

        panelRaiz.setBackground(BG);
        panelRaiz.add(crearPanelLogin(),    "LOGIN");
        panelRaiz.add(crearPanelRegistro(), "REGISTRO");
        add(panelRaiz, BorderLayout.CENTER);
    }

    // ─────────────────────────────────────────────────────────────
    // Panel Login
    // ─────────────────────────────────────────────────────────────

    private JPanel crearPanelLogin() {
        JPanel p = new JPanel(null);
        p.setBackground(BG);

        int logoY = 150;
        ImageIcon logoIcon = InstagramIcons.cargar("instagramlogo.jpeg", 82, 82);
        if (logoIcon != null) {
            JLabel lblLogo = new JLabel(logoIcon);
            lblLogo.setBounds(154, logoY, 82, 82);
            p.add(lblLogo);
            logoY += 100;
        } else {
            JLabel lbl = new JLabel("Instagram");
            lbl.setFont(new Font("Arial", Font.BOLD, 32));
            lbl.setForeground(NEGRO);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setBounds(40, logoY, 310, 50);
            p.add(lbl);
            logoY += 68;
        }

        JLabel sub = new JLabel("Inicia sesión para continuar.", SwingConstants.CENTER);
        sub.setBounds(40, logoY, 310, 20);
        sub.setFont(new Font("Arial", Font.PLAIN, 13));
        sub.setForeground(GRIS);
        p.add(sub);
        int y = logoY + 38;

        p.add(campoLabel("Nombre de usuario"), campoLabelBounds(40, y)); y += 20;
        txtUser = campoTextoConPlaceholder("Ingrese su usuario");
        txtUser.setBounds(40, y, 310, 44);
        p.add(txtUser);
        y += 52;

        p.add(campoLabel("Contraseña"), campoLabelBounds(40, y)); y += 20;
        txtPass = campoPasswordConHint("Ingrese su contraseña");
        txtPass.setBounds(40, y, 310, 44);
        p.add(txtPass);
        y += 52;

        lblErrLogin = new JLabel("", SwingConstants.CENTER);
        lblErrLogin.setBounds(40, y, 310, 20);
        lblErrLogin.setFont(new Font("Arial", Font.PLAIN, 12));
        lblErrLogin.setForeground(ROJO);
        p.add(lblErrLogin);
        y += 26;

        btnLogin = botonPrimario("Iniciar sesión");
        btnLogin.setBounds(40, y, 310, 44);
        btnLogin.addActionListener(e -> iniciarSesion());
        p.add(btnLogin);

        txtPass.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) iniciarSesion();
            }
        });

        y += 58;
        JSeparadorOR sep = new JSeparadorOR();
        sep.setBounds(40, y, 310, 20);
        p.add(sep);
        y += 38;

        JPanel link = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        link.setBackground(BG);
        link.setBounds(40, y, 310, 24);
        JLabel t1 = new JLabel("¿No tienes cuenta?");
        t1.setFont(new Font("Arial", Font.PLAIN, 13));
        t1.setForeground(GRIS);
        JLabel t2 = new JLabel("Regístrate");
        t2.setFont(new Font("Arial", Font.BOLD, 13));
        t2.setForeground(AZUL);
        t2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        t2.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { irARegistro(); }
        });
        link.add(t1); link.add(t2);
        p.add(link);

        return p;
    }

    // ─────────────────────────────────────────────────────────────
    // Panel Registro
    // ─────────────────────────────────────────────────────────────

    private JPanel crearPanelRegistro() {
        JPanel content = new JPanel(null);
        content.setBackground(BG);
        content.setPreferredSize(new Dimension(390, 1060));

        JLabel back = new JLabel("←");
        back.setBounds(15, 18, 30, 30);
        back.setFont(new Font("Arial", Font.PLAIN, 22));
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { irALogin(); }
        });
        content.add(back);

        JLabel titulo = new JLabel("Crear cuenta", SwingConstants.CENTER);
        titulo.setBounds(0, 16, 390, 30);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        titulo.setForeground(NEGRO);
        content.add(titulo);

        int y = 62;

        fotoRegCirculo = new ComponenteCircular(80);
        fotoRegCirculo.setBounds(155, y, 80, 80);
        fotoRegCirculo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fotoRegCirculo.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { seleccionarFotoReg(); }
            @Override public void mouseEntered(MouseEvent e) { fotoRegCirculo.setColorBorde(AZUL); fotoRegCirculo.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { fotoRegCirculo.setColorBorde(null); fotoRegCirculo.repaint(); }
        });
        content.add(fotoRegCirculo);
        y += 88;

        lblFotoRegHint = new JLabel("Añadir foto de perfil", SwingConstants.CENTER);
        lblFotoRegHint.setBounds(0, y, 390, 18);
        lblFotoRegHint.setFont(new Font("Arial", Font.BOLD, 13));
        lblFotoRegHint.setForeground(AZUL);
        lblFotoRegHint.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblFotoRegHint.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { seleccionarFotoReg(); }
        });
        content.add(lblFotoRegHint);
        y += 28;

        content.add(campoLabel("Nombre completo"), campoLabelBounds(20, y)); y += 20;
        txtNombre = campoTextoConPlaceholder("Ingrese nombre completo");
        txtNombre.setBounds(20, y, 350, 44); content.add(txtNombre); y += 52;

        content.add(campoLabel("Nombre de usuario"), campoLabelBounds(20, y)); y += 20;
        txtUserReg = campoTextoConPlaceholder("Ingrese su usuario");
        txtUserReg.setBounds(20, y, 350, 44); content.add(txtUserReg); y += 52;

        content.add(campoLabel("Contraseña"), campoLabelBounds(20, y)); y += 20;
        txtPassReg = campoPasswordConHint("Ingrese su contraseña");
        txtPassReg.setBounds(20, y, 350, 44);
        txtPassReg.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { validarPassYMatch(); }
            public void removeUpdate(DocumentEvent e)  { validarPassYMatch(); }
            public void changedUpdate(DocumentEvent e) { validarPassYMatch(); }
        });
        content.add(txtPassReg); y += 50;

        lblChkLen = checkLabel("✗   Más de 8 caracteres", false);
        lblChkLen.setBounds(36, y, 310, 18); content.add(lblChkLen); y += 22;
        lblChkMay = checkLabel("✗   Al menos 1 mayúscula", false);
        lblChkMay.setBounds(36, y, 310, 18); content.add(lblChkMay); y += 22;
        lblChkSim = checkLabel("✗   Al menos 1 símbolo  (!@#$...)", false);
        lblChkSim.setBounds(36, y, 310, 18); content.add(lblChkSim); y += 28;

        content.add(campoLabel("Confirmar contraseña"), campoLabelBounds(20, y)); y += 20;
        txtPassConfirm = campoPasswordConHint("Confirme su contraseña");
        txtPassConfirm.setBounds(20, y, 350, 44);
        txtPassConfirm.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { validarPassYMatch(); }
            public void removeUpdate(DocumentEvent e)  { validarPassYMatch(); }
            public void changedUpdate(DocumentEvent e) { validarPassYMatch(); }
        });
        content.add(txtPassConfirm); y += 50;

        lblChkMatch = checkLabel("✗   Las contraseñas coinciden", false);
        lblChkMatch.setBounds(36, y, 310, 18); content.add(lblChkMatch); y += 28;

        content.add(campoLabel("Género"), campoLabelBounds(20, y)); y += 20;
        cmbGenero = new JComboBox<>(Genero.values());
        estilizarCombo(cmbGenero);
        cmbGenero.setBounds(20, y, 350, 44); content.add(cmbGenero); y += 52;

        content.add(campoLabel("Edad"), campoLabelBounds(20, y)); y += 20;
        spnEdad = new JSpinner(new SpinnerNumberModel(18, 13, 99, 1));
        spnEdad.setBounds(20, y, 350, 44);
        spnEdad.setFont(new Font("Arial", Font.PLAIN, 15));
        JComponent spnEditor = spnEdad.getEditor();
        if (spnEditor instanceof JSpinner.DefaultEditor) {
            JTextField spnTxt = ((JSpinner.DefaultEditor) spnEditor).getTextField();
            spnTxt.setFont(new Font("Arial", Font.PLAIN, 15));
            spnTxt.setHorizontalAlignment(SwingConstants.CENTER);
            spnTxt.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        }
        spnEdad.setBorder(BorderFactory.createLineBorder(BORDE, 1));
        content.add(spnEdad); y += 52;

        content.add(campoLabel("Tipo de cuenta"), campoLabelBounds(20, y)); y += 20;
        cmbTipo = new JComboBox<>(TipoCuenta.values());
        estilizarCombo(cmbTipo);
        cmbTipo.setBounds(20, y, 350, 44); content.add(cmbTipo); y += 52;

        lblErrReg = new JLabel("", SwingConstants.CENTER);
        lblErrReg.setBounds(20, y, 350, 30);
        lblErrReg.setFont(new Font("Arial", Font.PLAIN, 11));
        lblErrReg.setForeground(ROJO);
        content.add(lblErrReg); y += 36;

        JButton btnReg = botonPrimario("Registrarse");
        btnReg.setBounds(20, y, 350, 44);
        btnReg.addActionListener(e -> registrar());
        content.add(btnReg);

        JScrollPane scroll = new JScrollPane(content,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ─────────────────────────────────────────────────────────────
    // Lógica de login
    // ─────────────────────────────────────────────────────────────

    private void iniciarSesion() {
        String userRaw = txtUser.getText().trim();
        String user    = userRaw.equals("Ingrese su usuario") ? "" : userRaw;
        String passRaw = new String(txtPass.getPassword());
        String pass    = passRaw.equals("Ingrese su contraseña") ? "" : passRaw;

        if (user.isEmpty()) { lblErrLogin.setText("Ingresa tu nombre de usuario"); return; }
        if (pass.isEmpty()) { lblErrLogin.setText("Ingresa tu contraseña"); return; }

        btnLogin.setEnabled(false);
        lblErrLogin.setForeground(AZUL);
        lblErrLogin.setText("Cargando tu feed...");

        final String u = user, p = pass;

        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() throws Exception {
                User preAuth = userManager.buscarUsuario(u);
                boolean estabaDesactivado = preAuth != null
                    && preAuth.getEstadoCuenta() == EstadoCuenta.DESACTIVADO;

                User usuario = userManager.autenticar(u, p);

                FollowManager fm  = new FollowManager(userManager);
                PostManager   pm  = new PostManager();
                ArrayList<String> siguiendo = fm.obtenerFollowing(usuario.getUsername());
                ArrayList<Post>   feed      = new ArrayList<>();
                // Posts propios del usuario
                feed.addAll(pm.obtenerPostsDeUsuario(usuario.getUsername()));
                // Posts de seguidos
                for (String uname : siguiendo)
                    feed.addAll(pm.obtenerPostsDeUsuario(uname));
                feed.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));

                return new Object[]{ usuario, feed, estabaDesactivado };
            }

            @Override protected void done() {
                try {
                    Object[] result       = get();
                    User            usr   = (User) result[0];
                    @SuppressWarnings("unchecked")
                    ArrayList<Post> feed  = (ArrayList<Post>) result[1];
                    boolean fueReactivado = (Boolean) result[2];

                    // Transición en la misma ventana
                    mainFrame.onLoginExitoso(usr, feed, fueReactivado);

                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    btnLogin.setEnabled(true);
                    lblErrLogin.setForeground(ROJO);
                    if (cause instanceof CredencialesInvalidasException) {
                        lblErrLogin.setText("Usuario o contraseña incorrectos");
                    } else {
                        lblErrLogin.setText("Error al iniciar sesión");
                    }
                } catch (Exception ex) {
                    btnLogin.setEnabled(true);
                    lblErrLogin.setForeground(ROJO);
                    lblErrLogin.setText("Error al iniciar sesión");
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────
    // Lógica de registro
    // ─────────────────────────────────────────────────────────────

    private void registrar() {
        String nombre = txtNombre.getText().trim();
        String user   = txtUserReg.getText().trim();
        int    edad   = (Integer) spnEdad.getValue();

        String passRaw    = new String(txtPassReg.getPassword());
        String confirmRaw = new String(txtPassConfirm.getPassword());
        String pass    = passRaw.equals("Ingrese su contraseña")    ? "" : passRaw;
        String confirm = confirmRaw.equals("Confirme su contraseña") ? "" : confirmRaw;

        if (nombre.isEmpty()) { mostrarErrReg("Ingresa tu nombre completo"); return; }
        if (user.isEmpty())   { mostrarErrReg("Ingresa un nombre de usuario"); return; }
        if (pass.isEmpty())   { mostrarErrReg("Ingresa una contraseña"); return; }

        validador.validar(pass);
        if (!validador.esValida()) {
            mostrarErrReg(validador.obtenerMensajeError().replace("\n", "  ")); return;
        }
        if (!pass.equals(confirm)) { mostrarErrReg("Las contraseñas no coinciden"); return; }

        try {
            User nuevo = new User(nombre, (Genero) cmbGenero.getSelectedItem(),
                user, pass, edad);
            nuevo.setTipoCuenta((TipoCuenta) cmbTipo.getSelectedItem());

            if (fotoRegArchivo != null && fotoRegArchivo.exists()) {
                try {
                    String userLower = user.toLowerCase();
                    Instagram.crearDir("INSTA_RAIZ/" + userLower);
                    Instagram.crearDir("INSTA_RAIZ/" + userLower + "/imagenes");
                    String ext  = fotoRegArchivo.getName().replaceAll(".*\\.", "");
                    if (ext.equals(fotoRegArchivo.getName())) ext = "jpg";
                    String dest = "INSTA_RAIZ/" + userLower + "/imagenes/perfil." + ext;
                    java.nio.file.Files.copy(fotoRegArchivo.toPath(),
                        new java.io.File(dest).toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    nuevo.setRutaFotoPerfil(dest);
                } catch (Exception ex) {
                    System.err.println("No se pudo copiar la foto: " + ex.getMessage());
                }
            }

            userManager.registrarUsuario(nuevo);

            // Auto-follow a cuentas institucionales al registrarse
            final String[] CUENTAS_DEFAULT = {"cnn", "barcanews", "gymtrends"};
            try {
                FollowManager fm = new FollowManager(userManager);
                for (String cuenta : CUENTAS_DEFAULT) {
                    try {
                        // Solo seguir si la cuenta existe
                        if (userManager.existeUsername(cuenta)) {
                            fm.seguir(nuevo.getUsername(), cuenta);
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            // Transición directa — sin abrir nueva ventana
            final User usuarioFinal = nuevo;
            mainFrame.onLoginExitoso(usuarioFinal, new ArrayList<>(), false);

        } catch (UserException ex) {
            mostrarErrReg(ex.getMessage());
        } catch (Exception ex) {
            mostrarErrReg("Error al crear la cuenta");
            ex.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Validación en tiempo real
    // ─────────────────────────────────────────────────────────────

    private void validarPassYMatch() {
        String passRaw2 = new String(txtPassReg.getPassword());
        String pass     = passRaw2.equals("Ingrese su contraseña") ? "" : passRaw2;
        String confRaw  = txtPassConfirm != null ? new String(txtPassConfirm.getPassword()) : "";
        String confirm  = confRaw.equals("Confirme su contraseña") ? "" : confRaw;

        validador.validar(pass);
        actualizarCheck(lblChkLen,   validador.tieneLongitudSuficiente(), "Más de 8 caracteres");
        actualizarCheck(lblChkMay,   validador.tieneMayuscula(),           "Al menos 1 mayúscula");
        actualizarCheck(lblChkSim,   validador.tieneSimbolo(),             "Al menos 1 símbolo  (!@#$...)");

        if (lblChkMatch != null) {
            if (confirm.isEmpty()) { lblChkMatch.setText("✗   Las contraseñas coinciden"); lblChkMatch.setForeground(ROJO); }
            else actualizarCheck(lblChkMatch, pass.equals(confirm), "Las contraseñas coinciden");
        }
    }

    private void actualizarCheck(JLabel lbl, boolean ok, String texto) {
        if (lbl == null) return;
        lbl.setText((ok ? "✓   " : "✗   ") + texto);
        lbl.setForeground(ok ? VERDE : ROJO);
    }

    // ─────────────────────────────────────────────────────────────
    // Navegación interna
    // ─────────────────────────────────────────────────────────────

    private void irARegistro() { limpiarFormularioRegistro(); cardLayout.show(panelRaiz, "REGISTRO"); }
    private void irALogin()    { limpiarFormularioLogin();    cardLayout.show(panelRaiz, "LOGIN"); }

    /**
     * Muestra un aviso de sesión duplicada en el panel de login.
     * Llamado por MainFrame cuando el servidor rechaza el login.
     */
    public void mostrarErrorSesion(String mensaje) {
        // Asegurarse de estar en el panel de login
        cardLayout.show(panelRaiz, "LOGIN");
        // Mostrar el error en el label de error del login
        if (lblErrLogin != null) {
            lblErrLogin.setForeground(new Color(237, 73, 86));
            lblErrLogin.setText("<html><center>⚠ " + mensaje + "</center></html>");
        }
        // Re-habilitar el botón por si estaba desactivado
        if (btnLogin != null) btnLogin.setEnabled(true);
    }

    private void limpiarFormularioLogin() {
        if (lblErrLogin != null) { lblErrLogin.setText(""); }
        if (btnLogin    != null) { btnLogin.setEnabled(true); }
    }

    private void limpiarFormularioRegistro() {
        if (txtNombre     != null) { txtNombre.setText("Ingrese nombre completo"); txtNombre.setForeground(GRIS); }
        if (txtUserReg    != null) { txtUserReg.setText("Ingrese su usuario"); txtUserReg.setForeground(GRIS); }
        if (txtPassReg    != null) { txtPassReg.setEchoChar((char)0); txtPassReg.setText("Ingrese su contraseña"); txtPassReg.setForeground(GRIS); }
        if (txtPassConfirm!= null) { txtPassConfirm.setEchoChar((char)0); txtPassConfirm.setText("Confirme su contraseña"); txtPassConfirm.setForeground(GRIS); }
        if (spnEdad       != null) spnEdad.setValue(18);
        if (cmbGenero     != null) cmbGenero.setSelectedIndex(0);
        if (cmbTipo       != null) cmbTipo.setSelectedIndex(0);
        if (lblErrReg     != null) lblErrReg.setText("");
        fotoRegArchivo = null;
        if (fotoRegCirculo != null) { fotoRegCirculo.setImagen(null); fotoRegCirculo.repaint(); }
        if (lblFotoRegHint != null) { lblFotoRegHint.setText("Añadir foto de perfil"); lblFotoRegHint.setForeground(AZUL); }
        resetCheck(lblChkLen,   "Más de 8 caracteres");
        resetCheck(lblChkMay,   "Al menos 1 mayúscula");
        resetCheck(lblChkSim,   "Al menos 1 símbolo  (!@#$...)");
        resetCheck(lblChkMatch, "Las contraseñas coinciden");
    }

    private void resetCheck(JLabel lbl, String texto) {
        if (lbl == null) return;
        lbl.setText("✗   " + texto);
        lbl.setForeground(ROJO);
    }

    private void mostrarErrReg(String msg) {
        if (lblErrReg == null) return;
        lblErrReg.setForeground(ROJO);
        lblErrReg.setText("<html>" + msg.replace("\n","<br>") + "</html>");
    }

    // ─────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────

    private void seleccionarFotoReg() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Elegir foto de perfil");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Imágenes (jpg, jpeg, png)", "jpg", "jpeg", "png"));
        fc.setAcceptAllFileFilterUsed(false);
        if (fc.showOpenDialog(mainFrame) != JFileChooser.APPROVE_OPTION) return;
        java.io.File f = fc.getSelectedFile();
        if (f == null || !f.exists()) return;
        try {
            java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(f);
            if (bi == null) { lblFotoRegHint.setText("❌ Imagen no válida"); lblFotoRegHint.setForeground(ROJO); return; }
            fotoRegArchivo = f;
            fotoRegCirculo.setImagen(bi); fotoRegCirculo.repaint();
            lblFotoRegHint.setText("✓ Foto seleccionada  (toca para cambiar)");
            lblFotoRegHint.setForeground(VERDE);
        } catch (Exception ex) {
            lblFotoRegHint.setText("❌ Error al cargar la imagen");
            lblFotoRegHint.setForeground(ROJO);
        }
    }

    private JTextField campoTextoConPlaceholder(String placeholder) {
        JTextField tf = campoTexto();
        tf.setText(placeholder); tf.setForeground(GRIS);
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (tf.getText().equals(placeholder)) { tf.setText(""); tf.setForeground(NEGRO); }
            }
            @Override public void focusLost(FocusEvent e) {
                if (tf.getText().isEmpty()) { tf.setText(placeholder); tf.setForeground(GRIS); }
            }
        });
        return tf;
    }

    private JPasswordField campoPasswordConHint(String hint) {
        JPasswordField pf = campoPassword();
        pf.setEchoChar((char) 0); pf.setText(hint); pf.setForeground(GRIS);
        pf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (new String(pf.getPassword()).equals(hint)) { pf.setText(""); pf.setForeground(NEGRO); pf.setEchoChar('•'); }
            }
            @Override public void focusLost(FocusEvent e) {
                if (pf.getPassword().length == 0) { pf.setEchoChar((char)0); pf.setText(hint); pf.setForeground(GRIS); }
            }
        });
        return pf;
    }

    private JTextField campoTexto() {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Arial", Font.PLAIN, 14));
        tf.setForeground(NEGRO); tf.setBackground(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE, 1),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        return tf;
    }

    private JPasswordField campoPassword() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(new Font("Arial", Font.PLAIN, 14));
        pf.setForeground(NEGRO); pf.setBackground(Color.WHITE);
        pf.setEchoChar('•');
        pf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE, 1),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        return pf;
    }

    private JLabel campoLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        lbl.setForeground(NEGRO);
        return lbl;
    }

    private Rectangle campoLabelBounds(int x, int y) {
        return new Rectangle(x, y, 310, 18);
    }

    private JLabel checkLabel(String texto, boolean ok) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Arial", Font.PLAIN, 12));
        lbl.setForeground(ok ? VERDE : ROJO);
        return lbl;
    }

    private JButton botonPrimario(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(AZUL);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(0,120,210)); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(AZUL); }
        });
        return btn;
    }

    private void estilizarCombo(JComboBox<?> cmb) {
        cmb.setFont(new Font("Arial", Font.PLAIN, 14));
        cmb.setBackground(Color.WHITE);
        cmb.setBorder(BorderFactory.createLineBorder(BORDE, 1));
    }

    // ── Separador "O" ─────────────────────────────────────────────
    private static class JSeparadorOR extends JPanel {
        JSeparadorOR() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int mid = getWidth() / 2;
            g2.setColor(new Color(219, 219, 219));
            g2.drawLine(0, getHeight()/2, mid-14, getHeight()/2);
            g2.drawLine(mid+14, getHeight()/2, getWidth(), getHeight()/2);
            g2.setColor(new Color(142, 142, 142));
            g2.setFont(new Font("Arial", Font.PLAIN, 13));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("O", mid - fm.stringWidth("O")/2, getHeight()/2 + fm.getAscent()/2 - 1);
        }
    }
}