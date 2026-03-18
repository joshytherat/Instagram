/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Listas;

/**
 *
 * @author janinadiaz
 */
import instagram.Utilities.Comment;
import java.util.ArrayList;
public class ListaComentarios {
    
    private NodoComentario cabeza;
    private int tamanio;
    
    public ListaComentarios() {
        this.cabeza = null;
        this.tamanio = 0;
    }
    
    /**
     * Agrega un comentario al final de la lista
     */
    public void agregar(Comment comentario) {
        NodoComentario nuevoNodo = new NodoComentario(comentario);
        
        if (cabeza == null) {
            cabeza = nuevoNodo;
        } else {
            NodoComentario actual = cabeza;
            while (actual.getSiguiente() != null) {
                actual = actual.getSiguiente();
            }
            actual.setSiguiente(nuevoNodo);
        }
        
        tamanio++;
    }
    
    /**
     * Busca un comentario por ID (RECURSIVO)
     */
    public Comment buscar(String id) {
        return buscarRecursivo(cabeza, id);
    }
    
    private Comment buscarRecursivo(NodoComentario nodo, String id) {
        if (nodo == null) {
            return null;
        }
        if (nodo.getComentario().getId().equals(id)) {
            return nodo.getComentario();
        }
        return buscarRecursivo(nodo.getSiguiente(), id);
    }
    
    /**
     * Convierte la lista a ArrayList (RECURSIVO)
     */
    public ArrayList<Comment> toArrayList() {
        ArrayList<Comment> lista = new ArrayList<>();
        toArrayListRecursivo(cabeza, lista);
        return lista;
    }
    
    private void toArrayListRecursivo(NodoComentario nodo, ArrayList<Comment> lista) {
        if (nodo == null) {
            return;
        }
        lista.add(nodo.getComentario());
        toArrayListRecursivo(nodo.getSiguiente(), lista);
    }
    
    /**
     * Cuenta elementos recursivamente
     */
    public int contarRecursivo() {
        return contarRecursivo(cabeza);
    }
    
    private int contarRecursivo(NodoComentario nodo) {
        if (nodo == null) {
            return 0;
        }
        return 1 + contarRecursivo(nodo.getSiguiente());
    }
    
    /**
     * Elimina un comentario por ID
     */
    public boolean eliminar(String id) {
        if (cabeza == null) {
            return false;
        }
        
        if (cabeza.getComentario().getId().equals(id)) {
            cabeza = cabeza.getSiguiente();
            tamanio--;
            return true;
        }
        
        NodoComentario actual = cabeza;
        while (actual.getSiguiente() != null) {
            if (actual.getSiguiente().getComentario().getId().equals(id)) {
                actual.setSiguiente(actual.getSiguiente().getSiguiente());
                tamanio--;
                return true;
            }
            actual = actual.getSiguiente();
        }
        
        return false;
    }
    
    public int getTamanio() {
        return tamanio;
    }
    
    public boolean estaVacia() {
        return cabeza == null;
    }
    
    public NodoComentario getCabeza() {
        return cabeza;
    }
}
