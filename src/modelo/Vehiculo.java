// Vehiculo.java
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

    private boolean cambiandoCarril = false;
    private double yObjetivo;
    private double angulo = 0;
    private static final double VELOCIDAD_CAMBIO_LATERAL = 40; // px/seg
    private static final double INCLINACION_MAX = 15; // grados

    public Vehiculo(double x, double y, Carril carril) {
        this.x = x;
        this.y = y;
        this.carril = carril;
        this.velocidad = 0;
        this.estaAveriado = false;
    }

    protected abstract void definirCaracteristicas();

    /** Atajo para cuando no hay semáforo/puente que considerar (ej. PruebaVehiculos). */
    public void mover(Vehiculo vehiculoAdelante, double deltaTime) {
        mover(vehiculoAdelante, null, deltaTime);
    }

    /**
     * @param distanciaHastaParada distancia hasta un semáforo en rojo/ámbar o un
     *        puente ocupado que está en la vía de este vehículo; null si no hay
     *        ninguno (o si ya está en verde / el puente está libre).
     */
    public void mover(Vehiculo vehiculoAdelante, Double distanciaHastaParada, double deltaTime) {
        if (estaAveriado) {
            tiempoAveriaRestante -= deltaTime;
            if (tiempoAveriaRestante <= 0) {
                estaAveriado = false;
            }
            return;
        }

        if (cambiandoCarril) {
            avanzarCambioCarril(deltaTime);
        }

        double margenMinimo = 10;

        // frenado por el vehículo de adelante (igual que antes)
        double distanciaSeguraVehiculo = velocidad * 1.5 + margenMinimo;
        if (vehiculoAdelante != null) {
            distanciaSeguraVehiculo += largo / 2 + vehiculoAdelante.largo / 2;
        }
        double distanciaLibreVehiculo = (vehiculoAdelante != null)
                ? distanciaHacia(vehiculoAdelante)
                : Double.MAX_VALUE;

        // frenado por semáforo en rojo / puente ocupado (nuevo)
        double distanciaSeguraParada = velocidad * 1.5 + margenMinimo + largo / 2;
        double distanciaLibreParada = (distanciaHastaParada != null)
                ? distanciaHastaParada
                : Double.MAX_VALUE;

        boolean debeFrenar = distanciaLibreVehiculo < distanciaSeguraVehiculo
                || distanciaLibreParada < distanciaSeguraParada;

        if (debeFrenar) {
            velocidad = Math.max(0, velocidad - aceleracion * 2 * deltaTime);
        } else if (velocidad < velocidadMaxima) {
            velocidad = Math.min(velocidadMaxima, velocidad + aceleracion * deltaTime);
        }

        double[] direccion = carril.getDireccion();
        x += direccion[0] * velocidad * deltaTime;
        y += direccion[1] * velocidad * deltaTime;
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

    public void reducirVelocidadPorBache(double severidad) {
        velocidad *= (1 - severidad);
    }

    public void intentarCambiarCarrilPorCongestion(Vehiculo adelante, Carril carrilVecino) {
        if (cambiandoCarril || carrilVecino == null) return;
        boolean atascado = velocidad < 5 && adelante != null && distanciaHacia(adelante) < 40;
        if (!atascado) return;
        if (carrilVecino.espacioLibreCerca(x, y) > largo + 10) {
            iniciarCambioCarril(carrilVecino);
        }
    }

    public void intentarEvadirBache(Bache bache, Carril carrilVecino, double distanciaDeteccion) {
        if (cambiandoCarril || carrilVecino == null) return;
        if (!bacheEnCamino(bache, distanciaDeteccion)) return;
        if (carrilVecino.espacioLibreCerca(x, y) > largo + 10) {
            iniciarCambioCarril(carrilVecino);
        }
    }

    private boolean bacheEnCamino(Bache b, double distanciaDeteccion) {
        double[] dir = carril.getDireccion();
        double dx = b.getX() - x;
        double dy = b.getY() - y;
        double proyeccion = dx * dir[0] + dy * dir[1];
        double lateral = Math.abs(dx * dir[1] - dy * dir[0]);
        return proyeccion > 0 && proyeccion < distanciaDeteccion && lateral < ancho + 5;
    }

    private void iniciarCambioCarril(Carril nuevo) {
        carril.quitarVehiculo(this);
        nuevo.agregarVehiculo(this);
        carril = nuevo;
        yObjetivo = nuevo.getY();
        cambiandoCarril = true;
    }

    private void avanzarCambioCarril(double deltaTime) {
        double restante = yObjetivo - y;
        double paso = VELOCIDAD_CAMBIO_LATERAL * deltaTime;

        if (Math.abs(restante) <= paso) {
            y = yObjetivo;
            cambiandoCarril = false;
            angulo = 0;
        } else {
            double dir = Math.signum(restante);
            y += dir * paso;
            angulo = dir * INCLINACION_MAX;
        }
    }

    public java.awt.Shape getHitbox() {
        java.awt.geom.Rectangle2D rect =
                new java.awt.geom.Rectangle2D.Double(x - largo / 2, y - ancho / 2, largo, ancho);
        if (angulo == 0) return rect;
        java.awt.geom.AffineTransform t =
                java.awt.geom.AffineTransform.getRotateInstance(Math.toRadians(angulo), x, y);
        return t.createTransformedShape(rect);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getVelocidad() { return velocidad; }
    public double getAngulo() { return angulo; }
    public Carril getCarril() { return carril; }
}