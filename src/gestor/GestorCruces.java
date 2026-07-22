package gestor;

import modelo.Cruce;
import modelo.Puente;
import modelo.Semaforo;
import modelo.Vehiculo;

import java.util.List;

public class GestorCruces {

    private static final double SEGUNDOS_EXTRA_POR_VEHICULO = 2.0;

    private List<Cruce> cruces;
    private List<Puente> puentes;

    public GestorCruces(List<Cruce> cruces, List<Puente> puentes) {
        this.cruces = cruces;
        this.puentes = puentes;
    }

    /** Se llama una vez por tick de simulación. */
    public void actualizar(double deltaTime) {
        for (Cruce cruce : cruces) {
            actualizarCruce(cruce, deltaTime);
        }
        for (Puente puente : puentes) {
            actualizarPuente(puente, deltaTime);
        }
    }

    private void actualizarCruce(Cruce cruce, double deltaTime) {
        int indiceVerde = cruce.getIndiceAccesoEnVerde();
        Cruce.Acceso accesoActivo = cruce.getAccesos().get(indiceVerde);
        Semaforo semaforo = accesoActivo.getSemaforo();

        semaforo.actualizar(deltaTime);

        if (!semaforo.terminoFase()) {
            return;
        }

        if (semaforo.estaEnVerde()) {
            // se acabó el verde -> pasa a ámbar
            semaforo.iniciarFase(Semaforo.Estado.AMARILLO, semaforo.getTiempoAmarillo());

        } else if (semaforo.estaEnAmarillo()) {
            // se acabó el ámbar -> este acceso queda en rojo y le toca a otro
            semaforo.iniciarFase(Semaforo.Estado.ROJO, 0);

            int siguiente = siguienteAccesoConEspera(cruce, indiceVerde);
            cruce.setIndiceAccesoEnVerde(siguiente);

            double duracionVerde = calcularTiempoVerde(cruce, siguiente);
            cruce.getAccesos().get(siguiente).getSemaforo()
                    .iniciarFase(Semaforo.Estado.VERDE, duracionVerde);
        }
        // si está en rojo no hacemos nada acá: los rojos "pasivos" no corren su reloj,
        // solo se actualiza el que está activo (verde/ámbar) en cada momento.
    }

    /**
     * Control adaptativo: el tiempo de verde crece con la cantidad de vehículos
     * esperando en ese acceso, pero nunca menos del mínimo ni más del máximo.
     */
    private double calcularTiempoVerde(Cruce cruce, int indiceAcceso) {
        Semaforo semaforo = cruce.getAccesos().get(indiceAcceso).getSemaforo();
        int vehiculosEsperando = cruce.cantidadEsperando(indiceAcceso);

        double duracion = semaforo.getTiempoMinimoVerde()
                + vehiculosEsperando * SEGUNDOS_EXTRA_POR_VEHICULO;

        return Math.min(duracion, semaforo.getTiempoMaximoVerde());
    }

    /**
     * Elige el próximo acceso a dar el verde: de los que no son el actual,
     * prioriza el que tenga la cola más larga (así el lado más congestionado
     * no espera de más). Si todos tienen la misma cola (o 0), sigue el orden normal.
     */
    private int siguienteAccesoConEspera(Cruce cruce, int indiceActual) {
        int total = cruce.getAccesos().size();
        int mejorIndice = -1;
        int mayorCola = -1;

        for (int i = 1; i < total; i++) {
            int candidato = (indiceActual + i) % total;
            int esperando = cruce.cantidadEsperando(candidato);
            if (esperando > mayorCola) {
                mayorCola = esperando;
                mejorIndice = candidato;
            }
        }

        return (mejorIndice == -1) ? (indiceActual + 1) % total : mejorIndice;
    }

    private void actualizarPuente(Puente puente, double deltaTime) {
        puente.actualizar(deltaTime);

        // si el puente está libre y hay alguien esperando en el sentido actual, que pase
        if (puente.estaLibre() && !puente.colaActualVacia()) {
            Vehiculo siguiente = puente.getColaActual().poll();
            puente.iniciarCruce(siguiente);
        }

        // si toca alternar y no hay nadie cruzando en este momento, se alterna
        if (puente.estaLibre() && puente.debeAlternar()) {
            puente.alternarSentido();
        }
    }

    /** true si los vehículos de esa vía deben detenerse por un peatón cruzando. */
    public boolean debeDetenerseParaPeaton(Cruce cruce) {
        return cruce.getZonaCruce().hayPeatonCruzando();
    }
}