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

    public Vehiculo getVehiculoAdelante(Vehiculo referencia) {
    double[] dir = getDireccion();
    Vehiculo masCercano = null;
    double menorDistancia = Double.MAX_VALUE;

    for (Vehiculo v : vehiculos) {
        if (v == referencia) continue;
        double dx = v.getX() - referencia.getX();
        double dy = v.getY() - referencia.getY();
        double proyeccion = dx * dir[0] + dy * dir[1];
        if (proyeccion > 0 && proyeccion < menorDistancia) {
            masCercano = v;
            menorDistancia = proyeccion;
        }
    }
    return masCercano;
}

    // vector unitario hacia donde avanza este carril, segun el trazado de la via y su sentido
public double[] getDireccion() {
    List<java.awt.geom.Point2D> trazado = via.getTrazado();
    java.awt.geom.Point2D inicio = trazado.get(0);
    java.awt.geom.Point2D fin = trazado.get(trazado.size() - 1);
    double dx = fin.getX() - inicio.getX();
    double dy = fin.getY() - inicio.getY();
    double largo = Math.hypot(dx, dy);
    dx /= largo;
    dy /= largo;
    if (!sentidoIda) {
        dx = -dx;
        dy = -dy;
    }
    return new double[]{dx, dy};
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