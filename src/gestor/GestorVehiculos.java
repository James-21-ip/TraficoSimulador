// GestorVehiculos.java
package gestor;

import modelo.*;
import java.awt.geom.Point2D;
import java.util.*;

public class GestorVehiculos {

    private List<Vehiculo> vehiculos;
    private List<Bache> baches;
    private List<Cruce> cruces;
    private List<Puente> puentes;
    private Random random;
    private Set<String> bachesYaAplicados = new HashSet<>();
    private static final double DISTANCIA_DETECCION_BACHE = 60;
    private static final double DISTANCIA_LINEA_PARADA = 15;

    public GestorVehiculos() {
        vehiculos = new ArrayList<>();
        baches = new ArrayList<>();
        cruces = new ArrayList<>();
        puentes = new ArrayList<>();
        random = new Random();
    }

    public void setBaches(List<Bache> baches) {
        this.baches = baches;
    }

    /** Cruces cuyos semáforos deben poder detener a estos vehículos. */
    public void setCruces(List<Cruce> cruces) {
        this.cruces = cruces;
    }

    /** Puentes que estos vehículos deben respetar (esperar si están ocupados). */
    public void setPuentes(List<Puente> puentes) {
        this.puentes = puentes;
    }

    public void intentarSpawn(Via viaEntrada, double lambda, double deltaTime) {
        for (Carril carril : viaEntrada.getCarriles()) {
            double probabilidad = 1 - Math.exp(-lambda * deltaTime);
            if (random.nextDouble() < probabilidad) {
                double xInicial = viaEntrada.getTrazado().get(0).getX();
                double yInicial = carril.getY();

                if (!hayEspacioParaSpawnear(carril, xInicial)) {
                    continue;
                }

                Vehiculo nuevo = crearVehiculoAleatorio(xInicial, yInicial, carril);
                carril.agregarVehiculo(nuevo);
                vehiculos.add(nuevo);
            }
        }
    }

    private boolean hayEspacioParaSpawnear(Carril carril, double xInicial) {
        double distanciaMinima = 80;
        for (Vehiculo v : carril.getVehiculos()) {
            if (Math.abs(v.getX() - xInicial) < distanciaMinima) {
                return false;
            }
        }
        return true;
    }

    private Vehiculo crearVehiculoAleatorio(double x, double y, Carril carril) {
        double r = random.nextDouble();
        if (r < 0.65) {
            return new Auto(x, y, carril);
        } else if (r < 0.85) {
            return new Bus(x, y, carril);
        } else {
            return new Moto(x, y, carril);
        }
    }

    public void actualizar(double deltaTime) {
        for (Vehiculo v : vehiculos) {
            Vehiculo adelante = v.getCarril().getVehiculoAdelante(v);
            Double distanciaParada = calcularDistanciaParada(v);
            v.mover(adelante, distanciaParada, deltaTime);
        }
        aplicarEvasionBaches();
        aplicarCambiosPorCongestion();
        procesarBaches();
        detectarColisiones();
        sincronizarColasDeCruces();
    }

    // ---------- integración con semáforos y puentes ----------

    /** Devuelve la distancia hasta el punto donde este vehículo debe frenar, o null si puede seguir. */
    private Double calcularDistanciaParada(Vehiculo v) {
        Via via = v.getCarril().getVia();

        Cruce.Acceso acceso = buscarAcceso(via);
        if (acceso != null && !acceso.getSemaforo().estaEnVerde()) {
            return distanciaHastaFinDeVia(v, via);
        }

        for (Puente puente : puentes) {
            if (puente.getVia() == via && !puente.estaLibre()) {
                return distanciaHastaFinDeVia(v, via);
            }
        }

        return null;
    }

    private Cruce.Acceso buscarAcceso(Via via) {
        for (Cruce cruce : cruces) {
            for (Cruce.Acceso acceso : cruce.getAccesos()) {
                if (acceso.getVia() == via) {
                    return acceso;
                }
            }
        }
        return null;
    }

