package pucp.edu.pe.glp_final.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pucp.edu.pe.glp_final.models.Almacen;
import pucp.edu.pe.glp_final.models.Nodo;

@Getter
@Setter
@NoArgsConstructor
public class Mapa {

    private NodoMapa[][] mapa;
    private int rows;
    private int columns;
    private List<Almacen> almacenes;

    public Mapa(int tipoSimulacion) {
        this.almacenes = new ArrayList<>();
        cargarAlmacenes(tipoSimulacion);

        this.rows = 70;
        this.columns = 50;
        this.mapa = new NodoMapa[rows + 1][columns + 1];
        cargarNodos();
    }

    public void cargarAlmacenes(int tipoSimulacion) {
        int capNorteEste = (tipoSimulacion == 1) ? 0 : 160;
        almacenes.add(new Almacen(1, "Central", Integer.MAX_VALUE, new Nodo(12, 8)));
        almacenes.add(new Almacen(2, "Norte", capNorteEste, new Nodo(42, 42)));
        almacenes.add(new Almacen(3, "Este", capNorteEste, new Nodo(63, 3)));
    }

    public void cargarNodos() {
        // Ubicaciones de almacenes
        Map<String, Boolean> ubicacionesAlmacenes = new HashMap<>();
        for (Almacen almacen : almacenes) {
            String key = almacen.getUbicacion().getX() + "," + almacen.getUbicacion().getY();
            ubicacionesAlmacenes.put(key, true);
        }

        // Nodos del mapa
        int id = 0;
        for (int i = 0; i <= rows; i++) {
            for (int j = 0; j <= columns; j++) {
                String coordenada = i + "," + j;
                boolean esAlmacen = ubicacionesAlmacenes.containsKey(coordenada);
                mapa[i][j] = new NodoMapa(id++, i, j, esAlmacen);
            }
        }
    }

    public void removerPedidos() {
        for (NodoMapa[] nodoMapas : mapa)
            for (int j = 0; j < mapa[1].length; j++)
                nodoMapas[j].setEsPedido(false);
    }
}
