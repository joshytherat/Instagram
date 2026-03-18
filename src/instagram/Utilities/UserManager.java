package instagram.Utilities;

import instagram.Exceptions.CredencialesInvalidasException;
import instagram.Exceptions.PasswordInvalidaException;
import instagram.Exceptions.UsernameYaExisteException;
import instagram.Exceptions.UserException;
import instagram.Enums.EstadoCuenta;
import instagram.Enums.TipoCuenta;
import instagram.Enums.Genero;
import instagram.Interfaces.Persistible;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UserManager — sin RandomAccessFile como campo de instancia.
 *
 * REGLA: cada método abre/cierra su propio RAF en try-with-resources.
 * Nunca hay un RAF compartido entre threads — elimina la corrupción
 * de lectura que causaba rutas de foto rotas y pfp que no cargaban.
 *
 * Cache de usuarios (ConcurrentHashMap) para evitar releer el disco
 * en cada SwingWorker.
 */
public class UserManager implements Persistible {

    private static final String ARCHIVO_USUARIOS = "INSTA_RAIZ/users.ins";
    private static final String CARPETA_RAIZ     = "INSTA_RAIZ";

    private final ValidadorPassword validadorPassword;

    // Cache thread-safe: username_lower -> User
    private final Map<String, User> cache = new ConcurrentHashMap<>();

    public UserManager() {
        validadorPassword = new ValidadorPassword();
        inicializarSistemaArchivos();
    }

    private void inicializarSistemaArchivos() {
        try {
            new File(CARPETA_RAIZ).mkdirs();
            File f = new File(ARCHIVO_USUARIOS);
            if (!f.exists()) f.createNewFile();
        } catch (IOException e) {
            System.err.println("Error init archivos: " + e.getMessage());
        }
    }

    @Override public void guardar()  throws IOException {}
    @Override public void cargar()   throws IOException {}
    @Override public void eliminar() throws IOException {}

    // ── Operaciones públicas ──────────────────────────────────────

