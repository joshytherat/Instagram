package instagram.Utilities;

public class ValidadorPassword {

    private boolean tieneMayuscula;
    private boolean tieneSimbolo;
    private boolean longitudSuficiente; // > 8 caracteres (no número)

    public ValidadorPassword() { reset(); }

    public void reset() {
        tieneMayuscula    = false;
        tieneSimbolo      = false;
        longitudSuficiente = false;
    }

    public void validar(String password) {
        reset();
        if (password == null || password.isEmpty()) return;

        // Más de 8 caracteres (estrictamente mayor)
        longitudSuficiente = password.length() > 8;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c))  tieneMayuscula = true;
            if (esSimboloEspecial(c))      tieneSimbolo   = true;
        }
    }

    private boolean esSimboloEspecial(char c) {
        return "!@#$%^&*()_+-=[]{}|;:,.<>?/~`".indexOf(c) != -1;
    }

    public boolean esValida() {
        return tieneMayuscula && tieneSimbolo && longitudSuficiente;
    }

    public boolean tieneMayuscula()     { return tieneMayuscula; }
    public boolean tieneSimbolo()       { return tieneSimbolo; }
    public boolean tieneLongitudSuficiente() { return longitudSuficiente; }

    // Mantener compatibilidad con código que llame tieneNumero()

    public String obtenerMensajeError() {
        if (esValida()) return "";
        StringBuilder sb = new StringBuilder("La contraseña debe tener:\n");
        if (!longitudSuficiente) sb.append("• Más de 8 caracteres\n");
        if (!tieneMayuscula)     sb.append("• Al menos 1 mayúscula\n");
        if (!tieneSimbolo)       sb.append("• Al menos 1 símbolo especial (!@#$...)\n");
        return sb.toString();
    }
}