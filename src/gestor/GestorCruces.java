package gestor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import modelo.Cruce;
import modelo.Puente;
import modelo.Semaforo;
import modelo.Vehiculo;

public class GestorCruces implements Serializable {

    // Tiempo fijo del verde
    private static final double TIEMPO_VERDE = 15.0;

    // Colchon de seguridad "todo rojo" entre el fin del amarillo de un acceso
    // y el verde del siguiente. Sin esto, un vehiculo que se comprometio a
    // cruzar en amarillo (ver Vehiculo.comprometerACruzar) puede seguir
    // fisicamente dentro del cruce justo cuando el acceso perpendicular ya
    // recibe verde: eso es lo que provoca que autos que van "para la derecha"
    // se crucen con autos que van "para arriba" cuando el timing se alinea.
    private static final double TIEMPO_TODO_ROJO = 3.0;

    private List<Cruce> cruces;
    private List<Puente> puentes;

    // por cruce: cuanto le queda al colchon de todo-rojo actual (si esta en uno)
    private Map<Cruce, Double> tiempoTodoRojoRestante = new HashMap<>();
    // por cruce: que acceso va a recibir verde apenas termine el colchon
    private Map<Cruce, Integer> siguienteIndiceEnEspera = new HashMap<>();

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

        // Si el cruce esta en el colchon de "todo rojo" entre fases, solo
        // hay que contar el tiempo: nadie recibe verde todavia, aunque el
        // temporizador del semaforo (puesto en rojo largo mas abajo) ya haya
        // terminado su fase. Esto le da tiempo a los vehiculos que se
        // comprometieron a cruzar en amarillo a terminar de salir del cruce
        // antes de que el siguiente acceso reciba luz verde.
        if (tiempoTodoRojoRestante.containsKey(cruce)) {
            double restante = tiempoTodoRojoRestante.get(cruce) - deltaTime;
            if (restante > 0) {
                tiempoTodoRojoRestante.put(cruce, restante);
                return;
            }

            tiempoTodoRojoRestante.remove(cruce);
            int siguiente = siguienteIndiceEnEspera.remove(cruce);
            cruce.setIndiceAccesoEnVerde(siguiente);

            double tiempoRojo =
                    TIEMPO_VERDE
                    + TIEMPO_TODO_ROJO
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
            return;
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

            // Todos los accesos (incluido el que recien termino su amarillo)
            // quedan en rojo durante el colchon de seguridad; recien cuando
            // termine ese colchon (mas arriba) el siguiente acceso pasa a
            // verde. La duracion puesta aca es solo un placeholder mientras
            // dura el colchon, se vuelve a fijar correctamente al salir de el.
            for (Cruce.Acceso acceso : cruce.getAccesos()) {
                acceso.getSemaforo().iniciarFase(
                        Semaforo.Estado.ROJO,
                        TIEMPO_TODO_ROJO
                );
            }

            siguienteIndiceEnEspera.put(cruce, siguiente);
            tiempoTodoRojoRestante.put(cruce, TIEMPO_TODO_ROJO);
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