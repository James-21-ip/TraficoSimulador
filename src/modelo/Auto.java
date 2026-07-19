package modelo;

public class Auto extends Vehiculo {

    public Auto(double x, double y, Carril carril) {
        super(x, y, carril);
        definirCaracteristicas();
    }

    @Override
    protected void definirCaracteristicas() {
        velocidadMaxima = 60;
        aceleracion = 8;
        largo = 30;
        ancho = 15;
    }
}