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
import java.io.Serializable;
import java.util.Date;

public class Notification implements Serializable, Comparable<Notification> {
    
    private static final long serialVersionUID = 1L;
    
    public static final int TAMANIO_REGISTRO = 
        50 +    // id
        50 +    // emisor
        50 +    // receptor
        4 +     // tipo (int)
        8 +     // fecha (long)
        200 +   // contenido extra
        4;      // leida (boolean -> int)
    // TOTAL: 366 bytes por notificación
    
    private String id;
    private String emisor;
    private String receptor;
    private TipoNotificacion tipo;
    private Date fecha;
    private String contenidoExtra; // ID de post, mensaje, etc.
    private boolean leida;
    
    public Notification() {
        this.fecha = new Date();
        this.leida = false;
        this.id = generarID();
    }
    
    public Notification(String emisor, String receptor, TipoNotificacion tipo) {
        this();
        this.emisor = emisor;
        this.receptor = receptor;
        this.tipo = tipo;
    }
    
    public Notification(String emisor, String receptor, TipoNotificacion tipo, String contenidoExtra) {
        this(emisor, receptor, tipo);
        this.contenidoExtra = contenidoExtra;
    }
    
    private String generarID() {
        return "notif_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
    
    /**
     * Genera el mensaje de la notificación
     */
    public String generarMensaje() {
        return "@" + emisor + " " + tipo.getDescripcion();
    }
    
    /**
     * Obtiene el tiempo transcurrido
     */
    public String getTiempoTranscurrido() {
        long diff = new Date().getTime() - fecha.getTime();
        long segundos = diff / 1000;
        long minutos = segundos / 60;
        long horas = minutos / 60;
        long dias = horas / 24;
        long semanas = dias / 7;
        
        if (segundos < 60) return "ahora";
        if (minutos < 60) return minutos + "m";
        if (horas < 24) return horas + "h";
        if (dias < 7) return dias + "d";
        if (semanas < 4) return semanas + "sem";
        
        return (dias / 30) + " meses";
    }
    
    /**
     * Comparador para ordenar por fecha (más reciente primero)
     */
    @Override
    public int compareTo(Notification otra) {
        return otra.fecha.compareTo(this.fecha);
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getEmisor() { return emisor; }
    public void setEmisor(String emisor) { this.emisor = emisor; }
    
    public String getReceptor() { return receptor; }
    public void setReceptor(String receptor) { this.receptor = receptor; }
    
    public TipoNotificacion getTipo() { return tipo; }
    public void setTipo(TipoNotificacion tipo) { this.tipo = tipo; }
    
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    
    public String getContenidoExtra() { return contenidoExtra; }
    public void setContenidoExtra(String contenidoExtra) { this.contenidoExtra = contenidoExtra; }
    
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    
    @Override
    public String toString() {
        return generarMensaje() + " - " + getTiempoTranscurrido();
    }
}
