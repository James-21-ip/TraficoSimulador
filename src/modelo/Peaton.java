package modelo;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Random;

public class Peaton implements Serializable {

    public enum Estado {
        CAMINANDO_POR_VEREDA,
        ESPERANDO_EN_BORDE,
        VERIFICANDO_TRAFICO,
        CRUZANDO_CALLE
    }

    private double x;
    private double y;
    private double velocidad = 12.0;
    private Estado estadoActual;
    private Rectangle2D limitesVereda; 
    private Rectangle2D zonaCruceAsignada;
    private double destinoX, destinoY;
    private double tiempoEspera = 0;
    private Random random = new Random();

    public Peaton(Rectangle2D limitesVereda) {
        this.limitesVereda = limitesVereda;
        this.x = limitesVereda.getX() + random.nextDouble() * limitesVereda.getWidth();
        this.y = limitesVereda.getY() + random.nextDouble() * limitesVereda.getHeight();
        this.estadoActual = Estado.CAMINANDO_POR_VEREDA;
        asignarNuevoDestinoEnVereda();
    }

    public void actualizar(double deltaTime, boolean hayPeligro) {
        switch (estadoActual) {
            case CAMINANDO_POR_VEREDA:
                moverHaciaDestino(deltaTime);
                if (haLlegadoAlDestino()) {
                    if (zonaCruceAsignada != null && random.nextDouble() < 0.3) {
                        estadoActual = Estado.ESPERANDO_EN_BORDE;
                        tiempoEspera = 2.0; // Espera 2 segundos en el borde
                    } else {
                        asignarNuevoDestinoEnVereda();
                    }
                }
                break;

            case ESPERANDO_EN_BORDE:
                tiempoEspera -= deltaTime;
                if (tiempoEspera <= 0) {
                    estadoActual = Estado.VERIFICANDO_TRAFICO;
                }
                break;

            case VERIFICANDO_TRAFICO:
                if (!hayPeligro) { // Si no hay autos cerca (calculado por el Gestor)
                    estadoActual = Estado.CRUZANDO_CALLE;
                    // Fija el destino al otro lado de la zona de cruce
                    if (zonaCruceAsignada.getWidth() < zonaCruceAsignada.getHeight()) {
                        destinoX = (x < zonaCruceAsignada.getCenterX()) ? zonaCruceAsignada.getMaxX() + 10 : zonaCruceAsignada.getMinX() - 10;
                    } else {
                        destinoY = (y < zonaCruceAsignada.getCenterY()) ? zonaCruceAsignada.getMaxY() + 10 : zonaCruceAsignada.getMinY() - 10;
                    }
                }
                break;

            case CRUZANDO_CALLE:
                moverHaciaDestino(deltaTime);
                if (haLlegadoAlDestino()) {
                    estadoActual = Estado.CAMINANDO_POR_VEREDA;
                    zonaCruceAsignada = null; // Libera el cruce
                    asignarNuevoDestinoEnVereda();
                }
                break;
        }
    }

    private void moverHaciaDestino(double deltaTime) {
        double dx = destinoX - x;
        double dy = destinoY - y;
        double distancia = Math.hypot(dx, dy);

        if (distancia > 0) {
            x += (dx / distancia) * velocidad * deltaTime;
            y += (dy / distancia) * velocidad * deltaTime;
        }
    }

    private boolean haLlegadoAlDestino() {
        return Math.hypot(destinoX - x, destinoY - y) < 2.0;
    }

    private void asignarNuevoDestinoEnVereda() {
        destinoX = limitesVereda.getX() + random.nextDouble() * limitesVereda.getWidth();
        destinoY = limitesVereda.getY() + random.nextDouble() * limitesVereda.getHeight();
    }

    public void asignarZonaCruce(Rectangle2D zona) {
        this.zonaCruceAsignada = zona;
        if (zona.getWidth() < zona.getHeight()) { 
            // Cebra vertical, el peatón se para en el borde Izquierdo/Derecho
            this.destinoY = zona.getCenterY() + (random.nextDouble() * 10 - 5);
            this.destinoX = (x < zona.getCenterX()) ? zona.getMinX() - 5 : zona.getMaxX() + 5;
        } else {
            // Cebra horizontal, el peatón se para en el borde Superior/Inferior
            this.destinoX = zona.getCenterX() + (random.nextDouble() * 10 - 5);
            this.destinoY = (y < zona.getCenterY()) ? zona.getMinY() - 5 : zona.getMaxY() + 5;
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public Estado getEstadoActual() { return estadoActual; }
}