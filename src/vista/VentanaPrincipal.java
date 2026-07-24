package vista;

import gestor.GestorCruces;
import gestor.GestorPeatones;
import gestor.GestorVehiculos;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.Timer;
import modelo.Auto;
import modelo.Bache;
import modelo.Bus;
import modelo.Carril;
import modelo.Cruce;
import modelo.Moto;
import modelo.Puente;
import modelo.Semaforo;
import modelo.Vehiculo;
import modelo.Via;
import modelo.ZonaCruce;

public class VentanaPrincipal extends JFrame {
    private GestorPeatones gestorPeatones;
    private final VentanaEstadisticas estadisticas = new VentanaEstadisticas();

    private static final int TICK_MS = 50; 

    // ---------- LAYOUT ----------
    private static final int MAPA_ANCHO = 1180;
    private static final int MAPA_ALTO = 800;
    private static final int X_CRUCE_1 = 340;
    private static final int X_CRUCE_2 = 760;
    private static final int Y_AVENIDA = 400;
    private static final int PUENTE_X1 = 1180;
    private static final int PUENTE_X2 = 1400;
    // borde real del canvas ampliado (puente + barrio del otro lado), ya casi fuera de vista
    private static final int BORDE_LEJANO = PUENTE_X2 + 190;

    private PanelSimulacion panelSimulacion;
    private GestorVehiculos gestorVehiculos;
    private GestorCruces gestorCruces;

    private List<Via> vias;
    private List<Cruce> cruces;
    private List<Puente> puentesDemo;
    private List<Bache> baches;

    private final List<Entrada> entradas = new ArrayList<>();

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

    // AÑADIDO: 'implements Serializable' para permitir guardar la configuración de spawns
    private static class Entrada implements Serializable {
        final Via via;
        final double lambda;
        Entrada(Via via, double lambda) { this.via = via; this.lambda = lambda; }
    }

    private static final double OFFSET_RAPIDO = 12;
    private static final double OFFSET_LENTO = 30;

