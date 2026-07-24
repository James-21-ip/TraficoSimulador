package modelo;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Via implements Serializable {

    private List<Point2D> trazado;
    private List<Carril> carriles;
    private boolean dobleSentido;
    private double velocidadMaxima;
    private List<Via> conexiones = new ArrayList<>(); // a que otras vias se puede continuar al llegar al final

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

    public void agregarConexion(Via destino) {
        conexiones.add(destino);
    }

    public List<Via> getConexiones() { return conexiones; }
    public List<Point2D> getTrazado() { return trazado; }
    public List<Carril> getCarriles() { return carriles; }
    public boolean isDobleSentido() { return dobleSentido; }
    public double getVelocidadMaxima() { return velocidadMaxima; }
}