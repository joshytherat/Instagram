/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Listas;

import instagram.Utilities.Comment;

/**
 *
 * @author janinadiaz
 */


public class NodoComentario {
    
    private Comment comentario;
    private NodoComentario siguiente;
    
    public NodoComentario(Comment comentario) {
        this.comentario = comentario;
        this.siguiente = null;
    }
    
    public Comment getComentario() {
        return comentario;
    }
    
    public void setComentario(Comment comentario) {
        this.comentario = comentario;
    }
    
    public NodoComentario getSiguiente() {
        return siguiente;
    }
    
    public void setSiguiente(NodoComentario siguiente) {
        this.siguiente = siguiente;
    }
}
