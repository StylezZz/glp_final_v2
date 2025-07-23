package pucp.edu.pe.glp_final.algorithm;

import lombok.Getter;
import lombok.Setter;
import pucp.edu.pe.glp_final.models.Camion;
import pucp.edu.pe.glp_final.models.Pedido;

@Getter
@Setter
public class NodoMapa {
    private int id;

    private Camion camion;
    private Pedido pedido;
    private NodoMapa nodoPrevio;

    private int x;
    private int y;
    private int xPrevio;
    private int yPrevio;

    private int anio;
    private int mes;
    private double tiempoInicio;
    private double tiempoFin;
    private boolean esAlmacen;
    private boolean esRuta;
    private boolean esPedido;
    private double costoTotal;

    public NodoMapa(
            int id,
            int x,
            int y,
            boolean esAlmacen
    ) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.esAlmacen = esAlmacen;
        this.tiempoInicio = 0;
        this.tiempoFin = 0;
    }

    public double calcularDistancia(NodoMapa nodoMapa) {
        return Math.sqrt(Math.pow(this.x - nodoMapa.getX(), 2) + Math.pow(this.y - nodoMapa.getY(), 2));
    }
}
