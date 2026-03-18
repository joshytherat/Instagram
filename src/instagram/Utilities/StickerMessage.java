package instagram.Utilities;

import instagram.Abstracts.Message;
import java.io.File;
import java.io.IOException;

public class StickerMessage extends Message {

    private String stickerCode;

    // PNGs predeterminados — están en INSTA_RAIZ/stickers_globales/
    public static final String[] BUILTIN_CODES = {
        "Feliz", "Triste", "Corazon", "Risa", "Aplauso"
    };

    public StickerMessage() {
        super();
        this.tipo = TipoMensaje.STICKER;
    }

    public StickerMessage(String emisor, String receptor, String stickerCode) {
        super(emisor, receptor, stickerCode);
        this.tipo = TipoMensaje.STICKER;
        this.stickerCode = stickerCode;
    }

    @Override
    public void enviar() throws IOException {
        if (!validar()) throw new IOException("Sticker inválido");
    }

    @Override
    public String formatearVisualizacion() {
        return stickerCode != null ? stickerCode : "Feliz";
    }

    /**
     * Resuelve la ruta del PNG para un código de sticker.
     * Orden:
     *   1. INSTA_RAIZ/stickers_globales/<code>.png  (predeterminados)
     *   2. INSTA_RAIZ/<username>/stickers_personales/<code>.png
     *   3. Si el código ya es una ruta de archivo completa
     */
    public static String resolverRuta(String code, String username) {
        if (code == null) code = "Feliz";

        // 1. Global — aquí viven los stickers predeterminados del proyecto
        File global = new File("INSTA_RAIZ/stickers_globales/" + code + ".png");
        if (global.exists()) return global.getPath();

        // 2. Personal del usuario
        if (username != null && !username.isEmpty()) {
            File personal = new File("INSTA_RAIZ/" + username + "/stickers_personales/" + code + ".png");
            if (personal.exists()) return personal.getPath();
        }

        // 3. Ruta absoluta o relativa completa (sticker importado)
        if (code.contains("/") || code.contains(File.separator)) {
            if (new File(code).exists()) return code;
        }

        return null; // no encontrado
    }

    /**
     * Carga el PNG del sticker. Devuelve null si no existe o está corrupto.
     */
    public static java.awt.image.BufferedImage cargarImagen(String code, String username) {
        if (code == null) return null;
        String ruta = resolverRuta(code, username);
        if (ruta == null) return null;
        try {
            File f = new File(ruta);
            if (!f.exists() || f.length() == 0) return null;
            return javax.imageio.ImageIO.read(f);
        } catch (Exception e) {
            return null;
        }
    }

    public String getStickerCode() { return stickerCode; }
    public void setStickerCode(String stickerCode) { this.stickerCode = stickerCode; }
}