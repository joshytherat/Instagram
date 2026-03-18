/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Utilities;

/**
 *
 * @author janinadiaz
 */


import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidadorContenido {
    
    private static final int MAX_CONTENIDO = 220;
    
    /**
     * Valida que el contenido no exceda el límite
     */
    public static boolean validarLongitud(String contenido) {
        return contenido != null && contenido.length() <= MAX_CONTENIDO;
    }
    
    /**
     * Extrae hashtags del contenido
     */
    public static ArrayList<String> extraerHashtags(String contenido) {
        ArrayList<String> hashtags = new ArrayList<>();
        
        if (contenido == null) {
            return hashtags;
        }
        
        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(contenido);
        
        while (matcher.find()) {
            hashtags.add(matcher.group(1));
        }
        
        return hashtags;
    }
    
    /**
     * Extrae menciones del contenido
     */
    public static ArrayList<String> extraerMenciones(String contenido) {
        ArrayList<String> menciones = new ArrayList<>();
        
        if (contenido == null) {
            return menciones;
        }
        
        Pattern pattern = Pattern.compile("@(\\w+)");
        Matcher matcher = pattern.matcher(contenido);
        
        while (matcher.find()) {
            menciones.add(matcher.group(1));
        }
        
        return menciones;
    }
    
    /**
     * Convierte contenido a HTML con hashtags y menciones clickeables
     */
    public static String convertirAHTML(String contenido) {
        if (contenido == null) {
            return "";
        }
        
        String html = contenido;
        
        // Convertir hashtags
        html = html.replaceAll("#(\\w+)", 
            "<span style='color: #0095F6; cursor: pointer;'>#$1</span>");
        
        // Convertir menciones
        html = html.replaceAll("@(\\w+)", 
            "<span style='color: #0095F6; cursor: pointer;'>@$1</span>");
        
        return "<html>" + html + "</html>";
    }
    
    /**
     * Formatea fecha relativa (hace 2h, hace 3d, etc.)
     */
    public static String formatearFechaRelativa(java.util.Date fecha) {
        long diff = new java.util.Date().getTime() - fecha.getTime();
        long segundos = diff / 1000;
        long minutos = segundos / 60;
        long horas = minutos / 60;
        long dias = horas / 24;
        long semanas = dias / 7;
        
        if (segundos < 60) return "ahora";
        if (minutos < 60) return "hace " + minutos + "m";
        if (horas < 24) return "hace " + horas + "h";
        if (dias < 7) return "hace " + dias + "d";
        if (semanas < 4) return "hace " + semanas + "sem";
        
        return "hace " + (dias / 30) + " meses";
    }
}