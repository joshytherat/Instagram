/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Utilities;

/**
 *
 * @author janinadiaz
 */


import instagram.Interfaces.Likeable;
import java.util.Date;
import java.util.ArrayList;

public class Comment implements Likeable {
    
    public static final int TAMANIO_REGISTRO = 
        50 +    // id
        50 +    // postId
        50 +    // username
        220 +   // contenido
        8 +     // fecha (long)
        200;    // usuariosLikes
    // TOTAL: 578 bytes por comentario
    
    private String id;
    private String postId;
    private String username;
    private String contenido;
    private Date fecha;
    private ArrayList<String> usuariosLikes;
    
    public Comment() {
        this.fecha = new Date();
        this.usuariosLikes = new ArrayList<>();
        this.id = generarID();
    }
    
    public Comment(String postId, String username, String contenido) {
        this();
        this.postId = postId;
        this.username = username;
        this.contenido = contenido;
    }
    
    private String generarID() {
        return "comment_" + System.currentTimeMillis() + "_" + 
               (int)(Math.random() * 1000);
    }
    
    // Implementación de Likeable
    @Override
    public void darLike(String username) {
        if (!usuariosLikes.contains(username)) {
            usuariosLikes.add(username);
        }
    }
    
    @Override
    public void quitarLike(String username) {
        usuariosLikes.remove(username);
    }
    
    @Override
    public boolean tienelike(String username) {
        return usuariosLikes.contains(username);
    }
    
    @Override
    public int contarLikes() {
        return usuariosLikes.size();
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    
    public ArrayList<String> getUsuariosLikes() { return usuariosLikes; }
    public void setUsuariosLikes(ArrayList<String> usuariosLikes) { 
        this.usuariosLikes = usuariosLikes; 
    }
    
    @Override
    public String toString() {
        return username + ": " + contenido;
    }
}