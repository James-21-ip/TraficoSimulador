package vista;

import modelo.Carril;
import modelo.Cruce;
import modelo.Puente;
import modelo.Vehiculo;
import modelo.Via;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Dibuja el estado actual de la simulación con Graphics2D:
 * vías (con línea central punteada), cruces con sus semáforos, la
 * cebra peatonal, el puente y los vehículos (rectángulos redondeados
 * rotados según la dirección de su vía).
 *
 * NOTA sobre Bache: todavía no tengo el código de esa clase (la hace
 * Integrante 1), así que no la dibujo acá. Cuando la tengas, se agrega
 * un dibujarBache(Bache) muy parecido a dibujarCebra, usando la
 * posición y severidad reales de esa clase.
 */
public class PanelSimulacion extends JPanel {

    private static final Color COLOR_FONDO = new Color(86, 140, 86);
    private static final Color COLOR_VIA = new Color(55, 55, 58);
    private static final Color COLOR_LINEA_CENTRAL = new Color(230, 230, 230);
    private static final Color COLOR_CEBRA = Color.WHITE;
    private static final Color COLOR_AUTO = new Color(60, 120, 200);
    private static final Color COLOR_BUS = new Color(200, 150, 40);
    private static final Color COLOR_MOTO = new Color(200, 60, 60);
    private static final int ANCHO_CARRIL_PX = 12;

    private List<Via> vias;
    private List<Cruce> cruces;
    private List<Puente> puentes;

    public PanelSimulacion() {
        setBackground(COLOR_FONDO);
    }

    public void setDatos(List<Via> vias, List<Cruce> cruces, List<Puente> puentes) {
        this.vias = vias;
        this.cruces = cruces;
        this.puentes = puentes;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (vias != null) {
            for (Via via : vias) {
                dibujarVia(g2, via);
            }
        }
        if (puentes != null) {
            for (Puente puente : puentes) {
                dibujarPuente(g2, puente);
            }
        }
        if (cruces != null) {
            for (Cruce cruce : cruces) {
                dibujarCruce(g2, cruce);
            }
        }
        if (vias != null) {
            for (Via via : vias) {
                dibujarVehiculosDeVia(g2, via);
            }
        }
    }

    // ---------- vías ----------

    private void dibujarVia(Graphics2D g2, Via via) {
        List<Point2D> puntos = via.getTrazado();
        if (puntos == null || puntos.size() < 2) {
            return;
        }

        int anchoTotal = Math.max(1, via.getCarriles().size()) * ANCHO_CARRIL_PX;

        g2.setColor(COLOR_VIA);
        g2.setStroke(new BasicStroke(anchoTotal, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        dibujarPolilinea(g2, puntos);

        // línea central punteada (solo si tiene sentido, es decir doble sentido o varios carriles)
        g2.setColor(COLOR_LINEA_CENTRAL);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                1f, new float[]{10f, 8f}, 0f));
        dibujarPolilinea(g2, puntos);
    }

    private void dibujarPolilinea(Graphics2D g2, List<Point2D> puntos) {
        for (int i = 0; i < puntos.size() - 1; i++) {
            Point2D a = puntos.get(i);
            Point2D b = puntos.get(i + 1);
            g2.draw(new java.awt.geom.Line2D.Double(a, b));
        }
    }

    // ---------- puente ----------

