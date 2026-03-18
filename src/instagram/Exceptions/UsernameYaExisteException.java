/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Exception.java to edit this template
 */
package instagram.Exceptions;

/**
 *
 * @author janinadiaz
 */

public class UsernameYaExisteException extends UserException {
    
    public UsernameYaExisteException(String username) {
        super("El username '" + username + "' ya está en uso");
    }
}