    private double distanciaHastaFinDeVia(Vehiculo v, Via via) {
        Point2D fin = via.getTrazado().get(via.getTrazado().size() - 1);
        return Math.hypot(fin.getX() - v.getX(), fin.getY() - v.getY());
    }

    /** Mantiene la cola de espera de cada acceso (para el semáforo adaptativo) al día con los vehículos reales. */
    private void sincronizarColasDeCruces() {
        for (Cruce cruce : cruces) {
            List<Cruce.Acceso> accesos = cruce.getAccesos();
            for (int i = 0; i < accesos.size(); i++) {
                Cruce.Acceso acceso = accesos.get(i);
                Via via = acceso.getVia();

                for (Vehiculo v : via.getCarriles().isEmpty() ? Collections.<Vehiculo>emptyList()
                        : via.getCarriles().get(0).getVehiculos()) {
                    boolean llegoALaLinea = distanciaHastaFinDeVia(v, via) < DISTANCIA_LINEA_PARADA;
                    boolean yaEnCola = acceso.getColaEspera().contains(v);

                    if (llegoALaLinea && !acceso.getSemaforo().estaEnVerde() && !yaEnCola) {
                        cruce.encolarVehiculo(i, v);
                    } else if (yaEnCola && (acceso.getSemaforo().estaEnVerde() || !llegoALaLinea)) {
                        cruce.desencolarVehiculo(i, v);
                    }
                }
            }
        }
    }

    // ---------- el resto, igual que ya lo tenías ----------

    private Carril carrilVecino(Carril actual) {
        List<Carril> carriles = actual.getVia().getCarriles();
        int idx = carriles.indexOf(actual);
        if (idx + 1 < carriles.size()) return carriles.get(idx + 1);
        if (idx - 1 >= 0) return carriles.get(idx - 1);
        return null;
    }

    private void aplicarCambiosPorCongestion() {
        for (Vehiculo v : vehiculos) {
            Vehiculo adelante = v.getCarril().getVehiculoAdelante(v);
            v.intentarCambiarCarrilPorCongestion(adelante, carrilVecino(v.getCarril()));
        }
    }

    private void aplicarEvasionBaches() {
        for (Vehiculo v : vehiculos) {
            for (Bache b : baches) {
                v.intentarEvadirBache(b, carrilVecino(v.getCarril()), DISTANCIA_DETECCION_BACHE);
            }
        }
    }

    private void procesarBaches() {
        for (Vehiculo v : vehiculos) {
            if (v.isAveriado()) continue;
            for (Bache b : baches) {
                String clave = System.identityHashCode(v) + "-" + System.identityHashCode(b);
                if (bachesYaAplicados.contains(clave)) continue;

                double distancia = Math.hypot(v.getX() - b.getX(), v.getY() - b.getY());
                if (distancia < 10) {
                    bachesYaAplicados.add(clave);
                    v.reducirVelocidadPorBache(b.getSeveridad());
                }
            }
        }
    }

    private void detectarColisiones() {
        for (int i = 0; i < vehiculos.size(); i++) {
            for (int j = i + 1; j < vehiculos.size(); j++) {
                Vehiculo a = vehiculos.get(i);
                Vehiculo b = vehiculos.get(j);
                if (a.getCarril() != b.getCarril()) continue;

                java.awt.geom.Area areaA = new java.awt.geom.Area(a.getHitbox());
                areaA.intersect(new java.awt.geom.Area(b.getHitbox()));
                if (!areaA.isEmpty()) {
                    manejarColision(a, b);
                }
            }
        }
    }

    private void manejarColision(Vehiculo a, Vehiculo b) {
        a.frenarEnSeco();
        b.frenarEnSeco();
    }

    public List<Vehiculo> getVehiculos() {
        return vehiculos;
    }
}