/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Utilities;

/**
 *
 * @author janinadiaz
 */



import instagram.Listas.ListaUsuarios;
import instagram.Exceptions.FollowException;
import instagram.Exceptions.SolicitudYaExisteException;
import instagram.Exceptions.NoSiguesUsuarioException;
import instagram.Exceptions.YaSiguesUsuarioException;
import instagram.Enums.TipoNotificacion;
import instagram.Enums.EstadoSolicitud;
import instagram.Enums.TipoCuenta;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;

public class FollowManager {
    
    private static final String CARPETA_RAIZ = "INSTA_RAIZ";
    private static final String ARCHIVO_FOLLOWERS = "followers.ins";
    private static final String ARCHIVO_FOLLOWING = "following.ins";
    private static final String ARCHIVO_REQUESTS = "follow_requests.ins";
    
    private UserManager userManager;
    
    public FollowManager(UserManager userManager) {
        this.userManager = userManager;
    }
    
    /**
     * Seguir a un usuario
     */
    public void seguir(String miUsername, String targetUsername) 
            throws FollowException, IOException {
        
        // Validaciones
        if (miUsername.equals(targetUsername)) {
            throw new FollowException("No puedes seguirte a ti mismo");
        }
        
        User targetUser = userManager.buscarUsuario(targetUsername);
        if (targetUser == null) {
            throw new FollowException("El usuario @" + targetUsername + " no existe");
        }
        
        // Verificar si ya lo sigo
        if (estaSiguiendo(miUsername, targetUsername)) {
            throw new YaSiguesUsuarioException(targetUsername);
        }
        
        // Si la cuenta es privada, crear solicitud
        if (targetUser.getTipoCuenta() == TipoCuenta.PRIVADA) {
            crearSolicitud(miUsername, targetUsername);
            return;
        }
        
        // Cuenta pública: seguir directamente
        ejecutarSeguimiento(miUsername, targetUsername);
    }
    
    /**
     * Dejar de seguir a un usuario
     */
    public void dejarDeSeguir(String miUsername, String targetUsername) 
            throws FollowException, IOException {
        
        if (!estaSiguiendo(miUsername, targetUsername)) {
            throw new NoSiguesUsuarioException(targetUsername);
        }
        
        // Eliminar de mi following
        ListaUsuarios miFollowing = cargarFollowing(miUsername);
        miFollowing.eliminar(targetUsername);
        guardarFollowing(miUsername, miFollowing);
        
        // Eliminar de sus followers
        ListaUsuarios susFollowers = cargarFollowers(targetUsername);
        susFollowers.eliminar(miUsername);
        guardarFollowers(targetUsername, susFollowers);
    }
    
    /**
     * Ejecuta el seguimiento (sin validaciones)
     */
    private void ejecutarSeguimiento(String follower, String seguido) throws IOException {
        // Agregar a mi following
        ListaUsuarios miFollowing = cargarFollowing(follower);
        miFollowing.agregar(seguido);
        guardarFollowing(follower, miFollowing);
        
        // Agregar a sus followers
        ListaUsuarios susFollowers = cargarFollowers(seguido);
        susFollowers.agregar(follower);
        guardarFollowers(seguido, susFollowers);

        // Notificación FOLLOW al usuario seguido
        try {
            new NotificationManager().crearNotificacion(
                follower, seguido, TipoNotificacion.FOLLOW);
        } catch (Exception ignored) {}
    }
    
    /**
     * Verifica si un usuario sigue a otro
     */
    public boolean estaSiguiendo(String follower, String seguido) throws IOException {
        ListaUsuarios following = cargarFollowing(follower);
        return following.contiene(seguido);
    }
    
    /**
     * Verifica si son amigos mutuos
     */
    public boolean sonAmigos(String user1, String user2) throws IOException {
        return estaSiguiendo(user1, user2) && estaSiguiendo(user2, user1);
    }
    
    /**
     * Obtiene los followers de un usuario
     */
    public ArrayList<String> obtenerFollowers(String username) throws IOException {
        ListaUsuarios lista = cargarFollowers(username);
        return lista.toArrayList();
    }
    
    /**
     * Obtiene los following de un usuario
     */
    public ArrayList<String> obtenerFollowing(String username) throws IOException {
        ListaUsuarios lista = cargarFollowing(username);
        return lista.toArrayList();
    }
    
    /**
     * Cuenta los followers
     */
    public int contarFollowers(String username) throws IOException {
        return cargarFollowers(username).getTamanio();
    }
    
    /**
     * Cuenta los following
     */
    public int contarFollowing(String username) throws IOException {
        return cargarFollowing(username).getTamanio();
    }
    
    // ==================== SOLICITUDES ====================
    
