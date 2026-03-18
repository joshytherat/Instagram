package instagram.GUI;

import instagram.Utilities.Instagram;
import instagram.Utilities.Post;
import instagram.Utilities.PostManager;
import instagram.Utilities.User;
import instagram.Utilities.ValidadorContenido;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import javax.imageio.ImageIO;

public class PanelCreatePost extends JPanel {

    private User        usuario;
    private PostManager postManager;
    private MainFrame   mainFrame;

    // Componentes del formulario
    private JLabel        lblPreview;
    private JTextArea     txtDesc;
    private JLabel        lblContador;
    private JButton       btnPublicar;
    private JLabel        lblFeedback;
    private JToggleButton btnCuadrada, btnVertical, btnHorizontal;

    // Estado imagen
    private File          imagenSeleccionada;
    private BufferedImage imagenOriginal = null;
    private Post.TipoMultimedia tipoSeleccionado = Post.TipoMultimedia.IMAGEN_CUADRADA;

    // Referencias para reposicionamiento dinámico
    private int           previewH     = 260;
    private JPanel        contenidoRef;
    private JButton       btnSelecRef;
    private JLabel        lblTipoRef;
    private JToggleButton btnCuadradaRef, btnVerticalRef, btnHorizontalRef;
    private JLabel        lblDescRef, lblContadorRef;
    private JScrollPane   scrollDescRef;
    private JLabel        lblInfoRef;

    public PanelCreatePost(User usuario, PostManager postManager, MainFrame mainFrame) {
        this.usuario     = usuario;
        this.postManager = postManager;
        this.mainFrame   = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        construirUI();
    }

    private void construirUI() {
        add(headerInterno("Nueva publicación"), BorderLayout.NORTH);

        JPanel contenido = new JPanel(null);
        contenidoRef = contenido;
        contenido.setBackground(Color.WHITE);
        contenido.setPreferredSize(new Dimension(390, 800));

        int y = 10;

        // ── Preview imagen ────────────────────────────────────
        previewH = 260;
        lblPreview = new JLabel("📷", SwingConstants.CENTER);
        lblPreview.setBounds(45, y, 300, previewH);
        lblPreview.setFont(new Font("Arial", Font.PLAIN, 70));
        lblPreview.setBackground(new Color(239, 239, 239));
        lblPreview.setOpaque(true);
        lblPreview.setBorder(BorderFactory.createLineBorder(InstagramColors.BORDE_GRIS, 1));
        contenido.add(lblPreview);
        y += previewH + 10;

        // ── Botón seleccionar ─────────────────────────────────
        btnSelecRef = new JButton("Seleccionar foto");
        btnSelecRef.setBounds(120, y, 150, 36);
        btnSelecRef.setFont(new Font("Arial", Font.BOLD, 13));
        btnSelecRef.setForeground(InstagramColors.INSTAGRAM_AZUL);
        btnSelecRef.setBackground(Color.WHITE);
        btnSelecRef.setBorder(BorderFactory.createLineBorder(InstagramColors.INSTAGRAM_AZUL, 1));
        btnSelecRef.setFocusPainted(false);
        btnSelecRef.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSelecRef.addActionListener(e -> seleccionarImagen());
        contenido.add(btnSelecRef);
        y += 50;

        // ── Formato ───────────────────────────────────────────
        lblTipoRef = new JLabel("Formato");
        lblTipoRef.setBounds(20, y, 350, 18);
        lblTipoRef.setFont(new Font("Arial", Font.BOLD, 13));
        contenido.add(lblTipoRef);
        y += 24;

        ButtonGroup grupoTipo = new ButtonGroup();
        btnCuadrada    = toggleBoton("Cuadrada");
        btnVertical    = toggleBoton("Vertical");
        btnHorizontal  = toggleBoton("Horizontal");
        btnCuadradaRef   = btnCuadrada;
        btnVerticalRef   = btnVertical;
        btnHorizontalRef = btnHorizontal;

        btnCuadrada  .setBounds(20,  y, 108, 34);
        btnVertical  .setBounds(140, y, 108, 34);
        btnHorizontal.setBounds(260, y, 108, 34);

        grupoTipo.add(btnCuadrada); grupoTipo.add(btnVertical); grupoTipo.add(btnHorizontal);
        btnCuadrada.setSelected(true);

        btnCuadrada  .addActionListener(e -> { tipoSeleccionado = Post.TipoMultimedia.IMAGEN_CUADRADA;   actualizarPreview(); });
        btnVertical  .addActionListener(e -> { tipoSeleccionado = Post.TipoMultimedia.IMAGEN_VERTICAL;   actualizarPreview(); });
        btnHorizontal.addActionListener(e -> { tipoSeleccionado = Post.TipoMultimedia.IMAGEN_HORIZONTAL; actualizarPreview(); });

        contenido.add(btnCuadrada); contenido.add(btnVertical); contenido.add(btnHorizontal);
        y += 46;

        // ── Descripción ───────────────────────────────────────
        lblDescRef = new JLabel("Descripción");
        lblDescRef.setBounds(20, y, 250, 18);
        lblDescRef.setFont(new Font("Arial", Font.BOLD, 13));
        contenido.add(lblDescRef);

        lblContador    = new JLabel("0/220", SwingConstants.RIGHT);
        lblContadorRef = lblContador;
        lblContador.setBounds(280, y, 90, 18);
        lblContador.setFont(new Font("Arial", Font.PLAIN, 11));
        lblContador.setForeground(InstagramColors.TEXTO_GRIS);
        contenido.add(lblContador);
        y += 24;

        txtDesc = new JTextArea();
        txtDesc.setFont(new Font("Arial", Font.PLAIN, 13));
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(InstagramColors.BORDE_GRIS, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        txtDesc.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { actualizarContador(); }
            public void removeUpdate(DocumentEvent e)  { actualizarContador(); }
            public void changedUpdate(DocumentEvent e) { actualizarContador(); }
        });

