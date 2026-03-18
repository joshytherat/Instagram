/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Abstracts;

/**
 *
 * @author janinadiaz
 */

import instagram.Interfaces.Enviable;
import java.io.Serializable;
import java.util.Date;
import java.util.Calendar;

public abstract class Message implements Enviable, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public static final int TAMANIO_REGISTRO = 
        50 +    // id
        50 +    // emisor
        50 +    // receptor
        8 +     // fecha (long)
        4 +     // hora (int)
        500 +   // contenido
        4 +     // tipo (int)
        4;      // estado (int)
    // TOTAL: 670 bytes por mensaje
    
    public enum TipoMensaje {
        TEXTO,
        STICKER,
        IMAGEN,
        AUDIO
    }
    
    public enum EstadoMensaje {
        ENVIADO,
        ENTREGADO,
        LEIDO
    }
    
    protected String id;
    protected String emisor;
    protected String receptor;
    protected Date fecha;
    protected Calendar hora;
    protected String contenido;
    protected TipoMensaje tipo;
    protected EstadoMensaje estado;
    
    public Message() {
        this.fecha = new Date();
        this.hora = Calendar.getInstance();
        this.estado = EstadoMensaje.ENVIADO;
        this.id = generarID();
    }
    
    public Message(String emisor, String receptor, String contenido) {
        this();
        this.emisor = emisor;
        this.receptor = receptor;
        this.contenido = contenido;
    }
    
    private String generarID() {
        return "msg_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
    
    @Override
    public boolean validar() {
        return emisor != null && !emisor.isEmpty() &&
               receptor != null && !receptor.isEmpty() &&
               contenido != null && !contenido.isEmpty();
    }
    
    @Override
    public String getContenido() {
        return contenido;
    }
    
    // Método abstracto para polimorfismo
    public abstract String formatearVisualizacion();
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getEmisor() { return emisor; }
    public void setEmisor(String emisor) { this.emisor = emisor; }
    
    public String getReceptor() { return receptor; }
    public void setReceptor(String receptor) { this.receptor = receptor; }
    
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    
    public Calendar getHora() { return hora; }
    public void setHora(Calendar hora) { this.hora = hora; }
    
    public void setContenido(String contenido) { this.contenido = contenido; }
    
    public TipoMensaje getTipo() { return tipo; }
    public void setTipo(TipoMensaje tipo) { this.tipo = tipo; }
    
    public EstadoMensaje getEstado() { return estado; }
    public void setEstado(EstadoMensaje estado) { this.estado = estado; }
    
    @Override
    public String toString() {
        return emisor + " → " + receptor + ": " + contenido;
    }
}
