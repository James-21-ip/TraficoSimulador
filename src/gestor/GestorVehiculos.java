package gestor;

import modelo.*;
import java.awt.geom.Point2D;
import java.util.*;

public class GestorVehiculos {

    private List<Vehiculo> vehiculos;
    private List<Bache> baches;
    private List<Cruce> cruces;
    private List<Puente> puentes;
    private Random random;
    private Set<String> bachesYaAplicados = new HashSet<>();
    

    private static final double DISTANCIA_DETECCION_BACHE = 60;
    private static final double DISTANCIA_LINEA_PARADA = 15;
    private static final double MARGEN_SALIDA = 100;
    private static final double UMBRAL_DECISION_GIRO = 0; // que tan cerca del final de la via se decide el giro
    private static final double PROBABILIDAD_GIRAR = 0.2;  // si hay conexiones, mitad de las veces gira, mitad sigue derecho (y sale del mapa)

    public GestorVehiculos() {
        vehiculos = new ArrayList<>();
        baches = new ArrayList<>();
        cruces = new ArrayList<>();
        puentes = new ArrayList<>();
        random = new Random();
    }

    public void setBaches(List<Bache> baches) { this.baches = baches; }
    public void setCruces(List<Cruce> cruces) { this.cruces = cruces; }
    public void setPuentes(List<Puente> puentes) { this.puentes = puentes; }

    public void intentarSpawn(Via viaEntrada, double lambda, double deltaTime) {
        for (Carril carril : viaEntrada.getCarriles()) {
            double probabilidad = 1 - Math.exp(-lambda * deltaTime);
            if (random.nextDouble() < probabilidad) {
                double[] puntoInicio = carril.getPuntoInicio();
                double xInicial = puntoInicio[0];
                double yInicial = puntoInicio[1];

                if (!hayEspacioParaSpawnear(carril, xInicial, yInicial)) {
                    continue;
                }

                Vehiculo nuevo = crearVehiculoAleatorio(xInicial, yInicial, carril);
                carril.agregarVehiculo(nuevo);
                vehiculos.add(nuevo);
            }
        }
    }

    private boolean hayEspacioParaSpawnear(Carril carril, double xInicial, double yInicial) {
        double distanciaMinima = 80;
        for (Vehiculo v : carril.getVehiculos()) {
            if (Math.hypot(v.getX() - xInicial, v.getY() - yInicial) < distanciaMinima) {
                return false;
            }
        }
        return true;
    }

    // menos autos y un poco menos de buses (son los mas grandes/lentos y los que mas
    // se notan cuando se amontonan en los cruces); mas motos, que ocupan menos espacio
    // y liberan la interseccion mas rapido.
    private Vehiculo crearVehiculoAleatorio(double x, double y, Carril carril) {
        double r = random.nextDouble();
        if (r < 0.50) {
            return new Auto(x, y, carril);
        } else if (r < 0.65) {
            return new Bus(x, y, carril);
        } else {
            return new Moto(x, y, carril);
        }
    }

    public void actualizar(double deltaTime) {
        for (Vehiculo v : vehiculos) {
            Vehiculo adelante = v.getCarril().getVehiculoAdelante(v);
            Double distanciaParada = calcularDistanciaParada(v);
            v.mover(adelante, distanciaParada, deltaTime);
        }
        aplicarEvasionBaches();
        aplicarCambiosPorCongestion();
        aplicarGirosEnIntersecciones();
        procesarBaches();
        detectarColisiones();
        detectarColisionesEnCruce(deltaTime);
        sincronizarColasDeCruces();
        eliminarVehiculosFueraDeRango();
    }

    // ---------- giros en intersecciones (nuevo) ----------

