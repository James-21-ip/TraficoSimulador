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
            actualizarAnguloBase(); // aunque este averiado, sigue "mirando" hacia donde iba
            tiempoDesdeUltimoCambio += deltaTime;
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

            double distanciaSeguraParada = velocidad * 1.5 + margenMinimo + largo / 2;
            double distanciaLibreParada = (distanciaHastaParada != null)
                    ? distanciaHastaParada
                    : Double.MAX_VALUE;

            boolean debeFrenar = distanciaLibreVehiculo < distanciaSeguraVehiculo
                    || distanciaLibreParada < distanciaSeguraParada;

            if (debeFrenar) {
                velocidad = Math.max(0, velocidad - aceleracion * 2 * deltaTime);
            } else if (velocidad < velocidadMaxima) {
                velocidad = Math.min(velocidadMaxima, velocidad + aceleracion * deltaTime);
            }

            double[] direccion = carril.getDireccion();
            x += direccion[0] * velocidad * deltaTime;
            y += direccion[1] * velocidad * deltaTime;
        }

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