package vista;
import gestor.GestorPeatones;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;
import modelo.Bache;
import modelo.Bus;
import modelo.Carril;
import modelo.Cruce;
import modelo.Moto;
import modelo.Peaton;
import modelo.Puente;
import modelo.Semaforo;
import modelo.Vehiculo;
import modelo.Via;

/**
 * Dibuja el estado de la simulación con Graphics2D: vías, cruces con
 * semáforos, cebras, baches, vehículos, y una decoración de ciudad
 * (edificios, parques, río + puente + un barrio al otro lado) para que
 * el mapa no se vea solo como líneas grises.
 *
 * IMPORTANTE: las coordenadas de layout de acá (LAYOUT) deben coincidir
 * con el mapa que arma VentanaPrincipal.construirMapa(). Si cambian el
 * layout allá, hay que actualizar estas mismas constantes acá. Las
 * constantes OTRO_LADO_* son puramente decorativas y no dependen de eso.
 */
public class PanelSimulacion extends JPanel {
    private GestorPeatones gestorPeatones;

    // ---------- LAYOUT: debe coincidir con VentanaPrincipal ----------
    private static final int MAPA_ANCHO = 1180;
    private static final int MAPA_ALTO = 800;
    private static final int X_CRUCE_1 = 340;
    private static final int X_CRUCE_2 = 760;
    private static final int Y_AVENIDA = 400;
    private static final int PUENTE_X1 = 1180;
    private static final int PUENTE_X2 = 1400;
    private static final int RIO_Y1 = 40;
    private static final int RIO_Y2 = 760;
    private static final int MITAD_CALZADA = 42; // debe coincidir con "mitadCebra" en VentanaPrincipal

    // zona decorativa al otro lado del río: para que el puente lleve a algo,
    // no depende del mapa real de vías (VentanaPrincipal), es puramente visual.
    private static final int OTRO_LADO_X1 = PUENTE_X2;
    private static final int OTRO_LADO_ANCHO = 240;
    private static final int OTRO_LADO_X2 = OTRO_LADO_X1 + OTRO_LADO_ANCHO;

    // ---------- colores ----------
    private static final Color COLOR_FONDO = new Color(210, 214, 205);
    private static final Color COLOR_ASFALTO = new Color(58, 60, 64);
    private static final Color COLOR_VEREDA = new Color(178, 176, 168);
    private static final Color COLOR_LINEA = new Color(235, 210, 60);
    private static final Color COLOR_CEBRA = Color.WHITE;
    private static final Color COLOR_PARQUE = new Color(96, 156, 84);
    private static final Color COLOR_PARQUE_OSCURO = new Color(68, 122, 60);
    private static final Color COLOR_RIO = new Color(94, 150, 196);
    private static final Color COLOR_RIO_OSCURO = new Color(70, 122, 168);
    private static final Color COLOR_PUENTE = new Color(122, 96, 68);
    private static final Color COLOR_AUTO = new Color(66, 133, 200);
    private static final Color COLOR_BUS = new Color(224, 156, 40);
    private static final Color COLOR_MOTO = new Color(206, 70, 70);
    private static final Color COLOR_AVERIADO = new Color(150, 40, 40);
    private static final Color COLOR_BACHE = new Color(40, 32, 26);

    private List<Via> vias = new ArrayList<>();
    private List<Cruce> cruces = new ArrayList<>();
    private List<Puente> puentes = new ArrayList<>();
    private List<Bache> baches = new ArrayList<>();

    private final List<Rectangle2D> edificios = new ArrayList<>();
    private final List<Color> coloresEdificios = new ArrayList<>();
    private final List<Point2D> arboles = new ArrayList<>();
    private final List<Point2D> arbolesOtroLado = new ArrayList<>();

    public PanelSimulacion() {
        setBackground(COLOR_FONDO);
        setPreferredSize(new java.awt.Dimension(OTRO_LADO_X2 + 40, MAPA_ALTO + 40));
        generarDecoracion();
    }

    public void setDatos(List<Via> vias, List<Cruce> cruces, List<Puente> puentes, List<Bache> baches) {
        this.vias = vias;
        this.cruces = cruces;
        this.puentes = puentes;
        this.baches = baches;
    }

