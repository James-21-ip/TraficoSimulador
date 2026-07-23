package modelo;

import java.util.ArrayList;
import java.util.List;

public class Carril {

    private Via via;
    private boolean sentidoIda; // true = va en la direccion normal del trazado, false = va al reves
    private List<Vehiculo> vehiculos;
    // Distancia lateral (perpendicular al trazado de la via) a la que esta este carril
    // respecto al eje de la via. OJO: antes esto se usaba como si fuera una coordenada Y
    // absoluta, lo cual solo funcionaba por casualidad en vias horizontales; ahora se
    // combina con getPerpendicular() para que funcione con cualquier orientacion (ver
    // getPuntoInicio()).
    private double offsetY;

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
    // antes media solo la diferencia en X, lo que solo tenia sentido en vias horizontales;
    // ahora proyecta sobre la direccion real del carril, asi sirve para cualquier orientacion.
    public double espacioLibreCerca(double x, double y) {
        double[] dir = getDireccion();
        double masCercano = Double.MAX_VALUE;
        for (Vehiculo v : vehiculos) {
            double dx = v.getX() - x;
            double dy = v.getY() - y;
            double proyeccion = Math.abs(dx * dir[0] + dy * dir[1]);
            if (proyeccion < masCercano) masCercano = proyeccion;
        }
        return masCercano;
    }
/** Distancia (proyectada sobre la direccion del carril) hasta el vehiculo mas
 * cercano que esta ADELANTE de (x,y) en este carril. Double.MAX_VALUE si no hay
 * ninguno. A diferencia de espacioLibreCerca(), no mezcla vehiculos de atras con
 * los de adelante: para decidir si vale la pena cambiarse de carril, lo que
 * importa es cuanto camino libre hay por delante, no solo que el costado este
 * despejado en este instante. */
public double espacioLibreAdelante(double x, double y) {
    double[] dir = getDireccion();
    double masCercano = Double.MAX_VALUE;
    for (Vehiculo v : vehiculos) {
        double dx = v.getX() - x;
        double dy = v.getY() - y;
        double proyeccion = dx * dir[0] + dy * dir[1];
        if (proyeccion > 0 && proyeccion < masCercano) masCercano = proyeccion;
    }
    return masCercano;
}

/** Igual que espacioLibreAdelante() pero mirando hacia ATRAS: distancia al
 * vehiculo mas cercano que viene detras de (x,y) en este carril. Sirve para no
 * meterse literalmente encima de alguien que viene por el carril vecino. */
public double espacioLibreAtras(double x, double y) {
    double[] dir = getDireccion();
    double masCercano = Double.MAX_VALUE;
    for (Vehiculo v : vehiculos) {
        double dx = v.getX() - x;
        double dy = v.getY() - y;
        double proyeccion = dx * dir[0] + dy * dir[1];
        if (proyeccion < 0 && -proyeccion < masCercano) masCercano = -proyeccion;
    }
    return masCercano;
}
    /**
     * Vector unitario perpendicular al trazado de la via (siempre calculado sobre el
     * sentido "ida" del trazado, sin importar el sentido de este carril en particular),
     * usado para separar carriles lateralmente sin importar si la via es horizontal,
     * vertical o diagonal.
     */
    public double[] getPerpendicular() {
        List<java.awt.geom.Point2D> trazado = via.getTrazado();
        java.awt.geom.Point2D inicio = trazado.get(0);
        java.awt.geom.Point2D fin = trazado.get(trazado.size() - 1);
        double dx = fin.getX() - inicio.getX();
        double dy = fin.getY() - inicio.getY();
        double largo = Math.hypot(dx, dy);
        dx /= largo;
        dy /= largo;
        return new double[]{-dy, dx};
    }

    /** Punto real (x,y) donde nace este carril: el extremo de la via que le toca segun su sentido, desplazado lateralmente por offsetY. */
    public double[] getPuntoInicio() {
        List<java.awt.geom.Point2D> trazado = via.getTrazado();
        java.awt.geom.Point2D base = sentidoIda ? trazado.get(0) : trazado.get(trazado.size() - 1);
        double[] perp = getPerpendicular();
        return new double[]{base.getX() + perp[0] * offsetY, base.getY() + perp[1] * offsetY};
    }

    public double getY() { return offsetY; }
    public void setOffsetY(double offsetY) { this.offsetY = offsetY; }
    public Via getVia() { return via; }
    public boolean isSentidoIda() { return sentidoIda; }
    public List<Vehiculo> getVehiculos() { return vehiculos; }
}