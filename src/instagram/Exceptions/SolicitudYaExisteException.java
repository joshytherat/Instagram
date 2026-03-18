/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Exception.java to edit this template
 */
package instagram.Exceptions;

/**
 *
 * @author janinadiaz
 */


public class SolicitudYaExisteException extends FollowException {
    
    public SolicitudYaExisteException() {
        super("Ya enviaste una solicitud a este usuario");
    }
}