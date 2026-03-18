/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Abstracts;

/**
 *
 * @author janinadiaz
 */



import instagram.Utilities.Comment;
import instagram.Interfaces.Comentable;
import instagram.Interfaces.Likeable;
import instagram.Listas.ListaComentarios;
import java.util.Date;
import java.util.Calendar;
import java.util.ArrayList;

public abstract class Publicacion implements Likeable, Comentable {
    
    protected String id;
    protected String username;
    protected String contenido;
    protected Date fecha;
    protected Calendar hora;
    protected ArrayList<String> hashtags;
    protected ArrayList<String> menciones;
    protected ArrayList<String> usuariosLikes;
    protected ListaComentarios comentarios;
    
    public Publicacion() {
        this.fecha = new Date();
        this.hora = Calendar.getInstance();
        this.hashtags = new ArrayList<>();
        this.menciones = new ArrayList<>();
        this.usuariosLikes = new ArrayList<>();
        this.comentarios = new ListaComentarios();
        this.id = generarID();
    }
    
    private String generarID() {
        return username + "_" + System.currentTimeMillis();
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
    
    // Implementación de Comentable
    @Override
    public void agregarComentario(Comment comentario) {
        comentarios.agregar(comentario);
    }
    
    @Override
    public ListaComentarios obtenerComentarios() {
        return comentarios;
    }
    
    @Override
    public int contarComentarios() {
        return comentarios.getTamanio();
    }
    
    // Método abstracto para polimorfismo
    public abstract String getTipoPublicacion();
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    
    public Calendar getHora() { return hora; }
    public void setHora(Calendar hora) { this.hora = hora; }
    
    public ArrayList<String> getHashtags() { return hashtags; }
    public void setHashtags(ArrayList<String> hashtags) { this.hashtags = hashtags; }
    
    public ArrayList<String> getMenciones() { return menciones; }
    public void setMenciones(ArrayList<String> menciones) { this.menciones = menciones; }
    
    public ArrayList<String> getUsuariosLikes() { return usuariosLikes; }
    public void setUsuariosLikes(ArrayList<String> usuariosLikes) { 
        this.usuariosLikes = usuariosLikes; 
    }

    public String getTiempoTranscurrido() {
        long d = new java.util.Date().getTime() - fecha.getTime();
        long s=d/1000, m=s/60, h=m/60, dias=h/24;
        if(s<60) return "ahora";
        if(m<60) return m+"m";
        if(h<24) return h+"h";
        if(dias<7) return dias+"d";
        return (dias/7)+"sem";
    }
}
