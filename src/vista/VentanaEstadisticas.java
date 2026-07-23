package vista;

import gestor.GestorVehiculos;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import modelo.Auto;
import modelo.Bus;
import modelo.Moto;
import modelo.Vehiculo;

public class VentanaEstadisticas {

    private double tiempoSimulacion = 0;

    // Colores basados en la paleta de tu PanelSimulacion
    private static final Color COLOR_AUTO = new Color(66, 133, 200);
    private static final Color COLOR_BUS = new Color(224, 156, 40);
    private static final Color COLOR_MOTO = new Color(206, 70, 70);

    public void agregarTiempo(double deltaTime) {
        tiempoSimulacion += deltaTime;
    }

    public void dibujar(Graphics2D g2, GestorVehiculos gestor) {
        int padding = 15;
        int x = 20; // Margen desde la izquierda
        int y = 20; // Margen desde arriba
        int ancho = 160;
        int alto = 135;

        // 1. Dibujar el fondo con Alpha Blending (técnica gráfica para generar transparencias)
        g2.setColor(new Color(25, 25, 25, 210)); 
        g2.fill(new RoundRectangle2D.Double(x, y, ancho, alto, 15, 15));

        // 2. Dibujar Título
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        int textoY = y + 25;
        g2.drawString("ESTADÍSTICAS", x + padding, textoY);
        
        // 3. Dibujar Tiempo
        textoY += 25;
        int minutos = (int) (tiempoSimulacion / 60);
        int segundos = (int) (tiempoSimulacion % 60);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2.drawString(String.format("Tiempo: %02d:%02d", minutos, segundos), x + padding, textoY);

        // 4. Contar vehículos dinámicamente usando instanceOf (operador para verificar la clase de un objeto)
        int autos = 0, buses = 0, motos = 0;
        if (gestor != null) {
            for (Vehiculo v : gestor.getVehiculos()) {
                if (v instanceof Auto) autos++;
                else if (v instanceof Bus) buses++;
                else if (v instanceof Moto) motos++;
            }
        }

        // 5. Dibujar filas de vehículos
        textoY += 25;
        dibujarFilaColor(g2, "Autos", autos, COLOR_AUTO, x + padding, textoY);
        textoY += 20;
        dibujarFilaColor(g2, "Buses", buses, COLOR_BUS, x + padding, textoY);
        textoY += 20;
        dibujarFilaColor(g2, "Motos", motos, COLOR_MOTO, x + padding, textoY);
    }

    private void dibujarFilaColor(Graphics2D g2, String etiqueta, int cantidad, Color color, int x, int y) {
        // Cuadro de color representativo
        g2.setColor(color);
        g2.fillRect(x, y - 11, 12, 12);

        // Texto del contador
        g2.setColor(Color.WHITE);
        g2.drawString(etiqueta + ": " + cantidad, x + 22, y);
    }
}