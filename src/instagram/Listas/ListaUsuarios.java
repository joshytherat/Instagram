/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Listas;

/**
 *
 * @author janinadiaz
 */

import java.util.ArrayList;

public class ListaUsuarios {
    
    private NodoUsuario cabeza;
    private int tamanio;
    
    public ListaUsuarios() {
        this.cabeza = null;
        this.tamanio = 0;
    }
    
    /**
     * Agrega un usuario a la lista (si no existe)
     */
    public boolean agregar(String username) {
        if (contiene(username)) {
            return false; // Ya existe
        }
        
        NodoUsuario nuevoNodo = new NodoUsuario(username);
        
        if (cabeza == null) {
            cabeza = nuevoNodo;
        } else {
            NodoUsuario actual = cabeza;
            while (actual.getSiguiente() != null) {
                actual = actual.getSiguiente();
            }
            actual.setSiguiente(nuevoNodo);
        }
        
        tamanio++;
        return true;
    }
    
    /**
     * Elimina un usuario de la lista
     */
    public boolean eliminar(String username) {
        if (cabeza == null) {
            return false;
        }
        
        // Si es el primero
        if (cabeza.getUsername().equals(username)) {
            cabeza = cabeza.getSiguiente();
            tamanio--;
            return true;
        }
        
        // Buscar en el resto
        NodoUsuario actual = cabeza;
        while (actual.getSiguiente() != null) {
            if (actual.getSiguiente().getUsername().equals(username)) {
                actual.setSiguiente(actual.getSiguiente().getSiguiente());
                tamanio--;
                return true;
            }
            actual = actual.getSiguiente();
        }
        
        return false;
    }
    
    /**
     * Verifica si contiene un usuario (RECURSIVO)
     */
    public boolean contiene(String username) {
        return contieneRecursivo(cabeza, username);
    }
    
    private boolean contieneRecursivo(NodoUsuario nodo, String username) {
        if (nodo == null) {
            return false;
        }
        if (nodo.getUsername().equals(username)) {
            return true;
        }
        return contieneRecursivo(nodo.getSiguiente(), username);
    }
    
    /**
     * Convierte la lista a ArrayList
     */
    public ArrayList<String> toArrayList() {
        ArrayList<String> lista = new ArrayList<>();
        NodoUsuario actual = cabeza;
        
        while (actual != null) {
            lista.add(actual.getUsername());
            actual = actual.getSiguiente();
        }
        
        return lista;
    }
    
    /**
     * Busca un usuario recursivamente
     */
    public String buscar(String username) {
        return buscarRecursivo(cabeza, username);
    }
    
    private String buscarRecursivo(NodoUsuario nodo, String username) {
        if (nodo == null) {
            return null;
        }
        if (nodo.getUsername().equals(username)) {
            return nodo.getUsername();
        }
        return buscarRecursivo(nodo.getSiguiente(), username);
    }
    
    /**
     * Obtiene el tamaño de la lista
     */
    public int getTamanio() {
        return tamanio;
    }
    
    /**
     * Verifica si está vacía
     */
    public boolean estaVacia() {
        return cabeza == null;
    }
    
    /**
     * Limpia toda la lista
     */
    public void limpiar() {
        cabeza = null;
        tamanio = 0;
    }
    
    /**
     * Imprime la lista (para debug)
     */
    public void imprimir() {
        NodoUsuario actual = cabeza;
        while (actual != null) {
            System.out.print(actual.getUsername() + " -> ");
            actual = actual.getSiguiente();
        }
        System.out.println("null");
    }
    
    public NodoUsuario getCabeza() {
        return cabeza;
    }
}
