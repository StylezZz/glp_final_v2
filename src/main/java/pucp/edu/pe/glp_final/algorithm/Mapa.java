package pucp.edu.pe.glp_final.algorithm;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import pucp.edu.pe.glp_final.models.Almacen;
import pucp.edu.pe.glp_final.models.Nodo;

@Getter
@Setter
// Representación del área geográfica de operación para el sistema de ruteo de vehículos
public class Mapa {
    // Matriz que representa la grilla del mapa
    private NodePosition[][] grid;
    // Número de filas y columnas del mapa
    private int rows;
    private int columns;
    // Lista de almacenes en el mapa
    private List<Almacen> almacenes;

    // Constructor por defecto que inicializa el mapa con dimensiones predeterminadas
    public Mapa() {
        this.rows = 70;
        this.columns = 50;
        this.grid = new NodePosition[rows + 1][columns + 1];
        this.almacenes = new ArrayList<>();
    }

    // Incializa el mapa con dimensiones específicas y crendo nodos en todas las dimensiones
    public void initialGrid() {
        int id = 0;
        for (int i = 0; i <= rows; i++) {
            for (int j = 0; j <= columns; j++) {
                grid[i][j] = new NodePosition(id, i, j, false);
                // Verificar si esta posición corresponde a un almacén
                for (Almacen almacen : almacenes) {
                    if (almacen.getUbicacion().getX() == i && almacen.getUbicacion().getY() == j) {
                        grid[i][j].setDepot(true);
                    }
                }
                id++;
            }
        }
    }
    // Inicializa los almacenes en el mapa según el tipo de simulación
    public void inicializarAlmacenes(int tipoSimulacion) {
        // SIMULACIÓN DIARIA - Configuración centralizada
        // Almacén principal en posición estratégica central
        if (tipoSimulacion == 1) {
            Almacen central = new Almacen(1, "Central", Integer.MAX_VALUE, new Nodo(12, 8));
            Almacen norte = new Almacen(2, "Norte", 0, new Nodo(42, 42));
            Almacen este = new Almacen(3, "Este", 0, new Nodo(63, 3));

            almacenes.add(central);
            almacenes.add(norte);
            almacenes.add(este);
        }
        else {
            // SIMULACIÓN TIEMPO REAL - Red distribuida de almacenes
            // Almacén principal (central de operaciones)
            Almacen central = new Almacen(1, "Central", Integer.MAX_VALUE, new Nodo(12, 8));
            Almacen norte = new Almacen(2, "Norte", 160, new Nodo(42, 42));
            Almacen este = new Almacen(3, "Este", 160, new Nodo(63, 3));

            almacenes.add(central);
            almacenes.add(norte);
            almacenes.add(este);
        }
    }

    // Limpia los pedidos de todos los nodos del mapa
    public void limpiarPedidos() {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[1].length; j++) {
                grid[i][j].setPedido(false);
            }
        }
    }
    // Limpia las rutas de todos los nodos del mapa
    public void clearRutas() {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[1].length; j++) {
                grid[i][j].setRoute(false);
            }
        }
    }
}