    /**
     * Crea una solicitud de seguimiento
     */
    private void crearSolicitud(String remitente, String destinatario) 
            throws FollowException, IOException {
        
        // Verificar si ya existe una solicitud pendiente
        if (tieneSolicitudPendiente(remitente, destinatario)) {
            throw new SolicitudYaExisteException();
        }
        
        FollowRequest request = new FollowRequest(remitente, destinatario);
        guardarSolicitud(request);

        // Notificación FOLLOW_REQUEST al destinatario
        try {
            new NotificationManager().crearNotificacion(
                remitente, destinatario, TipoNotificacion.FOLLOW_REQUEST);
        } catch (Exception ignored) {}
    }
    
    /**
     * Acepta una solicitud de seguimiento
     */
    public void aceptarSolicitud(String destinatario, String remitente) 
            throws FollowException, IOException {
        
        FollowRequest request = buscarSolicitud(remitente, destinatario);
        
        if (request == null) {
            throw new FollowException("Solicitud no encontrada");
        }
        
        if (request.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new FollowException("La solicitud ya fue procesada");
        }
        
        // Ejecutar seguimiento
        ejecutarSeguimiento(remitente, destinatario);
        
        // Actualizar estado de solicitud
        request.setEstado(EstadoSolicitud.ACEPTADA);
        actualizarSolicitud(request);

        // Notificar al remitente que aceptaron su solicitud
        try {
            new NotificationManager().crearNotificacion(
                destinatario, remitente, TipoNotificacion.FOLLOW, "accepted");
        } catch (Exception ignored) {}
    }
    
    /**
     * Rechaza una solicitud de seguimiento
     */
    public void rechazarSolicitud(String destinatario, String remitente) 
            throws FollowException, IOException {
        
        FollowRequest request = buscarSolicitud(remitente, destinatario);
        
        if (request == null) {
            throw new FollowException("Solicitud no encontrada");
        }
        
        request.setEstado(EstadoSolicitud.RECHAZADA);
        actualizarSolicitud(request);
    }
    
    /**
     * Cancela una solicitud enviada
     */
    public void cancelarSolicitud(String remitente, String destinatario) 
            throws FollowException, IOException {
        
        FollowRequest request = buscarSolicitud(remitente, destinatario);
        
        if (request == null) {
            throw new FollowException("Solicitud no encontrada");
        }
        
        request.setEstado(EstadoSolicitud.CANCELADA);
        actualizarSolicitud(request);
    }
    
    /**
     * Obtiene solicitudes pendientes de un usuario
     */
    public ArrayList<FollowRequest> obtenerSolicitudesPendientes(String username) 
            throws IOException {
        
        ArrayList<FollowRequest> pendientes = new ArrayList<>();
        ArrayList<FollowRequest> todas = obtenerTodasLasSolicitudes();
        
        for (FollowRequest req : todas) {
            if (req.getDestinatarioUsername().equals(username) && 
                req.getEstado() == EstadoSolicitud.PENDIENTE) {
                pendientes.add(req);
            }
        }
        
        return pendientes;
    }
    
    /**
     * Obtiene solicitudes enviadas por un usuario
     */
    public ArrayList<FollowRequest> obtenerSolicitudesEnviadas(String username) 
            throws IOException {
        
        ArrayList<FollowRequest> enviadas = new ArrayList<>();
        ArrayList<FollowRequest> todas = obtenerTodasLasSolicitudes();
        
        for (FollowRequest req : todas) {
            if (req.getRemitenteUsername().equals(username) && 
                req.getEstado() == EstadoSolicitud.PENDIENTE) {
                enviadas.add(req);
            }
        }
        
        return enviadas;
    }
    
    /**
     * Verifica si tiene solicitud pendiente
     */
    public boolean tieneSolicitudPendiente(String remitente, String destinatario) 
            throws IOException {
        
        FollowRequest request = buscarSolicitud(remitente, destinatario);
        return request != null && request.getEstado() == EstadoSolicitud.PENDIENTE;
    }
    
    // ==================== PERSISTENCIA ====================
    
    /**
     * Carga los followers de un usuario
     */
    private ListaUsuarios cargarFollowers(String username) throws IOException {
        String ruta = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_FOLLOWERS;
        return cargarLista(ruta);
    }
    
    /**
     * Carga los following de un usuario
     */
    private ListaUsuarios cargarFollowing(String username) throws IOException {
        String ruta = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_FOLLOWING;
        return cargarLista(ruta);
    }
    
    /**
     * Guarda los followers de un usuario
     */
    private void guardarFollowers(String username, ListaUsuarios lista) throws IOException {
        String ruta = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_FOLLOWERS;
        guardarLista(ruta, lista);
    }
    
    /**
     * Guarda los following de un usuario
     */
    private void guardarFollowing(String username, ListaUsuarios lista) throws IOException {
        String ruta = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_FOLLOWING;
        guardarLista(ruta, lista);
    }
    
    /**
     * Carga una lista desde archivo
     */
    private ListaUsuarios cargarLista(String ruta) throws IOException {
        ListaUsuarios lista = new ListaUsuarios();
        File archivo = new File(ruta);
        
        if (!archivo.exists()) {
            return lista;
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(archivo, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                String username = leerStringFijo(raf, 50);
                lista.agregar(username);
            }
        }
        
        return lista;
    }
    
