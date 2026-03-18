/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Utilities;

/**
 *
 * @author janinadiaz
 */

import instagram.Enums.TipoNotificacion;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class NotificationManager {
    
    private static final String CARPETA_RAIZ = "INSTA_RAIZ";
    private static final String ARCHIVO_NOTIFICACIONES = "notifications.ins";
    
    /**
     * Crea una nueva notificación
     */
    public void crearNotificacion(String emisor, String receptor, TipoNotificacion tipo) 
            throws IOException {
        crearNotificacion(emisor, receptor, tipo, null);
    }
    
    /**
     * Crea una nueva notificación con contenido extra
     */
    public void crearNotificacion(String emisor, String receptor, TipoNotificacion tipo, 
                                  String contenidoExtra) throws IOException {
        
        // No crear notificación si el emisor y receptor son la misma persona
        if (emisor.equals(receptor)) {
            return;
        }
        
        Notification notificacion = new Notification(emisor, receptor, tipo, contenidoExtra);
        guardarNotificacion(notificacion);
    }
    
    /**
     * Guarda una notificación
     */
    private void guardarNotificacion(Notification notificacion) throws IOException {
        String rutaNotificaciones = CARPETA_RAIZ + "/" + 
            notificacion.getReceptor() + "/" + ARCHIVO_NOTIFICACIONES;
        
        File archivo = new File(rutaNotificaciones);
        
        // Crear archivo si no existe
        if (!archivo.exists()) {
            archivo.getParentFile().mkdirs();
            archivo.createNewFile();
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(archivo, "rw")) {
            raf.seek(raf.length()); // Al final
            escribirNotificacion(raf, notificacion);
        }
    }
    
    /**
     * Obtiene todas las notificaciones de un usuario
     */
    public ArrayList<Notification> obtenerNotificaciones(String username) 
            throws IOException {
        
        ArrayList<Notification> notificaciones = new ArrayList<>();
        String rutaNotificaciones = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_NOTIFICACIONES;
        
        File archivo = new File(rutaNotificaciones);
        if (!archivo.exists()) {
            return notificaciones;
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(archivo, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                Notification notif = leerNotificacion(raf);
                notificaciones.add(notif);
            }
        }
        
        // Ordenar por fecha (más reciente primero)
        Collections.sort(notificaciones);
        
        return notificaciones;
    }
    
    /**
     * Obtiene notificaciones no leídas
     */
    public ArrayList<Notification> obtenerNoLeidas(String username) 
            throws IOException {
        
        ArrayList<Notification> todas = obtenerNotificaciones(username);
        ArrayList<Notification> noLeidas = new ArrayList<>();
        
        for (Notification notif : todas) {
            if (!notif.isLeida()) {
                noLeidas.add(notif);
            }
        }
        
        return noLeidas;
    }
    
    /**
     * Cuenta notificaciones no leídas
     */
    public int contarNoLeidas(String username) throws IOException {
        return obtenerNoLeidas(username).size();
    }
    
    /**
     * Marca una notificación como leída
     */
    public void marcarComoLeida(String username, String notificationId) 
            throws IOException {
        
        ArrayList<Notification> notificaciones = obtenerNotificaciones(username);
        boolean encontrada = false;
        
        for (Notification notif : notificaciones) {
            if (notif.getId().equals(notificationId)) {
                notif.setLeida(true);
                encontrada = true;
                break;
            }
        }
        
        if (encontrada) {
            reescribirNotificaciones(username, notificaciones);
        }
    }
    
    /**
     * Marca todas las notificaciones como leídas
     */
    public void marcarTodasComoLeidas(String username) throws IOException {
        ArrayList<Notification> notificaciones = obtenerNotificaciones(username);
        
        for (Notification notif : notificaciones) {
            notif.setLeida(true);
        }
        
        reescribirNotificaciones(username, notificaciones);
    }
    
    /**
     * Elimina una notificación
     */
    public void eliminarNotificacion(String username, String notificationId) 
            throws IOException {
        
        ArrayList<Notification> notificaciones = obtenerNotificaciones(username);
        notificaciones.removeIf(notif -> notif.getId().equals(notificationId));
        
        reescribirNotificaciones(username, notificaciones);
    }
    
    /**
     * Elimina todas las notificaciones de un usuario
     */
    public void eliminarTodas(String username) throws IOException {
        String rutaNotificaciones = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_NOTIFICACIONES;
        File archivo = new File(rutaNotificaciones);
        
        if (archivo.exists()) {
            try (RandomAccessFile raf = new RandomAccessFile(archivo, "rw")) {
                raf.setLength(0); // Limpiar archivo
            }
        }
    }
    
    /**
     * Obtiene notificaciones por tipo
     */
    public ArrayList<Notification> obtenerPorTipo(String username, TipoNotificacion tipo) 
            throws IOException {
        
        ArrayList<Notification> todas = obtenerNotificaciones(username);
        ArrayList<Notification> filtradas = new ArrayList<>();
        
        for (Notification notif : todas) {
            if (notif.getTipo() == tipo) {
                filtradas.add(notif);
            }
        }
        
        return filtradas;
    }
    
    /**
     * Obtiene notificaciones recientes (últimas 24 horas)
     */
    public ArrayList<Notification> obtenerRecientes(String username) 
            throws IOException {
        
        ArrayList<Notification> todas = obtenerNotificaciones(username);
        ArrayList<Notification> recientes = new ArrayList<>();
        
        long ahora = new Date().getTime();
        long unDia = 24 * 60 * 60 * 1000; // 24 horas en milisegundos
        
        for (Notification notif : todas) {
            if ((ahora - notif.getFecha().getTime()) <= unDia) {
                recientes.add(notif);
            }
        }
        
        return recientes;
    }
    
    /**
     * Reescribe el archivo de notificaciones
     */
    private void reescribirNotificaciones(String username, ArrayList<Notification> notificaciones) 
            throws IOException {
        
        String rutaNotificaciones = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_NOTIFICACIONES;
        
        try (RandomAccessFile raf = new RandomAccessFile(rutaNotificaciones, "rw")) {
            raf.setLength(0); // Limpiar archivo
            
            for (Notification notif : notificaciones) {
                escribirNotificacion(raf, notif);
            }
        }
    }
    
    /**
     * Escribe una notificación en RandomAccessFile
     */
    private void escribirNotificacion(RandomAccessFile raf, Notification notif) 
            throws IOException {
        
        // ID (50 bytes)
        escribirStringFijo(raf, notif.getId(), 50);
        
        // Emisor (50 bytes)
        escribirStringFijo(raf, notif.getEmisor(), 50);
        
        // Receptor (50 bytes)
        escribirStringFijo(raf, notif.getReceptor(), 50);
        
        // Tipo (4 bytes)
        raf.writeInt(notif.getTipo().ordinal());
        
        // Fecha (8 bytes)
        raf.writeLong(notif.getFecha().getTime());
        
        // Contenido extra (200 bytes)
        escribirStringFijo(raf, 
            notif.getContenidoExtra() != null ? notif.getContenidoExtra() : "", 
            200);
        
        // Leída (4 bytes)
        raf.writeInt(notif.isLeida() ? 1 : 0);
    }
    
    /**
     * Lee una notificación desde RandomAccessFile
     */
    private Notification leerNotificacion(RandomAccessFile raf) 
            throws IOException {
        
        Notification notif = new Notification();
        
        // ID
        notif.setId(leerStringFijo(raf, 50));
        
        // Emisor
        notif.setEmisor(leerStringFijo(raf, 50));
        
        // Receptor
        notif.setReceptor(leerStringFijo(raf, 50));
        
        // Tipo
        int tipoOrdinal = raf.readInt();
        notif.setTipo(TipoNotificacion.values()[tipoOrdinal]);
        
        // Fecha
        long timestamp = raf.readLong();
        notif.setFecha(new Date(timestamp));
        
        // Contenido extra
        String contenidoExtra = leerStringFijo(raf, 200);
        if (!contenidoExtra.isEmpty()) {
            notif.setContenidoExtra(contenidoExtra);
        }
        
        // Leída
        notif.setLeida(raf.readInt() == 1);
        
        return notif;
    }
    
    /**
     * Escribe string de tamaño fijo
     */
    private void escribirStringFijo(RandomAccessFile raf, String str, int tamanio) 
            throws IOException {
        
        if (str == null) str = "";
        
        StringBuilder sb = new StringBuilder(str);
        sb.setLength(tamanio);
        
        for (int i = 0; i < tamanio; i++) {
            raf.writeChar(sb.charAt(i));
        }
    }
    
    /**
     * Lee string de tamaño fijo
     */
    private String leerStringFijo(RandomAccessFile raf, int tamanio) 
            throws IOException {
        
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < tamanio; i++) {
            sb.append(raf.readChar());
        }
        
        return sb.toString().trim();
    }
}
