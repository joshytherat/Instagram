/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package instagram.Exceptions;

/**
 *
 * @author janinadiaz
 */


public class UsuarioNoConectadoException extends MessagingException {
    
    public UsuarioNoConectadoException(String username) {
        super("El usuario @" + username + " no está conectado");
    }
}