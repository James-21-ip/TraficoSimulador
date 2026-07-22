package vista;

import gestor.GestorCruces;
import modelo.Auto;
import modelo.Carril;
import modelo.Cruce;
import modelo.Puente;
import modelo.Semaforo;
import modelo.Via;
import modelo.ZonaCruce;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Ventana principal: contiene el panel de dibujo y los controles de
 * reproducción (play/pausa/velocidad).
 *
 * IMPORTANTE (léeme): mientras GestorVehiculos y GestorPeatones no estén
 * integrados, construirEscenaDePrueba() arma a mano un cruce de 2 accesos
 * y un puente, y los botones de abajo agregan vehículos a las colas para
 * poder demostrar:
 *   1) el semáforo adaptativo: si le das más clicks a "Norte" que a
 *      "Este", vas a ver que el semáforo Norte se queda en verde más
 *      tiempo la próxima vez que le toque.
 *   2) el puente alternando de sentido con su cola visible.
 *   3) el corte de paso cuando hay un peatón cruzando (botón toggle).
 *
 * Cuando el mapa real (ArchivoMapa) y los otros gestores estén listos,
 * se reemplaza construirEscenaDePrueba() por la carga real, y en
 * iniciarLoopSimulacion() se agregan las llamadas a GestorVehiculos y
 * GestorPeatones junto a la de GestorCruces.
 */
public class VentanaPrincipal extends JFrame {

    private static final int TICK_MS = 50; // 20 ticks por segundo

    private PanelSimulacion panelSimulacion;
    private GestorCruces gestorCruces;

    private List<Via> vias;
    private List<Cruce> cruces;
    private List<Puente> puentes;

    private Cruce cruceDemo;
    private Puente puenteDemo;
    private Via viaAccesoNorte;
    private Via viaAccesoEste;
    private ZonaCruce zonaCruceDemo;

    private Timer timer;
    private boolean enPausa = false;
    private double multiplicadorVelocidad = 1.0;

    public VentanaPrincipal() {
        super("Simulador de Tráfico");
        construirEscenaDePrueba();
        construirInterfaz();
        iniciarLoopSimulacion();
    }

    // ---------- escena de prueba (cruce + puente) ----------

    private void construirEscenaDePrueba() {
        // acceso Norte: vía vertical que baja hacia el cruce, ubicado en (400, 260)
        viaAccesoNorte = new Via(
                Arrays.asList(new Point2D.Double(400, 20), new Point2D.Double(400, 260)),
                1, false, 50);

        // acceso Este: vía horizontal que llega al cruce por la derecha
        viaAccesoEste = new Via(
                Arrays.asList(new Point2D.Double(760, 300), new Point2D.Double(440, 300)),
                1, false, 50);

        zonaCruceDemo = new ZonaCruce(380, 280, 60, 40);
        cruceDemo = new Cruce(zonaCruceDemo);

        Semaforo semaforoNorte = new Semaforo(5, 20, 2);
        Semaforo semaforoEste = new Semaforo(5, 20, 2);
        cruceDemo.agregarAcceso(viaAccesoNorte, semaforoNorte);
        cruceDemo.agregarAcceso(viaAccesoEste, semaforoEste);

        // arranca con el acceso Norte en verde
        cruceDemo.setIndiceAccesoEnVerde(0);
        semaforoNorte.iniciarFase(Semaforo.Estado.VERDE, semaforoNorte.getTiempoMinimoVerde());

        Via viaPuente = new Via(
                Arrays.asList(new Point2D.Double(150, 400), new Point2D.Double(650, 400)),
                1, true, 30);
        puenteDemo = new Puente(viaPuente, 15);

        vias = new ArrayList<>(Arrays.asList(viaAccesoNorte, viaAccesoEste, viaPuente));
        cruces = new ArrayList<>(Arrays.asList(cruceDemo));
        puentes = new ArrayList<>(Arrays.asList(puenteDemo));

        gestorCruces = new GestorCruces(cruces, puentes);
    }

    // ---------- interfaz ----------

    private void construirInterfaz() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        panelSimulacion = new PanelSimulacion();
        panelSimulacion.setDatos(vias, cruces, puentes);
        add(panelSimulacion, BorderLayout.CENTER);

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

        JButton botonVehiculoNorte = new JButton("+ Vehículo Norte");
        botonVehiculoNorte.addActionListener(e -> agregarVehiculoACola(viaAccesoNorte, 0));
        panel.add(botonVehiculoNorte);

        JButton botonVehiculoEste = new JButton("+ Vehículo Este");
        botonVehiculoEste.addActionListener(e -> agregarVehiculoACola(viaAccesoEste, 1));
        panel.add(botonVehiculoEste);

        JButton botonPuenteIda = new JButton("+ Vehículo Puente (ida)");
        botonPuenteIda.addActionListener(e -> agregarVehiculoAPuente(Puente.Sentido.IDA));
        panel.add(botonPuenteIda);

        JButton botonPuenteVuelta = new JButton("+ Vehículo Puente (vuelta)");
        botonPuenteVuelta.addActionListener(e -> agregarVehiculoAPuente(Puente.Sentido.VUELTA));
        panel.add(botonPuenteVuelta);

        JButton botonPeaton = new JButton("Peatón cruzando: OFF");
        botonPeaton.addActionListener(e -> {
            boolean nuevoValor = !zonaCruceDemo.hayPeatonCruzando();
            zonaCruceDemo.setDemoPeatonCruzando(nuevoValor);
            botonPeaton.setText("Peatón cruzando: " + (nuevoValor ? "ON" : "OFF"));
        });
        panel.add(botonPeaton);

        return panel;
    }

    // ---------- helpers para los botones de demo ----------

    private void agregarVehiculoACola(Via via, int indiceAcceso) {
        Carril carril = via.getCarriles().get(0);
        Point2D inicio = via.getTrazado().get(0);

        // los vehículos en cola se dibujan apilados un poco antes del inicio de la vía
        int posicionEnCola = cruceDemo.cantidadEsperando(indiceAcceso);
        double x = inicio.getX();
        double y = inicio.getY() - posicionEnCola * 25;

        Auto auto = new Auto(x, y, carril);
        carril.agregarVehiculo(auto);
        cruceDemo.encolarVehiculo(indiceAcceso, auto);
        panelSimulacion.repaint();
    }

    private void agregarVehiculoAPuente(Puente.Sentido sentido) {
        Via viaPuente = puenteDemo.getVia();
        Carril carril = viaPuente.getCarriles().get(0);
        Point2D inicio = viaPuente.getTrazado().get(0);

        int posicionEnCola = puenteDemo.getColaActual().size() + puenteDemo.getColaOpuesta().size();
        double x = inicio.getX() - 30 - posicionEnCola * 20;
        double y = inicio.getY();

        Auto auto = new Auto(x, y, carril);
        carril.agregarVehiculo(auto);
        puenteDemo.encolar(auto, sentido);
        panelSimulacion.repaint();
    }

    // ---------- loop de simulación ----------

    private void iniciarLoopSimulacion() {
        timer = new Timer(TICK_MS, e -> {
            if (enPausa) {
                return;
            }
            double deltaTime = (TICK_MS / 1000.0) * multiplicadorVelocidad;

            gestorCruces.actualizar(deltaTime);
            // TODO: cuando existan, acá también van:
            //   gestorVehiculos.actualizar(deltaTime);
            //   gestorPeatones.actualizar(deltaTime);

            panelSimulacion.repaint();
        });
        timer.start();
    }
}