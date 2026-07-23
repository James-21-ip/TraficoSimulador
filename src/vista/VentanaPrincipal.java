package vista;

import fichero.ArchivoMapa;
import gestor.GestorCruces;
import gestor.GestorVehiculos;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.Timer;
import modelo.Auto;
import modelo.Bache;
import modelo.Bus;
import modelo.Carril;
import modelo.Cruce;
import modelo.Entrada;
import modelo.Moto;
import modelo.Puente;
import modelo.Semaforo;
import modelo.Vehiculo;
import modelo.Via;
import modelo.ZonaCruce;

/**
 * Ventana principal: arma el mapa (avenida + 2 calles + 2 cruces + baches),
 * lo conecta con el motor de vehículos y de cruces, y muestra los
 * controles de reproducción.
 *
 * El mapa que se arma acá es "a mano" (construirMapa()), como reemplazo
 * temporal de ArchivoMapa (que todavía es un archivo vacío / sin leer
 * desde disco). Cuando ArchivoMapa esté listo, esta clase solo necesita
 * cambiar construirMapa() por la carga real; el resto (gestores, timer,
 * panel) queda igual.
 *
 * El PUENTE sigue aparte del motor principal (GestorVehiculos todavía no
 * sabe rutear autos hacia su cola FIFO), pero ya no depende de botones:
 * esta clase le genera tráfico propio con el mismo esquema de probabilidad
 * (Poisson) que usa GestorVehiculos para el resto del mapa, y mueve al
 * vehículo que va cruzando con su propio Vehiculo.mover(). La alternancia
 * de sentido y el "solo uno a la vez" siguen siendo responsabilidad de
 * Puente + GestorCruces, sin tocar esas clases.
 */
public class VentanaPrincipal extends JFrame {
    private List<Entrada> entradas;

    private static final int TICK_MS = 50; // 20 ticks por segundo

    // ---------- LAYOUT del mapa (debe coincidir con PanelSimulacion) ----------
    private static final int MAPA_ANCHO = 1180;
    private static final int MAPA_ALTO = 800;
    private static final int X_CRUCE_1 = 340;
    private static final int X_CRUCE_2 = 760;
    private static final int Y_AVENIDA = 400;
    private static final int PUENTE_X1 = 1180;
    private static final int PUENTE_X2 = 1400;

    private PanelSimulacion panelSimulacion;
    private GestorVehiculos gestorVehiculos;
    private GestorCruces gestorCruces;

    private List<Via> vias;
    private List<Cruce> cruces;
    private List<Puente> puentesDemo;
    private List<Bache> baches;


    private Puente puenteDemo;

    private Timer timer;
    private boolean enPausa = false;
    private double multiplicadorVelocidad = 1.0;

    public VentanaPrincipal() {
        super("Simulador de Tráfico");
        construirMapa();
        construirPuenteDemo();
        construirInterfaz();
        iniciarLoopSimulacion();
    }

    // ---------- mapa real: avenida + 2 calles + 2 cruces ----------
    private void construirMapa() {
        ArchivoMapa lector = new ArchivoMapa();
        
        // Le indicamos que busque dentro del paquete 'mapas'
        lector.cargarMapa("/mapas/mapa.txt");

        this.vias = lector.getVias();
        this.cruces = lector.getCruces();
        this.baches = lector.getBaches();
        this.entradas = lector.getEntradas();

        gestorVehiculos = new GestorVehiculos();
        gestorVehiculos.setCruces(cruces);
        gestorVehiculos.setBaches(baches);
    }


    private Via crearVia(double x1, double y1, double x2, double y2) {
        List<Point2D> trazado = Arrays.asList(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
        return new Via(trazado, 1, false, 50);
    }

    private Cruce crearCruce(double cx, double cy, Via... accesos) {
        ZonaCruce zona = new ZonaCruce(cx - 26, cy - 26, 52, 52);
        Cruce cruce = new Cruce(zona);
        for (Via via : accesos) {
            cruce.agregarAcceso(via, new Semaforo(5, 20, 2));
        }
        cruce.setIndiceAccesoEnVerde(0);
        cruce.getAccesos().get(0).getSemaforo().iniciarFase(Semaforo.Estado.VERDE, 5);
        return cruce;
    }

    // ---------- puente de demostración (aislado del motor principal) ----------

    private void construirPuenteDemo() {
        double x1 = PUENTE_X1 + 30, x2 = PUENTE_X2 - 30, y = 400;
        Via viaPuente = new Via(Arrays.asList(new Point2D.Double(x1, y), new Point2D.Double(x2, y)), 2, true, 30);
        puenteDemo = new Puente(viaPuente, 15);

        puentesDemo = new ArrayList<>(Arrays.asList(puenteDemo));
        gestorCruces = new GestorCruces(cruces, puentesDemo);
    }

    // ---------- interfaz ----------

    private void construirInterfaz() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 900);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        panelSimulacion = new PanelSimulacion();
        panelSimulacion.setDatos(vias, cruces, puentesDemo, baches);
        add(new JScrollPane(panelSimulacion), BorderLayout.CENTER);

