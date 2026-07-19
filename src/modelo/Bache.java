package modelo;

public class Bache {

    private double x;
    private double y;
    private double severidad; // 0.0 a 1.0 - que tan probable es que afecte a quien pasa

    public Bache(double x, double y, double severidad) {
        this.x = x;
        this.y = y;
        this.severidad = severidad;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getSeveridad() { return severidad; }
}