package vista;

import modelo.*;
import gestor.GestorVehiculos;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class PruebaVehiculos extends JPanel {

    private final Via via;
    private final GestorVehiculos gestor = new GestorVehiculos();
    private final double lambda = 0.4;
    private final Timer timer;
    private final double deltaTime = 1.0 / 60.0;

    public PruebaVehiculos() {
        setPreferredSize(new Dimension(900, 200));
        setBackground(Color.DARK_GRAY);

        List<Point2D> trazado = new ArrayList<>();
        trazado.add(new Point2D.Double(0, 100));
        trazado.add(new Point2D.Double(900, 100));

        via = new Via(trazado, 2, false, 15.0);
        via.getCarriles().get(0).setOffsetY(85);
        via.getCarriles().get(1).setOffsetY(115);

        List<Bache> baches = new ArrayList<>();
        baches.add(new Bache(800, 85, 0.5));  // cerca del final, carril de arriba
        baches.add(new Bache(450, 115, 0.6)); // a la mitad, carril de abajo
        gestor.setBaches(baches);

        timer = new Timer(16, e -> {
            gestor.intentarSpawn(via, lambda, deltaTime);
            gestor.actualizar(deltaTime);
            repaint();
        });
    }

    public void iniciar() {
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.LIGHT_GRAY);
        g2.drawLine(0, 85, 900, 85);
        g2.drawLine(0, 115, 900, 115);

        g2.setColor(Color.BLACK);
        g2.fillOval(795, 80, 10, 10);  // bache 1
        g2.fillOval(445, 110, 10, 10); // bache 2

        for (Vehiculo v : gestor.getVehiculos()) {
            dibujarVehiculo(g2, v);
        }
    }

   private void dibujarVehiculo(Graphics2D g2, Vehiculo v) {
    java.awt.Shape hitbox = v.getHitbox(); // ya viene rotado si esta cambiando de carril

    if (v instanceof Bus) {
        g2.setColor(Color.ORANGE);
    } else if (v instanceof Moto) {
        g2.setColor(Color.CYAN);
    } else {
        g2.setColor(Color.WHITE);
    }

    if (v.isAveriado()) {
        g2.setColor(Color.RED);
    }

    g2.fill(hitbox);
}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Prueba GestorVehiculos");
        PruebaVehiculos panel = new PruebaVehiculos();
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.iniciar();
    }
}