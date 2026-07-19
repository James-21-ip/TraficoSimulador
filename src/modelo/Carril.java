package modelo;

import java.util.ArrayList;
import java.util.List;

public class Carril {

    private Via via;
    private boolean sentidoIda; // true = va en la direccion normal del trazado, false = va al reves
    private List<Vehiculo> vehiculos;
    private double offsetY; // que tan separado esta este carril del centro de la via

    public Carril(Via via, boolean sentidoIda) {
        this.via = via;
        this.sentidoIda = sentidoIda;
        this.vehiculos = new ArrayList<>();
    }

    public void agregarVehiculo(Vehiculo v) {
        vehiculos.add(v);
    }

    public void quitarVehiculo(Vehiculo v) {
        vehiculos.remove(v);
    }

    // el de mas adelante en este carril, o null si no hay nadie
    public Vehiculo getVehiculoAdelante(Vehiculo referencia) {
        Vehiculo masCercano = null;
        double menorDistancia = Double.MAX_VALUE;

        for (Vehiculo v : vehiculos) {
            if (v == referencia) continue;
            double dist = v.getX() - referencia.getX();
            if (sentidoIda && dist > 0 && dist < menorDistancia) {
                masCercano = v;
                menorDistancia = dist;
            }
        }
        return masCercano;
    }

    // usado por la moto para saber si se puede filtrar
    public double espacioLibreCerca(double x, double y) {
        double masCercano = Double.MAX_VALUE;
        for (Vehiculo v : vehiculos) {
            double dist = Math.abs(v.getX() - x);
            if (dist < masCercano) masCercano = dist;
        }
        return masCercano;
    }

    public double getY() { return offsetY; }
    public void setOffsetY(double offsetY) { this.offsetY = offsetY; }
    public Via getVia() { return via; }
    public boolean isSentidoIda() { return sentidoIda; }
    public List<Vehiculo> getVehiculos() { return vehiculos; }
}