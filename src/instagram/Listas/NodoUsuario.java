/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Listas;

/**
 *
 * @author janinadiaz
 */


public class NodoUsuario {
    
    private String username;
    private NodoUsuario siguiente;
    
    public NodoUsuario(String username) {
        this.username = username;
        this.siguiente = null;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public NodoUsuario getSiguiente() {
        return siguiente;
    }
    
    public void setSiguiente(NodoUsuario siguiente) {
        this.siguiente = siguiente;
    }
    
    @Override
    public String toString() {
        return username;
    }
}
