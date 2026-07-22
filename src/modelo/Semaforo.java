package modelo;

// Estado actual de un semáforo y su temporizador.

public class Semaforo {
    public enum Estado { ROJO, AMARILLO, VERDE }

    private Estado estadoActual;
    private double tiempoRestante; // segundos que faltan para que termine la fase actual

    // límites usados por el control adaptativo (ver GestorCruces.calcularTiempoVerde)
    private final double tiempoMinimoVerde;
    private final double tiempoMaximoVerde;
    private final double tiempoAmarillo;

    public Semaforo(double tiempoMinimoVerde, double tiempoMaximoVerde, double tiempoAmarillo) {
        this.tiempoMinimoVerde = tiempoMinimoVerde;
        this.tiempoMaximoVerde = tiempoMaximoVerde;
        this.tiempoAmarillo = tiempoAmarillo;
        this.estadoActual = Estado.ROJO; // por defecto arranca en rojo; Cruce pone el primer acceso en verde
        this.tiempoRestante = 0;
    }

    /** Avanza el reloj interno. Se llama una vez por tick de simulación. */
    public void actualizar(double deltaTime) {
        tiempoRestante -= deltaTime;
    }

    /** true cuando ya se acabó el tiempo asignado a la fase actual. */
    public boolean terminoFase() {
        return tiempoRestante <= 0;
    }

    /** Cambia de fase y le asigna una nueva duración. */
    public void iniciarFase(Estado nuevoEstado, double duracion) {
        this.estadoActual = nuevoEstado;
        this.tiempoRestante = duracion;
    }

    public boolean estaEnVerde() { return estadoActual == Estado.VERDE; }
    public boolean estaEnRojo() { return estadoActual == Estado.ROJO; }
    public boolean estaEnAmarillo() { return estadoActual == Estado.AMARILLO; }

    public Estado getEstadoActual() { return estadoActual; }
    public double getTiempoRestante() { return tiempoRestante; }
    public double getTiempoMinimoVerde() { return tiempoMinimoVerde; }
    public double getTiempoMaximoVerde() { return tiempoMaximoVerde; }
    public double getTiempoAmarillo() { return tiempoAmarillo; }
}