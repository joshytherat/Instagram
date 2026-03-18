/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Utilities;

import instagram.Abstracts.Publicacion;

/**
 *
 * @author janinadiaz
 */

public class Post extends Publicacion {
    
    public static final int TAMANIO_REGISTRO = 
        50 +    // id
        50 +    // username
        220 +   // contenido
        8 +     // fecha (long)
        4 +     // hora (int - minutos del día)
        200 +   // hashtags (String concatenado)
        200 +   // menciones (String concatenado)
        200 +   // rutaImagen
        4 +     // tipoMultimedia (int)
        200 +   // usuariosLikes (String concatenado)
        4;      // cantidadComentarios (int)
    // TOTAL: 1140 bytes por post
    
    public enum TipoMultimedia {
        IMAGEN_CUADRADA,    // 1080x1080
        IMAGEN_VERTICAL,    // 1080x1350
        IMAGEN_HORIZONTAL,  // 1080x566
        VIDEO,
        NINGUNO
    }
    
    private String rutaImagen;
    private TipoMultimedia tipoMultimedia;
    
    public Post() {
        super();
        this.tipoMultimedia = TipoMultimedia.NINGUNO;
    }
    
    public Post(String username, String contenido) {
        super();
        this.username = username;
        this.contenido = contenido;
        this.tipoMultimedia = TipoMultimedia.NINGUNO;
    }
    
    public Post(String username, String contenido, String rutaImagen, TipoMultimedia tipo) {
        this(username, contenido);
        this.rutaImagen = rutaImagen;
        this.tipoMultimedia = tipo;
    }
    
    @Override
    public String getTipoPublicacion() {
        return "POST";
    }
    
    public String getRutaImagen() {
        return rutaImagen;
    }
    
    public void setRutaImagen(String rutaImagen) {
        this.rutaImagen = rutaImagen;
    }
    
    public TipoMultimedia getTipoMultimedia() {
        return tipoMultimedia;
    }
    
    public void setTipoMultimedia(TipoMultimedia tipoMultimedia) {
        this.tipoMultimedia = tipoMultimedia;
    }
    
    @Override
    public String toString() {
        return "Post{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", contenido='" + contenido + '\'' +
                ", likes=" + contarLikes() +
                ", comentarios=" + contarComentarios() +
                '}';
    }
}