    // ---------- decoración (se genera una sola vez, con semilla fija) ----------

    private void generarDecoracion() {
        Random rnd = new Random(42); // semilla fija: los edificios no "bailan" entre repaints

        // bloques de la ciudad: entre las calles/avenida y los bordes del mapa
        agregarBloqueEdificios(rnd, X_CRUCE_1 + 55, 60, X_CRUCE_2 - 55, Y_AVENIDA - 55, 3, 2);            // centro-norte
        agregarBloqueEdificios(rnd, X_CRUCE_2 + 55, 60, MAPA_ANCHO - 20, Y_AVENIDA - 55, 3, 2);            // noreste
        agregarBloqueEdificios(rnd, 60, Y_AVENIDA + 55, X_CRUCE_1 - 55, MAPA_ALTO - 20, 3, 2);             // noroeste... (sur en realidad)
        agregarBloqueEdificios(rnd, X_CRUCE_1 + 55, Y_AVENIDA + 55, X_CRUCE_2 - 55, MAPA_ALTO - 20, 3, 2); // centro-sur
        agregarBloqueEdificios(rnd, X_CRUCE_2 + 55, Y_AVENIDA + 55, MAPA_ANCHO - 20, MAPA_ALTO - 20, 3, 2);// sureste

        // parque en el bloque noroeste, con arbolitos en vez de edificios
        for (int i = 0; i < 14; i++) {
            double x = 60 + rnd.nextDouble() * (X_CRUCE_1 - 100);
            double y = 60 + rnd.nextDouble() * (Y_AVENIDA - 100);
            arboles.add(new Point2D.Double(x, y));
        }

        // al otro lado del puente: una plaza + un par de edificios, para que el
        // puente lleve a un barrio real y no se quede "flotando" sobre el río
        agregarBloqueEdificios(rnd, OTRO_LADO_X1 + 20, Y_AVENIDA + 55, OTRO_LADO_X2 - 20, MAPA_ALTO - 20, 2, 2);
        for (int i = 0; i < 8; i++) {
            double x = OTRO_LADO_X1 + 25 + rnd.nextDouble() * (OTRO_LADO_ANCHO - 70);
            double y = 60 + rnd.nextDouble() * (Y_AVENIDA - 130);
            arbolesOtroLado.add(new Point2D.Double(x, y));
        }
    }

