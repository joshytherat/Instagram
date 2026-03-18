/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Utilities;

/**
 *
 * @author janinadiaz
 */


import instagram.Abstracts.Message;
import java.io.IOException;

public class TextMessage extends Message {
    
    public TextMessage() {
        super();
        this.tipo = TipoMensaje.TEXTO;
    }
    
    public TextMessage(String emisor, String receptor, String contenido) {
        super(emisor, receptor, contenido);
        this.tipo = TipoMensaje.TEXTO;
    }
    
    @Override
    public void enviar() throws IOException {
        if (!validar()) {
            throw new IOException("Mensaje de texto inválido");
        }
        // Lógica de envío manejada por ChatClient
    }
    
    @Override
    public String formatearVisualizacion() {
        return contenido;
    }
}