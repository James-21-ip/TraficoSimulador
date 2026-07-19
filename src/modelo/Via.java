package modelo;

import java.util.ArrayList;
import java.util.List;
import java.awt.geom.Point2D;

public class Via {

    private List<Point2D> trazado;
    private List<Carril> carriles;
    private boolean dobleSentido;
    private double velocidadMaxima;

    public Via(List<Point2D> trazado, int numCarriles, boolean dobleSentido, double velocidadMaxima) {
        this.trazado = trazado;
        this.dobleSentido = dobleSentido;
        this.velocidadMaxima = velocidadMaxima;
        this.carriles = new ArrayList<>();

        for (int i = 0; i < numCarriles; i++) {
            boolean sentidoIda = !dobleSentido || i < numCarriles / 2;
            carriles.add(new Carril(this, sentidoIda));
        }
    }

    public List<Point2D> getTrazado() { return trazado; }
    public List<Carril> getCarriles() { return carriles; }
    public boolean isDobleSentido() { return dobleSentido; }
    public double getVelocidadMaxima() { return velocidadMaxima; }
}