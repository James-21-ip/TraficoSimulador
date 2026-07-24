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
    private List<Rectangle2D> mapaVeredas; 
    private Rectangle2D zonaCruceAsignada;
    private double destinoX, destinoY;
    private double tiempoEspera = 0;
    private double cooldownCruce = 0; // Memoria para no cruzarse de regreso inmediatamente
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
        if (cooldownCruce > 0) cooldownCruce -= deltaTime;

        switch (estadoActual) {
            case CAMINANDO_POR_VEREDA:
                moverHaciaDestino(deltaTime);
                if (haLlegadoAlDestino()) {
                    if (zonaCruceAsignada != null) {
                        estadoActual = Estado.ESPERANDO_EN_BORDE;
                        tiempoEspera = 2.0; 
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
                if (!hayPeligro) { 
                    estadoActual = Estado.CRUZANDO_CALLE;
                    
                    // CORRECCIÓN MATEMÁTICA: OBLIGAR A QUE CRUCEN EN LÍNEA RECTA HASTA EL OTRO LADO
                    if (zonaCruceAsignada.getWidth() < zonaCruceAsignada.getHeight()) { 
                        // Cebra Vertical (Conecta Norte y Sur). El peatón altera su eje Y.
                        destinoX = zonaCruceAsignada.getCenterX(); // Fijar línea recta exacta
                        destinoY = (y < zonaCruceAsignada.getCenterY()) ? zonaCruceAsignada.getMaxY() + 6 : zonaCruceAsignada.getMinY() - 6;
                    } else { 
                        // Cebra Horizontal (Conecta Este y Oeste). El peatón altera su eje X.
                        destinoY = zonaCruceAsignada.getCenterY(); // Fijar línea recta exacta
                        destinoX = (x < zonaCruceAsignada.getCenterX()) ? zonaCruceAsignada.getMaxX() + 6 : zonaCruceAsignada.getMinX() - 6;
                    }
                }
                break;

            case CRUZANDO_CALLE:
                moverHaciaDestino(deltaTime);
                if (haLlegadoAlDestino()) {
                    estadoActual = Estado.CAMINANDO_POR_VEREDA;
                    zonaCruceAsignada = null; 
                    cooldownCruce = 8.0; // Pasan al menos 8 segundos antes de querer cruzar otra calle
                    actualizarVeredaLocal(); // Escanea y ancla la vereda nueva
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
            double avance = velocidad * deltaTime;
            // Previene que se queden vibrando si se pasan de largo del destino
            if (avance >= distancia) {
                x = destinoX;
                y = destinoY;
            } else {
                x += (dx / distancia) * avance;
                y += (dy / distancia) * avance;
            }
        }
    }

    private boolean haLlegadoAlDestino() {
        return Math.hypot(destinoX - x, destinoY - y) < 1.0;
    }

    private void asignarNuevoDestinoEnVereda() {
        // Un ligero margen interno (2px) para que caminen exactos sin salirse de la delgada línea
        destinoX = limitesVereda.getMinX() + 2 + random.nextDouble() * (limitesVereda.getWidth() - 4);
        destinoY = limitesVereda.getMinY() + 2 + random.nextDouble() * (limitesVereda.getHeight() - 4);
    }

    public void asignarZonaCruce(Rectangle2D zona) {
        this.zonaCruceAsignada = zona;
        
        // Alinear al borde del paso peatonal: el peatón debe dirigirse al canto
        // de la cebra en su lado (no al centro), y estar centrado en el eje de la cebra.
        double margen = 2.0;
        if (zona.getWidth() < zona.getHeight()) { 
            // Cebra vertical (Cruza Norte-Sur): fijar X al centro de la cebra,
            // Y al borde más cercano de la cebra (fuera de la calzada).
            this.destinoX = zona.getCenterX();
            if (this.y < zona.getCenterY()) {
                // Peatón al norte: acercarse al borde superior de la cebra
                this.destinoY = zona.getMinY() - margen;
            } else {
                // Peatón al sur: acercarse al borde inferior de la cebra
                this.destinoY = zona.getMaxY() + margen;
            }
        } else {
            // Cebra horizontal (Cruza Este-Oeste): fijar Y al centro de la cebra,
            // X al borde más cercano (fuera de la calzada).
            this.destinoY = zona.getCenterY();
            if (this.x < zona.getCenterX()) {
                // Peatón al oeste: acercarse al borde izquierdo de la cebra
                this.destinoX = zona.getMinX() - margen;
            } else {
                // Peatón al este: acercarse al borde derecho de la cebra
                this.destinoX = zona.getMaxX() + margen;
            }
        }
    }

    private void actualizarVeredaLocal() {
        Rectangle2D veredaMasCercana = limitesVereda;
        double distMin = Double.MAX_VALUE;
        for (Rectangle2D v : mapaVeredas) {
            // CORRECCIÓN DE DISTANCIA: Calcular la distancia a los BORDES del rectángulo, no a su centro
            double cercaX = Math.max(v.getMinX(), Math.min(x, v.getMaxX()));
            double cercaY = Math.max(v.getMinY(), Math.min(y, v.getMaxY()));
            
            double dist = Math.hypot(x - cercaX, y - cercaY);
            if (dist < distMin) {
                distMin = dist;
                veredaMasCercana = v;
            }
        }
        this.limitesVereda = veredaMasCercana;
    }

    public boolean puedeCruzar() { return cooldownCruce <= 0; }
    public Rectangle2D getZonaCruceAsignada() { return zonaCruceAsignada; }
    public double getX() { return x; }
    public double getY() { return y; }
    public Estado getEstadoActual() { return estadoActual; }
}