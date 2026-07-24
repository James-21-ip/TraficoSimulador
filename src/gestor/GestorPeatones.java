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
            peatones.add(new Peaton(veredaRandom));
        }
    }

    public void actualizar(double deltaTime, List<Vehiculo> vehiculos) {
        for (Peaton p : peatones) {
            // Asignar paso peatonal si está cerca de uno y no tiene uno asignado
            if (p.getEstadoActual() == Peaton.Estado.CAMINANDO_POR_VEREDA && random.nextDouble() < 0.01) {
                Rectangle2D pasoCercano = obtenerPasoMasCercano(p.getX(), p.getY());
                if (pasoCercano != null) p.asignarZonaCruce(pasoCercano);
            }

            // Detección de peligro (Si hay un auto a menos de 100px)
            boolean hayPeligro = false;
            if (p.getEstadoActual() == Peaton.Estado.VERIFICANDO_TRAFICO) {
                for (Vehiculo v : vehiculos) {
                    if (Math.hypot(v.getX() - p.getX(), v.getY() - p.getY()) < 100) {
                        hayPeligro = true;
                        break;
                    }
                }
            }
            p.actualizar(deltaTime, hayPeligro);
        }
    }

    private Rectangle2D obtenerPasoMasCercano(double px, double py) {
            for (Rectangle2D paso : pasosAleatorios) {
                // Evalúa la distancia exacta en 360 grados usando Pitágoras
                if (Math.hypot(paso.getCenterX() - px, paso.getCenterY() - py) < 100) return paso;
            }
            return null;
        }

    public List<Peaton> getPeatones() { return peatones; }
    public List<Rectangle2D> getPasosAleatorios() { return pasosAleatorios; }
}