    public void registrarUsuario(User usuario) throws UserException, IOException {
        try { usuario.validar(); } catch (Exception e) { throw new UserException(e.getMessage()); }

        validadorPassword.validar(usuario.getPassword());
        if (!validadorPassword.esValida())
            throw new PasswordInvalidaException(validadorPassword.obtenerMensajeError());

        String lower = usuario.getUsername().toLowerCase();
        if (existeUsername(lower))
            throw new UsernameYaExisteException(usuario.getUsername());

        usuario.setUsername(lower);
        crearEstructuraUsuario(lower);

        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_USUARIOS, "rw")) {
            raf.seek(raf.length());
            escribirUsuario(raf, usuario);
        }
        cache.put(lower, usuario);
    }

    public User autenticar(String username, String password)
            throws CredencialesInvalidasException, IOException {
        User usuario = buscarUsuario(username.toLowerCase());
        if (usuario == null || !usuario.getPassword().equals(password))
            throw new CredencialesInvalidasException();
        if (usuario.getEstadoCuenta() == EstadoCuenta.DESACTIVADO) {
            usuario.setEstadoCuenta(EstadoCuenta.ACTIVO);
            try { actualizarUsuario(usuario.getUsername(), usuario); } catch (UserException ignored) {}
        }
        return usuario;
    }

    /**
     * Busca usuario case-insensitive.
     * Primero el cache; si no está, RAF local propio (thread-safe).
     */
    public User buscarUsuario(String username) throws IOException {
        if (username == null) return null;
        String lower = username.toLowerCase();

        User cached = cache.get(lower);
        if (cached != null) return cached;

        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_USUARIOS, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                User u = leerUsuario(raf);
                if (u.getUsername().toLowerCase().equals(lower)) {
                    cache.put(lower, u);
                    return u;
                }
            }
        }
        return null;
    }

    public boolean existeUsername(String username) throws IOException {
        return buscarUsuario(username) != null;
    }

    public ArrayList<User> obtenerTodosLosUsuarios() throws IOException {
        ArrayList<User> lista = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_USUARIOS, "r")) {
            while (raf.getFilePointer() < raf.length())
                lista.add(leerUsuario(raf));
        }
        return lista;
    }

    public void actualizarUsuario(String username, User actualizado)
            throws UserException, IOException {
        ArrayList<User> todos = obtenerTodosLosUsuarios();
        boolean ok = false;
        for (int i = 0; i < todos.size(); i++) {
            if (todos.get(i).getUsername().toLowerCase().equals(username.toLowerCase())) {
                todos.set(i, actualizado);
                ok = true;
                break;
            }
        }
        if (!ok) throw new UserException("Usuario no encontrado");
        reescribirArchivo(todos);
        cache.put(username.toLowerCase(), actualizado);
    }

    public void actualizarBiografia(String username, String bio)
            throws UserException, IOException {
        User u = buscarUsuario(username);
        if (u == null) throw new UserException("Usuario no encontrado");
        u.setBiografia(bio);
        actualizarUsuario(username, u);
    }

    public ValidadorPassword getValidadorPassword() { return validadorPassword; }

    // ── Serialización — RAF siempre como parámetro, nunca global ─

    private void escribirUsuario(RandomAccessFile raf, User u) throws IOException {
        escribirString(raf, u.getUsername(),       User.USERNAME_SIZE);
        escribirString(raf, u.getPassword(),       User.PASSWORD_SIZE);
        escribirString(raf, u.getNombreCompleto(), User.NOMBRE_SIZE);
        escribirString(raf, u.getBiografia() != null ? u.getBiografia() : "", User.BIOGRAFIA_SIZE);
        escribirString(raf, u.getRutaFotoPerfil() != null ? u.getRutaFotoPerfil() : "", User.RUTA_FOTO_SIZE);
        raf.writeInt(u.getGenero().ordinal());
        raf.writeInt(u.getEdad());
        raf.writeLong(u.getFechaRegistro().getTime());
        raf.writeInt(u.getEstadoCuenta().ordinal());
        raf.writeInt(u.getTipoCuenta().ordinal());
    }

    private User leerUsuario(RandomAccessFile raf) throws IOException {
        User u = new User();
        u.setUsername(leerString(raf, User.USERNAME_SIZE));
        u.setPassword(leerString(raf, User.PASSWORD_SIZE));
        u.setNombreCompleto(leerString(raf, User.NOMBRE_SIZE));
        u.setBiografia(leerString(raf, User.BIOGRAFIA_SIZE));
        u.setRutaFotoPerfil(leerString(raf, User.RUTA_FOTO_SIZE));
        u.setGenero(Genero.values()[raf.readInt()]);
        u.setEdad(raf.readInt());
        u.setFechaRegistro(new Date(raf.readLong()));
        u.setEstadoCuenta(EstadoCuenta.values()[raf.readInt()]);
        u.setTipoCuenta(TipoCuenta.values()[raf.readInt()]);
        return u;
    }

    private void escribirString(RandomAccessFile raf, String s, int tam) throws IOException {
        if (s == null) s = "";
        StringBuilder sb = new StringBuilder(s);
        sb.setLength(tam);
        for (int i = 0; i < tam; i++) raf.writeChar(sb.charAt(i));
    }

    private String leerString(RandomAccessFile raf, int tam) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tam; i++) sb.append(raf.readChar());
        // Eliminar \u0000 que RAF rellena — esta es la causa raíz de rutas corruptas
        return sb.toString().replaceAll("[\\x00-\\x1F]", "").trim();
    }

    private void reescribirArchivo(ArrayList<User> lista) throws IOException {
        cache.clear();
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_USUARIOS, "rw")) {
            raf.setLength(0);
            for (User u : lista) {
                escribirUsuario(raf, u);
                cache.put(u.getUsername().toLowerCase(), u);
            }
        }
    }

    private void crearEstructuraUsuario(String username) {
        try {
            File base = new File(CARPETA_RAIZ + "/" + username);
            base.mkdirs();
            new File(base, "imagenes").mkdir();
            new File(base, "stickers_personales").mkdir();
            for (String n : new String[]{"followers.ins", "following.ins",
                    "insta.ins", "inbox.ins", "notifications.ins"}) {
                File f = new File(base, n);
                if (!f.exists()) f.createNewFile();
            }
        } catch (IOException e) {
            System.err.println("Error estructura: " + e.getMessage());
        }
    }
}