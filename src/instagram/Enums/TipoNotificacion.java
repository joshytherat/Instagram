/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package instagram.Enums;

/**
 *
 * @author janinadiaz
 */

public enum TipoNotificacion {
    FOLLOW("te empezó a seguir", "👤"),
    FOLLOW_REQUEST("quiere seguirte", "👥"),
    MENTION("te mencionó en una publicación", "📣"),
    MESSAGE("te envió un mensaje", "💬"),
    COMMENT("comentó tu publicación", "💭"),
    LIKE("le dio me gusta a tu publicación", "❤️"),
    COMMENT_LIKE("le dio me gusta a tu comentario", "💙");
    
    private String descripcion;
    private String emoji;
    
    TipoNotificacion(String descripcion, String emoji) {
        this.descripcion = descripcion;
        this.emoji = emoji;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public String getEmoji() {
        return emoji;
    }
}
