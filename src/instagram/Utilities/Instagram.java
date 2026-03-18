package instagram.Utilities;

import instagram.GUI.MainFrame;
import javax.swing.*;
import java.io.*;
import java.nio.file.*;

/**
 * Punto de entrada único de la aplicación.
 * Lanza MainFrame, que es la única ventana del sistema.
 * Main.java ya no se usa — este archivo lo reemplaza.
 */
public class Instagram {

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}

        inicializarSistema();
        copiarRecursos();

        SwingUtilities.invokeLater(MainFrame::new);
    }

    private static void inicializarSistema() {
        crearDir("INSTA_RAIZ");
        crearDir("INSTA_RAIZ/stickers_globales");
        crearDir("resources");
        crearArchivo("INSTA_RAIZ/users.ins");
        crearArchivo("INSTA_RAIZ/follow_requests.ins");
    }

    private static void copiarRecursos() {
        String[] imgs = {
            "instagramlogo.jpeg","home.png","nueva.png","buscar.png",
            "like.png","comentar.png","notificacion.png","mensaje.png","perfil.png"
        };
        for (String img : imgs) {
            File src = new File("resources/" + img);
            if (!src.exists()) {
                File alt = new File("../../../mnt/user-data/uploads/" + img);
                if (alt.exists()) {
                    try { Files.copy(alt.toPath(), src.toPath(), StandardCopyOption.REPLACE_EXISTING); }
                    catch (Exception ignored) {}
                }
            }
        }
    }

    public static void crearDir(String path) {
        File d = new File(path);
        if (!d.exists()) d.mkdirs();
    }

    public static void crearArchivo(String path) {
        File f = new File(path);
        if (!f.exists()) try { f.createNewFile(); } catch (Exception ignored) {}
    }
}