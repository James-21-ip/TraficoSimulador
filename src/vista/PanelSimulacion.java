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

    
}