    private void aplicarGirosEnIntersecciones() {
    for (Vehiculo v : vehiculos) {
        Via via = v.getCarril().getVia();
        List<Via> conexiones = via.getConexiones();
        if (conexiones.isEmpty() || v.isDecisionGiroTomada()) continue;

        // si esta via tiene semaforo propio y esta en rojo, no lo dejes pasar todavia
        Cruce.Acceso acceso = buscarAcceso(via);
        if (acceso != null && !acceso.getSemaforo().estaEnVerde() && !v.estaComprometidoACruzar()) continue;

        double avance = avanceEnVia(v, via);
        double largoVia = largoDeVia(via);

        if (avance >= largoVia - UMBRAL_DECISION_GIRO) {
            v.marcarDecisionGiroTomada();

          Via destino = elegirViaDestino(via, v.getCarril(), conexiones);
            Carril entrada = elegirCarrilEntrada(via, v.getCarril(), destino);
            if (entrada != null) {
                v.girarHaciaVia(entrada);
            }
        }
    }
}
    private Via elegirViaDestino(Via via, Carril carrilOrigen, List<Via> conexiones) {
    double[] dirActual = carrilOrigen.getDireccion();

    List<Via> rectas = new ArrayList<>();
    List<Via> giros = new ArrayList<>();
    for (Via candidata : conexiones) {
        double[] dirCandidata = direccionEntrada(candidata);
        double alineacion = dirActual[0] * dirCandidata[0] + dirActual[1] * dirCandidata[1];
        if (alineacion > 0.85) {
            rectas.add(candidata);
        } else {
            giros.add(candidata);
        }
    }

    if (!rectas.isEmpty() && !giros.isEmpty()) {
        List<Via> grupoElegido = (random.nextDouble() < PROBABILIDAD_GIRAR) ? giros : rectas;
        return grupoElegido.get(random.nextInt(grupoElegido.size()));
    }
    return conexiones.get(random.nextInt(conexiones.size()));
}

private double[] direccionEntrada(Via via) {
    for (Carril c : via.getCarriles()) {
        if (c.isSentidoIda()) return c.getDireccion();
    }
    return via.getCarriles().get(0).getDireccion();
}

    private Carril elegirCarrilEntrada(Via origen, Carril carrilOrigen, Via destino) {
    List<Carril> candidatos = new ArrayList<>();
    for (Carril c : destino.getCarriles()) {
        if (c.isSentidoIda()) candidatos.add(c);
    }
    if (candidatos.isEmpty()) return null;

    List<Carril> propiosMismoSentido = new ArrayList<>();
    for (Carril c : origen.getCarriles()) {
        if (c.isSentidoIda() == carrilOrigen.isSentidoIda()) propiosMismoSentido.add(c);
    }
    int indice = propiosMismoSentido.indexOf(carrilOrigen);
    indice = Math.max(0, Math.min(indice, candidatos.size() - 1));

    return candidatos.get(indice);
}

    // ---------- despawn ----------

    private void eliminarVehiculosFueraDeRango() {
        Iterator<Vehiculo> it = vehiculos.iterator();
        while (it.hasNext()) {
            Vehiculo v = it.next();
            if (haSalidoDeLaVia(v)) {
                liberarPuenteSiCorresponde(v);
                v.getCarril().quitarVehiculo(v);
                it.remove();
                limpiarBachesAplicados(v);
            }
        }
    }

    private boolean haSalidoDeLaVia(Vehiculo v) {
        Via via = v.getCarril().getVia();
        return avanceEnVia(v, via) > largoDeVia(via) + MARGEN_SALIDA;
    }

    private double avanceEnVia(Vehiculo v, Via via) {
        List<Point2D> trazado = via.getTrazado();
        Point2D inicio = trazado.get(0);
        double[] dir = v.getCarril().getDireccion();
        double dx = v.getX() - inicio.getX();
        double dy = v.getY() - inicio.getY();
        return dx * dir[0] + dy * dir[1];
    }

    private double largoDeVia(Via via) {
        List<Point2D> trazado = via.getTrazado();
        Point2D inicio = trazado.get(0);
        Point2D fin = trazado.get(trazado.size() - 1);
        return inicio.distance(fin);
    }

    // se llama justo antes de remover un vehiculo: si era el que estaba cruzando un puente, lo libera
    private void liberarPuenteSiCorresponde(Vehiculo v) {
        for (Puente puente : puentes) {
            if (puente.getVehiculoCruzando() == v) {
                puente.terminarCruce();
            }
        }
    }

    private void limpiarBachesAplicados(Vehiculo v) {
        String prefijo = System.identityHashCode(v) + "-";
        bachesYaAplicados.removeIf(clave -> clave.startsWith(prefijo));
    }

    // ---------- semaforos y puentes ----------