    /**
     * Guarda una lista en archivo
     */
    private void guardarLista(String ruta, ListaUsuarios lista) throws IOException {
        File archivo = new File(ruta);
        
        try (RandomAccessFile raf = new RandomAccessFile(archivo, "rw")) {
            raf.setLength(0); // Limpiar archivo
            
            ArrayList<String> usernames = lista.toArrayList();
            for (String username : usernames) {
                escribirStringFijo(raf, username, 50);
            }
        }
    }
    
    /**
     * Guarda una solicitud
     */
    private void guardarSolicitud(FollowRequest request) throws IOException {
        String ruta = CARPETA_RAIZ + "/" + ARCHIVO_REQUESTS;
        File archivo = new File(ruta);
        
        try (RandomAccessFile raf = new RandomAccessFile(archivo, "rw")) {
            raf.seek(raf.length()); // Al final
            escribirSolicitud(raf, request);
        }
    }
    
    /**
     * Busca una solicitud específica
     */
    private FollowRequest buscarSolicitud(String remitente, String destinatario) 
            throws IOException {
        
        ArrayList<FollowRequest> todas = obtenerTodasLasSolicitudes();
        
        for (FollowRequest req : todas) {
            if (req.getRemitenteUsername().equals(remitente) && 
                req.getDestinatarioUsername().equals(destinatario) &&
                req.getEstado() == EstadoSolicitud.PENDIENTE) {
                return req;
            }
        }
        
        return null;
    }
    
    /**
     * Actualiza una solicitud
     */
    private void actualizarSolicitud(FollowRequest requestActualizada) throws IOException {
        ArrayList<FollowRequest> todas = obtenerTodasLasSolicitudes();
        
        for (int i = 0; i < todas.size(); i++) {
            FollowRequest req = todas.get(i);
            if (req.getRemitenteUsername().equals(requestActualizada.getRemitenteUsername()) &&
                req.getDestinatarioUsername().equals(requestActualizada.getDestinatarioUsername())) {
                todas.set(i, requestActualizada);
                break;
            }
        }
        
        // Reescribir archivo
        reescribirSolicitudes(todas);
    }
    
    /**
     * Obtiene todas las solicitudes
     */
    private ArrayList<FollowRequest> obtenerTodasLasSolicitudes() throws IOException {
        ArrayList<FollowRequest> solicitudes = new ArrayList<>();
        String ruta = CARPETA_RAIZ + "/" + ARCHIVO_REQUESTS;
        File archivo = new File(ruta);
        
        if (!archivo.exists()) {
            return solicitudes;
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(archivo, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                FollowRequest request = leerSolicitud(raf);
                solicitudes.add(request);
            }
        }
        
        return solicitudes;
    }
    
    /**
     * Reescribe el archivo de solicitudes
     */
    private void reescribirSolicitudes(ArrayList<FollowRequest> solicitudes) throws IOException {
        String ruta = CARPETA_RAIZ + "/" + ARCHIVO_REQUESTS;
        
        try (RandomAccessFile raf = new RandomAccessFile(ruta, "rw")) {
            raf.setLength(0); // Limpiar
            
            for (FollowRequest req : solicitudes) {
                escribirSolicitud(raf, req);
            }
        }
    }
    
    /**
     * Escribe una solicitud en RandomAccessFile
     */
    private void escribirSolicitud(RandomAccessFile raf, FollowRequest request) 
            throws IOException {
        
        escribirStringFijo(raf, request.getRemitenteUsername(), 50);
        escribirStringFijo(raf, request.getDestinatarioUsername(), 50);
        raf.writeLong(request.getFechaSolicitud().getTime());
        raf.writeInt(request.getEstado().ordinal());
    }
    
    /**
     * Lee una solicitud desde RandomAccessFile
     */
    private FollowRequest leerSolicitud(RandomAccessFile raf) throws IOException {
        FollowRequest request = new FollowRequest();
        
        request.setRemitenteUsername(leerStringFijo(raf, 50));
        request.setDestinatarioUsername(leerStringFijo(raf, 50));
        request.setFechaSolicitud(new Date(raf.readLong()));
        request.setEstado(EstadoSolicitud.values()[raf.readInt()]);
        
        return request;
    }
    
    /**
     * Escribe un string de tamaño fijo
     */
    private void escribirStringFijo(RandomAccessFile raf, String str, int tamanio) 
            throws IOException {
        
        if (str == null) str = "";
        
        StringBuilder sb = new StringBuilder(str);
        sb.setLength(tamanio);
        
        for (int i = 0; i < tamanio; i++) {
            raf.writeChar(sb.charAt(i));
        }
    }
    
    /**
     * Lee un string de tamaño fijo
     */
    private String leerStringFijo(RandomAccessFile raf, int tamanio) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < tamanio; i++) {
            sb.append(raf.readChar());
        }
        
        return sb.toString().trim();
    }
}