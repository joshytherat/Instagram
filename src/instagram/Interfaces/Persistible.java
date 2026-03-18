/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package instagram.Interfaces;

/**
 *
 * @author janinadiaz
 */

import java.io.IOException;

public interface Persistible {
    void guardar() throws IOException;
    void cargar() throws IOException;
    void eliminar() throws IOException;
}