    private void dibujarPuente(Graphics2D g2, Puente puente) {
        Via via = puente.getVia();
        if (via == null || via.getTrazado() == null || via.getTrazado().size() < 2) {
            return;
        }

        // el tablero del puente, un poco más claro que una vía normal
        g2.setColor(new Color(120, 90, 60));
        g2.setStroke(new BasicStroke(ANCHO_CARRIL_PX + 4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        dibujarPolilinea(g2, via.getTrazado());

        Point2D centro = via.getTrazado().get(via.getTrazado().size() / 2);

        String etiqueta = "Puente: " + (puente.getSentidoActual() == Puente.Sentido.IDA ? "IDA" : "VUELTA")
                + "  (esperan " + puente.getColaOpuesta().size() + ")";
        g2.setColor(Color.WHITE);
        g2.drawString(etiqueta, (float) centro.getX() - 40, (float) centro.getY() - 15);

        if (!puente.estaLibre()) {
            g2.setColor(Color.YELLOW);
            g2.fillOval((int) centro.getX() - 4, (int) centro.getY() - 4, 8, 8);
        }
    }

    // ---------- cruce (semáforos + cebra) ----------

    private void dibujarCruce(Graphics2D g2, Cruce cruce) {
        Rectangle2D zona = cruce.getZonaCruce().getArea();
        dibujarCebra(g2, zona);

        if (cruce.getZonaCruce().hayPeatonCruzando()) {
            g2.setColor(Color.WHITE);
            g2.fillOval((int) zona.getCenterX() - 5, (int) zona.getCenterY() - 5, 10, 10);
        }

        List<Cruce.Acceso> accesos = cruce.getAccesos();
        for (int i = 0; i < accesos.size(); i++) {
            Cruce.Acceso acceso = accesos.get(i);
            Via via = acceso.getVia();
            if (via.getTrazado() == null || via.getTrazado().isEmpty()) {
                continue;
            }
            // el semáforo se dibuja cerca del último punto del trazado de ese acceso
            Point2D punto = via.getTrazado().get(via.getTrazado().size() - 1);
            dibujarSemaforo(g2, punto, acceso.getSemaforo().getEstadoActual());

            String cola = "cola: " + acceso.getColaEspera().size();
            g2.setColor(Color.WHITE);
            g2.drawString(cola, (float) punto.getX() + 12, (float) punto.getY() + 12 * (i + 1));
        }
    }

    private void dibujarCebra(Graphics2D g2, Rectangle2D zona) {
        g2.setColor(COLOR_CEBRA);
        int franjas = 6;
        double anchoFranja = zona.getWidth() / (franjas * 2.0);
        for (int i = 0; i < franjas; i++) {
            double x = zona.getX() + i * anchoFranja * 2;
            g2.fill(new Rectangle2D.Double(x, zona.getY(), anchoFranja, zona.getHeight()));
        }
    }

    private void dibujarSemaforo(Graphics2D g2, Point2D punto, modelo.Semaforo.Estado estado) {
        Color color;
        switch (estado) {
            case VERDE: color = Color.GREEN; break;
            case AMARILLO: color = Color.YELLOW; break;
            default: color = Color.RED; break;
        }
        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect((int) punto.getX() - 8, (int) punto.getY() - 10, 16, 20, 6, 6);
        g2.setColor(color);
        g2.fillOval((int) punto.getX() - 5, (int) punto.getY() - 4, 10, 10);
    }

    // ---------- vehículos ----------

    private void dibujarVehiculosDeVia(Graphics2D g2, Via via) {
        List<Point2D> trazado = via.getTrazado();
        if (trazado == null || trazado.size() < 2) {
            return;
        }
        double angulo = anguloDelTrazado(trazado);

        for (Carril carril : via.getCarriles()) {
            double anguloCarril = carril.isSentidoIda() ? angulo : angulo + Math.PI;
            for (Vehiculo v : carril.getVehiculos()) {
                dibujarVehiculo(g2, v, anguloCarril);
            }
        }
    }

    private double anguloDelTrazado(List<Point2D> trazado) {
        Point2D a = trazado.get(0);
        Point2D b = trazado.get(trazado.size() - 1);
        return Math.atan2(b.getY() - a.getY(), b.getX() - a.getX());
    }

    private void dibujarVehiculo(Graphics2D g2, Vehiculo v, double angulo) {
        Rectangle2D hitbox = v.getHitbox();

        Color color;
        if (v instanceof modelo.Bus) {
            color = COLOR_BUS;
        } else if (v instanceof modelo.Moto) {
            color = COLOR_MOTO;
        } else {
            color = COLOR_AUTO;
        }

        AffineTransform transformOriginal = g2.getTransform();
        g2.translate(v.getX(), v.getY());
        g2.rotate(angulo);

        Shape cuerpo = new RoundRectangle2D.Double(
                -hitbox.getWidth() / 2, -hitbox.getHeight() / 2,
                hitbox.getWidth(), hitbox.getHeight(),
                6, 6);

        g2.setColor(color);
        g2.fill(cuerpo);
        g2.setColor(Color.BLACK);
        g2.draw(cuerpo);

        g2.setTransform(transformOriginal);
    }
}