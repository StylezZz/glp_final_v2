package pucp.edu.pe.glp_final.algorithm;

import lombok.Getter;
import lombok.Setter;
import pucp.edu.pe.glp_final.models.Camion;
import pucp.edu.pe.glp_final.models.Pedido;

@Getter
@Setter
// Representa la posición de un nodo en el mapa, incluyendo sus coordenadas y estado
public class NodePosition {

    private int id;
    private int x;
    private int y;
    private double startTime;
    private double arriveTime;
    private boolean isDepot;
    private boolean isRoute;
    private boolean isPedido;
    private Camion vehiculo;
    private Pedido pedidoRuta;
    private int anio;
    private int mes;

    private int nodoAnteriorX;
    private int nodoAnteriorY;
    private NodePosition antecesor;
    private double costoTotal;

    // Constructor principal que inicializa un nodo con sus coordenadas y tipo
    public NodePosition(int id ,int x, int y, boolean isDepot) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.isDepot = isDepot;
        startTime = 0;
        arriveTime = 0;
    }

    // Calcula la distancia entre este nodo y otro nodo
    public double distance(NodePosition nodePosition) {
        return Math.sqrt(Math.pow(this.x - nodePosition.getX(), 2) + Math.pow(this.y - nodePosition.getY(), 2));
    }


    public String imprimir(){
        return "id: "+ id +" x: "+ x +" y: "+ y ;
    }
}
