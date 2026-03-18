/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package instagram.Enums;

/**
 *
 * @author janinadiaz
 */

public enum EstadoCuenta {
    ACTIVO("Activo"),
    DESACTIVADO("Desactivado"),
    SUSPENDIDO("Suspendido");
    
    private String descripcion;
    
    EstadoCuenta(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    @Override
    public String toString() {
        return descripcion;
    }
}
