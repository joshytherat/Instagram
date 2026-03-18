/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Exception.java to edit this template
 */
package instagram.Exceptions;

/**
 *
 * @author janinadiaz
 */

public class UserException extends Exception {
    
    public UserException(String mensaje) {
        super(mensaje);
    }
    
    public UserException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}