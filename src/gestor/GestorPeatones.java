package gestor;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import modelo.Cruce;
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
            peatones.add(new Peaton(veredaRandom, limitesVeredas));
        }
    }

    public void actualizar(double deltaTime, List<Vehiculo> vehiculos, List<Cruce> cruces) {
        for (Peaton p : peatones) {
            
            // Asigna un cruce solo si no está en cooldown
            if (p.getEstadoActual() == Peaton.Estado.CAMINANDO_POR_VEREDA && p.puedeCruzar() && random.nextDouble() < 0.05) {
                Rectangle2D pasoCercano = obtenerPasoMasCercano(p.getX(), p.getY());
                if (pasoCercano != null) {
                    p.asignarZonaCruce(pasoCercano);
                }
            }

            boolean hayPeligro = false;
            
            if (p.getEstadoActual() == Peaton.Estado.VERIFICANDO_TRAFICO) {
                Rectangle2D cebra = p.getZonaCruceAsignada();
                
                // 1. INTELIGENCIA DE SEMÁFOROS (CORREGIDA)
                if (cebra != null && cruces != null) {
                    Cruce cruceCercano = obtenerCruceMasCercano(p.getX(), p.getY(), cruces);
                    if (cruceCercano != null) {
                        boolean cebraVertical = cebra.getWidth() < cebra.getHeight();
                        
                        for (Cruce.Acceso acceso : cruceCercano.getAccesos()) {
                            List<java.awt.geom.Point2D> trazado = acceso.getVia().getTrazado();
                            if (trazado.size() >= 2) {
                                double dxVia = Math.abs(trazado.get(1).getX() - trazado.get(0).getX());
                                double dyVia = Math.abs(trazado.get(1).getY() - trazado.get(0).getY());
                                boolean viaHorizontal = dxVia > dyVia;

                                // Si la cebra es vertical, el peatón atraviesa la calle HORIZONTAL
                                if (cebraVertical && viaHorizontal) {
                                    if (acceso.getSemaforo().estaEnVerde() || acceso.getSemaforo().estaEnAmarillo()) {
                                        hayPeligro = true;
                                    }
                                } 
                                // Si la cebra es horizontal, el peatón atraviesa la calle VERTICAL
                                else if (!cebraVertical && !viaHorizontal) {
                                    if (acceso.getSemaforo().estaEnVerde() || acceso.getSemaforo().estaEnAmarillo()) {
                                        hayPeligro = true;
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. RADAR DE EMERGENCIA: Por si un auto no alcanzó a frenar
                if (!hayPeligro) {
                    for (Vehiculo v : vehiculos) {
                        double dist = Math.hypot(v.getX() - p.getX(), v.getY() - p.getY());
                        if (v.getVelocidad() > 5 && dist < 120) {
                            double[] dirAuto = v.getCarril().getDireccion();
                            double dx = p.getX() - v.getX();
                            double dy = p.getY() - v.getY();
                            double proyeccion = (dx * dirAuto[0] + dy * dirAuto[1]);
                            
                            if (proyeccion > 0) {
                                hayPeligro = true;
                                break;
                            }
                        }
                    }
                }
            }
            
            p.actualizar(deltaTime, hayPeligro);
        }
    }

    private Rectangle2D obtenerPasoMasCercano(double px, double py) {
        for (Rectangle2D paso : pasosAleatorios) {
            if (Math.hypot(paso.getCenterX() - px, paso.getCenterY() - py) < 45) return paso;
        }
        return null;
    }

    private Cruce obtenerCruceMasCercano(double px, double py, List<Cruce> cruces) {
        Cruce masCercano = null;
        double minDist = Double.MAX_VALUE;
        for (Cruce c : cruces) {
            double cx = c.getZonaCruce().getArea().getCenterX();
            double cy = c.getZonaCruce().getArea().getCenterY();
            double dist = Math.hypot(cx - px, cy - py);
            if (dist < 150 && dist < minDist) { 
                minDist = dist;
                masCercano = c;
            }
        }
        return masCercano;
    }

    public List<Peaton> getPeatones() { return peatones; }
    public List<Rectangle2D> getPasosAleatorios() { return pasosAleatorios; }
}