package gestor;

import modelo.Cruce;
import modelo.Puente;
import modelo.Semaforo;
import modelo.Vehiculo;

import java.util.List;

public class GestorCruces {

    // Tiempo fijo del verde
    private static final double TIEMPO_VERDE = 15.0;

    private List<Cruce> cruces;
    private List<Puente> puentes;

    public GestorCruces(List<Cruce> cruces, List<Puente> puentes) {
        this.cruces = cruces;
        this.puentes = puentes;
        inicializarEstadosSemaforos();
    }

    private void inicializarEstadosSemaforos() {

        for (Cruce cruce : cruces) {

            if (cruce.getAccesos().isEmpty()) {
                continue;
            }

            int indiceVerde = cruce.getIndiceAccesoEnVerde();

            Cruce.Acceso accesoActivo = cruce.getAccesos().get(indiceVerde);

            accesoActivo.getSemaforo().iniciarFase(
                    Semaforo.Estado.VERDE,
                    TIEMPO_VERDE
            );

            double tiempoRojo = TIEMPO_VERDE
                    + accesoActivo.getSemaforo().getTiempoAmarillo();

            for (int i = 0; i < cruce.getAccesos().size(); i++) {

                if (i != indiceVerde) {

                    cruce.getAccesos()
                            .get(i)
                            .getSemaforo()
                            .iniciarFase(
                                    Semaforo.Estado.ROJO,
                                    tiempoRojo
                            );
                }
            }
        }
    }

    public void actualizar(double deltaTime) {

        for (Cruce cruce : cruces) {
            actualizarCruce(cruce, deltaTime);
        }

        for (Puente puente : puentes) {
            actualizarPuente(puente, deltaTime);
        }
    }

    private void actualizarCruce(Cruce cruce, double deltaTime) {

        for (Cruce.Acceso acceso : cruce.getAccesos()) {
            acceso.getSemaforo().actualizar(deltaTime);
        }

        int indiceActual = cruce.getIndiceAccesoEnVerde();

        Cruce.Acceso accesoActual =
                cruce.getAccesos().get(indiceActual);

        Semaforo semaforoActual =
                accesoActual.getSemaforo();

        if (!semaforoActual.terminoFase()) {
            return;
        }

        if (semaforoActual.estaEnVerde()) {

            semaforoActual.iniciarFase(
                    Semaforo.Estado.AMARILLO,
                    semaforoActual.getTiempoAmarillo()
            );

        } else if (semaforoActual.estaEnAmarillo()) {

            int siguiente =
                    (indiceActual + 1) % cruce.getAccesos().size();

            cruce.setIndiceAccesoEnVerde(siguiente);

            double tiempoRojo =
                    TIEMPO_VERDE
                    + cruce.getAccesos()
                           .get(siguiente)
                           .getSemaforo()
                           .getTiempoAmarillo();

            for (int i = 0; i < cruce.getAccesos().size(); i++) {

                Semaforo sem =
                        cruce.getAccesos().get(i).getSemaforo();

                if (i == siguiente) {

                    sem.iniciarFase(
                            Semaforo.Estado.VERDE,
                            TIEMPO_VERDE
                    );

                } else {

                    sem.iniciarFase(
                            Semaforo.Estado.ROJO,
                            tiempoRojo
                    );
                }
            }
        }
    }

    private void actualizarPuente(Puente puente, double deltaTime) {

        puente.actualizar(deltaTime);

        if (puente.estaLibre() && !puente.colaActualVacia()) {

            Vehiculo siguiente =
                    puente.getColaActual().poll();

            puente.iniciarCruce(siguiente);
        }

        if (puente.estaLibre() && puente.debeAlternar()) {
            puente.alternarSentido();
        }
    }

    public boolean debeDetenerseParaPeaton(Cruce cruce) {
        return cruce.getZonaCruce().hayPeatonCruzando();
    }

}