    private void construirMapa() {
        int mitadCebra = 42; 

        Via avE1 = crearViaHorizontal(40, Y_AVENIDA, X_CRUCE_1 - mitadCebra, Y_AVENIDA);
        Via avE2 = crearViaHorizontal(X_CRUCE_1 + mitadCebra, Y_AVENIDA, X_CRUCE_2 - mitadCebra, Y_AVENIDA);
        Via avE3 = crearViaHorizontal(X_CRUCE_2 + mitadCebra, Y_AVENIDA, BORDE_LEJANO, Y_AVENIDA);
        avE1.agregarConexion(avE2);
        avE2.agregarConexion(avE3);

        Via avW1 = crearViaHorizontal(BORDE_LEJANO, Y_AVENIDA, X_CRUCE_2 + mitadCebra, Y_AVENIDA);
        Via avW2 = crearViaHorizontal(X_CRUCE_2 - mitadCebra, Y_AVENIDA, X_CRUCE_1 + mitadCebra, Y_AVENIDA);
        Via avW3 = crearViaHorizontal(X_CRUCE_1 - mitadCebra, Y_AVENIDA, 40, Y_AVENIDA);
        avW1.agregarConexion(avW2);
        avW2.agregarConexion(avW3);

        Via c1S1 = crearViaVertical(X_CRUCE_1, 40, X_CRUCE_1, Y_AVENIDA - mitadCebra);
        Via c1S2 = crearViaVertical(X_CRUCE_1, Y_AVENIDA + mitadCebra, X_CRUCE_1, MAPA_ALTO - 20);
        c1S1.agregarConexion(c1S2);
        Via c1N1 = crearViaVertical(X_CRUCE_1, MAPA_ALTO - 20, X_CRUCE_1, Y_AVENIDA + mitadCebra);
        Via c1N2 = crearViaVertical(X_CRUCE_1, Y_AVENIDA - mitadCebra, X_CRUCE_1, 40);
        c1N1.agregarConexion(c1N2);

        Via c2S1 = crearViaVertical(X_CRUCE_2, 40, X_CRUCE_2, Y_AVENIDA - mitadCebra);
        Via c2S2 = crearViaVertical(X_CRUCE_2, Y_AVENIDA + mitadCebra, X_CRUCE_2, MAPA_ALTO - 20);
        c2S1.agregarConexion(c2S2);
        Via c2N1 = crearViaVertical(X_CRUCE_2, MAPA_ALTO - 20, X_CRUCE_2, Y_AVENIDA + mitadCebra);
        Via c2N2 = crearViaVertical(X_CRUCE_2, Y_AVENIDA - mitadCebra, X_CRUCE_2, 40);
        c2N1.agregarConexion(c2N2);

        Cruce cruce1 = crearCruce(X_CRUCE_1, Y_AVENIDA, mitadCebra, avE1, avW2, c1S1, c1N1);
        Cruce cruce2 = crearCruce(X_CRUCE_2, Y_AVENIDA, mitadCebra, avE2, avW1, c2S1, c2N1);
        
        avE1.agregarConexion(c1S2);
        avE1.agregarConexion(c1N2);
        avW2.agregarConexion(c1N2);
        avW2.agregarConexion(c1S2);
        c1S1.agregarConexion(avW3);
        c1S1.agregarConexion(avE2);
        c1N1.agregarConexion(avE2);
        c1N1.agregarConexion(avW3);

        avE2.agregarConexion(c2S2);
        avE2.agregarConexion(c2N2);
        avW1.agregarConexion(c2N2);
        avW1.agregarConexion(c2S2);
        c2S1.agregarConexion(avW2);
        c2S1.agregarConexion(avE3);
        c2N1.agregarConexion(avE3);
        c2N1.agregarConexion(avW2);
        
        vias = new ArrayList<>(Arrays.asList(
                avE1, avE2, avE3, avW1, avW2, avW3,
                c1S1, c1S2, c1N1, c1N2, c2S1, c2S2, c2N1, c2N2));
        cruces = new ArrayList<>(Arrays.asList(cruce1, cruce2));

        baches = new ArrayList<>(Arrays.asList(
                new Bache((X_CRUCE_1 + X_CRUCE_2) / 2.0, Y_AVENIDA - OFFSET_RAPIDO, 0.5),
                new Bache((X_CRUCE_1 + X_CRUCE_2) / 2.0, Y_AVENIDA + OFFSET_RAPIDO, 0.6),
                new Bache(X_CRUCE_1 - OFFSET_RAPIDO, 220, 0.4)));

        entradas.add(new Entrada(avE1, 0.12)); 
        entradas.add(new Entrada(avW1, 0.12)); 
        entradas.add(new Entrada(c1S1, 0.06)); 
        entradas.add(new Entrada(c1N1, 0.06)); 
        entradas.add(new Entrada(c2S1, 0.06)); 
        entradas.add(new Entrada(c2N1, 0.06)); 
        
        gestorVehiculos = new GestorVehiculos();
        gestorVehiculos.setCruces(cruces);
        gestorVehiculos.setBaches(baches);
        int calzada = 42;
        int anchoVereda = 12;
        List<Rectangle2D> veredas = Arrays.asList(
            // Líneas horizontales de la Avenida (Norte y Sur)
            new Rectangle2D.Double(40, Y_AVENIDA - calzada - anchoVereda, MAPA_ANCHO - 80, anchoVereda),
            new Rectangle2D.Double(40, Y_AVENIDA + calzada, MAPA_ANCHO - 80, anchoVereda),
            
            // Líneas verticales de la Calle 1 (Izquierda y Derecha)
            new Rectangle2D.Double(X_CRUCE_1 - calzada - anchoVereda, 40, anchoVereda, MAPA_ALTO - 80),
            new Rectangle2D.Double(X_CRUCE_1 + calzada, 40, anchoVereda, MAPA_ALTO - 80),
            
            // Líneas verticales de la Calle 2 (Izquierda y Derecha)
            new Rectangle2D.Double(X_CRUCE_2 - calzada - anchoVereda, 40, anchoVereda, MAPA_ALTO - 80),
            new Rectangle2D.Double(X_CRUCE_2 + calzada, 40, anchoVereda, MAPA_ALTO - 80)
        );

        int grosorCebra = 24; // Ancho del paso peatonal
        int mC = 42; // Mitad del tamaño del cruce
        
        // 4 cebras rodeando el Cruce 1 y 4 rodeando el Cruce 2
        List<Rectangle2D> pasosEsquinas = Arrays.asList(
            // Cruce 1 (Izquierda)
            new Rectangle2D.Double(X_CRUCE_1 - mC - grosorCebra, Y_AVENIDA - mC, grosorCebra, mC * 2), // Oeste
            new Rectangle2D.Double(X_CRUCE_1 + mC, Y_AVENIDA - mC, grosorCebra, mC * 2),               // Este
            new Rectangle2D.Double(X_CRUCE_1 - mC, Y_AVENIDA - mC - grosorCebra, mC * 2, grosorCebra), // Norte
            new Rectangle2D.Double(X_CRUCE_1 - mC, Y_AVENIDA + mC, mC * 2, grosorCebra),               // Sur
            
            // Cruce 2 (Derecha)
            new Rectangle2D.Double(X_CRUCE_2 - mC - grosorCebra, Y_AVENIDA - mC, grosorCebra, mC * 2), // Oeste
            new Rectangle2D.Double(X_CRUCE_2 + mC, Y_AVENIDA - mC, grosorCebra, mC * 2),               // Este
            new Rectangle2D.Double(X_CRUCE_2 - mC, Y_AVENIDA - mC - grosorCebra, mC * 2, grosorCebra), // Norte
            new Rectangle2D.Double(X_CRUCE_2 - mC, Y_AVENIDA + mC, mC * 2, grosorCebra)                // Sur
        );

        // Reducimos la cantidad de peatones por defecto para menos congestión
        this.gestorPeatones = new GestorPeatones(veredas, pasosEsquinas, 10);
    }

