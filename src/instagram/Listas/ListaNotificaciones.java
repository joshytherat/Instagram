/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Listas;

/**
 *
 * @author janinadiaz
 */

import instagram.Enums.TipoNotificacion;
import instagram.Utilities.Notification;
import java.util.ArrayList;

public class ListaNotificaciones {
    
    private NodoNotificacion cabeza;
    private int tamanio;
    
    public ListaNotificaciones() {
        this.cabeza = null;
        this.tamanio = 0;
    }
    
    /**
     * Agrega una notificación al inicio (más recientes primero)
     */
    public void agregar(Notification notificacion) {
        NodoNotificacion nuevoNodo = new NodoNotificacion(notificacion);
        
        if (cabeza == null) {
            cabeza = nuevoNodo;
        } else {
            nuevoNodo.setSiguiente(cabeza);
            cabeza = nuevoNodo;
        }
        
        tamanio++;
    }
    
    /**
     * Busca una notificación por ID (RECURSIVO)
     */
    public Notification buscar(String id) {
        return buscarRecursivo(cabeza, id);
    }
    
    private Notification buscarRecursivo(NodoNotificacion nodo, String id) {
        if (nodo == null) {
            return null;
        }
        if (nodo.getNotificacion().getId().equals(id)) {
            return nodo.getNotificacion();
        }
        return buscarRecursivo(nodo.getSiguiente(), id);
    }
    
    /**
     * Cuenta notificaciones no leídas (RECURSIVO)
     */
    public int contarNoLeidas() {
        return contarNoLeidasRecursivo(cabeza);
    }
    
    private int contarNoLeidasRecursivo(NodoNotificacion nodo) {
        if (nodo == null) {
            return 0;
        }
        int cuenta = nodo.getNotificacion().isLeida() ? 0 : 1;
        return cuenta + contarNoLeidasRecursivo(nodo.getSiguiente());
    }
    
    /**
     * Marca todas como leídas (RECURSIVO)
     */
    public void marcarTodasLeidas() {
        marcarTodasLeidasRecursivo(cabeza);
    }
    
    private void marcarTodasLeidasRecursivo(NodoNotificacion nodo) {
        if (nodo == null) {
            return;
        }
        nodo.getNotificacion().setLeida(true);
        marcarTodasLeidasRecursivo(nodo.getSiguiente());
    }
    
    /**
     * Convierte a ArrayList (RECURSIVO)
     */
    public ArrayList<Notification> toArrayList() {
        ArrayList<Notification> lista = new ArrayList<>();
        toArrayListRecursivo(cabeza, lista);
        return lista;
    }
    
    private void toArrayListRecursivo(NodoNotificacion nodo, ArrayList<Notification> lista) {
        if (nodo == null) {
            return;
        }
        lista.add(nodo.getNotificacion());
        toArrayListRecursivo(nodo.getSiguiente(), lista);
    }
    
    /**
     * Filtra por tipo
     */
    public ArrayList<Notification> filtrarPorTipo(TipoNotificacion tipo) {
        ArrayList<Notification> filtradas = new ArrayList<>();
        NodoNotificacion actual = cabeza;
        
        while (actual != null) {
            if (actual.getNotificacion().getTipo() == tipo) {
                filtradas.add(actual.getNotificacion());
            }
            actual = actual.getSiguiente();
        }
        
        return filtradas;
    }
    
    /**
     * Elimina una notificación por ID
     */
    public boolean eliminar(String id) {
        if (cabeza == null) {
            return false;
        }
        
        if (cabeza.getNotificacion().getId().equals(id)) {
            cabeza = cabeza.getSiguiente();
            tamanio--;
            return true;
        }
        
        NodoNotificacion actual = cabeza;
        while (actual.getSiguiente() != null) {
            if (actual.getSiguiente().getNotificacion().getId().equals(id)) {
                actual.setSiguiente(actual.getSiguiente().getSiguiente());
                tamanio--;
                return true;
            }
            actual = actual.getSiguiente();
        }
        
        return false;
    }
    
    /**
     * Limpia la lista
     */
    public void limpiar() {
        cabeza = null;
        tamanio = 0;
    }
    
    public int getTamanio() {
        return tamanio;
    }
    
    public boolean estaVacia() {
        return cabeza == null;
    }
    
    public NodoNotificacion getCabeza() {
        return cabeza;
    }
}