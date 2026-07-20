package gestor;

import modelo.*;
import java.util.*;

public class GestorVehiculos {

    private List<Vehiculo> vehiculos;
    private List<Bache> baches;
    private Random random;
    private Set<String> bachesYaAplicados = new HashSet<>();
    private static final double DISTANCIA_DETECCION_BACHE = 60;

    public GestorVehiculos() {
        vehiculos = new ArrayList<>();
        baches = new ArrayList<>();
        random = new Random();
    }

    public void setBaches(List<Bache> baches) {
        this.baches = baches;
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
            v.mover(adelante, deltaTime);
        }
        aplicarEvasionBaches();
        aplicarCambiosPorCongestion();
        procesarBaches();
        detectarColisiones();
    }

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