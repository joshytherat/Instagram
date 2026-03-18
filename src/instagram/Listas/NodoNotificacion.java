/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Listas;

import instagram.Utilities.Notification;

/**
 *
 * @author janinadiaz
 */



public class NodoNotificacion {
    
    private Notification notificacion;
    private NodoNotificacion siguiente;
    
    public NodoNotificacion(Notification notificacion) {
        this.notificacion = notificacion;
        this.siguiente = null;
    }
    
    public Notification getNotificacion() {
        return notificacion;
    }
    
    public void setNotificacion(Notification notificacion) {
        this.notificacion = notificacion;
    }
    
    public NodoNotificacion getSiguiente() {
        return siguiente;
    }
    
    public void setSiguiente(NodoNotificacion siguiente) {
        this.siguiente = siguiente;
    }
}