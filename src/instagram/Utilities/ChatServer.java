package instagram.Utilities;

import instagram.Abstracts.Message;
import instagram.Utilities.NetworkProtocol.MessageType;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {

    private static final int PORT = 5000;

    private ServerSocket              serverSocket;
    private Map<String, ClientHandler> clientesConectados;
    private MessageManager            messageManager;
    private NotificationManager       notificationManager;  // FIX: campo compartido
    private FollowManager             followManager;
    private ExecutorService           threadPool;
    private volatile boolean          running;

    public ChatServer(MessageManager messageManager, FollowManager followManager) {
        this.messageManager      = messageManager;
        this.notificationManager = new NotificationManager(); // FIX: una sola instancia
        this.followManager       = followManager;
        this.clientesConectados  = new ConcurrentHashMap<>();
        this.threadPool          = Executors.newCachedThreadPool();
        this.running             = false;
    }

    public void iniciar() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("✓ Servidor de chat iniciado en puerto " + PORT);

            Thread acceptThread = new Thread(() -> {
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Nueva conexión desde: " + clientSocket.getInetAddress());
                        threadPool.execute(new ClientHandler(clientSocket));
                    } catch (IOException e) {
                        if (running) System.err.println("Error aceptando conexión: " + e.getMessage());
                    }
                }
            });
            acceptThread.setDaemon(true);
            acceptThread.start();

        } catch (IOException e) {
            System.err.println("Error iniciando servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void detener() {
        running = false;
        for (ClientHandler handler : clientesConectados.values()) handler.desconectar();
        threadPool.shutdown();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
            System.out.println("✓ Servidor detenido");
        } catch (IOException e) {
            System.err.println("Error deteniendo servidor: " + e.getMessage());
        }
    }

    private void enviarMensaje(String receptor, NetworkProtocol protocol) {
        ClientHandler handler = clientesConectados.get(receptor);
        if (handler != null && handler.isConnected()) {
            handler.enviar(protocol);
        } else {
            System.out.println("Usuario " + receptor + " no conectado. Mensaje guardado.");
        }
    }

    private void broadcast(NetworkProtocol protocol) {
        for (ClientHandler handler : clientesConectados.values())
            if (handler.isConnected()) handler.enviar(protocol);
    }

    private boolean puedeEnviarMensaje(String emisor, String receptor) {
        return true; // la UI ya restringe a quién se puede escribir
    }

    // ── Handler de cliente ────────────────────────────────────────

    private class ClientHandler implements Runnable {

        private Socket             socket;
        private ObjectOutputStream salida;
        private ObjectInputStream  entrada;
        private String             username;
        private volatile boolean   connected;

        public ClientHandler(Socket socket) {
            this.socket    = socket;
            this.connected = true;
        }

        @Override
        public void run() {
            try {
                salida = new ObjectOutputStream(socket.getOutputStream());
                salida.flush();
                entrada = new ObjectInputStream(socket.getInputStream());

                while (connected) {
                    NetworkProtocol protocol = (NetworkProtocol) entrada.readObject();
                    procesarProtocolo(protocol);
                }
            } catch (EOFException e) {
                System.out.println("Cliente desconectado: " + username);
            } catch (IOException | ClassNotFoundException e) {
                if (connected)
                    System.err.println("Error con cliente " + username + ": " + e.getMessage());
            } finally {
                desconectar();
            }
        }

        private void procesarProtocolo(NetworkProtocol protocol) {
            try {
                switch (protocol.getType()) {
                    case LOGIN:                    manejarLogin(protocol);                break;
                    case SEND_MESSAGE:             manejarEnvioMensaje(protocol);         break;
                    case MESSAGE_READ:             manejarMensajeLeido(protocol);         break;
                    case GET_CONVERSATIONS:        manejarObtenerConversaciones(protocol);break;
                    case DELETE_CONVERSATION:      manejarEliminarConversacion(protocol); break;
                    case LOGOUT:                   desconectar();                         break;
                    case NOTIFICATION_GET_ALL:     manejarObtenerNotificaciones(protocol);break;
                    case NOTIFICATION_GET_UNREAD:  manejarObtenerNoLeidas(protocol);      break;
                    case NOTIFICATION_MARK_READ:   manejarMarcarLeida(protocol);          break;
                    case NOTIFICATION_MARK_ALL_READ: manejarMarcarTodasLeidas(protocol);  break;
                    case NOTIFICATION_DELETE:      manejarEliminarNotificacion(protocol); break;
                    case NOTIFICATION_COUNT_UNREAD: manejarContarNoLeidas(protocol);      break;
                    default:
                        System.out.println("Protocolo desconocido: " + protocol.getType());
                }
            } catch (Exception e) {
                System.err.println("Error procesando protocolo: " + e.getMessage());
                enviar(new NetworkProtocol(MessageType.ERROR, "server", e.getMessage()));
            }
        }

        // ── Login ─────────────────────────────────────────────────

        private void manejarLogin(NetworkProtocol protocol) {
            String nuevoUsername = protocol.getSender();

            // Sesión única: si ya hay sesión activa para este usuario,
            // rechazar el nuevo intento — NO desconectar la sesión activa.
            ClientHandler sesionExistente = clientesConectados.get(nuevoUsername);
            if (sesionExistente != null && sesionExistente != this && sesionExistente.isConnected()) {
                System.out.println("⚠ Doble sesión rechazada para: " + nuevoUsername);
                // Avisar al NUEVO intento que ya hay sesión activa
                enviar(new NetworkProtocol(
                    MessageType.ERROR, "server",
                    "DOBLE_SESION:Ya hay una sesión activa para esta cuenta."));
                // Cerrar solo la nueva conexión entrante, la vieja sigue intacta
                desconectar();
                return;
            }

            username = nuevoUsername;
            clientesConectados.put(username, this);
            System.out.println("✓ Usuario conectado: " + username);

            enviar(new NetworkProtocol(MessageType.SUCCESS, "server", "Conectado exitosamente"));
            broadcast(new NetworkProtocol(MessageType.USER_ONLINE, username, null));
            entregarMensajesPendientes();
        }

        // ── Mensajes ──────────────────────────────────────────────

        private void manejarEnvioMensaje(NetworkProtocol protocol) {
            Message mensaje = (Message) protocol.getPayload();

            // Permiso verificado en el cliente (UI); el servidor confía en el cliente.
            try {
                messageManager.guardarMensaje(mensaje);
                enviar(new NetworkProtocol(MessageType.SUCCESS, "server", "Mensaje enviado"));

                ClientHandler receptorHandler = clientesConectados.get(mensaje.getReceptor());
                if (receptorHandler != null && receptorHandler.isConnected()) {
                    receptorHandler.enviar(new NetworkProtocol(
                        MessageType.RECEIVE_MESSAGE,
                        mensaje.getEmisor(), mensaje.getReceptor(), mensaje));

                    mensaje.setEstado(Message.EstadoMensaje.ENTREGADO);
                    messageManager.actualizarEstadoMensaje(mensaje);

                    receptorHandler.enviar(new NetworkProtocol(
                        MessageType.NEW_MESSAGE_NOTIFICATION,
                        mensaje.getEmisor(), mensaje.getReceptor(),
                        "Nuevo mensaje de @" + mensaje.getEmisor()));
                }
            } catch (IOException e) {
                System.err.println("Error guardando mensaje: " + e.getMessage());
                enviar(new NetworkProtocol(MessageType.ERROR, "server", "Error al enviar mensaje"));
            }
        }

        private void manejarMensajeLeido(NetworkProtocol protocol) {
            try {
                messageManager.marcarComoLeido((String) protocol.getPayload());
            } catch (IOException e) {
                System.err.println("Error marcando mensaje como leído: " + e.getMessage());
            }
        }

        private void manejarObtenerConversaciones(NetworkProtocol protocol) {
            try {
                ArrayList<String> convs = messageManager.obtenerConversaciones(username);
                enviar(new NetworkProtocol(MessageType.CONVERSATIONS_LIST, "server", username, convs));
            } catch (IOException e) {
                System.err.println("Error obteniendo conversaciones: " + e.getMessage());
            }
        }

        private void manejarEliminarConversacion(NetworkProtocol protocol) {
            try {
                messageManager.eliminarConversacion(username, (String) protocol.getPayload());
                enviar(new NetworkProtocol(MessageType.SUCCESS, "server", "Conversación eliminada"));
            } catch (IOException e) {
                System.err.println("Error eliminando conversación: " + e.getMessage());
            }
        }

        private void entregarMensajesPendientes() {
            try {
                for (Message m : messageManager.obtenerMensajesNoEntregados(username)) {
                    enviar(new NetworkProtocol(MessageType.RECEIVE_MESSAGE,
                        m.getEmisor(), username, m));
                    m.setEstado(Message.EstadoMensaje.ENTREGADO);
                    messageManager.actualizarEstadoMensaje(m);
                }
            } catch (IOException e) {
                System.err.println("Error entregando mensajes pendientes: " + e.getMessage());
            }
        }

        // ── Notificaciones ────────────────────────────────────────

        private void manejarObtenerNotificaciones(NetworkProtocol protocol) {
            try {
                ArrayList<Notification> lista = notificationManager.obtenerNotificaciones(username);
                enviar(new NetworkProtocol(MessageType.NOTIFICATION_LIST, "server", username, lista));
            } catch (IOException e) {
                enviar(new NetworkProtocol(MessageType.ERROR, "server", "Error obteniendo notificaciones"));
            }
        }

        private void manejarObtenerNoLeidas(NetworkProtocol protocol) {
            try {
                ArrayList<Notification> lista = notificationManager.obtenerNoLeidas(username);
                enviar(new NetworkProtocol(MessageType.NOTIFICATION_LIST, "server", username, lista));
            } catch (IOException e) {
                enviar(new NetworkProtocol(MessageType.ERROR, "server", "Error obteniendo no leídas"));
            }
        }

        private void manejarMarcarLeida(NetworkProtocol protocol) {
            try {
                notificationManager.marcarComoLeida(username, (String) protocol.getPayload());
                enviar(new NetworkProtocol(MessageType.SUCCESS, "server", "Marcada como leída"));
            } catch (IOException e) {
                enviar(new NetworkProtocol(MessageType.ERROR, "server", "Error marcando notificación"));
            }
        }

        private void manejarMarcarTodasLeidas(NetworkProtocol protocol) {
            try {
                notificationManager.marcarTodasComoLeidas(username);
                enviar(new NetworkProtocol(MessageType.SUCCESS, "server", "Todas marcadas como leídas"));
            } catch (IOException e) {
                enviar(new NetworkProtocol(MessageType.ERROR, "server", "Error marcando notificaciones"));
            }
        }

        private void manejarEliminarNotificacion(NetworkProtocol protocol) {
            try {
                notificationManager.eliminarNotificacion(username, (String) protocol.getPayload());
                enviar(new NetworkProtocol(MessageType.SUCCESS, "server", "Notificación eliminada"));
            } catch (IOException e) {
                enviar(new NetworkProtocol(MessageType.ERROR, "server", "Error eliminando notificación"));
            }
        }

        private void manejarContarNoLeidas(NetworkProtocol protocol) {
            try {
                int count = notificationManager.contarNoLeidas(username);
                enviar(new NetworkProtocol(MessageType.NOTIFICATION_COUNT, "server", username, count));
            } catch (IOException e) {
                enviar(new NetworkProtocol(MessageType.ERROR, "server", "Error contando notificaciones"));
            }
        }

        /** Envía notificación en tiempo real a un usuario conectado. */
        public void enviarNotificacion(String receptor, Notification notificacion) {
            ClientHandler handler = clientesConectados.get(receptor);
            if (handler != null && handler.isConnected()) {
                handler.enviar(new NetworkProtocol(
                    MessageType.NOTIFICATION_NEW,
                    notificacion.getEmisor(), receptor, notificacion));
            }
        }

        // ── Utilidades ────────────────────────────────────────────

        public void enviar(NetworkProtocol protocol) {
            try {
                if (salida != null) {
                    synchronized (salida) {
                        salida.writeObject(protocol);
                        salida.flush();
                    }
                }
            } catch (IOException e) {
                System.err.println("Error enviando a " + username + ": " + e.getMessage());
                desconectar();
            }
        }

        public void desconectar() {
            connected = false;
            if (username != null) {
                clientesConectados.remove(username);
                broadcast(new NetworkProtocol(MessageType.USER_OFFLINE, username, null));
                System.out.println("✗ Usuario desconectado: " + username);
            }
            try {
                if (entrada != null) entrada.close();
                if (salida  != null) salida.close();
                if (socket  != null) socket.close();
            } catch (IOException ignored) {}
        }

        public boolean isConnected() { return connected; }
    }

    // ── Main ──────────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            // FIX: UserManager real para que FollowManager funcione
            UserManager    userManager    = new UserManager();
            MessageManager messageManager = new MessageManager();
            FollowManager  followManager  = new FollowManager(userManager);

            ChatServer server = new ChatServer(messageManager, followManager);
            server.iniciar();

            System.out.println("Presiona ENTER para detener el servidor...");
            new Scanner(System.in).nextLine();

            server.detener();

        } catch (Exception e) {
            System.err.println("Error fatal en servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}