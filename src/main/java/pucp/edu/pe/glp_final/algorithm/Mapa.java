package pucp.edu.pe.glp_final.algorithm;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import pucp.edu.pe.glp_final.models.Almacen;
import pucp.edu.pe.glp_final.models.Nodo;

@Getter
@Setter
public class Mapa {

    private NodePosition[][] grid;
    private int rows;
    private int columns;
    private List<Almacen> almacenes;

    public Mapa() {
        this.rows = 70;
        this.columns = 50;
        this.grid = new NodePosition[rows + 1][columns + 1];
        this.almacenes = new ArrayList<>();
    }

    // Inicializar la grilla del mapa
    public void initialGrid() {
        int id = 0;
        for (int i = 0; i <= rows; i++) {
            for (int j = 0; j <= columns; j++) {
                grid[i][j] = new NodePosition(id, i, j, false);
                for (Almacen almacen : almacenes) {
                    if (almacen.getUbicacion().getX() == i && almacen.getUbicacion().getY() == j) {
                        grid[i][j].setDepot(true);
                    }
                }
                id++;
            }
        }
    }

    public void inicializarAlmacenes(int tipoSimulacion) {
        if (tipoSimulacion == 1) {
            Almacen central = new Almacen(1, "Central", Integer.MAX_VALUE, new Nodo(12, 8));
            Almacen norte = new Almacen(2, "Norte", 0, new Nodo(42, 42));
            Almacen este = new Almacen(3, "Este", 0, new Nodo(63, 3));

            almacenes.add(central);
            almacenes.add(norte);
            almacenes.add(este);
        }
        else {
            // Almacenes de la simulacion 2 con capacidad 160
            Almacen central = new Almacen(1, "Central", Integer.MAX_VALUE, new Nodo(12, 8));
            Almacen norte = new Almacen(2, "Norte", 160, new Nodo(42, 42));
            Almacen este = new Almacen(3, "Este", 160, new Nodo(63, 3));

            almacenes.add(central);
            almacenes.add(norte);
            almacenes.add(este);
        }

    }

    // Limpiar las rutas

    public void limpiarPedidos() {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[1].length; j++) {
                grid[i][j].setPedido(false);
            }
        }
    }

    public void clearRutas() {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[1].length; j++) {
                grid[i][j].setRoute(false);
            }
        }
    }
}
