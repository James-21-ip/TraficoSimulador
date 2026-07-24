package modelo;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.List;
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
    private double velocidad = 14.0;
    private Estado estadoActual;
    private Rectangle2D limitesVereda; 
    private List<Rectangle2D> mapaVeredas; // Memoria de todas las veredas de la ciudad
    private Rectangle2D zonaCruceAsignada;
    private double destinoX, destinoY;
    private double tiempoEspera = 0;
    private Random random = new Random();

    public Peaton(Rectangle2D limitesVereda, List<Rectangle2D> mapaVeredas) {
        this.limitesVereda = limitesVereda;
        this.mapaVeredas = mapaVeredas;
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
                    if (zonaCruceAsignada != null && random.nextDouble() < 0.5) {
                        estadoActual = Estado.ESPERANDO_EN_BORDE;
                        tiempoEspera = 2.0; 
                    } else {
                        zonaCruceAsignada = null; // Descartar cebra si decide no cruzar
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
                if (!hayPeligro) { 
                    estadoActual = Estado.CRUZANDO_CALLE;
                    // Obliga a cruzar perfectamente derecho (X o Y rectos según la cebra)
                    if (zonaCruceAsignada.getWidth() < zonaCruceAsignada.getHeight()) {
                        destinoX = (x < zonaCruceAsignada.getCenterX()) ? zonaCruceAsignada.getMaxX() + 15 : zonaCruceAsignada.getMinX() - 15;
                        destinoY = this.y; 
                    } else {
                        destinoY = (y < zonaCruceAsignada.getCenterY()) ? zonaCruceAsignada.getMaxY() + 15 : zonaCruceAsignada.getMinY() - 15;
                        destinoX = this.x; 
                    }
                }
                break;

            case CRUZANDO_CALLE:
                moverHaciaDestino(deltaTime);
                if (haLlegadoAlDestino()) {
                    estadoActual = Estado.CAMINANDO_POR_VEREDA;
                    zonaCruceAsignada = null; 
                    actualizarVeredaLocal(); // Escanea y adopta la vereda del otro lado
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
        // Obliga al peatón a caminar hasta la orilla SIN salirse de la vereda (no en diagonal)
        if (zona.getWidth() < zona.getHeight()) { 
            this.destinoX = (x < zona.getCenterX()) ? zona.getMinX() - 5 : zona.getMaxX() + 5;
            this.destinoY = Math.max(zona.getMinY(), Math.min(zona.getMaxY(), this.y));
        } else {
            this.destinoX = Math.max(zona.getMinX(), Math.min(zona.getMaxX(), this.x));
            this.destinoY = (y < zona.getCenterY()) ? zona.getMinY() - 5 : zona.getMaxY() + 5;
        }
    }

    private void actualizarVeredaLocal() {
        Rectangle2D veredaMasCercana = limitesVereda;
        double distMin = Double.MAX_VALUE;
        // Busca en la memoria de la ciudad cuál es la vereda donde está parado ahora
        for (Rectangle2D v : mapaVeredas) {
            double cx = v.getCenterX();
            double cy = v.getCenterY();
            double dist = Math.hypot(x - cx, y - cy);
            if (dist < distMin) {
                distMin = dist;
                veredaMasCercana = v;
            }
        }
        this.limitesVereda = veredaMasCercana;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public Estado getEstadoActual() { return estadoActual; }
}