    private Via crearVia(double x1, double y1, double x2, double y2) {
        List<Point2D> trazado = Arrays.asList(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
        return new Via(trazado, 2, false, 50);
    }

    private Via crearViaHorizontal(double x1, double y1, double x2, double y2) {
        Via via = crearVia(x1, y1, x2, y2);
        via.getCarriles().get(0).setOffsetY(-OFFSET_RAPIDO);
        via.getCarriles().get(1).setOffsetY(-OFFSET_LENTO);
        return via;
    }

    private Via crearViaVertical(double x1, double y1, double x2, double y2) {
        Via via = crearVia(x1, y1, x2, y2);
        via.getCarriles().get(0).setOffsetY(OFFSET_RAPIDO);
        via.getCarriles().get(1).setOffsetY(OFFSET_LENTO);
        return via;
    }

    private Cruce crearCruce(double cx, double cy, double mitadCebra, Via... accesos) {
        ZonaCruce zona = new ZonaCruce(cx - mitadCebra, cy - mitadCebra, mitadCebra * 2, mitadCebra * 2);
        Cruce cruce = new Cruce(zona);
        for (Via via : accesos) {
            cruce.agregarAcceso(via, new Semaforo(5, 20, 2));
        }
        cruce.setIndiceAccesoEnVerde(0);
        cruce.getAccesos().get(0).getSemaforo().iniciarFase(Semaforo.Estado.VERDE, 5);
        return cruce;
    }

    private void construirPuenteDemo() {
        // arranca justo donde termina avE3/avW1 (MAPA_ANCHO - 20) para que no quede
        // un hueco entre la pista de la ciudad y el tablero del puente.
        double x1 = MAPA_ANCHO - 20, x2 = PUENTE_X2 - 30, y = 400;
        Via viaPuente = new Via(Arrays.asList(new Point2D.Double(x1, y), new Point2D.Double(x2, y)), 4, true, 30);

        // 4 carriles, mismo esquema rápido/lento que las calles: carriles 0-1 = IDA
        // (rápido pegado al centro, lento pegado a la baranda), carriles 2-3 = VUELTA,
        // reflejados del otro lado de la línea central.
        viaPuente.getCarriles().get(0).setOffsetY(-OFFSET_RAPIDO);
        viaPuente.getCarriles().get(1).setOffsetY(-OFFSET_LENTO);
        viaPuente.getCarriles().get(2).setOffsetY(OFFSET_RAPIDO);
        viaPuente.getCarriles().get(3).setOffsetY(OFFSET_LENTO);

        puenteDemo = new Puente(viaPuente, 15);

        puentesDemo = new ArrayList<>(Arrays.asList(puenteDemo));
        gestorCruces = new GestorCruces(cruces, puentesDemo);
    }

    private void construirInterfaz() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 900);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        panelSimulacion = new PanelSimulacion();
        panelSimulacion.setDatos(vias, cruces, puentesDemo, baches);
        panelSimulacion.setGestorPeatones(this.gestorPeatones);
        panelSimulacion.setEstadisticas(estadisticas, gestorVehiculos);

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

