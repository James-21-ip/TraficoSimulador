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


}