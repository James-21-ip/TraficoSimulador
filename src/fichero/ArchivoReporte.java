package fichero;

import gestor.GestorVehiculos;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import modelo.Auto;
import modelo.Bus;
import modelo.Moto;
import modelo.Vehiculo;

public class ArchivoReporte {
    
    public static void generarReporteTxt(File archivo, GestorVehiculos gestor, double tiempoSimulacion) throws IOException {
        int autos = 0, buses = 0, motos = 0;
        
        if (gestor != null) {
            for (Vehiculo v : gestor.getVehiculos()) {
                if (v instanceof Auto) autos++;
                else if (v instanceof Bus) buses++;
                else if (v instanceof Moto) motos++;
            }
        }

        int minutos = (int) (tiempoSimulacion / 60);
        int segundos = (int) (tiempoSimulacion % 60);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            writer.write("=========================================\n");
            writer.write("      REPORTE DE SIMULACIÓN DE TRÁFICO   \n");
            writer.write("=========================================\n\n");
            writer.write(String.format("Tiempo Transcurrido: %02d:%02d\n", minutos, segundos));
            writer.write("Vehículos Activos Totales: " + (autos + buses + motos) + "\n\n");
            writer.write("Desglose por Tipo:\n");
            writer.write("- Autos: " + autos + "\n");
            writer.write("- Buses: " + buses + "\n");
            writer.write("- Motos: " + motos + "\n\n");
            writer.write("=========================================\n");
            writer.write("Generado automáticamente por Simulador de Tráfico.\n");
        }
    }
}