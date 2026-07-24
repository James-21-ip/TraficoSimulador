package modelo;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ZonaCruce implements Serializable {

    private Rectangle2D area;
    private List<Peaton> peatonesCruzando;

    private boolean demoPeatonForzado;

    public ZonaCruce(double x, double y, double ancho, double alto) {
        this.area = new Rectangle2D.Double(x, y, ancho, alto);
        this.peatonesCruzando = new ArrayList<>();
        this.demoPeatonForzado = false;
    }

    public boolean hayPeatonCruzando() {
        return !peatonesCruzando.isEmpty() || demoPeatonForzado;
    }

    public void agregarPeaton(Peaton p) {
        peatonesCruzando.add(p);
    }

    public void quitarPeaton(Peaton p) {
        peatonesCruzando.remove(p);
    }

    public void setDemoPeatonCruzando(boolean valor) {
        this.demoPeatonForzado = valor;
    }

    public Rectangle2D getArea() {
        return area;
    }

    public List<Peaton> getPeatonesCruzando() {
        return peatonesCruzando;
    }
}