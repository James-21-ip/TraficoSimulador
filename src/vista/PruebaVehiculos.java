package vista;

import modelo.*;
import gestor.GestorVehiculos;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class PruebaVehiculos extends JPanel {

    private final Via viaEntrada;      // (0,300) -> (300,300)
    private final Via viaBajada;       // (300,300) -> (300,550)  giro hacia abajo
    private final Via viaTramoMedio;   // (300,300) -> (650,300)  sigue derecho
    private final Via viaSubida;       // (650,300) -> (650,50)   giro hacia arriba
    private final Via viaTramoFinal;   // (650,300) -> (1000,300) sigue derecho hasta salir

    private final GestorVehiculos gestor = new GestorVehiculos();
    private final double lambda = 0.35;
    private final Timer timer;
    private final double deltaTime = 1.0 / 60.0;

    public PruebaVehiculos() {
        setPreferredSize(new Dimension(1000, 600));
        setBackground(Color.DARK_GRAY);

        viaEntrada = crearViaHorizontal(0, 300, 300, 2);
        viaBajada = crearViaVertical(300, 300, 550, 1);
        viaTramoMedio = crearViaHorizontal(300, 300, 650, 2);
        viaSubida = crearViaVertical(650, 300, 50, 1);
        viaTramoFinal = crearViaHorizontal(650, 300, 1000, 2);

        // primer punto de giro: al final de la entrada, puede bajar o seguir derecho
        viaEntrada.agregarConexion(viaBajada);
        viaEntrada.agregarConexion(viaTramoMedio);

        // segundo punto de giro, mas adelante: puede subir o seguir derecho hasta salir
        viaTramoMedio.agregarConexion(viaSubida);
        viaTramoMedio.agregarConexion(viaTramoFinal);

        // baches bien separados entre si y lejos de los puntos de giro
        List<Bache> baches = new ArrayList<>();
        baches.add(new Bache(120, 292, 0.5)); // en viaEntrada, carril de arriba
        baches.add(new Bache(480, 308, 0.6)); // en viaTramoMedio, carril de abajo
        baches.add(new Bache(820, 292, 0.5)); // en viaTramoFinal, carril de arriba
        gestor.setBaches(baches);

        timer = new Timer(16, e -> {
            gestor.intentarSpawn(viaEntrada, lambda, deltaTime);
            gestor.actualizar(deltaTime);
            repaint();
        });
    }

    private Via crearViaHorizontal(double xInicio, double y, double xFin, int carriles) {
        List<Point2D> trazado = new ArrayList<>();
        trazado.add(new Point2D.Double(xInicio, y));
        trazado.add(new Point2D.Double(xFin, y));
        Via via = new Via(trazado, carriles, false, 60.0);
        if (carriles == 2) {
            via.getCarriles().get(0).setOffsetY(-8);
            via.getCarriles().get(1).setOffsetY(8);
        }
        return via;
    }

    private Via crearViaVertical(double x, double yInicio, double yFin, int carriles) {
        List<Point2D> trazado = new ArrayList<>();
        trazado.add(new Point2D.Double(x, yInicio));
        trazado.add(new Point2D.Double(x, yFin));
        Via via = new Via(trazado, carriles, false, 60.0);
        via.getCarriles().get(0).setOffsetY(0);
        return via;
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
        for (Via via : List.of(viaEntrada, viaBajada, viaTramoMedio, viaSubida, viaTramoFinal)) {
            dibujarVia(g2, via);
        }

        g2.setColor(Color.BLACK);
        g2.fillOval(120 - 5, 292 - 5, 10, 10);
        g2.fillOval(480 - 5, 308 - 5, 10, 10);
        g2.fillOval(820 - 5, 292 - 5, 10, 10);

        for (Vehiculo v : gestor.getVehiculos()) {
            dibujarVehiculo(g2, v);
        }
    }

    private void dibujarVia(Graphics2D g2, Via via) {
        for (Carril c : via.getCarriles()) {
            double[] p = c.getPuntoInicio();
            List<Point2D> trazado = via.getTrazado();
            Point2D fin = trazado.get(trazado.size() - 1);
            double[] perp = c.getPerpendicular();
            double fx = fin.getX() + perp[0] * c.getY();
            double fy = fin.getY() + perp[1] * c.getY();
            g2.drawLine((int) p[0], (int) p[1], (int) fx, (int) fy);
        }
    }

    private void dibujarVehiculo(Graphics2D g2, Vehiculo v) {
        Shape hitbox = v.getHitbox();

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
        JFrame frame = new JFrame("Prueba GestorVehiculos - mapa extenso");
        PruebaVehiculos panel = new PruebaVehiculos();
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.iniciar();
    }
}