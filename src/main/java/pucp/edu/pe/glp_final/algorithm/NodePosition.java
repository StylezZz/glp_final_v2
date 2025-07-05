package pucp.edu.pe.glp_final.algorithm;

import lombok.Getter;
import lombok.Setter;
import pucp.edu.pe.glp_final.models.Camion;
import pucp.edu.pe.glp_final.models.Pedido;

@Getter
@Setter
public class NodePosition {

    private int id;
    private int x;
    private int y;
    private boolean isDepot;
    private boolean isRoute;
    private boolean isPedido;
    //private boolean isBloqueo;
    private double startTime;
    private double arriveTime;
    private Camion vehiculo;
    private Pedido pedidoRuta;
    private int anio;
    private int mes;

    private int nodoAnteriorX;
    private int nodoAnteriorY;

    private NodePosition antecesor;
    private double costoTotal;

    public NodePosition(int id ,int x, int y, boolean isDepot) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.isDepot = isDepot;
        startTime = 0;
        arriveTime = 0;
    }


    //Calcula la distancia entre dos nodos considerando que pueden haber tramos bloqueados, tendria que devolverme la distancia del nodo mas cercano

    public double distance(NodePosition nodePosition) {
        return Math.sqrt(Math.pow(this.x - nodePosition.getX(), 2) + Math.pow(this.y - nodePosition.getY(), 2));
    }


    public String imprimir(){
        return "id: "+ id +" x: "+ x +" y: "+ y ;
    }
}
