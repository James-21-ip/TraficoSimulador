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

    // si esta atascada, se cuela por el espacio entre carriles
    public void intentarFiltrarse(Vehiculo adelante, Carril carrilVecino) {
        if (velocidad < 5 && carrilVecino != null) {
            double espacioLibre = carrilVecino.espacioLibreCerca(x, y);
            if (espacioLibre > ancho + 5) {
                y += (carrilVecino.getY() - y) * 0.1; // se desplaza poco a poco hacia el carril vecino
            }
        }
    }
}