 private Double calcularDistanciaParada(Vehiculo v) {
    Via via = v.getCarril().getVia();

    Cruce.Acceso acceso = buscarAcceso(via);
    if (acceso != null) {
        if (v.estaComprometidoACruzar()) {
            return null;
        }

        double distanciaLinea = avanceRestanteHastaFinDeVia(v, via);

        // si el vehiculo YA paso la linea (ej. entro con verde y el semaforo
        // cambio justo despues), no tiene sentido congelarlo ahi: ya esta
        // fisicamente del otro lado, asi que se compromete a terminar de
        // cruzar en vez de forzarlo a parar sobre una linea que ya dejo atras.
        if (distanciaLinea <= 0) {
            v.comprometerACruzar();
            return null;
        }

        Semaforo semaforo = acceso.getSemaforo();

        if (semaforo.estaEnRojo()) {
            return distanciaLinea;
        }

        if (semaforo.estaEnAmarillo()) {
            if (v.puedeFrenarComodamenteAntesDe(distanciaLinea)) {
                return distanciaLinea;
            }
            v.comprometerACruzar();
            return null;
        }

        // VERDE, pero hay un peaton cruzando la zona del cruce: el boton de
        // demo (o un GestorPeatones real a futuro) marca hayPeatonCruzando()
        // y antes esto no se chequeaba nunca aca, asi que el peaton no
        // frenaba a nadie. Se trata igual que un rojo: el vehiculo se detiene
        // en la linea hasta que la zona quede libre.
        Cruce cruce = buscarCruce(via);
        if (cruce != null && cruce.getZonaCruce().hayPeatonCruzando()) {
            return distanciaLinea;
        }
        // VERDE y sin peatones: sigue de largo, no hay parada
    }

    for (Puente puente : puentes) {
        if (puente.getVia() == via && !puente.estaLibre()) {
            return distanciaHastaFinDeVia(v, via);
        }
    }

    return null;
}
/** Distancia con signo hacia adelante que le falta a "v" para llegar al
 * final de su via. Si ya la paso, da negativo o cero. A diferencia de
 * distanciaHastaFinDeVia (distancia euclidiana sin signo), esta si
 * distingue "todavia no llego" de "ya paso", evitando que un vehiculo
 * que ya cruzo la linea se quede congelado ahi. */
private double avanceRestanteHastaFinDeVia(Vehiculo v, Via via) {
    return largoDeVia(via) - avanceEnVia(v, via);
}
 
    private Cruce.Acceso buscarAcceso(Via via) {
        for (Cruce cruce : cruces) {
            for (Cruce.Acceso acceso : cruce.getAccesos()) {
                if (acceso.getVia() == via) {
                    return acceso;
                }
            }
        }
        return null;
    }

    /** El Cruce (interseccion completa) al que pertenece esta via de entrada, si es que es un acceso de alguno. */
    private Cruce buscarCruce(Via via) {
        for (Cruce cruce : cruces) {
            for (Cruce.Acceso acceso : cruce.getAccesos()) {
                if (acceso.getVia() == via) {
                    return cruce;
                }
            }
        }
        return null;
    }

    private double distanciaHastaFinDeVia(Vehiculo v, Via via) {
        Point2D fin = via.getTrazado().get(via.getTrazado().size() - 1);
        return Math.hypot(fin.getX() - v.getX(), fin.getY() - v.getY());
    }

    private void sincronizarColasDeCruces() {
        for (Cruce cruce : cruces) {
            List<Cruce.Acceso> accesos = cruce.getAccesos();
            for (int i = 0; i < accesos.size(); i++) {
                Cruce.Acceso acceso = accesos.get(i);
                Via via = acceso.getVia();

                for (Vehiculo v : via.getCarriles().isEmpty() ? Collections.<Vehiculo>emptyList()
                        : via.getCarriles().get(0).getVehiculos()) {
                    boolean llegoALaLinea = distanciaHastaFinDeVia(v, via) < DISTANCIA_LINEA_PARADA;
                    boolean yaEnCola = acceso.getColaEspera().contains(v);

                    if (llegoALaLinea && !acceso.getSemaforo().estaEnVerde() && !yaEnCola) {
                        cruce.encolarVehiculo(i, v);
                    } else if (yaEnCola && (acceso.getSemaforo().estaEnVerde() || !llegoALaLinea)) {
                        cruce.desencolarVehiculo(i, v);
                    }
                }
            }
        }
    }

    // ---------- carriles, baches, colisiones ----------

    private Carril carrilVecino(Carril actual) {
        List<Carril> carriles = actual.getVia().getCarriles();
        int idx = carriles.indexOf(actual);

        if (idx + 1 < carriles.size() && carriles.get(idx + 1).isSentidoIda() == actual.isSentidoIda()) {
            return carriles.get(idx + 1);
        }
        if (idx - 1 >= 0 && carriles.get(idx - 1).isSentidoIda() == actual.isSentidoIda()) {
            return carriles.get(idx - 1);
        }
        return null;
    }

