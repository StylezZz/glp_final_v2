package pucp.edu.pe.glp_final.algorithm;

import java.util.ArrayList;
import java.util.List;

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
        this.mapa = new NodoMapa[rows + 1][columns + 1];
        this.almacenes = new ArrayList<>();
        this.columns = 50;
        this.rows = 70;

        cargarAlmacenes(tipoSimulacion);
        cargarNodos();
    }

    public void cargarAlmacenes(int tipoSimulacion) {
        int capNorteEste = (tipoSimulacion == 1) ? 0 : 160;
        almacenes.add(new Almacen(1, "Central", Integer.MAX_VALUE, new Nodo(12, 8)));
        almacenes.add(new Almacen(2, "Norte", capNorteEste, new Nodo(42, 42)));
        almacenes.add(new Almacen(3, "Este", capNorteEste, new Nodo(63, 3)));
    }

    public void cargarNodos() {
        int id = 0;
        for (int i = 0; i <= rows; i++) {
            for (int j = 0; j <= columns; j++) {
                mapa[i][j] = new NodoMapa(id, i, j, false);
                for (Almacen almacen : almacenes) {
                    if (almacen.getUbicacion().getX() == i && almacen.getUbicacion().getY() == j) {
                        mapa[i][j].setEsAlmacen(true);
                    }
                }
                id++;
            }
        }
    }

    public void removerPedidos() {
        for (int i = 0; i < mapa.length; i++)
            for (int j = 0; j < mapa[1].length; j++)
                mapa[i][j].setEsPedido(false);
    }
}
