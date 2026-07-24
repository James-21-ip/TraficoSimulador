package modelo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Cruce implements Serializable {

    private List<Acceso> accesos;
    private ZonaCruce zonaCruce;
    private int indiceAccesoEnVerde;

    public Cruce(ZonaCruce zonaCruce) {
        this.accesos = new ArrayList<>();
        this.zonaCruce = zonaCruce;
        this.indiceAccesoEnVerde = 0;
    }

    public void agregarAcceso(Via via, Semaforo semaforo) {
        accesos.add(new Acceso(via, semaforo));
    }

    public int cantidadEsperando(int indiceAcceso) {
        return accesos.get(indiceAcceso).colaEspera.size();
    }

    public void encolarVehiculo(int indiceAcceso, Vehiculo v) {
        accesos.get(indiceAcceso).colaEspera.add(v);
    }

    public void desencolarVehiculo(int indiceAcceso, Vehiculo v) {
        accesos.get(indiceAcceso).colaEspera.remove(v);
    }

    public List<Acceso> getAccesos() {
        return accesos;
    }

    public ZonaCruce getZonaCruce() {
        return zonaCruce;
    }

    public int getIndiceAccesoEnVerde() {
        return indiceAccesoEnVerde;
    }

    public void setIndiceAccesoEnVerde(int indice) {
        this.indiceAccesoEnVerde = indice;
    }

    /** Una vía que entra al cruce, con su propio semáforo y su cola de espera. */
    public static class Acceso implements Serializable {
        private Via via;
        private Semaforo semaforo;
        private List<Vehiculo> colaEspera;

        public Acceso(Via via, Semaforo semaforo) {
            this.via = via;
            this.semaforo = semaforo;
            this.colaEspera = new ArrayList<>();
        }

        public Via getVia() {
            return via;
        }

        public Semaforo getSemaforo() {
            return semaforo;
        }

        public List<Vehiculo> getColaEspera() {
            return colaEspera;
        }
    }
}