        JScrollPane scrollDesc = new JScrollPane(txtDesc);
        scrollDescRef = scrollDesc;
        scrollDesc.setBounds(20, y, 350, 100);
        scrollDesc.setBorder(BorderFactory.createLineBorder(InstagramColors.BORDE_GRIS, 1));
        contenido.add(scrollDesc);
        y += 110;

        // ── Info ──────────────────────────────────────────────
        lblInfoRef = new JLabel("<html><small style='color:gray'>Usa # para hashtags y @ para menciones</small></html>");
        lblInfoRef.setBounds(20, y, 350, 18);
        contenido.add(lblInfoRef);
        y += 30;

        // ── Feedback ──────────────────────────────────────────
        lblFeedback = new JLabel("", SwingConstants.CENTER);
        lblFeedback.setBounds(20, y, 350, 24);
        lblFeedback.setFont(new Font("Arial", Font.PLAIN, 12));
        contenido.add(lblFeedback);
        y += 30;

        // ── Publicar ──────────────────────────────────────────
        btnPublicar = new JButton("Compartir");
        btnPublicar.setBounds(20, y, 350, 44);
        btnPublicar.setFont(new Font("Arial", Font.BOLD, 15));
        btnPublicar.setForeground(Color.WHITE);
        btnPublicar.setBackground(InstagramColors.INSTAGRAM_AZUL);
        btnPublicar.setBorderPainted(false);
        btnPublicar.setFocusPainted(false);
        btnPublicar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPublicar.addActionListener(e -> publicar());
        contenido.add(btnPublicar);

