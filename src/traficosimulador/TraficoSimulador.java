package traficosimulador;

import vista.VentanaPrincipal;

import javax.swing.SwingUtilities;

public class TraficoSimulador {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal();
            ventana.setVisible(true);
        });
    }
}