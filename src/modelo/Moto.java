package modelo;

public class Moto extends Vehiculo {

    public Moto(double x, double y, Carril carril) {
        super(x, y, carril);
        definirCaracteristicas();
    }

    @Override
    protected void definirCaracteristicas() {
        velocidadMaxima = 70;
        aceleracion = 12;
        largo = 15;
        ancho = 8;
    }
}