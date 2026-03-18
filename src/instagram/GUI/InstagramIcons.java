package instagram.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.net.URL;

public class InstagramIcons {


    public static ImageIcon cargar(String nombre, int w, int h) {
        if (nombre == null || nombre.isEmpty()) return null;
        try {
            // 1) Classpath (dentro del JAR o carpeta de clases compiladas)
            URL url = InstagramIcons.class.getResource("/instagram/resources/" + nombre);
            if (url == null) url = InstagramIcons.class.getResource("/resources/" + nombre);
            if (url != null) {
                BufferedImage bi = ImageIO.read(url);
                if (bi != null)
                    return new ImageIcon(bi.getScaledInstance(w, h, Image.SCALE_SMOOTH));
            }

            // 2-4) Sistema de archivos — múltiples rutas candidatas
            String[] rutas = {
                "resources/" + nombre,
                "src/resources/" + nombre,
                "src/instagram/resources/" + nombre,
                nombre  // por si el path ya es absoluto o relativo desde el root
            };
            for (String ruta : rutas) {
                File f = new File(ruta);
                if (f.exists() && f.isFile()) {
                    BufferedImage bi = ImageIO.read(f);
                    if (bi != null)
                        return new ImageIcon(bi.getScaledInstance(w, h, Image.SCALE_SMOOTH));
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Botón ícono. Si no hay imagen usa el emoji de respaldo.
     * Font size 18 (no "size") para que no se trunque a "..." en un botón de 40x40.
     */
    public static JButton botonIcono(String archivo, int size, String emojiRespaldo) {
        JButton btn = new JButton();
        ImageIcon icon = cargar(archivo, size, size);
        if (icon != null) {
            btn.setIcon(icon);
        } else {
            btn.setText(emojiRespaldo);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 18));
        }
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Alias por compatibilidad. */
    public static JButton boton(String archivo, int size, String respaldo) {
        return botonIcono(archivo, size, respaldo);
    }
}