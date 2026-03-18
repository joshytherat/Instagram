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
import instagram.Abstracts.Message.EstadoMensaje;   
import instagram.Abstracts.Message.TipoMensaje;
import java.io.*;
import java.util.*;

public class MessageManager {
    
    private static final String CARPETA_RAIZ = "INSTA_RAIZ";
    private static final String ARCHIVO_INBOX = "inbox.ins";
    
    /**
     * Guarda un mensaje
     */
    public void guardarMensaje(Message mensaje) throws IOException {
        // Guardar en inbox del emisor
        guardarEnInbox(mensaje.getEmisor(), mensaje);
        
        // Guardar en inbox del receptor
        guardarEnInbox(mensaje.getReceptor(), mensaje);
    }
    
    /**
     * Guarda mensaje en el inbox de un usuario
     */
    private void guardarEnInbox(String username, Message mensaje) throws IOException {
        String rutaInbox = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_INBOX;
        File archivo = new File(rutaInbox);
        
        // Crear archivo si no existe
        if (!archivo.exists()) {
            archivo.getParentFile().mkdirs();
            archivo.createNewFile();
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(archivo, "rw")) {
            raf.seek(raf.length()); // Al final
            escribirMensaje(raf, mensaje);
        }
    }
    
    /**
     * Obtiene mensajes de una conversación
     */
    public ArrayList<Message> obtenerConversacion(String usuario1, String usuario2) 
            throws IOException {
        
        ArrayList<Message> mensajes = new ArrayList<>();
        String rutaInbox = CARPETA_RAIZ + "/" + usuario1 + "/" + ARCHIVO_INBOX;
        File archivo = new File(rutaInbox);
        
        if (!archivo.exists()) {
            return mensajes;
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(archivo, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                Message mensaje = leerMensaje(raf);
                
                // Filtrar mensajes de la conversación
                if ((mensaje.getEmisor().equals(usuario1) && mensaje.getReceptor().equals(usuario2)) ||
                    (mensaje.getEmisor().equals(usuario2) && mensaje.getReceptor().equals(usuario1))) {
                    mensajes.add(mensaje);
                }
            }
        }
        
        // Ordenar por fecha
        mensajes.sort(Comparator.comparing(Message::getFecha));
        
        return mensajes;
    }
    
    /**
     * Obtiene lista de conversaciones de un usuario
     */
    public ArrayList<String> obtenerConversaciones(String username) throws IOException {
        Set<String> conversaciones = new HashSet<>();
        String rutaInbox = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_INBOX;
        File archivo = new File(rutaInbox);
        
        if (!archivo.exists()) {
            return new ArrayList<>();
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(archivo, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                Message mensaje = leerMensaje(raf);
                
                // Agregar el otro usuario de la conversación
                if (mensaje.getEmisor().equals(username)) {
                    conversaciones.add(mensaje.getReceptor());
                } else {
                    conversaciones.add(mensaje.getEmisor());
                }
            }
        }
        
        return new ArrayList<>(conversaciones);
    }
    
    /**
     * Obtiene mensajes no entregados
     */
    public ArrayList<Message> obtenerMensajesNoEntregados(String username) 
            throws IOException {
        
        ArrayList<Message> noEntregados = new ArrayList<>();
        String rutaInbox = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_INBOX;
        File archivo = new File(rutaInbox);
        
        if (!archivo.exists()) {
            return noEntregados;
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(archivo, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                Message mensaje = leerMensaje(raf);
                
                if (mensaje.getReceptor().equals(username) && 
                    mensaje.getEstado() == EstadoMensaje.ENVIADO) {
                    noEntregados.add(mensaje);
                }
            }
        }
        
        return noEntregados;
    }
    
    /**
     * Marca un mensaje como LEIDO dado su ID, buscando en todos los inboxes.
     * Escanea CARPETA_RAIZ buscando el mensaje por ID y actualiza su estado.
     */
    public void marcarComoLeido(String messageId) throws IOException {
        if (messageId == null || messageId.isEmpty()) return;
        File raiz = new File(CARPETA_RAIZ);
        if (!raiz.exists()) return;
        for (File userDir : raiz.listFiles(f -> f.isDirectory())) {
            File inbox = new File(userDir, ARCHIVO_INBOX);
            if (!inbox.exists()) continue;
            String username = userDir.getName();
            ArrayList<Message> mensajes = obtenerTodosMensajes(username);
            boolean cambio = false;
            for (Message m : mensajes) {
                if (m.getId().equals(messageId)
                        && m.getEstado() != Message.EstadoMensaje.LEIDO) {
                    m.setEstado(Message.EstadoMensaje.LEIDO);
                    cambio = true;
                }
            }
            if (cambio) reescribirInbox(username, mensajes);
        }
    }
    
    /**
     * Actualiza el estado de un mensaje en el inbox de ambos usuarios.
     * Busca por ID y reescribe solo el campo de estado.
     */
    public void actualizarEstadoMensaje(Message mensaje) throws IOException {
        actualizarEstadoEnInbox(mensaje.getEmisor(), mensaje);
        actualizarEstadoEnInbox(mensaje.getReceptor(), mensaje);
    }

