/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package instagram.Interfaces;

/**
 *
 * @author janinadiaz
 */

public interface Validable {
    boolean validar() throws Exception;
    String obtenerMensajeError();
}
