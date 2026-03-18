/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.GUI;

/**
 *
 * @author janinadiaz
 */


import javax.swing.*;
import java.awt.*;

public class BadgeLabel extends JLabel {
    
    private int count;
    private boolean visible;
    
    public BadgeLabel() {
        this.count = 0;
        this.visible = false;
        
        setPreferredSize(new Dimension(18, 18));
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setFont(new Font("Arial", Font.BOLD, 10));
        setForeground(Color.WHITE);
        setOpaque(false);
    }
    
    public int getCount() { return count; }

    public void setCount(int count) {
        this.count = count;
        this.visible = count > 0;
        setText(count > 9 ? "9+" : String.valueOf(count));
        setVisible(visible);
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        if (!visible || count == 0) return;
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Círculo rojo
        g2d.setColor(InstagramColors.BADGE_ROJO);
        g2d.fillOval(0, 0, getWidth(), getHeight());
        
        g2d.dispose();
        super.paintComponent(g);
    }
}