    private void actualizarEstadoEnInbox(String username, Message target) throws IOException {
        String ruta = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_INBOX;
        File archivo = new File(ruta);
        if (!archivo.exists()) return;

        ArrayList<Message> mensajes = obtenerTodosMensajes(username);
        boolean cambio = false;
        for (Message m : mensajes) {
            if (m.getId().equals(target.getId())) {
                m.setEstado(target.getEstado());
                cambio = true;
            }
        }
        if (cambio) reescribirInbox(username, mensajes);
    }
    
    /**
     * Elimina una conversación
     */
    public void eliminarConversacion(String usuario1, String usuario2) throws IOException {
        ArrayList<Message> todosMensajes = obtenerTodosMensajes(usuario1);
        
        // Filtrar mensajes que NO son de esta conversación
        ArrayList<Message> mensajesFiltrados = new ArrayList<>();
        for (Message msg : todosMensajes) {
            boolean esDeConversacion = 
                (msg.getEmisor().equals(usuario1) && msg.getReceptor().equals(usuario2)) ||
                (msg.getEmisor().equals(usuario2) && msg.getReceptor().equals(usuario1));
            
            if (!esDeConversacion) {
                mensajesFiltrados.add(msg);
            }
        }
        
        // Reescribir archivo
        reescribirInbox(usuario1, mensajesFiltrados);
    }
    
    /**
     * Obtiene todos los mensajes de un usuario
     */
    private ArrayList<Message> obtenerTodosMensajes(String username) throws IOException {
        ArrayList<Message> mensajes = new ArrayList<>();
        String rutaInbox = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_INBOX;
        File archivo = new File(rutaInbox);
        
        if (!archivo.exists()) {
            return mensajes;
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(archivo, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                mensajes.add(leerMensaje(raf));
            }
        }
        
        return mensajes;
    }
    
    /**
     * Reescribe el inbox completo
     */
    private void reescribirInbox(String username, ArrayList<Message> mensajes) 
            throws IOException {
        
        String rutaInbox = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_INBOX;
        
        try (RandomAccessFile raf = new RandomAccessFile(rutaInbox, "rw")) {
            raf.setLength(0); // Limpiar
            
            for (Message mensaje : mensajes) {
                escribirMensaje(raf, mensaje);
            }
        }
    }
    
    /**
     * Escribe un mensaje en RandomAccessFile
     */
    private void escribirMensaje(RandomAccessFile raf, Message mensaje) throws IOException {
        // ID (50 bytes)
        escribirStringFijo(raf, mensaje.getId(), 50);
        
        // Emisor (50 bytes)
        escribirStringFijo(raf, mensaje.getEmisor(), 50);
        
        // Receptor (50 bytes)
        escribirStringFijo(raf, mensaje.getReceptor(), 50);
        
        // Fecha (8 bytes)
        raf.writeLong(mensaje.getFecha().getTime());
        
        // Hora - minutos del día (4 bytes)
        int minutos = mensaje.getHora().get(Calendar.HOUR_OF_DAY) * 60 +
                     mensaje.getHora().get(Calendar.MINUTE);
        raf.writeInt(minutos);
        
        // Contenido (500 bytes)
        escribirStringFijo(raf, mensaje.getContenido(), 500);
        
        // Tipo (4 bytes)
        raf.writeInt(mensaje.getTipo().ordinal());
        
        // Estado (4 bytes)
        raf.writeInt(mensaje.getEstado().ordinal());
    }
    
    /**
     * Lee un mensaje desde RandomAccessFile
     */
    private Message leerMensaje(RandomAccessFile raf) throws IOException {
        // ID
        String id = leerStringFijo(raf, 50);
        
        // Emisor
        String emisor = leerStringFijo(raf, 50);
        
        // Receptor
        String receptor = leerStringFijo(raf, 50);
        
        // Fecha
        Date fecha = new Date(raf.readLong());
        
        // Hora
        int minutos = raf.readInt();
        Calendar hora = Calendar.getInstance();
        hora.set(Calendar.HOUR_OF_DAY, minutos / 60);
        hora.set(Calendar.MINUTE, minutos % 60);
        
        // Contenido
        String contenido = leerStringFijo(raf, 500);
        
        // Tipo
        TipoMensaje tipo = TipoMensaje.values()[raf.readInt()];
        
        // Estado
        EstadoMensaje estado = EstadoMensaje.values()[raf.readInt()];
        
        // Crear mensaje según tipo
        Message mensaje;
        if (tipo == TipoMensaje.STICKER) {
            mensaje = new StickerMessage(emisor, receptor, contenido);
        } else {
            mensaje = new TextMessage(emisor, receptor, contenido);
        }
        
        mensaje.setId(id);
        mensaje.setFecha(fecha);
        mensaje.setHora(hora);
        mensaje.setEstado(estado);
        
        return mensaje;
    }
    
    private void escribirStringFijo(RandomAccessFile raf, String str, int tamanio) 
            throws IOException {
        if (str == null) str = "";
        StringBuilder sb = new StringBuilder(str);
        sb.setLength(tamanio);
        for (int i = 0; i < tamanio; i++) {
            raf.writeChar(sb.charAt(i));
        }
    }
    
    private String leerStringFijo(RandomAccessFile raf, int tamanio) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tamanio; i++) {
            sb.append(raf.readChar());
        }
        return sb.toString().trim();
    }
}