package gestor;

import modelo.*;
import java.util.*;

public class GestorVehiculos {

    private List<Vehiculo> vehiculos;
    private List<Bache> baches;
    private Random random;

    public GestorVehiculos() {
        vehiculos = new ArrayList<>();
        baches = new ArrayList<>();
        random = new Random();
    }

    public void setBaches(List<Bache> baches) {
        this.baches = baches;
    }

    public void intentarSpawn(Via viaEntrada, double lambda, double deltaTime) {
        double probabilidad = 1 - Math.exp(-lambda * deltaTime);

        if (random.nextDouble() < probabilidad) {
            Carril carril = viaEntrada.getCarriles().get(0);
            double xInicial = viaEntrada.getTrazado().get(0).getX();
            double yInicial = carril.getY();

            Vehiculo nuevo = crearVehiculoAleatorio(xInicial, yInicial, carril);
            carril.agregarVehiculo(nuevo);
            vehiculos.add(nuevo);
        }
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
        aplicarFiltradoMotos();
        procesarBaches();
        detectarColisiones();
    }

    private void aplicarFiltradoMotos() {
        for (Vehiculo v : vehiculos) {
            if (v instanceof Moto) {
                Moto moto = (Moto) v;
                List<Carril> carriles = moto.getCarril().getVia().getCarriles();
                int idx = carriles.indexOf(moto.getCarril());
                Carril vecino = null;
                if (idx + 1 < carriles.size()) {
                    vecino = carriles.get(idx + 1);
                } else if (idx - 1 >= 0) {
                    vecino = carriles.get(idx - 1);
                }
                Vehiculo adelante = moto.getCarril().getVehiculoAdelante(moto);
                moto.intentarFiltrarse(adelante, vecino);
            }
        }
    }

    private void procesarBaches() {
        for (Vehiculo v : vehiculos) {
            if (v.isAveriado()) continue;

            for (Bache b : baches) {
                double distancia = Math.hypot(v.getX() - b.getX(), v.getY() - b.getY());
                if (distancia < 10) {
                    if (random.nextDouble() < b.getSeveridad()) {
                        if (random.nextDouble() < 0.2) {
                            v.averiar(2 + random.nextDouble() * 3); // avería de 2 a 5 seg
                        } else {
                            v.frenarEnSeco();
                        }
                    }
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

                if (a.getHitbox().intersects(b.getHitbox())) {
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