        JScrollPane scroll = new JScrollPane(contenido,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    // ── Preview ───────────────────────────────────────────────────

    private int alturaPreviewPara(Post.TipoMultimedia tipo) {
        switch (tipo) {
            case IMAGEN_VERTICAL:   return (int)(300 * 1350.0 / 1080);
            case IMAGEN_HORIZONTAL: return (int)(300 * 566.0  / 1080);
            default:                return 300;
        }
    }

    private void actualizarPreview() {
        int newH  = (imagenOriginal != null) ? alturaPreviewPara(tipoSeleccionado) : previewH;
        int delta = newH - previewH;

        lblPreview.setBounds(lblPreview.getX(), lblPreview.getY(), 300, newH);
        if (imagenOriginal != null) {
            Image img = imagenOriginal.getScaledInstance(300, newH, Image.SCALE_SMOOTH);
            lblPreview.setIcon(new ImageIcon(img));
            lblPreview.setText("");
        }
        previewH = newH;

        if (delta == 0) return;

        desplazar(btnSelecRef,     delta);
        desplazar(lblTipoRef,      delta);
        desplazar(btnCuadradaRef,  delta);
        desplazar(btnVerticalRef,  delta);
        desplazar(btnHorizontalRef,delta);
        desplazar(lblDescRef,      delta);
        desplazar(lblContadorRef,  delta);
        desplazar(scrollDescRef,   delta);
        desplazar(lblInfoRef,      delta);
        desplazar(lblFeedback,     delta);
        desplazar(btnPublicar,     delta);

        Dimension d = contenidoRef.getPreferredSize();
        contenidoRef.setPreferredSize(new Dimension(d.width, d.height + delta));
        contenidoRef.revalidate();
        contenidoRef.repaint();
    }

    private void desplazar(Component comp, int dy) {
        if (comp == null) return;
        Rectangle r = comp.getBounds();
        comp.setBounds(r.x, r.y + dy, r.width, r.height);
    }

    // ── Seleccionar imagen ────────────────────────────────────────

    private void seleccionarImagen() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Imágenes (jpg, png, jpeg)", "jpg", "jpeg", "png"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            imagenSeleccionada = fc.getSelectedFile();
            try {
                imagenOriginal = ImageIO.read(imagenSeleccionada);
                actualizarPreview();
                mostrarFeedback("✓ Imagen cargada", InstagramColors.VERDE_EXITO);
            } catch (Exception ex) {
                mostrarFeedback("❌ Error cargando imagen", InstagramColors.ROJO_ERROR);
            }
        }
    }

    // ── Contador ──────────────────────────────────────────────────

    private void actualizarContador() {
        int len = txtDesc.getText().length();
        lblContador.setText(len + "/220");
        if (len > 220) { lblContador.setForeground(InstagramColors.ROJO_ERROR); btnPublicar.setEnabled(false); }
        else           { lblContador.setForeground(InstagramColors.TEXTO_GRIS); btnPublicar.setEnabled(true);  }
    }

    // ── Publicar ──────────────────────────────────────────────────

    private void publicar() {
        String desc = txtDesc.getText().trim();
        if (imagenSeleccionada == null) { mostrarFeedback("❌ Selecciona una imagen primero",    InstagramColors.ROJO_ERROR); return; }
        if (desc.isEmpty())             { mostrarFeedback("❌ Agrega una descripción",            InstagramColors.ROJO_ERROR); return; }
        if (desc.length() > 220)        { mostrarFeedback("❌ La descripción es demasiado larga", InstagramColors.ROJO_ERROR); return; }

        btnPublicar.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                String dirImg   = "INSTA_RAIZ/" + usuario.getUsername() + "/imagenes";
                Instagram.crearDir(dirImg);
                String ext      = imagenSeleccionada.getName().replaceAll(".*\\.", "");
                String destPath = dirImg + "/post_" + System.currentTimeMillis() + "." + ext;
                Files.copy(imagenSeleccionada.toPath(), new File(destPath).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
                Post p = new Post(usuario.getUsername(), desc, destPath, tipoSeleccionado);
                p.setHashtags(ValidadorContenido.extraerHashtags(desc));
                p.setMenciones(ValidadorContenido.extraerMenciones(desc));
                postManager.publicar(p);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    mostrarFeedback("✓ Publicado correctamente", InstagramColors.VERDE_EXITO);
                    Timer t = new Timer(1500, e -> limpiar());
                    t.setRepeats(false); t.start();
                } catch (Exception ex) {
                    mostrarFeedback("❌ Error al publicar", InstagramColors.ROJO_ERROR);
                    btnPublicar.setEnabled(true);
                }
            }
        }.execute();
    }

    private void limpiar() {
        txtDesc.setText("");
        lblPreview.setIcon(null);
        lblPreview.setText("📷");
        lblPreview.setFont(new Font("Arial", Font.PLAIN, 70));
        imagenSeleccionada = null;
        imagenOriginal     = null;

        // Restaurar altura del preview a 260 (estado sin imagen)
        int delta = 260 - previewH;
        if (delta != 0) {
            lblPreview.setBounds(lblPreview.getX(), lblPreview.getY(), 300, 260);
            desplazar(btnSelecRef, delta); desplazar(lblTipoRef, delta);
            desplazar(btnCuadradaRef, delta); desplazar(btnVerticalRef, delta);
            desplazar(btnHorizontalRef, delta); desplazar(lblDescRef, delta);
            desplazar(lblContadorRef, delta); desplazar(scrollDescRef, delta);
            desplazar(lblInfoRef, delta); desplazar(lblFeedback, delta);
            desplazar(btnPublicar, delta);
            Dimension d = contenidoRef.getPreferredSize();
            contenidoRef.setPreferredSize(new Dimension(d.width, d.height + delta));
            contenidoRef.revalidate(); contenidoRef.repaint();
        }
        previewH = 260;
        btnCuadrada.setSelected(true);
        tipoSeleccionado = Post.TipoMultimedia.IMAGEN_CUADRADA;
        btnPublicar.setEnabled(true);
        actualizarContador();
    }

    /** Llamado por MainFrame cada vez que se navega a CREATE para reiniciar el formulario */
    public void resetear() {
        limpiar();
        lblFeedback.setText("");
    }

    private void mostrarFeedback(String txt, Color color) {
        lblFeedback.setText(txt);
        lblFeedback.setForeground(color);
    }

    // ── Helpers UI ────────────────────────────────────────────────

    private JPanel headerInterno(String titulo) {
        JPanel h = new JPanel(null);
        h.setPreferredSize(new Dimension(390, 46));
        h.setBackground(Color.WHITE);
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, InstagramColors.BORDE_GRIS));
        JLabel lbl = new JLabel(titulo, SwingConstants.CENTER);
        lbl.setBounds(0, 10, 390, 26);
        lbl.setFont(new Font("Arial", Font.BOLD, 15));
        h.add(lbl);
        return h;
    }

    private JToggleButton toggleBoton(String texto) {
        JToggleButton btn = new JToggleButton(texto);
        btn.setFont(new Font("Arial", Font.PLAIN, 12));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createLineBorder(InstagramColors.BORDE_GRIS, 1));
        btn.setBackground(Color.WHITE);
        btn.setForeground(InstagramColors.TEXTO_NEGRO);
        btn.addChangeListener(e -> {
            if (btn.isSelected()) { btn.setBackground(InstagramColors.TEXTO_NEGRO); btn.setForeground(Color.WHITE); }
            else                  { btn.setBackground(Color.WHITE); btn.setForeground(InstagramColors.TEXTO_NEGRO); }
        });
        return btn;
    }
}