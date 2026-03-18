/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package instagram.Interfaces;

import instagram.Utilities.Comment;
import instagram.Listas.ListaComentarios;

/**
 *
 * @author janinadiaz
 */


public interface Comentable {
    void agregarComentario(Comment comentario);
    ListaComentarios obtenerComentarios();
    int contarComentarios();
}