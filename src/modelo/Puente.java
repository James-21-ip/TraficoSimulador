package modelo;

import java.util.LinkedList;
import java.util.Queue;

public class Puente {

    public enum Sentido { IDA, VUELTA }

    private Via via; // el tramo de vía (un solo carril) que representa el puente
    private Sentido sentidoActual;
    private Queue<Vehiculo> colaIda;
    private Queue<Vehiculo> colaVuelta;
    private Vehiculo vehiculoCruzando; // solo puede haber uno a la vez en el puente
    private double tiempoEnSentidoActual;
    private final double tiempoMaximoPorSentido;

    public Puente(Via via, double tiempoMaximoPorSentido) {
        this.via = via;
        this.sentidoActual = Sentido.IDA;
        this.colaIda = new LinkedList<>();
        this.colaVuelta = new LinkedList<>();
        this.vehiculoCruzando = null;
        this.tiempoEnSentidoActual = 0;
        this.tiempoMaximoPorSentido = tiempoMaximoPorSentido;
    }

    public void encolar(Vehiculo v, Sentido sentido) {
        if (sentido == Sentido.IDA) {
            colaIda.add(v);
        } else {
            colaVuelta.add(v);
        }
    }

    public Queue<Vehiculo> getColaActual() {
        return sentidoActual == Sentido.IDA ? colaIda : colaVuelta;
    }

    public Queue<Vehiculo> getColaOpuesta() {
        return sentidoActual == Sentido.IDA ? colaVuelta : colaIda;
    }

    public boolean colaActualVacia() {
        return getColaActual().isEmpty();
    }

    public void actualizar(double deltaTime) {
        tiempoEnSentidoActual += deltaTime;
    }

    public boolean debeAlternar() {
        boolean otroLadoEspera = !getColaOpuesta().isEmpty();
        if (!otroLadoEspera) {
            return false;
        }
        boolean vacia = colaActualVacia();
        boolean tiempoAgotado = tiempoEnSentidoActual >= tiempoMaximoPorSentido;
        return vacia || tiempoAgotado;
    }

    public void alternarSentido() {
        sentidoActual = (sentidoActual == Sentido.IDA) ? Sentido.VUELTA : Sentido.IDA;
        tiempoEnSentidoActual = 0;
    }

    public boolean estaLibre() {
        return vehiculoCruzando == null;
    }

    public void iniciarCruce(Vehiculo v) {
        vehiculoCruzando = v;
    }

    /** Se llama (desde GestorVehiculos, cuando el vehículo llega al otro lado) para liberar el puente. */
    public void terminarCruce() {
        vehiculoCruzando = null;
    }

    public Via getVia() {
        return via;
    }

    public Sentido getSentidoActual() {
        return sentidoActual;
    }

    public Vehiculo getVehiculoCruzando() {
        return vehiculoCruzando;
    }

    public double getTiempoEnSentidoActual() {
        return tiempoEnSentidoActual;
    }
}