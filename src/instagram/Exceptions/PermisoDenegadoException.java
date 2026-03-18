/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Exception.java to edit this template
 */
package instagram.Exceptions;

/**
 *
 * @author janinadiaz
 */


public class PermisoDenegadoException extends MessagingException {
    
    public PermisoDenegadoException(String emisor, String receptor) {
        super("No tienes permiso para enviar mensajes a @" + receptor);
    }
}