        add(construirPanelControles(), BorderLayout.SOUTH);
    }

    private JPanel construirPanelControles() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JButton botonPlayPausa = new JButton("Pausa");
        botonPlayPausa.addActionListener(e -> {
            enPausa = !enPausa;
            botonPlayPausa.setText(enPausa ? "Reanudar" : "Pausa");
        });
        panel.add(botonPlayPausa);

        panel.add(new JLabel("Velocidad:"));
        JSlider sliderVelocidad = new JSlider(1, 5, 1);
        sliderVelocidad.setMajorTickSpacing(1);
        sliderVelocidad.setPaintTicks(true);
        sliderVelocidad.setPaintLabels(true);
        sliderVelocidad.addChangeListener(e -> multiplicadorVelocidad = sliderVelocidad.getValue());
        panel.add(sliderVelocidad);

        JButton botonPeaton = new JButton("Peatón cruzando (Cruce 1): OFF");
        botonPeaton.addActionListener(e -> {
            ZonaCruce zona = cruces.get(0).getZonaCruce();
            boolean nuevoValor = !zona.hayPeatonCruzando();
            zona.setDemoPeatonCruzando(nuevoValor);
            botonPeaton.setText("Peatón cruzando (Cruce 1): " + (nuevoValor ? "ON" : "OFF"));
        });
        panel.add(botonPeaton);

        return panel;
    }

    // ---------- tráfico automático del puente (reemplaza los botones) ----------

    // vehículos por segundo, por sentido; bien bajo para que se vea espaciado y no amontonado
    private static final double LAMBDA_PUENTE = 0.10;
    private static final int MAX_ESPERANDO_POR_SENTIDO = 2;
    private static final double DISTANCIA_MINIMA_SPAWN = 60;
    private static final double DISTANCIA_LLEGADA = 12;

    private final Random randomPuente = new Random();

    /** Se llama una vez por tick, igual que gestorVehiculos/gestorCruces. */
    private void actualizarPuenteDemo(double deltaTime) {
        intentarSpawnPuente(Puente.Sentido.IDA, 0, deltaTime);
        intentarSpawnPuente(Puente.Sentido.VUELTA, 1, deltaTime);
        avanzarVehiculoCruzando(deltaTime);
    }

    private void intentarSpawnPuente(Puente.Sentido sentido, int indiceCarril, double deltaTime) {
        double probabilidad = 1 - Math.exp(-LAMBDA_PUENTE * deltaTime);
        if (randomPuente.nextDouble() >= probabilidad) return;

        Queue<Vehiculo> cola = (sentido == puenteDemo.getSentidoActual())
                ? puenteDemo.getColaActual() : puenteDemo.getColaOpuesta();
        if (cola.size() >= MAX_ESPERANDO_POR_SENTIDO) return; // no amontonar de más

        Carril carril = puenteDemo.getVia().getCarriles().get(indiceCarril);
        double[] punto = carril.getPuntoInicio();

        for (Vehiculo v : carril.getVehiculos()) {
            if (Math.hypot(v.getX() - punto[0], v.getY() - punto[1]) < DISTANCIA_MINIMA_SPAWN) {
                return; // ya hay uno recién aparecido justo ahí
            }
        }

        Vehiculo nuevo = crearVehiculoAleatorio(punto[0], punto[1], carril);
        carril.agregarVehiculo(nuevo);
        puenteDemo.encolar(nuevo, sentido);
    }

    private Vehiculo crearVehiculoAleatorio(double x, double y, Carril carril) {
        double r = randomPuente.nextDouble();
        if (r < 0.70) return new Auto(x, y, carril);
        if (r < 0.90) return new Bus(x, y, carril);
        return new Moto(x, y, carril);
    }

    /** Mueve al único vehículo que Puente deja cruzar a la vez, y lo despacha al llegar al otro lado. */
    private void avanzarVehiculoCruzando(double deltaTime) {
        Vehiculo cruzando = puenteDemo.getVehiculoCruzando();
        if (cruzando == null) return;

        cruzando.mover(null, null, deltaTime); // reusa la fisica normal del vehiculo (acelera y avanza)

        List<Point2D> trazado = puenteDemo.getVia().getTrazado();
        Point2D destino = cruzando.getCarril().isSentidoIda()
                ? trazado.get(trazado.size() - 1)
                : trazado.get(0);

        if (Math.hypot(cruzando.getX() - destino.getX(), cruzando.getY() - destino.getY()) < DISTANCIA_LLEGADA) {
            cruzando.getCarril().quitarVehiculo(cruzando);
            puenteDemo.terminarCruce(); // libera el puente; GestorCruces ya se encarga de sacar al siguiente de la cola
        }
    }

// ---------- loop de simulación ----------

    private void iniciarLoopSimulacion() {
        timer = new Timer(TICK_MS, e -> {
            if (enPausa) return;
            double deltaTime = (TICK_MS / 1000.0) * multiplicadorVelocidad;

            for (Entrada entrada : entradas) {
                // AQUÍ ESTÁ LA CORRECCIÓN: Usar getVia() y getLambda()
                gestorVehiculos.intentarSpawn(entrada.getVia(), entrada.getLambda(), deltaTime);
            }
            gestorVehiculos.actualizar(deltaTime);
            gestorCruces.actualizar(deltaTime);
            actualizarPuenteDemo(deltaTime);
            // TODO: cuando GestorPeatones exista, también va acá su actualizar(deltaTime).

            panelSimulacion.repaint();
        });
        timer.start();
    }
}