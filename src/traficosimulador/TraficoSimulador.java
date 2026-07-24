package traficosimulador;

import javax.swing.SwingUtilities;
import vista.VentanaPrincipal;

public class TraficoSimulador {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal();
            ventana.setVisible(true);
        }); 
    }
}