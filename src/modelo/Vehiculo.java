package modelo;

public abstract class Vehiculo {

    protected double x;
    protected double y;
    protected double velocidad;
    protected double velocidadMaxima;
    protected double aceleracion;
    protected double ancho;
    protected double largo;
    protected Carril carril;
    protected boolean estaAveriado;
    protected double tiempoAveriaRestante;

    public Vehiculo(double x, double y, Carril carril) {
        this.x = x;
        this.y = y;
        this.carril = carril;
        this.velocidad = 0;
        this.estaAveriado = false;
    }

    protected abstract void definirCaracteristicas();

    public void mover(Vehiculo vehiculoAdelante, double deltaTime) {
        if (estaAveriado) {
            tiempoAveriaRestante -= deltaTime;
            if (tiempoAveriaRestante <= 0) {
                estaAveriado = false;
            }
            return; // averiado no se mueve
        }

        double distanciaSegura = velocidad * 1.5;
        double distanciaLibre = (vehiculoAdelante != null)
                ? distanciaHacia(vehiculoAdelante)
                : Double.MAX_VALUE;

        if (distanciaLibre < distanciaSegura) {
            velocidad = Math.max(0, velocidad - aceleracion * 2 * deltaTime);
        } else if (velocidad < velocidadMaxima) {
            velocidad = Math.min(velocidadMaxima, velocidad + aceleracion * deltaTime);
        }

        x += velocidad * deltaTime; // TODO: ajustar cuando tengamos la direccion real del carril
    }

    protected double distanciaHacia(Vehiculo otro) {
        return Math.hypot(otro.x - this.x, otro.y - this.y);
    }

    public void frenarEnSeco() {
        velocidad = 0;
    }

    public void averiar(double duracion) {
        estaAveriado = true;
        tiempoAveriaRestante = duracion;
        velocidad = 0;
    }

    public boolean isAveriado() {
        return estaAveriado;
    }

    public java.awt.geom.Rectangle2D getHitbox() {
        return new java.awt.geom.Rectangle2D.Double(x - largo / 2, y - ancho / 2, largo, ancho);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getVelocidad() { return velocidad; }
    public Carril getCarril() { return carril; }
}