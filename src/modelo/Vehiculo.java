package modelo;

    public abstract class Vehiculo {

        protected double x;
        protected double y;
        protected double velocidad;
        protected double velocidadMaxima;
        protected double aceleracion;
        protected double ancho;
        protected double largo;
        protected Carril carril;
        protected boolean estaAveriado;
        protected double tiempoAveriaRestante;
        private double tiempoDesdeUltimoCambio = 999;
    private static final double COOLDOWN_CAMBIO_CARRIL = 2.0;
    private static final double COOLDOWN_EVASION_BACHE = 0.5;

        private boolean cambiandoCarril = false;
        private double lateralRestante;
        private double[] perpendicularCambio;

        private double anguloBase = 0;   // orientacion real segun la via/carril actual
        private double inclinacion = 0;  // offset temporal por el "lean" al cambiar de carril
        private boolean decisionGiroTomada = false; // evita tirar el dado del giro en cada tick

        private static final double VELOCIDAD_CAMBIO_LATERAL = 40;
        private static final double INCLINACION_MAX = 15;

        // --- cruce animado de intersecciones (girarHaciaVia ya no telepea) ---
        private boolean cruzandoInterseccion = false;
        private double[] origenInterseccion;
        private double[] destinoInterseccion;
        private double tiempoInterseccion;
        private double duracionInterseccion;
        private static final double VELOCIDAD_MINIMA_CRUCE = 25; // que el giro se vea fluido aunque el auto venga casi detenido

        // margen de seguridad: nunca pisar la linea de parada / cebra si hay que frenar
      

        public Vehiculo(double x, double y, Carril carril) {
            this.x = x;
            this.y = y;
            this.carril = carril;
            this.velocidad = 0;
            this.estaAveriado = false;
            actualizarAnguloBase();
        }

        protected abstract void definirCaracteristicas();

        public void mover(Vehiculo vehiculoAdelante, double deltaTime) {
    mover(vehiculoAdelante, null, deltaTime);
}

public void mover(Vehiculo vehiculoAdelante, Double distanciaHastaParada, double deltaTime) {
    tiempoDesdeUltimoCambio += deltaTime;

    if (cruzandoInterseccion) {
        avanzarCruceInterseccion(deltaTime);
        return;
    }

    actualizarAnguloBase(); // aunque este averiado, sigue "mirando" hacia donde iba
    if (estaAveriado) {
        tiempoAveriaRestante -= deltaTime;
        if (tiempoAveriaRestante <= 0) {
            estaAveriado = false;
        }
        return;
    }

    if (cambiandoCarril) {
        avanzarCambioCarril(deltaTime);
    }

    double margenMinimo = 10;

    double distanciaSeguraVehiculo = velocidad * 1.5 + margenMinimo;
    if (vehiculoAdelante != null) {
        distanciaSeguraVehiculo += largo / 2 + vehiculoAdelante.largo / 2;
    }
    double distanciaLibreVehiculo = (vehiculoAdelante != null)
            ? distanciaHacia(vehiculoAdelante)
            : Double.MAX_VALUE;

    // --- frenado ante semaforos / paradas obligatorias ---
    // Idea: mientras mas rapido va el vehiculo, mas lejos de la linea empieza a frenar
    // (colchon de seguridad proporcional a la velocidad) y, si aun asi se quedo corto,
    // frena con mayor intensidad cuanto mas cerca esta de la linea de parada.
    double desaceleracionConfort = aceleracion * 2;   // frenado normal, suave
    double desaceleracionMaxima = aceleracion * 5;    // frenado fuerte, solo si el margen ya se acorto mucho
    double colchonPorVelocidad = velocidad * 0.4;     // a mas velocidad, empieza a frenar antes

    double distanciaFrenadoFisica = (desaceleracionConfort > 0)
            ? (velocidad * velocidad) / (2 * desaceleracionConfort)
            : 0;
    double distanciaSeguraParada = distanciaFrenadoFisica + margenMinimo + colchonPorVelocidad + largo / 2;

    double distanciaLibreParada = (distanciaHastaParada != null)
            ? distanciaHastaParada
            : Double.MAX_VALUE;

    boolean debeFrenarPorVehiculo = distanciaLibreVehiculo < distanciaSeguraVehiculo;
    boolean debeFrenarPorParada = distanciaLibreParada < distanciaSeguraParada;
    boolean debeFrenar = debeFrenarPorVehiculo || debeFrenarPorParada;

    double desaceleracionAplicada = desaceleracionConfort;
    if (debeFrenarPorParada) {
        // desaceleracion minima necesaria para detenerse justo en la linea con lo que queda
        double distanciaDisponible = Math.max(distanciaLibreParada - margenMinimo - largo / 2, 0.5);
        double desaceleracionRequerida = (velocidad * velocidad) / (2 * distanciaDisponible);
        // nunca menos que el frenado de confort, ni mas que el frenado maximo permitido
        desaceleracionAplicada = Math.max(desaceleracionConfort,
                Math.min(desaceleracionRequerida, desaceleracionMaxima));
    }

    if (debeFrenar) {
        velocidad = Math.max(0, velocidad - desaceleracionAplicada * deltaTime);
    } else if (velocidad < velocidadMaxima) {
        velocidad = Math.min(velocidadMaxima, velocidad + aceleracion * deltaTime);
    }

    double[] direccion = carril.getDireccion();
    double avanceTick = velocidad * deltaTime;

    // Salvaguarda dura e independiente del calculo de frenado de arriba: si hay una
    // parada obligatoria (semaforo en rojo/amarillo o puente ocupado), el vehiculo
    // jamas debe quedar con el FRENTE a menos de "margenMinimo" unidades de esa
    // linea en este tick, sin importar que tan grande sea el deltaTime (ej.
    // simulacion a velocidad x5) o que tan justo haya calculado el frenado.
    // Por eso se resta tambien largo/2: distanciaHastaParada esta medida desde el
    // CENTRO del vehiculo, pero lo que no debe cruzar la linea es su frente.
    if (distanciaHastaParada != null) {
        double limite = Math.max(0, distanciaHastaParada - margenMinimo - largo / 2);
        if (avanceTick > limite) {
            avanceTick = limite;
            velocidad = 0;
        }
    }

    x += direccion[0] * avanceTick;
    y += direccion[1] * avanceTick;
}

        /** Avanza la animacion de cruce de interseccion (ver girarHaciaVia): interpola en linea recta
         * desde donde el vehiculo entro al cruce hasta el punto de inicio de su nuevo carril. */
        private void avanzarCruceInterseccion(double deltaTime) {
            tiempoInterseccion += deltaTime;
            double t = (duracionInterseccion > 0) ? Math.min(1.0, tiempoInterseccion / duracionInterseccion) : 1.0;

            double dx = destinoInterseccion[0] - origenInterseccion[0];
            double dy = destinoInterseccion[1] - origenInterseccion[1];
            x = origenInterseccion[0] + dx * t;
            y = origenInterseccion[1] + dy * t;
            if (dx != 0 || dy != 0) {
                anguloBase = Math.toDegrees(Math.atan2(dy, dx));
            }

            if (t >= 1.0) {
                cruzandoInterseccion = false;
            }
        }

        public boolean isCruzandoInterseccion() { return cruzandoInterseccion; }

        private void actualizarAnguloBase() {
            double[] dir = carril.getDireccion();
            anguloBase = Math.toDegrees(Math.atan2(dir[1], dir[0]));
        }

        protected double distanciaHacia(Vehiculo otro) {
            return Math.hypot(otro.x - this.x, otro.y - this.y);
        }

        public void frenarEnSeco() {
            velocidad = 0;
        }

        public void averiar(double duracion) {
            estaAveriado = true;
            tiempoAveriaRestante = duracion;
            velocidad = 0;
        }

        public boolean isAveriado() {
            return estaAveriado;
        }

        public void reducirVelocidadPorBache(double severidad) {
            velocidad *= (1 - severidad);
        }

     public void intentarCambiarCarrilPorCongestion(Vehiculo adelante, Carril carrilVecino) {
        if (cambiandoCarril || carrilVecino == null || tiempoDesdeUltimoCambio < COOLDOWN_CAMBIO_CARRIL) return;

        boolean atascado = velocidad < 5 && adelante != null
                && distanciaHacia(adelante) < (largo / 2 + adelante.largo / 2 + 15);

        if (!atascado) return;
        if (carrilVecino.espacioLibreCerca(x, y) > largo + 10) {
            iniciarCambioCarril(carrilVecino);
        }
    }

        public void intentarEvadirBache(Bache bache, Carril carrilVecino, double distanciaDeteccion) {
        if (cambiandoCarril || carrilVecino == null || tiempoDesdeUltimoCambio < COOLDOWN_EVASION_BACHE) return;

            if (!bacheEnCamino(bache, distanciaDeteccion)) return;
            if (carrilVecino.espacioLibreCerca(x, y) > largo + 10) {
                iniciarCambioCarril(carrilVecino);
            }
        }   

       private boolean bacheEnCamino(Bache b, double distanciaDeteccion) {
    double[] dir = carril.getDireccion();
    double[] perp = carril.getPerpendicular();
    java.util.List<java.awt.geom.Point2D> trazado = carril.getVia().getTrazado();
    java.awt.geom.Point2D inicio = trazado.get(0);

    // posicion lateral del bache respecto al eje central de la via (no del vehiculo)
    double dxBache = b.getX() - inicio.getX();
    double dyBache = b.getY() - inicio.getY();
    double offsetLateralBache = dxBache * perp[0] + dyBache * perp[1];

    // el bache solo "pertenece" a este carril si esta cerca de SU offset, no del de al lado
    boolean mismoCarril = Math.abs(offsetLateralBache - carril.getY()) < ancho / 2 + 3;
    if (!mismoCarril) return false;

    double dx = b.getX() - x;
    double dy = b.getY() - y;
    double proyeccion = dx * dir[0] + dy * dir[1];
    return proyeccion > 0 && proyeccion < distanciaDeteccion;
}
       private void iniciarCambioCarril(Carril nuevo) {
    double offsetActual = carril.getY();
    carril.quitarVehiculo(this);
    nuevo.agregarVehiculo(this);
    double offsetNuevo = nuevo.getY();
    carril = nuevo;

    perpendicularCambio = nuevo.getPerpendicular();
    lateralRestante = offsetNuevo - offsetActual;
    cambiandoCarril = true;
    tiempoDesdeUltimoCambio = 0; // <-- esta es la que faltaba
}

        private void avanzarCambioCarril(double deltaTime) {
            double paso = VELOCIDAD_CAMBIO_LATERAL * deltaTime;

            if (Math.abs(lateralRestante) <= paso) {
                x += perpendicularCambio[0] * lateralRestante;
                y += perpendicularCambio[1] * lateralRestante;
                lateralRestante = 0;
                cambiandoCarril = false;
                inclinacion = 0;
            } else {
                double dir = Math.signum(lateralRestante);
                x += perpendicularCambio[0] * dir * paso;
                y += perpendicularCambio[1] * dir * paso;
                lateralRestante -= dir * paso;
                inclinacion = dir * INCLINACION_MAX;
            }
        }

        /** Salta a otra vía (giro en una intersección). Instantáneo: aparece en el punto de entrada del carril nuevo, ya orientado hacia su nueva dirección. */
        public void girarHaciaVia(Carril nuevoCarril) {
            carril.quitarVehiculo(this);
            nuevoCarril.agregarVehiculo(this);
            carril = nuevoCarril;

            double[] punto = nuevoCarril.getPuntoInicio();
            x = punto[0];
            y = punto[1];

            actualizarAnguloBase();
            inclinacion = 0;
            cambiandoCarril = false;
            decisionGiroTomada = false; // en la via nueva, todavia no decidio nada
        }

        public boolean isDecisionGiroTomada() { return decisionGiroTomada; }
        public void marcarDecisionGiroTomada() { decisionGiroTomada = true; }

        public void retroceder(double[] direccion, double distancia) {
            x -= direccion[0] * distancia;
            y -= direccion[1] * distancia;
        }

        /** Hitbox rotado segun la orientacion REAL del vehiculo (via + cambio de carril en curso), no solo horizontal. */
        public java.awt.Shape getHitbox() {
            java.awt.geom.Rectangle2D rect =
                    new java.awt.geom.Rectangle2D.Double(x - largo / 2, y - ancho / 2, largo, ancho);
            double anguloTotal = getAngulo();
            if (anguloTotal == 0) return rect;
            java.awt.geom.AffineTransform t =
                    java.awt.geom.AffineTransform.getRotateInstance(Math.toRadians(anguloTotal), x, y);
            return t.createTransformedShape(rect);
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getLargo() { return largo; }
        public double getAncho() { return ancho; }
        public double getVelocidad() { return velocidad; }
        public double getAngulo() { return anguloBase + inclinacion; }
        public Carril getCarril() { return carril; }
    }