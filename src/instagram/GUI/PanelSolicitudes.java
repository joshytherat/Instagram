/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.GUI;

/**
 *
 * @author janinadiaz
 */



import instagram.Utilities.FollowManager;
import instagram.Utilities.FollowRequest;
import instagram.Utilities.User;
import instagram.Utilities.UserManager;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class PanelSolicitudes extends JPanel {
    
    private static final Color FONDO = new Color(250, 250, 250);
    private static final Color INSTAGRAM_AZUL = new Color(0, 149, 246);
    private static final Color GRIS_BOTON = new Color(239, 239, 239);
    
    private UserManager userManager;
    private FollowManager followManager;
    private User usuarioActual;
    
    private JPanel panelLista;
    private JLabel lblFeedback;
    
    public PanelSolicitudes(UserManager userManager, FollowManager followManager, 
                           User usuarioActual) {
        
        this.userManager = userManager;
        this.followManager = followManager;
        this.usuarioActual = usuarioActual;
        
        setLayout(new BorderLayout());
        setBackground(FONDO);
        
        construirInterfaz();
        cargarSolicitudes();
    }
    
    private void construirInterfaz() {
        // Header
        JPanel header = new JPanel(null);
        header.setPreferredSize(new Dimension(390, 60));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        
        JLabel lblTitulo = new JLabel("Solicitudes de seguimiento");
        lblTitulo.setBounds(15, 15, 300, 30);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 16));
        header.add(lblTitulo);
        
        add(header, BorderLayout.NORTH);
        
        // Lista scrolleable
        panelLista = new JPanel();
        panelLista.setLayout(new BoxLayout(panelLista, BoxLayout.Y_AXIS));
        panelLista.setBackground(FONDO);
        
        JScrollPane scroll = new JScrollPane(panelLista);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
        
        // Feedback
        lblFeedback = new JLabel("", SwingConstants.CENTER);
        lblFeedback.setPreferredSize(new Dimension(390, 40));
        lblFeedback.setFont(new Font("Arial", Font.PLAIN, 13));
        add(lblFeedback, BorderLayout.SOUTH);
    }
    
    private void cargarSolicitudes() {
        try {
            ArrayList<FollowRequest> solicitudes = 
                followManager.obtenerSolicitudesPendientes(usuarioActual.getUsername());
            
            panelLista.removeAll();
            
            if (solicitudes.isEmpty()) {
                JLabel lblVacio = new JLabel("No tienes solicitudes pendientes", SwingConstants.CENTER);
                lblVacio.setFont(new Font("Arial", Font.PLAIN, 14));
                lblVacio.setForeground(Color.GRAY);
                lblVacio.setAlignmentX(Component.CENTER_ALIGNMENT);
                panelLista.add(Box.createVerticalStrut(100));
                panelLista.add(lblVacio);
            } else {
                for (FollowRequest request : solicitudes) {
                    panelLista.add(crearItemSolicitud(request));
                    panelLista.add(Box.createVerticalStrut(5));
                }
            }
            
            panelLista.revalidate();
            panelLista.repaint();
            
        } catch (Exception e) {
            mostrarError("Error cargando solicitudes");
            e.printStackTrace();
        }
    }
    
    private JPanel crearItemSolicitud(FollowRequest request) {
        JPanel panel = new JPanel(null);
        panel.setPreferredSize(new Dimension(390, 70));
        panel.setMaximumSize(new Dimension(390, 70));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        
        // Foto de perfil
        JLabel lblFoto = new JLabel("👤");
        lblFoto.setBounds(15, 15, 40, 40);
        lblFoto.setOpaque(true);
        lblFoto.setBackground(Color.LIGHT_GRAY);
        lblFoto.setHorizontalAlignment(SwingConstants.CENTER);
        lblFoto.setFont(new Font("Arial", Font.PLAIN, 20));
        panel.add(lblFoto);
        
        // Username
        JLabel lblUsername = new JLabel("@" + request.getRemitenteUsername());
        lblUsername.setBounds(65, 15, 150, 20);
        lblUsername.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(lblUsername);
        
        JLabel lblTexto = new JLabel("quiere seguirte");
        lblTexto.setBounds(65, 35, 150, 18);
        lblTexto.setFont(new Font("Arial", Font.PLAIN, 13));
        lblTexto.setForeground(Color.GRAY);
        panel.add(lblTexto);
        
        // Botón Aceptar
        JButton btnAceptar = new JButton("Aceptar");
        btnAceptar.setBounds(220, 18, 75, 32);
        btnAceptar.setFont(new Font("Arial", Font.BOLD, 12));
        btnAceptar.setForeground(Color.WHITE);
        btnAceptar.setBackground(INSTAGRAM_AZUL);
        btnAceptar.setBorder(BorderFactory.createEmptyBorder());
        btnAceptar.setFocusPainted(false);
        btnAceptar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAceptar.addActionListener(e -> aceptarSolicitud(request));
        panel.add(btnAceptar);
        
        // Botón Rechazar
        JButton btnRechazar = new JButton("Rechazar");
        btnRechazar.setBounds(305, 18, 75, 32);
        btnRechazar.setFont(new Font("Arial", Font.BOLD, 12));
        btnRechazar.setForeground(Color.BLACK);
        btnRechazar.setBackground(GRIS_BOTON);
        btnRechazar.setBorder(BorderFactory.createEmptyBorder());
        btnRechazar.setFocusPainted(false);
        btnRechazar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRechazar.addActionListener(e -> rechazarSolicitud(request));
        panel.add(btnRechazar);
        
        return panel;
    }
    
    private void aceptarSolicitud(FollowRequest request) {
        try {
            followManager.aceptarSolicitud(
                request.getDestinatarioUsername(),
                request.getRemitenteUsername()
            );
            
            mostrarExito("✓ Solicitud aceptada");
            cargarSolicitudes();
            
        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }
    
    private void rechazarSolicitud(FollowRequest request) {
        try {
            followManager.rechazarSolicitud(
                request.getDestinatarioUsername(),
                request.getRemitenteUsername()
            );
            
            mostrarExito("✓ Solicitud rechazada");
            cargarSolicitudes();
            
        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }
    
    private void mostrarExito(String mensaje) {
        lblFeedback.setText(mensaje);
        lblFeedback.setForeground(new Color(0, 200, 117));
        
        Timer timer = new Timer(3000, e -> lblFeedback.setText(""));
        timer.setRepeats(false);
        timer.start();
    }
    
    private void mostrarError(String mensaje) {
        lblFeedback.setText("❌ " + mensaje);
        lblFeedback.setForeground(Color.RED);
        
        Timer timer = new Timer(3000, e -> lblFeedback.setText(""));
        timer.setRepeats(false);
        timer.start();
    }
}