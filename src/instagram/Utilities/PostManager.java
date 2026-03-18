/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Utilities;

/**
 *
 * @author janinadiaz
 */




import instagram.Enums.TipoNotificacion;
import instagram.Utilities.Post.TipoMultimedia;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.Collections;

public class PostManager {
    
    private static final String CARPETA_RAIZ = "INSTA_RAIZ";
    private static final String ARCHIVO_POSTS = "insta.ins";
    private static final String ARCHIVO_COMENTARIOS = "comments.ins";
    private static final String ARCHIVO_LIKES = "likes.ins";
    
    /**
     * Publica un nuevo post
     */
    public void publicar(Post post) throws IOException {
        String rutaUsuario = CARPETA_RAIZ + "/" + post.getUsername();
        String rutaPosts = rutaUsuario + "/" + ARCHIVO_POSTS;
        
        // Crear carpeta si no existe
        File carpeta = new File(rutaUsuario);
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(rutaPosts, "rw")) {
            raf.seek(raf.length()); // Al final
            escribirPost(raf, post);
        }
    }
    
    /**
     * Obtiene todos los posts de un usuario
     */
    public ArrayList<Post> obtenerPostsDeUsuario(String username) throws IOException {
        ArrayList<Post> posts = new ArrayList<>();
        String rutaPosts = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_POSTS;
        
        File archivo = new File(rutaPosts);
        if (!archivo.exists()) {
            return posts;
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(archivo, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                Post post = leerPost(raf);
                cargarComentarios(post);
                posts.add(post);
            }
        }
        
        return posts;
    }
    
    /**
     * Obtiene todos los posts del feed (todos los usuarios)
     */
    public ArrayList<Post> obtenerFeedCompleto() throws IOException {
        ArrayList<Post> feed = new ArrayList<>();
        File carpetaRaiz = new File(CARPETA_RAIZ);
        
        if (!carpetaRaiz.exists()) {
            return feed;
        }
        
        File[] carpetasUsuarios = carpetaRaiz.listFiles();
        if (carpetasUsuarios == null) {
            return feed;
        }
        
        for (File carpetaUsuario : carpetasUsuarios) {
            if (carpetaUsuario.isDirectory()) {
                File archivoPosts = new File(carpetaUsuario, ARCHIVO_POSTS);
                
                if (archivoPosts.exists()) {
                    try (RandomAccessFile raf = new RandomAccessFile(archivoPosts, "r")) {
                        while (raf.getFilePointer() < raf.length()) {
                            Post post = leerPost(raf);
                            cargarComentarios(post);
                            feed.add(post);
                        }
                    }
                }
            }
        }
        
        // Ordenar por fecha (más reciente primero)
        Collections.sort(feed, (p1, p2) -> p2.getFecha().compareTo(p1.getFecha()));
        
        return feed;
    }
    
    /**
     * Busca un post por ID
     */
    public Post buscarPost(String username, String postId) throws IOException {
        ArrayList<Post> posts = obtenerPostsDeUsuario(username);
        
        for (Post post : posts) {
            if (post.getId().equals(postId)) {
                return post;
            }
        }
        
        return null;
    }
    
    /**
     * Dar like a un post
     */
    public void darLike(String postId, String username, String autorPost) 
            throws IOException {
        
        Post post = buscarPost(autorPost, postId);
        if (post == null) {
            throw new IOException("Post no encontrado");
        }
        
        post.darLike(username);
        actualizarPost(post);
        guardarLike(postId, username);

        // Notificación LIKE al autor del post
        try {
            new NotificationManager().crearNotificacion(
                username, autorPost, TipoNotificacion.LIKE, postId);
        } catch (Exception ignored) {}
    }
    
    /**
     * Quitar like de un post
     */
    public void quitarLike(String postId, String username, String autorPost) 
            throws IOException {
        
        Post post = buscarPost(autorPost, postId);
        if (post == null) {
            throw new IOException("Post no encontrado");
        }
        
        post.quitarLike(username);
        actualizarPost(post);
        eliminarLike(postId, username);
    }
    
    /**
     * Agregar comentario a un post
     */
    public void agregarComentario(Comment comentario) throws IOException {
        String rutaComentarios = CARPETA_RAIZ + "/" + ARCHIVO_COMENTARIOS;
        
        try (RandomAccessFile raf = new RandomAccessFile(rutaComentarios, "rw")) {
            raf.seek(raf.length());
            escribirComentario(raf, comentario);
        }

        // Notificación COMMENT al autor del post
        // El postId tiene formato "username_timestamp" — extraer el autor
        try {
            String pid = comentario.getPostId();
            if (pid != null && pid.contains("_")) {
                String autorPost = pid.substring(0, pid.indexOf("_"));
                if (!autorPost.isEmpty() && !autorPost.equals(comentario.getUsername())) {
                    new NotificationManager().crearNotificacion(
                        comentario.getUsername(), autorPost,
                        TipoNotificacion.COMMENT, pid);
                }
            }
        } catch (Exception ignored) {}
    }
    
    /**
     * Carga comentarios de un post (RECURSIVO)
     */
    private void cargarComentarios(Post post) throws IOException {
        String rutaComentarios = CARPETA_RAIZ + "/" + ARCHIVO_COMENTARIOS;
        File archivo = new File(rutaComentarios);
        
        if (!archivo.exists()) {
            return;
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(archivo, "r")) {
            cargarComentariosRecursivo(raf, post);
        }
    }
    
    /**
     * Carga comentarios recursivamente
     */
    private void cargarComentariosRecursivo(RandomAccessFile raf, Post post) 
            throws IOException {
        
        if (raf.getFilePointer() >= raf.length()) {
            return;
        }
        
        Comment comentario = leerComentario(raf);
        
        if (comentario.getPostId().equals(post.getId())) {
            post.agregarComentario(comentario);
        }
        
        cargarComentariosRecursivo(raf, post);
    }
    
    /**
     * Actualiza un post existente
     */
    private void actualizarPost(Post postActualizado) throws IOException {
        String username = postActualizado.getUsername();
        ArrayList<Post> posts = obtenerPostsDeUsuario(username);
        
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getId().equals(postActualizado.getId())) {
                posts.set(i, postActualizado);
                break;
            }
        }
        
        reescribirPosts(username, posts);
    }
    
    /**
     * Reescribe el archivo de posts de un usuario
     */
    private void reescribirPosts(String username, ArrayList<Post> posts) 
            throws IOException {
        
        String rutaPosts = CARPETA_RAIZ + "/" + username + "/" + ARCHIVO_POSTS;
        
        try (RandomAccessFile raf = new RandomAccessFile(rutaPosts, "rw")) {
            raf.setLength(0); // Limpiar
            
            for (Post post : posts) {
                escribirPost(raf, post);
            }
        }
    }
    
    /**
     * Escribe un post en RandomAccessFile
     */
    private void escribirPost(RandomAccessFile raf, Post post) throws IOException {
        // ID (50 bytes)
        escribirStringFijo(raf, post.getId(), 50);
        
        // Username (50 bytes)
        escribirStringFijo(raf, post.getUsername(), 50);
        
        // Contenido (220 bytes)
        escribirStringFijo(raf, post.getContenido(), 220);
        
        // Fecha (8 bytes)
        raf.writeLong(post.getFecha().getTime());
        
        // Hora - minutos del día (4 bytes)
        int minutos = post.getHora().get(Calendar.HOUR_OF_DAY) * 60 + 
                     post.getHora().get(Calendar.MINUTE);
        raf.writeInt(minutos);
        
        // Hashtags (200 bytes)
        String hashtags = String.join(",", post.getHashtags());
        escribirStringFijo(raf, hashtags, 200);
        
        // Menciones (200 bytes)
        String menciones = String.join(",", post.getMenciones());
        escribirStringFijo(raf, menciones, 200);
        
        // Ruta imagen (200 bytes)
        escribirStringFijo(raf, post.getRutaImagen() != null ? post.getRutaImagen() : "", 200);
        
        // Tipo multimedia (4 bytes)
        raf.writeInt(post.getTipoMultimedia().ordinal());
        
        // Usuarios likes (200 bytes)
        String likes = String.join(",", post.getUsuariosLikes());
        escribirStringFijo(raf, likes, 200);
        
        // Cantidad comentarios (4 bytes)
        raf.writeInt(post.contarComentarios());
    }
    
    /**
     * Lee un post desde RandomAccessFile
     */
    private Post leerPost(RandomAccessFile raf) throws IOException {
        Post post = new Post();
        
        // ID
        post.setId(leerStringFijo(raf, 50));
        
        // Username
        post.setUsername(leerStringFijo(raf, 50));
        
        // Contenido
        post.setContenido(leerStringFijo(raf, 220));
        
        // Fecha
        post.setFecha(new Date(raf.readLong()));
        
        // Hora
        int minutos = raf.readInt();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, minutos / 60);
        cal.set(Calendar.MINUTE, minutos % 60);
        post.setHora(cal);
        
        // Hashtags
        String hashtagsStr = leerStringFijo(raf, 200);
        ArrayList<String> hashtags = new ArrayList<>();
        if (!hashtagsStr.isEmpty()) {
            for (String tag : hashtagsStr.split(",")) {
                if (!tag.trim().isEmpty()) {
                    hashtags.add(tag.trim());
                }
            }
        }
        post.setHashtags(hashtags);
        
        // Menciones
        String mencionesStr = leerStringFijo(raf, 200);
        ArrayList<String> menciones = new ArrayList<>();
        if (!mencionesStr.isEmpty()) {
            for (String mencion : mencionesStr.split(",")) {
                if (!mencion.trim().isEmpty()) {
                    menciones.add(mencion.trim());
                }
            }
        }
        post.setMenciones(menciones);
        
        // Ruta imagen
        post.setRutaImagen(leerStringFijo(raf, 200));
        
        // Tipo multimedia
        int tipoOrdinal = raf.readInt();
        post.setTipoMultimedia(TipoMultimedia.values()[tipoOrdinal]);
        
        // Usuarios likes
        String likesStr = leerStringFijo(raf, 200);
        ArrayList<String> likes = new ArrayList<>();
        if (!likesStr.isEmpty()) {
            for (String like : likesStr.split(",")) {
                if (!like.trim().isEmpty()) {
                    likes.add(like.trim());
                }
            }
        }
        post.setUsuariosLikes(likes);
        
        // Cantidad comentarios (no se usa aquí, se cargan después)
        raf.readInt();
        
        return post;
    }
    
    /**
     * Escribe un comentario
     */
    private void escribirComentario(RandomAccessFile raf, Comment comment) 
            throws IOException {
        
        escribirStringFijo(raf, comment.getId(), 50);
        escribirStringFijo(raf, comment.getPostId(), 50);
        escribirStringFijo(raf, comment.getUsername(), 50);
        escribirStringFijo(raf, comment.getContenido(), 220);
        raf.writeLong(comment.getFecha().getTime());
        
        String likes = String.join(",", comment.getUsuariosLikes());
        escribirStringFijo(raf, likes, 200);
    }
    
    /**
     * Lee un comentario
     */
    private Comment leerComentario(RandomAccessFile raf) throws IOException {
        Comment comment = new Comment();
        
        comment.setId(leerStringFijo(raf, 50));
        comment.setPostId(leerStringFijo(raf, 50));
        comment.setUsername(leerStringFijo(raf, 50));
        comment.setContenido(leerStringFijo(raf, 220));
        comment.setFecha(new Date(raf.readLong()));
        
        String likesStr = leerStringFijo(raf, 200);
        ArrayList<String> likes = new ArrayList<>();
        if (!likesStr.isEmpty()) {
            for (String like : likesStr.split(",")) {
                if (!like.trim().isEmpty()) {
                    likes.add(like.trim());
                }
            }
        }
        comment.setUsuariosLikes(likes);
        
        return comment;
    }
    
    /**
     * Guarda un like
     */
    private void guardarLike(String postId, String username) throws IOException {
        String rutaLikes = CARPETA_RAIZ + "/" + ARCHIVO_LIKES;
        
        try (RandomAccessFile raf = new RandomAccessFile(rutaLikes, "rw")) {
            raf.seek(raf.length());
            escribirStringFijo(raf, postId, 50);
            escribirStringFijo(raf, username, 50);
        }
    }
    
    /**
     * Elimina un like
     */
    private void eliminarLike(String postId, String username) throws IOException {
        // Implementación simplificada
        // En producción, se reescribiría el archivo sin ese like
    }
    
    /**
     * Escribe string de tamaño fijo
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
     * Lee string de tamaño fijo
     */
    private String leerStringFijo(RandomAccessFile raf, int tamanio) 
            throws IOException {
        
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < tamanio; i++) {
            sb.append(raf.readChar());
        }
        
        return sb.toString().trim();
    }
}   