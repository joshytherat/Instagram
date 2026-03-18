/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Utilities;

/**
 *
 * @author janinadiaz
 **/

import instagram.Enums.EstadoCuenta;
import instagram.Enums.TipoCuenta;
import instagram.Enums.Genero;
import instagram.Interfaces.Validable;
import java.util.Date;
import java.util.Calendar;

public class User implements Validable {
    
    // Constantes para tamaño de campos en RandomAccessFile
    public static final int USERNAME_SIZE = 50;
    public static final int PASSWORD_SIZE = 50;
    public static final int NOMBRE_SIZE = 100;
    public static final int BIOGRAFIA_SIZE = 220;  // NUEVO
    public static final int RUTA_FOTO_SIZE = 200;
    public static final int TAMANIO_REGISTRO = 
        USERNAME_SIZE + PASSWORD_SIZE + NOMBRE_SIZE + BIOGRAFIA_SIZE + RUTA_FOTO_SIZE + 
        4 + // genero (int)
        4 + // edad (int)
        8 + // fechaRegistro (long)
        4 + // estadoCuenta (int)
        4;  // tipoCuenta (int)
    // TOTAL: 690 bytes (antes era 470)
    
    private String nombreCompleto;
    private Genero genero;
    private String username;
    private String password;
    private int edad;
    private Date fechaRegistro;
    private EstadoCuenta estadoCuenta;
    private TipoCuenta tipoCuenta;
    private String biografia;  // NUEVO
    private String rutaFotoPerfil;
    
    // Constructor vacío
    public User() {
        this.fechaRegistro = new Date();
        this.estadoCuenta = EstadoCuenta.ACTIVO;
        this.tipoCuenta = TipoCuenta.PUBLICA;
        this.rutaFotoPerfil = "default.jpg";
        this.biografia = "";  // NUEVO
    }
    
    // Constructor completo
    public User(String nombreCompleto, Genero genero, String username, 
                String password, int edad) {
        this();
        this.nombreCompleto = nombreCompleto;
        this.genero = genero;
        this.username = username;
        this.password = password;
        this.edad = edad;
    }
    
    @Override
    public boolean validar() throws Exception {
        if (username == null || username.trim().isEmpty()) {
            throw new Exception("El username no puede estar vacío");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new Exception("La contraseña no puede estar vacía");
        }
        if (nombreCompleto == null || nombreCompleto.trim().isEmpty()) {
            throw new Exception("El nombre completo no puede estar vacío");
        }
        if (edad < 13) {
            throw new Exception("Debes tener al menos 13 años");
        }
        if (genero == null) {
            throw new Exception("Debes seleccionar un género");
        }
        return true;
    }
    
    @Override
    public String obtenerMensajeError() {
        try {
            validar();
            return "";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    // Getters y Setters
    public String getNombreCompleto() {
        return nombreCompleto;
    }
    
    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }
    
    public Genero getGenero() {
        return genero;
    }
    
    public void setGenero(Genero genero) {
        this.genero = genero;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public int getEdad() {
        return edad;
    }
    
    public void setEdad(int edad) {
        this.edad = edad;
    }
    
    public Date getFechaRegistro() {
        return fechaRegistro;
    }
    
    public void setFechaRegistro(Date fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
    
    public EstadoCuenta getEstadoCuenta() {
        return estadoCuenta;
    }
    
    public void setEstadoCuenta(EstadoCuenta estadoCuenta) {
        this.estadoCuenta = estadoCuenta;
    }
    
    public TipoCuenta getTipoCuenta() {
        return tipoCuenta;
    }
    
    public void setTipoCuenta(TipoCuenta tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }
    
    // NUEVO: Getter y Setter para biografía
    public String getBiografia() {
        return biografia;
    }
    
    public void setBiografia(String biografia) {
        if (biografia != null && biografia.length() > BIOGRAFIA_SIZE) {
            this.biografia = biografia.substring(0, BIOGRAFIA_SIZE);
        } else {
            this.biografia = biografia;
        }
    }
    
    public String getRutaFotoPerfil() {
        return rutaFotoPerfil;
    }
    
    public void setRutaFotoPerfil(String rutaFotoPerfil) {
        this.rutaFotoPerfil = rutaFotoPerfil;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", nombreCompleto='" + nombreCompleto + '\'' +
                ", edad=" + edad +
                ", genero=" + genero +
                ", estadoCuenta=" + estadoCuenta +
                ", tipoCuenta=" + tipoCuenta +
                '}';
    }
}