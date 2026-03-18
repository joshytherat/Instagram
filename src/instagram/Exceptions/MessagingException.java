/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Exception.java to edit this template
 */
package instagram.Exceptions;

/**
 *
 * @author janinadiaz
 */


public class MessagingException extends Exception {
    
    public MessagingException(String mensaje) {
        super(mensaje);
    }
    
    public MessagingException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}