        JButton botonGuardar = new JButton("Guardar");
        botonGuardar.addActionListener(e -> guardarEstado());
        panel.add(botonGuardar);

        JButton botonCargar = new JButton("Cargar");
        botonCargar.addActionListener(e -> cargarEstado());
        panel.add(botonCargar);

        // AÑADIMOS EL BOTÓN DE REPORTE AQUÍ
        JButton botonReporte = new JButton("Generar Reporte");
        botonReporte.addActionListener(e -> exportarReporte());
        panel.add(botonReporte);

        return panel;
    }
    // ---------- LÓGICA DE REPORTE ----------
    private void exportarReporte() {
        boolean estadoPausaPrevio = enPausa;
        enPausa = true; // Congelamos la simulación para que los datos no cambien mientras se escribe

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Exportar Reporte (TXT)");

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            
            // Forzar que tenga extensión .txt
            if (!archivo.getName().toLowerCase().endsWith(".txt")) {
                archivo = new File(archivo.getParentFile(), archivo.getName() + ".txt");
            }
            
            try {
                // Llamamos a la clase ArchivoReporte que creamos en el paso 1
                fichero.ArchivoReporte.generarReporteTxt(archivo, gestorVehiculos, estadisticas.getTiempoSimulacion());
                JOptionPane.showMessageDialog(this, "Reporte exportado exitosamente.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al generar el reporte: " + ex.getMessage(), "Error de I/O", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
        enPausa = estadoPausaPrevio;
    }

    // ---------- LÓGICA DE PERSISTENCIA (GUARDAR Y CARGAR) ----------
    private void guardarEstado() {
        boolean estadoPausaPrevio = enPausa;
        enPausa = true; // Congelamos hilos para evitar mutación durante la exportación

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Estado de Simulación");

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(archivo))) {
                
                oos.writeObject(vias);
                oos.writeObject(cruces);
                oos.writeObject(puentesDemo);
                oos.writeObject(baches);
                oos.writeObject(entradas);
                oos.writeObject(gestorVehiculos);
                oos.writeObject(gestorCruces);
                oos.writeObject(gestorPeatones);
                
                JOptionPane.showMessageDialog(this, "Simulación guardada exitosamente.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error crítico al guardar: " + ex.getMessage(), "Error de I/O", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
        enPausa = estadoPausaPrevio;
    }

    @SuppressWarnings("unchecked")
    private void cargarEstado() {
        boolean estadoPausaPrevio = enPausa;
        enPausa = true;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Cargar Estado de Simulación");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(archivo))) {
                
                this.vias = (List<Via>) ois.readObject();
                this.cruces = (List<Cruce>) ois.readObject();
                this.puentesDemo = (List<Puente>) ois.readObject();
                this.baches = (List<Bache>) ois.readObject();
                
                List<Entrada> entradasCargadas = (List<Entrada>) ois.readObject();
                this.entradas.clear();
                this.entradas.addAll(entradasCargadas);
                
                this.gestorVehiculos = (GestorVehiculos) ois.readObject();
                this.gestorCruces = (GestorCruces) ois.readObject();
                this.gestorPeatones = (GestorPeatones) ois.readObject();

                if (this.puentesDemo != null && !this.puentesDemo.isEmpty()) {
                    this.puenteDemo = this.puentesDemo.get(0);
                }

                // los vehiculos del puente viajan sueltos en 'vehiculosPuente' (no se serializa
                // directamente); tras cargar, se reconstruye a partir de lo que quedó en los
                // carriles del puente (eso sí viaja dentro de puentesDemo -> Via -> Carril).
                this.vehiculosPuente.clear();
                if (this.puenteDemo != null) {
                    for (Carril carril : this.puenteDemo.getVia().getCarriles()) {
                        this.vehiculosPuente.addAll(carril.getVehiculos());
                    }
                }

                panelSimulacion.setDatos(this.vias, this.cruces, this.puentesDemo, this.baches);
                panelSimulacion.setGestorPeatones(this.gestorPeatones);
                panelSimulacion.setEstadisticas(estadisticas, this.gestorVehiculos);
                panelSimulacion.repaint();

                JOptionPane.showMessageDialog(this, "Simulación cargada exitosamente.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error crítico al cargar: " + ex.getMessage(), "Error de I/O", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
        enPausa = estadoPausaPrevio;
    }

    // ---------- tráfico del puente (dos carriles independientes, sin alternancia) ----------
    //
    // NOTA para el informe: Puente y GestorCruces siguen intactos y funcionando (cola FIFO
    // + alternancia de sentido), es el algoritmo que pide el enunciado. Ya NO se usa para
    // mover autos (ver iniciarLoopSimulacion): ahora el tráfico visible del puente es el
    // mismo de avE3/avW1, que ya llega/sale por los bordes reales del mapa. Estos métodos
    // quedan sin llamarse, pero se dejan tal cual para no romper guardarEstado/cargarEstado.

    private static final double LAMBDA_PUENTE = 0.10;
    private static final double DISTANCIA_MINIMA_SPAWN = 60;
    // qué tan lejos, más allá del tablero del puente, sigue el auto antes de desaparecer
    // (para que no "desaparezca justo ahí": entra a la zona del otro barrio / vuelve a la
    // ciudad y recién ahí se pierde de vista).
    private static final double DISTANCIA_EXTRA_TRAS_CRUZAR = 350;

    private final Random randomPuente = new Random();
    private final List<Vehiculo> vehiculosPuente = new ArrayList<>();

    private void actualizarPuenteDemo(double deltaTime) {
        for (Carril carril : puenteDemo.getVia().getCarriles()) {
            intentarSpawnCarrilPuente(carril, deltaTime);
        }
        moverVehiculosPuente(deltaTime);
    }

    private void intentarSpawnCarrilPuente(Carril carril, double deltaTime) {
        double probabilidad = 1 - Math.exp(-LAMBDA_PUENTE * deltaTime);
        if (randomPuente.nextDouble() >= probabilidad) return;

        double[] punto = carril.getPuntoInicio();
        for (Vehiculo v : carril.getVehiculos()) {
            if (Math.hypot(v.getX() - punto[0], v.getY() - punto[1]) < DISTANCIA_MINIMA_SPAWN) {
                return; // ya hay uno recién aparecido justo ahí
            }
        }

        Vehiculo nuevo = crearVehiculoAleatorio(punto[0], punto[1], carril);
        carril.agregarVehiculo(nuevo);
        vehiculosPuente.add(nuevo);
    }

    private Vehiculo crearVehiculoAleatorio(double x, double y, Carril carril) {
        double r = randomPuente.nextDouble();
        if (r < 0.55) return new Auto(x, y, carril);
        if (r < 0.70) return new Bus(x, y, carril);
        return new Moto(x, y, carril);
    }

    /** Mueve cada auto del puente con seguimiento normal de distancia, y lo despacha
     *  recién cuando ya avanzó bastante más allá del tablero (fuera de la vista). */
    private void moverVehiculosPuente(double deltaTime) {
        double largoPuente = puenteDemo.getVia().getTrazado().get(0)
                .distance(puenteDemo.getVia().getTrazado().get(1));
        double limite = largoPuente + DISTANCIA_EXTRA_TRAS_CRUZAR;

        Iterator<Vehiculo> it = vehiculosPuente.iterator();
        while (it.hasNext()) {
            Vehiculo v = it.next();
            Carril carril = v.getCarril();
            Vehiculo adelante = carril.getVehiculoAdelante(v);
            v.mover(adelante, null, null, deltaTime);

            double[] inicio = carril.getPuntoInicio();
            double[] dir = carril.getDireccion();
            double avance = (v.getX() - inicio[0]) * dir[0] + (v.getY() - inicio[1]) * dir[1];

            if (avance >= limite) {
                carril.quitarVehiculo(v);
                it.remove();
            }
        }
    }

    private void iniciarLoopSimulacion() {
        timer = new Timer(TICK_MS, e -> {
            if (enPausa) return;
            double deltaTime = (TICK_MS / 1000.0) * multiplicadorVelocidad;

            estadisticas.agregarTiempo(deltaTime);

            for (Entrada entrada : entradas) {
                gestorVehiculos.intentarSpawn(entrada.via, entrada.lambda, deltaTime);
            }
            gestorVehiculos.actualizar(deltaTime);
            gestorCruces.actualizar(deltaTime);
            
            // CAMBIA ESTO EN TU BUCLE DE SIMULACIÓN:
            if (gestorPeatones != null) {
                // Ahora le pasamos también la lista global 'cruces'
                gestorPeatones.actualizar(deltaTime, gestorVehiculos.getVehiculos(), cruces);
            }

            panelSimulacion.repaint();
        });
        timer.start();
    }
}