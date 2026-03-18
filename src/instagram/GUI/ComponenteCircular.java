package instagram.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class ComponenteCircular extends JPanel {

    private BufferedImage imagen;
    private int           diametro;
    private Color         colorBorde;
    private int           anchoBorde;
    private String        emoji;

    public ComponenteCircular(int diametro) {
        this(diametro, null);
    }

    public ComponenteCircular(int diametro, Color colorBorde) {
        this.diametro    = diametro;
        this.colorBorde  = colorBorde;
        this.anchoBorde  = colorBorde != null ? 2 : 0;
        this.emoji       = "👤";
        setPreferredSize(new Dimension(diametro, diametro));
        setOpaque(false);
    }

    /**
     * Asigna la imagen y fuerza un repintado completo.
     * revalidate() asegura que el layout recalcule tamaños
     * si el componente cambió de estado (evita imágenes viejas pegadas).
     */
    public void setImagen(BufferedImage imagen) {
        this.imagen = imagen;
        revalidate();
        repaint();
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
        repaint();
    }

    public void setColorBorde(Color colorBorde) {
        this.colorBorde = colorBorde;
        this.anchoBorde = colorBorde != null ? 2 : 0;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                             RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int x    = anchoBorde;
        int y    = anchoBorde;
        int size = diametro - (anchoBorde * 2);

        // Borde exterior (si aplica)
        if (colorBorde != null) {
            g2d.setColor(colorBorde);
            g2d.fillOval(0, 0, diametro, diametro);
        }

        // Fondo gris — placeholder mientras carga la foto
        g2d.setColor(new Color(200, 200, 200));
        g2d.fillOval(x, y, size, size);

        if (imagen != null) {
            // Clip circular para la imagen
            Ellipse2D.Double clip = new Ellipse2D.Double(x, y, size, size);
            g2d.setClip(clip);
            g2d.drawImage(imagen, x, y, size, size, null);
        } else if (emoji != null) {
            g2d.setClip(null);
            g2d.setColor(new Color(110, 110, 110));
            g2d.setFont(new Font("SansSerif", Font.PLAIN, size / 2));
            FontMetrics fm = g2d.getFontMetrics();
            int ex = x + (size - fm.stringWidth(emoji)) / 2;
            int ey = y + ((size - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(emoji, ex, ey);
        }

        g2d.dispose();
    }
}