    private void agregarBloqueEdificios(Random rnd, double x1, double y1, double x2, double y2, int cols, int rows) {
        if (x2 <= x1 || y2 <= y1) return;
        double anchoCelda = (x2 - x1) / cols;
        double altoCelda = (y2 - y1) / rows;
        Color[] paleta = {
                new Color(224, 214, 196), new Color(198, 206, 214),
                new Color(214, 196, 196), new Color(206, 214, 198)
        };
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                double margen = 8;
                double bx = x1 + i * anchoCelda + margen;
                double by = y1 + j * altoCelda + margen;
                double bw = anchoCelda - margen * 2 - rnd.nextDouble() * 10;
                double bh = altoCelda - margen * 2 - rnd.nextDouble() * 10;
                edificios.add(new Rectangle2D.Double(bx, by, Math.max(20, bw), Math.max(20, bh)));
                coloresEdificios.add(paleta[rnd.nextInt(paleta.length)]);
            }
        }
    }

    // ---------- dibujo principal ----------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        dibujarRioDecorativo(g2);
        dibujarEdificiosYParque(g2);
        dibujarCalzadas(g2);
        dibujarOtroLadoCalle(g2);

        for (Via via : vias) {
            dibujarLineaCentral(g2, via);
            dibujarDivisorCarriles(g2, via);
        }
        for (Bache b : baches) {
            dibujarBache(g2, b);
        }
        for (Cruce cruce : cruces) {
            //dibujarCruce(g2, cruce);
        }
        for (Via via : vias) {
            dibujarVehiculosDeVia(g2, via);
            if (gestorPeatones != null) {
                for (Rectangle2D paso : gestorPeatones.getPasosAleatorios()) {
                    dibujarCebra(g2, paso);
    }
    // Dibujar Peatones
        for (Peaton p : gestorPeatones.getPeatones()) {
            g2.setColor(new java.awt.Color(200, 100, 100)); // Color distintivo para peatón
            g2.fill(new java.awt.geom.Ellipse2D.Double(p.getX() - 4, p.getY() - 4, 8, 8));
        }
    }
        }

        dibujarPuenteDemo(g2);
    }
    public void setGestorPeatones(GestorPeatones gp) { this.gestorPeatones = gp; }

    // ---------- ciudad (fondo decorativo) ----------

    private void dibujarEdificiosYParque(Graphics2D g2) {
        Rectangle2D parque = new Rectangle2D.Double(60, 60, X_CRUCE_1 - 120, Y_AVENIDA - 120);
        g2.setColor(COLOR_PARQUE);
        g2.fill(parque);
        g2.setColor(COLOR_PARQUE_OSCURO);
        for (Point2D arbol : arboles) {
            g2.fill(new Ellipse2D.Double(arbol.getX() - 7, arbol.getY() - 7, 14, 14));
        }

        Rectangle2D plazaOtroLado = new Rectangle2D.Double(
                OTRO_LADO_X1 + 20, 60, OTRO_LADO_ANCHO - 40, Y_AVENIDA - 120);
        g2.setColor(COLOR_PARQUE);
        g2.fill(plazaOtroLado);
        g2.setColor(COLOR_PARQUE_OSCURO);
        for (Point2D arbol : arbolesOtroLado) {
            g2.fill(new Ellipse2D.Double(arbol.getX() - 7, arbol.getY() - 7, 14, 14));
        }

        for (int i = 0; i < edificios.size(); i++) {
            Rectangle2D edificio = edificios.get(i);
            g2.setColor(coloresEdificios.get(i));
            g2.fill(edificio);
            g2.setColor(new Color(0, 0, 0, 60));
            g2.draw(edificio);

            g2.setColor(new Color(255, 255, 255, 90));
            int filas = 2, columnas = 2;
            double pad = 6;
            double celdaW = (edificio.getWidth() - pad * (columnas + 1)) / columnas;
            double celdaH = (edificio.getHeight() - pad * (filas + 1)) / filas;
            if (celdaW > 4 && celdaH > 4) {
                for (int fila = 0; fila < filas; fila++) {
                    for (int col = 0; col < columnas; col++) {
                        double wx = edificio.getX() + pad + col * (celdaW + pad);
                        double wy = edificio.getY() + pad + fila * (celdaH + pad);
                        g2.fill(new Rectangle2D.Double(wx, wy, celdaW, celdaH));
                    }
                }
            }
        }
    }

    private void dibujarRioDecorativo(Graphics2D g2) {
        Rectangle2D rio = new Rectangle2D.Double(PUENTE_X1 + 60, RIO_Y1, PUENTE_X2 - PUENTE_X1 - 20, RIO_Y2 - RIO_Y1);
        g2.setColor(COLOR_RIO);
        g2.fill(rio);
        g2.setColor(COLOR_RIO_OSCURO);
        g2.setStroke(new BasicStroke(2f));
        for (int y = RIO_Y1 + 20; y < RIO_Y2; y += 34) {
            g2.draw(new QuadCurve2D.Double(rio.getX(), y, rio.getCenterX(), y + 14, rio.getMaxX(), y));
        }
        g2.setColor(new Color(255, 255, 255, 140));
        g2.drawString("Río Grande", (float) rio.getX() + 10, (float) rio.getY() + 20);
    }

    // ---------- calle al otro lado del puente ----------

    /**
     * Continúa la calzada del puente hacia el nuevo barrio y termina en un
     * remate redondeado (como una plazoleta), sugiriendo que la calle sigue
     * más allá del mapa visible en vez de terminar de la nada sobre el río.
     */
    private void dibujarOtroLadoCalle(Graphics2D g2) {
        double xInicio = PUENTE_X2 - 30; // mismo punto donde termina el tablero del puente
        double xFin = OTRO_LADO_X2 - 50;

        dibujarBandaVial(g2, xInicio, Y_AVENIDA - MITAD_CALZADA, xFin - xInicio, MITAD_CALZADA * 2);

        g2.setColor(COLOR_VEREDA);
        g2.fill(new Ellipse2D.Double(xFin - MITAD_CALZADA - 6, Y_AVENIDA - MITAD_CALZADA - 6, MITAD_CALZADA * 2 + 12, MITAD_CALZADA * 2 + 12));
        g2.setColor(COLOR_ASFALTO);
        g2.fill(new Ellipse2D.Double(xFin - MITAD_CALZADA, Y_AVENIDA - MITAD_CALZADA, MITAD_CALZADA * 2, MITAD_CALZADA * 2));
    }

    // ---------- calzadas ----------

    private void dibujarCalzadas(Graphics2D g2) {
        dibujarBandaVial(g2, 40, Y_AVENIDA - MITAD_CALZADA, MAPA_ANCHO - 40, MITAD_CALZADA * 2);
        dibujarBandaVial(g2, X_CRUCE_1 - MITAD_CALZADA, 40, MITAD_CALZADA * 2, MAPA_ALTO - 40);
        dibujarBandaVial(g2, X_CRUCE_2 - MITAD_CALZADA, 40, MITAD_CALZADA * 2, MAPA_ALTO - 40);
    }

    private void dibujarBandaVial(Graphics2D g2, double x, double y, double ancho, double alto) {
        int veredaGrosor = 6;
        g2.setColor(COLOR_VEREDA);
        g2.fill(new Rectangle2D.Double(x - veredaGrosor, y - veredaGrosor, ancho + veredaGrosor * 2, alto + veredaGrosor * 2));
        g2.setColor(COLOR_ASFALTO);
        g2.fill(new Rectangle2D.Double(x, y, ancho, alto));
    }

    private void dibujarLineaCentral(Graphics2D g2, Via via) {
        List<Point2D> trazado = via.getTrazado();
        if (trazado.size() < 2) return;
        g2.setColor(COLOR_LINEA);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{9f, 7f}, 0f));
        g2.draw(new Line2D.Double(trazado.get(0), trazado.get(trazado.size() - 1)));
    }

    /** Línea punteada fina entre el carril rápido y el carril lento de una misma vía (mismo sentido). */
    private void dibujarDivisorCarriles(Graphics2D g2, Via via) {
        List<Carril> carriles = via.getCarriles();
        List<Point2D> trazado = via.getTrazado();
        if (carriles.size() < 2 || trazado.size() < 2) return;

        double offsetMedio = (carriles.get(0).getY() + carriles.get(1).getY()) / 2.0;
        double[] perp = carriles.get(0).getPerpendicular();

        Point2D a = desplazar(trazado.get(0), perp, offsetMedio);
        Point2D b = desplazar(trazado.get(trazado.size() - 1), perp, offsetMedio);

        g2.setColor(new Color(255, 255, 255, 150));
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 6f}, 0f));
        g2.draw(new Line2D.Double(a, b));
    }

    private Point2D desplazar(Point2D p, double[] perp, double offset) {
        return new Point2D.Double(p.getX() + perp[0] * offset, p.getY() + perp[1] * offset);
    }

    // ---------- cruces (semáforos + cebra) ----------

    private void dibujarCruce(Graphics2D g2, Cruce cruce) {
        Rectangle2D zona = cruce.getZonaCruce().getArea();
        dibujarCebra(g2, zona);

        if (cruce.getZonaCruce().hayPeatonCruzando()) {
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(zona.getCenterX() - 5, zona.getCenterY() - 5, 10, 10));
        }

        List<Cruce.Acceso> accesos = cruce.getAccesos();
        for (int i = 0; i < accesos.size(); i++) {
            Cruce.Acceso acceso = accesos.get(i);
            Via via = acceso.getVia();
            if (via.getTrazado().isEmpty()) continue;

            Point2D punto = via.getTrazado().get(via.getTrazado().size() - 1);
            dibujarSemaforo(g2, punto, acceso.getSemaforo().getEstadoActual());

            g2.setColor(Color.BLACK);
            g2.drawString(String.valueOf(acceso.getColaEspera().size()),
                    (float) punto.getX() + 10, (float) punto.getY() - 8 + i * 12);
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

    private void dibujarSemaforo(Graphics2D g2, Point2D punto, Semaforo.Estado estado) {
        Color color;
        switch (estado) {
            case VERDE: color = new Color(70, 200, 90); break;
            case AMARILLO: color = new Color(230, 200, 40); break;
            default: color = new Color(210, 60, 60); break;
        }
        g2.setColor(new Color(35, 35, 38));
        g2.fill(new RoundRectangle2D.Double(punto.getX() - 8, punto.getY() - 11, 16, 22, 6, 6));
        g2.setColor(color);
        g2.fill(new Ellipse2D.Double(punto.getX() - 5, punto.getY() - 5, 10, 10));
    }

    // ---------- baches ----------

    private void dibujarBache(Graphics2D g2, Bache bache) {
        double radio = 6 + bache.getSeveridad() * 8;
        g2.setColor(COLOR_BACHE);
        g2.fill(new Ellipse2D.Double(bache.getX() - radio, bache.getY() - radio, radio * 2, radio * 2));
        g2.setColor(new Color(0, 0, 0, 90));
        g2.draw(new Line2D.Double(bache.getX() - radio, bache.getY(), bache.getX() + radio * 0.4, bache.getY() + radio * 0.6));
    }

    // ---------- vehículos ----------

    private void dibujarVehiculosDeVia(Graphics2D g2, Via via) {
        for (Carril carril : via.getCarriles()) {
            for (Vehiculo v : carril.getVehiculos()) {
                dibujarVehiculo(g2, v);
            }
        }
    }

    private void dibujarVehiculo(Graphics2D g2, Vehiculo v) {
        Shape hitbox = v.getHitbox(); // ya viene rotado según su ángulo real (Vehiculo.getHitbox)

        Color color;
        if (v.isAveriado()) {
            color = COLOR_AVERIADO;
        } else if (v instanceof Bus) {
            color = COLOR_BUS;
        } else if (v instanceof Moto) {
            color = COLOR_MOTO;
        } else {
            color = COLOR_AUTO;
        }

        g2.setColor(color);
        g2.fill(hitbox);
        g2.setColor(new Color(0, 0, 0, 140));
        g2.draw(hitbox);

        if (v.isAveriado()) {
            g2.setColor(Color.YELLOW);
            g2.drawString("!", (float) v.getX() - 2, (float) (v.getY() - v.getAncho()));
        }
    }

    // ---------- puente de demostración (aislado del tráfico automático) ----------

    private void dibujarPuenteDemo(Graphics2D g2) {
        for (Puente puente : puentes) {
            Via via = puente.getVia();
            if (via == null || via.getTrazado().size() < 2) continue;

            Point2D a = via.getTrazado().get(0);
            Point2D b = via.getTrazado().get(via.getTrazado().size() - 1);

            g2.setColor(COLOR_PUENTE);
            g2.setStroke(new BasicStroke(22, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(a, b));

            g2.setColor(new Color(230, 230, 230));
            g2.setStroke(new BasicStroke(2f));
            for (double t = 0; t <= 1.0; t += 0.08) {
                double x = a.getX() + (b.getX() - a.getX()) * t;
                double y = a.getY() + (b.getY() - a.getY()) * t;
                g2.draw(new Line2D.Double(x, y - 12, x, y + 12));
            }

            Point2D centro = new Point2D.Double((a.getX() + b.getX()) / 2, (a.getY() + b.getY()) / 2);
            String texto = "Puente: " + (puente.getSentidoActual() == Puente.Sentido.IDA ? "IDA" : "VUELTA")
                    + "  (esperan " + puente.getColaOpuesta().size() + ")";
            g2.setColor(Color.BLACK);
            g2.drawString(texto, (float) centro.getX() - 60, (float) centro.getY() - 20);

            if (!puente.estaLibre()) {
                g2.setColor(Color.YELLOW);
                g2.fill(new Ellipse2D.Double(centro.getX() - 4, centro.getY() - 4, 8, 8));
            }

            for (Carril carril : via.getCarriles()) {
                for (Vehiculo v : carril.getVehiculos()) {
                    dibujarVehiculo(g2, v);
                }
            }
        }
    }
}