    private void aplicarCambiosPorCongestion() {
        for (Vehiculo v : vehiculos) {
            Vehiculo adelante = v.getCarril().getVehiculoAdelante(v);
            v.intentarCambiarCarrilPorCongestion(adelante, carrilVecino(v.getCarril()));
        }
    }

    private void aplicarEvasionBaches() {
        for (Vehiculo v : vehiculos) {
            for (Bache b : baches) {
                v.intentarEvadirBache(b, carrilVecino(v.getCarril()), DISTANCIA_DETECCION_BACHE);
            }
        }
    }

    private void procesarBaches() {
        for (Vehiculo v : vehiculos) {
            if (v.isAveriado()) continue;
            for (Bache b : baches) {
                String clave = System.identityHashCode(v) + "-" + System.identityHashCode(b);
                if (bachesYaAplicados.contains(clave)) continue;

                double distancia = Math.hypot(v.getX() - b.getX(), v.getY() - b.getY());
                if (distancia < 10) {
                    bachesYaAplicados.add(clave);
                    v.reducirVelocidadPorBache(b.getSeveridad());
                }
            }
        }
    }

    // vuelve a usar Area (forma real, rotada), en vez del bounding box aproximado
    private void detectarColisiones() {
        for (int i = 0; i < vehiculos.size(); i++) {
            for (int j = i + 1; j < vehiculos.size(); j++) {
                Vehiculo a = vehiculos.get(i);
                Vehiculo b = vehiculos.get(j);
                if (a.getCarril() != b.getCarril()) continue;

                java.awt.geom.Area areaA = new java.awt.geom.Area(a.getHitbox());
                areaA.intersect(new java.awt.geom.Area(b.getHitbox()));
                if (!areaA.isEmpty()) {
                    manejarColision(a, b);
                }
            }
        }
    }

    // Red de seguridad especifica para el "se atraviesan en el cruce": detectarColisiones()
    // de arriba solo compara vehiculos del MISMO carril, asi que dos vehiculos que estan
    // cruzando (animacion girarHaciaVia) por carriles distintos de la misma interseccion al
    // mismo tiempo nunca se comparaban entre si y podian pasar uno a traves del otro. Esto
    // se restringe a pares donde AMBOS estan cruzando (cruzandoInterseccion) para no afectar
    // el cambio de carril normal ni el seguimiento vehiculo-a-vehiculo, que ya estan cubiertos.
    private void detectarColisionesEnCruce(double deltaTime) {
        for (int i = 0; i < vehiculos.size(); i++) {
            Vehiculo a = vehiculos.get(i);
            if (!a.isCruzandoInterseccion()) continue;

            for (int j = i + 1; j < vehiculos.size(); j++) {
                Vehiculo b = vehiculos.get(j);
                if (!b.isCruzandoInterseccion()) continue;
                if (a.getCarril() == b.getCarril()) continue; // ya cubierto por detectarColisiones()

                java.awt.geom.Area areaA = new java.awt.geom.Area(a.getHitbox());
                areaA.intersect(new java.awt.geom.Area(b.getHitbox()));
                if (areaA.isEmpty()) continue;

                // sus caminos se tocan: el que entro despues (menos progreso) cede el paso
                // congelandose este tick, en vez de seguir atravesando al que iba adelante.
                Vehiculo cede = a.getProgresoInterseccion() <= b.getProgresoInterseccion() ? a : b;
                cede.retrocederCruce(deltaTime);
            }
        }
    }

    private void manejarColision(Vehiculo a, Vehiculo b) {
        a.frenarEnSeco();
        b.frenarEnSeco();
        separarVehiculos(a, b);
    }

    private void separarVehiculos(Vehiculo a, Vehiculo b) {
        double[] dir = a.getCarril().getDireccion();
        double proyeccion = (b.getX() - a.getX()) * dir[0] + (b.getY() - a.getY()) * dir[1];
        Vehiculo atras = proyeccion >= 0 ? a : b;
        Vehiculo adelante = proyeccion >= 0 ? b : a;

        double distanciaActual = Math.hypot(b.getX() - a.getX(), b.getY() - a.getY());
        double distanciaMinima = adelante.getLargo() / 2 + atras.getLargo() / 2 + 2;
        double faltante = distanciaMinima - distanciaActual;

        if (faltante > 0) {
            atras.retroceder(dir, faltante);
        }
    }

    public List<Vehiculo> getVehiculos() {
        return vehiculos;
    }
}