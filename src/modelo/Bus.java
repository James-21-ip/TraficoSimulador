package modelo;

import java.util.ArrayList;
import java.util.List;

public class Bus extends Vehiculo {

    private List<Peaton> pasajeros;
    private int capacidadMaxima;

    public Bus(double x, double y, Carril carril) {
        super(x, y, carril);
        definirCaracteristicas();
        pasajeros = new ArrayList<>();
    }

    @Override
    protected void definirCaracteristicas() {
        velocidadMaxima = 40;
        aceleracion = 4;
        largo = 60;
        ancho = 20;
        capacidadMaxima = 20;
    }

    public boolean tieneEspacio() {
        return pasajeros.size() < capacidadMaxima;
    }

    public void subirPasajero(Peaton peaton) {
        if (tieneEspacio()) {
            pasajeros.add(peaton);
        }
    }

    public void bajarPasajero(Peaton peaton) {
        pasajeros.remove(peaton);
    }

    public List<Peaton> getPasajeros() {
        return pasajeros;
    }
}