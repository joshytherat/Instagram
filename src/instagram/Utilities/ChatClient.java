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
import instagram.Interfaces.Notificable;
import instagram.Utilities.NetworkProtocol.MessageType;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ChatClient implements Notificable {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private String username;
    private Thread listenerThread;
    private volatile boolean connected;

    private MessageListener messageListener;

    public interface MessageListener {

        void onMessageReceived(Message mensaje);

        void onNotificationReceived(String tipo, Object datos);

        void onConversationsReceived(ArrayList<String> conversaciones);

        void onError(String error);

        void onConnectionStatusChanged(boolean connected);
    }

    public ChatClient(String username) {
        this.username = username;
        this.connected = false;
    }

    /**
     * Conecta al servidor
     */
    public void conectar() throws IOException {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socket.getInputStream());

            connected = true;

            // Enviar LOGIN
            NetworkProtocol loginProtocol = new NetworkProtocol(
                    MessageType.LOGIN,
                    username,
                    null
            );
            enviarProtocolo(loginProtocol);

            // Iniciar thread de escucha
            iniciarListener();

            notificar("CONNECTION", true);

        } catch (IOException e) {
            connected = false;
            throw new IOException("No se pudo conectar al servidor: " + e.getMessage());
        }
    }

    /**
     * Desconecta del servidor
     */
    public void desconectar() {
        try {
            if (connected) {
                NetworkProtocol logoutProtocol = new NetworkProtocol(
                        MessageType.LOGOUT,
                        username,
                        null
                );
                enviarProtocolo(logoutProtocol);
            }

            connected = false;

            if (entrada != null) {
                entrada.close();
            }
            if (salida != null) {
                salida.close();
            }
            if (socket != null) {
                socket.close();
            }

            if (listenerThread != null) {
                listenerThread.interrupt();
            }

            notificar("CONNECTION", false);

        } catch (IOException e) {
            System.err.println("Error desconectando: " + e.getMessage());
        }
    }

    /**
     * Envía un mensaje
     */
    public void enviarMensaje(Message mensaje) throws IOException {
        if (!connected) {
            throw new IOException("No estás conectado al servidor");
        }

        NetworkProtocol protocol = new NetworkProtocol(
                MessageType.SEND_MESSAGE,
                username,
                mensaje.getReceptor(),
                mensaje
        );

        enviarProtocolo(protocol);
    }

    /**
     * Marca un mensaje como leído
     */
    public void marcarComoLeido(String messageId) throws IOException {
        NetworkProtocol protocol = new NetworkProtocol(
                MessageType.MESSAGE_READ,
                username,
                messageId
        );

        enviarProtocolo(protocol);
    }

    /**
     * Solicita lista de conversaciones
     */
    public void solicitarConversaciones() throws IOException {
        NetworkProtocol protocol = new NetworkProtocol(
                MessageType.GET_CONVERSATIONS,
                username,
                null
        );

        enviarProtocolo(protocol);
    }

    /**
     * Elimina una conversación
     */
    public void eliminarConversacion(String otroUsuario) throws IOException {
        NetworkProtocol protocol = new NetworkProtocol(
                MessageType.DELETE_CONVERSATION,
                username,
                otroUsuario
        );

        enviarProtocolo(protocol);
    }

    /**
     * Envía un protocolo al servidor
     */
    private void enviarProtocolo(NetworkProtocol protocol) throws IOException {
        try {
            synchronized (salida) {
                salida.writeObject(protocol);
                salida.flush();
            }
        } catch (IOException e) {
            connected = false;
            throw new IOException("Error enviando datos: " + e.getMessage());
        }
    }

    /**
     * Inicia el thread de escucha
     */
    private void iniciarListener() {
        listenerThread = new Thread(() -> {
            try {
                while (connected) {
                    NetworkProtocol protocol = (NetworkProtocol) entrada.readObject();
                    procesarProtocolo(protocol);
                }
            } catch (EOFException e) {
                System.out.println("Conexión cerrada por el servidor");
                connected = false;
            } catch (IOException | ClassNotFoundException e) {
                if (connected) {
                    System.err.println("Error en listener: " + e.getMessage());
                    connected = false;
                }
            } finally {
                notificar("CONNECTION", false);
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Procesa protocolos recibidos
     */
    private void procesarProtocolo(NetworkProtocol protocol) {
        switch (protocol.getType()) {
            case RECEIVE_MESSAGE:
                if (messageListener != null) {
                    Message mensaje = (Message) protocol.getPayload();
                    messageListener.onMessageReceived(mensaje);
                }
                break;

            case NEW_MESSAGE_NOTIFICATION:
                if (messageListener != null) {
                    messageListener.onNotificationReceived("NEW_MESSAGE", protocol.getPayload());
                }
                break;

            case NEW_FOLLOWER_NOTIFICATION:
                if (messageListener != null) {
                    messageListener.onNotificationReceived("NEW_FOLLOWER", protocol.getPayload());
                }
                break;

            case NEW_MENTION_NOTIFICATION:
                if (messageListener != null) {
                    messageListener.onNotificationReceived("NEW_MENTION", protocol.getPayload());
                }
                break;

            case CONVERSATIONS_LIST:
                if (messageListener != null) {
                    @SuppressWarnings("unchecked")
                    ArrayList<String> conversaciones = (ArrayList<String>) protocol.getPayload();
                    messageListener.onConversationsReceived(conversaciones);
                }
                break;

            case SUCCESS:
                System.out.println("✓ " + protocol.getPayload());
                break;

            case ERROR:
                if (messageListener != null) {
                    String errMsg = (String) protocol.getPayload();
                    if (errMsg != null && errMsg.startsWith("DOBLE_SESION:")) {
                        // Notificar como error especial de doble sesión
                        messageListener.onError(errMsg);
                    } else {
                        messageListener.onError(errMsg);
                    }
                }
                break;

            case USER_ONLINE:
            case USER_OFFLINE:
                if (messageListener != null) {
                    String user = protocol.getSender();
                    boolean online = protocol.getType() == MessageType.USER_ONLINE;
                    messageListener.onNotificationReceived(
                        online ? "USER_ONLINE" : "USER_OFFLINE", user);
                }
                break;
            case NOTIFICATION_NEW:
                if (notificationListener != null) {
                    Notification notif = (Notification) protocol.getPayload();
                    notificationListener.onNotificationReceived(notif);
                }
                break;

            case NOTIFICATION_LIST:
                if (notificationListener != null) {
                    @SuppressWarnings("unchecked")
                    ArrayList<Notification> notifs = (ArrayList<Notification>) protocol.getPayload();
                    notificationListener.onNotificationListReceived(notifs);
                }
                break;

            case NOTIFICATION_COUNT:
                if (notificationListener != null) {
                    Integer count = (Integer) protocol.getPayload();
                    notificationListener.onUnreadCountReceived(count);
                }
                break;

            default:
                System.out.println("Protocolo no manejado: " + protocol.getType());
        }
    }

    @Override
    public void notificar(String tipo, Object datos) {
        if (messageListener != null) {
            if ("CONNECTION".equals(tipo)) {
                messageListener.onConnectionStatusChanged((Boolean) datos);
            } else {
                messageListener.onNotificationReceived(tipo, datos);
            }
        }
    }

    @Override
    public void actualizarEstado() {
        // Implementación si es necesaria
    }

    public boolean isConnected() {
        return connected;
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public interface NotificationListener {

        void onNotificationReceived(Notification notification);

        void onNotificationListReceived(ArrayList<Notification> notifications);

        void onUnreadCountReceived(int count);
    }

    private NotificationListener notificationListener;

    public void setNotificationListener(NotificationListener listener) {
        this.notificationListener = listener;
    }

// Métodos para notificaciones:
    public void solicitarNotificaciones() throws IOException {
        NetworkProtocol protocol = new NetworkProtocol(
                MessageType.NOTIFICATION_GET_ALL,
                username,
                null
        );
        enviarProtocolo(protocol);
    }

    public void solicitarNoLeidas() throws IOException {
        NetworkProtocol protocol = new NetworkProtocol(
                MessageType.NOTIFICATION_GET_UNREAD,
                username,
                null
        );
        enviarProtocolo(protocol);
    }

    public void marcarComoLeida(String notificationId) throws IOException {
        NetworkProtocol protocol = new NetworkProtocol(
                MessageType.NOTIFICATION_MARK_READ,
                username,
                notificationId
        );
        enviarProtocolo(protocol);
    }

    public void marcarTodasComoLeidas() throws IOException {
        NetworkProtocol protocol = new NetworkProtocol(
                MessageType.NOTIFICATION_MARK_ALL_READ,
                username,
                null
        );
        enviarProtocolo(protocol);
    }

    public void eliminarNotificacion(String notificationId) throws IOException {
        NetworkProtocol protocol = new NetworkProtocol(
                MessageType.NOTIFICATION_DELETE,
                username,
                notificationId
        );
        enviarProtocolo(protocol);
    }

    public void solicitarConteoNoLeidas() throws IOException {
        NetworkProtocol protocol = new NetworkProtocol(
                MessageType.NOTIFICATION_COUNT_UNREAD,
                username,
                null
        );
        enviarProtocolo(protocol);
    }

}