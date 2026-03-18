/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package instagram.Interfaces;

/**
 *
 * @author janinadiaz
 */

public interface Likeable {
    void darLike(String username);
    void quitarLike(String username);
    boolean tienelike(String username);
    int contarLikes();
}