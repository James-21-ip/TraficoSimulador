package modelo;

public class Entrada {
    private Via via;
    private double lambda;

    public Entrada(Via via, double lambda) { 
        this.via = via; 
        this.lambda = lambda; 
    }

    public Via getVia() { return via; }
    public double getLambda() { return lambda; }
}