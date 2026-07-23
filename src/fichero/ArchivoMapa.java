package fichero;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import modelo.Bache;
import modelo.Cruce;
import modelo.Entrada;
import modelo.Semaforo;
import modelo.Via;
import modelo.ZonaCruce;

public class ArchivoMapa {

    private List<Via> vias = new ArrayList<>();
    private List<Cruce> cruces = new ArrayList<>();
    private List<Bache> baches = new ArrayList<>();
    private List<Entrada> entradas = new ArrayList<>();
    private Map<Integer, Via> mapaVias = new HashMap<>();

    public void cargarMapa(String rutaRecurso) {
        // Usamos InputStream para leer el archivo desde el interior del proyecto (src)
        InputStream is = getClass().getResourceAsStream(rutaRecurso);
        
        if (is == null) {
            System.err.println("¡CRÍTICO! No se encontró el archivo en los paquetes: " + rutaRecurso);
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;

                String[] partes = linea.split(",");
                String tipo = partes[0].toUpperCase();

                switch (tipo) {
                    case "VIA": procesarVia(partes); break;
                    case "CONEXION": procesarConexion(partes); break;
                    case "CRUCE": procesarCruce(partes); break;
                    case "BACHE": procesarBache(partes); break;
                    case "SPAWN": procesarSpawn(partes); break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer el archivo del mapa: " + e.getMessage());
        }
    }

    private void procesarVia(String[] partes) {
        int id = Integer.parseInt(partes[1]);
        double x1 = Double.parseDouble(partes[2]);
        double y1 = Double.parseDouble(partes[3]);
        double x2 = Double.parseDouble(partes[4]);
        double y2 = Double.parseDouble(partes[5]);
        int carriles = Integer.parseInt(partes[6]);
        boolean dobleSentido = Boolean.parseBoolean(partes[7]);
        double velocidad = Double.parseDouble(partes[8]);

        List<Point2D> trazado = Arrays.asList(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
        Via nuevaVia = new Via(trazado, carriles, dobleSentido, velocidad);
        
        vias.add(nuevaVia);
        mapaVias.put(id, nuevaVia);
    }

    private void procesarConexion(String[] partes) {
        Via origen = mapaVias.get(Integer.parseInt(partes[1]));
        Via destino = mapaVias.get(Integer.parseInt(partes[2]));
        if (origen != null && destino != null) origen.agregarConexion(destino);
    }

    private void procesarCruce(String[] partes) {
        double cx = Double.parseDouble(partes[1]);
        double cy = Double.parseDouble(partes[2]);
        ZonaCruce zona = new ZonaCruce(cx - 26, cy - 26, 52, 52);
        Cruce cruce = new Cruce(zona);

        for (int i = 3; i < partes.length; i++) {
            Via viaAcceso = mapaVias.get(Integer.parseInt(partes[i]));
            if (viaAcceso != null) {
                cruce.agregarAcceso(viaAcceso, new Semaforo(5, 20, 2));
            }
        }
        if (!cruce.getAccesos().isEmpty()) {
            cruce.setIndiceAccesoEnVerde(0);
            cruce.getAccesos().get(0).getSemaforo().iniciarFase(Semaforo.Estado.VERDE, 5);
        }
        cruces.add(cruce);
    }

    private void procesarBache(String[] partes) {
        double x = Double.parseDouble(partes[1]);
        double y = Double.parseDouble(partes[2]);
        double severidad = Double.parseDouble(partes[3]);
        baches.add(new Bache(x, y, severidad));
    }

    private void procesarSpawn(String[] partes) {
        Via via = mapaVias.get(Integer.parseInt(partes[1]));
        double lambda = Double.parseDouble(partes[2]);
        if (via != null) entradas.add(new Entrada(via, lambda));
    }

    public List<Via> getVias() { return vias; }
    public List<Cruce> getCruces() { return cruces; }
    public List<Bache> getBaches() { return baches; }
    public List<Entrada> getEntradas() { return entradas; }
}