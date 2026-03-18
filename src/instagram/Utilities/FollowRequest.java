/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Utilities;

/**
 *
 * @author janinadiaz
 */


import instagram.Enums.EstadoSolicitud;
import java.util.Date;

public class FollowRequest {
    
    public static final int TAMANIO_REGISTRO = 
        50 + // remitenteUsername
        50 + // destinatarioUsername
        8 +  // fechaSolicitud (long)
        4;   // estado (int)
    // TOTAL: 112 bytes
    
    private String remitenteUsername;
    private String destinatarioUsername;
    private Date fechaSolicitud;
    private EstadoSolicitud estado;
    
    
    
    public FollowRequest() {
        this.fechaSolicitud = new Date();
        this.estado = EstadoSolicitud.PENDIENTE;
    }
    
    public FollowRequest(String remitente, String destinatario) {
        this();
        this.remitenteUsername = remitente;
        this.destinatarioUsername = destinatario;
    }
    
    // Getters y Setters
    public String getRemitenteUsername() {
        return remitenteUsername;
    }
    
    public void setRemitenteUsername(String remitenteUsername) {
        this.remitenteUsername = remitenteUsername;
    }
    
    public String getDestinatarioUsername() {
        return destinatarioUsername;
    }
    
    public void setDestinatarioUsername(String destinatarioUsername) {
        this.destinatarioUsername = destinatarioUsername;
    }
    
    public Date getFechaSolicitud() {
        return fechaSolicitud;
    }
    
    public void setFechaSolicitud(Date fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }
    
    public EstadoSolicitud getEstado() {
        return estado;
    }
    
    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }
    
    @Override
    public String toString() {
        return "FollowRequest{" +
                "remitente='" + remitenteUsername + '\'' +
                ", destinatario='" + destinatarioUsername + '\'' +
                ", estado=" + estado +
                '}';
    }
}
