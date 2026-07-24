package modelo;

import java.io.Serializable;

// Estado actual de un semáforo y su temporizador.

public class Semaforo implements Serializable {

    public enum Estado {
        ROJO,
        AMARILLO,
        VERDE
    }

    private Estado estadoActual;
    private double tiempoRestante;

    private final double tiempoAmarillo;

    public Semaforo(double tiempoMinimoVerde, double tiempoMaximoVerde, double tiempoAmarillo) {
        this.tiempoAmarillo = tiempoAmarillo;
        this.estadoActual = Estado.ROJO;
        this.tiempoRestante = 0;
    }

    public void actualizar(double deltaTime) {
        tiempoRestante -= deltaTime;
    }

    public boolean terminoFase() {
        return tiempoRestante <= 0;
    }

    public void iniciarFase(Estado nuevoEstado, double duracion) {
        estadoActual = nuevoEstado;
        tiempoRestante = duracion;
    }

    public boolean estaEnVerde() {
        return estadoActual == Estado.VERDE;
    }

    public boolean estaEnRojo() {
        return estadoActual == Estado.ROJO;
    }

    public boolean estaEnAmarillo() {
        return estadoActual == Estado.AMARILLO;
    }

    public Estado getEstadoActual() {
        return estadoActual;
    }

    public double getTiempoRestante() {
        return tiempoRestante;
    }

    public double getTiempoAmarillo() {
        return tiempoAmarillo;
    }
}