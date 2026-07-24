package gestor;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import modelo.Peaton;
import modelo.Vehiculo;

public class GestorPeatones implements Serializable {

    private List<Peaton> peatones;
    private List<Rectangle2D> pasosAleatorios;
    private Random random = new Random();

    public GestorPeatones(List<Rectangle2D> limitesVeredas, List<Rectangle2D> pasosAleatorios, int cantidad) {
        this.peatones = new ArrayList<>();
        this.pasosAleatorios = pasosAleatorios;
        
        for (int i = 0; i < cantidad; i++) {
            Rectangle2D veredaRandom = limitesVeredas.get(random.nextInt(limitesVeredas.size()));
            // Ahora le pasamos la lista entera 'limitesVeredas' como segundo parámetro
            peatones.add(new Peaton(veredaRandom, limitesVeredas)); 
        }
    }
    public void actualizar(double deltaTime, List<Vehiculo> vehiculos) {
        for (Peaton p : peatones) {
            if (p.getEstadoActual() == Peaton.Estado.CAMINANDO_POR_VEREDA && random.nextDouble() < 0.05) {
                Rectangle2D pasoCercano = obtenerPasoMasCercano(p.getX(), p.getY());
                if (pasoCercano != null) p.asignarZonaCruce(pasoCercano);
            }

            boolean hayPeligro = false;
            if (p.getEstadoActual() == Peaton.Estado.VERIFICANDO_TRAFICO) {
                for (Vehiculo v : vehiculos) {
                    // INTELIGENCIA: Solo es peligro si el auto ESTÁ EN MOVIMIENTO (> 5 vel)
                    if (v.getVelocidad() > 5 && Math.hypot(v.getX() - p.getX(), v.getY() - p.getY()) < 120) {
                        hayPeligro = true;
                        break;
                    }
                }
            }
            
            // FÍSICA DE IMPACTO: Si lo choca un auto, el peatón se detiene en seco en lugar de seguir caminando
            boolean impactado = false;
            if (p.getEstadoActual() == Peaton.Estado.CRUZANDO_CALLE) {
                for (Vehiculo v : vehiculos) {
                    if (Math.hypot(v.getX() - p.getX(), v.getY() - p.getY()) < 18) {
                        impactado = true;
                        break;
                    }
                }
            }

            // Si está impactado, se congela este frame. Si no, camina normal.
            if (!impactado) {
                p.actualizar(deltaTime, hayPeligro);
            }
        }
    }

    private Rectangle2D obtenerPasoMasCercano(double px, double py) {
        for (Rectangle2D paso : pasosAleatorios) {
            // Reducimos el radar a 30px. Así nunca detectará la cebra del frente de la avenida.
            if (Math.hypot(paso.getCenterX() - px, paso.getCenterY() - py) < 30) return paso;
        }
        return null;
    }

    public List<Peaton> getPeatones() { return peatones; }
    public List<Rectangle2D> getPasosAleatorios() { return pasosAleatorios; }
}