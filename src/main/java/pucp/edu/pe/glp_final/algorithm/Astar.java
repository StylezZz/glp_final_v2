package pucp.edu.pe.glp_final.algorithm;

import pucp.edu.pe.glp_final.models.Bloqueo;
import pucp.edu.pe.glp_final.models.Camion;
import pucp.edu.pe.glp_final.models.Nodo;
import pucp.edu.pe.glp_final.models.Pedido;

import java.util.*;


public class Astar {
    // Encuentra el camino optimo entre un nodo inicial y un objetivo
    public void encontrarCamino(Mapa gridGraph, NodePosition nodoInicial, NodePosition objetivo,
                                List<Bloqueo> bloqueos, int anio, int mes, Camion vehiculo, Pedido pedido,
                                boolean inicio, int tipoSimulacion) {
        // Estructura de datos para almacenar los nodos a explorar
        PriorityQueue<NodePosition> colaPrioridad = new PriorityQueue<>(
                Comparator.comparingDouble(n -> n.getCostoTotal()));
        Map<NodePosition, Double> costoRealAcumulado = new HashMap<>();
        Set<NodePosition> nodosVisitados = new HashSet<>();

        // Inicializar busqueda
        colaPrioridad.add(nodoInicial);
        costoRealAcumulado.put(nodoInicial, 0.0);
        // Bucle principal de búsqueda
        while (!colaPrioridad.isEmpty()) {
            NodePosition actual = colaPrioridad.poll();
            // Verificar si se alcanzó el objetivo
            if (actual.getX() == objetivo.getX() && actual.getY() == objetivo.getY()) {
                if (pedido != null && actual.getPedidoRuta() == pedido) {
                    actual.setPedidoRuta(pedido);
                    actual.getPedidoRuta().setEntregado(true);
                    reconstruirCamino(actual, vehiculo, pedido, anio, mes, tipoSimulacion);

                    break;
                }
                // Reconstruir camino estándar
                reconstruirCamino(actual, vehiculo, pedido, anio, mes, tipoSimulacion);
                // Manejar pedidos con bloqueos especiales
                if (pedido != null && pedido.isIsbloqueo()) {
                    NodePosition nodoAnterior = new NodePosition(0, actual.getAntecesor().getX(),
                            actual.getAntecesor().getY(), false);
                    nodoAnterior.setAntecesor(actual);
                    nodoAnterior.setStartTime(vehiculo.getUbicacionActual().getArriveTime());
                    // Configurar tiempo según tipo de simulación
                    if (tipoSimulacion == 1) {
                        nodoAnterior.setArriveTime(vehiculo.getUbicacionActual().getArriveTime() + 1);
                    } else {
                        nodoAnterior.setArriveTime(vehiculo.getUbicacionActual().getArriveTime() + 1.2);
                    }
                    nodoAnterior.setAnio(anio);
                    nodoAnterior.setMes(mes);
                    vehiculo.asignarPosicion(nodoAnterior);

                    pedido.setIsbloqueo(false);
                }
                break;
            }
            // Marcar nodo como visitado y expandir vecinos
            nodosVisitados.add(actual);
            // Configurar tiempo según tipo de simulación
            if (tipoSimulacion == 1) {
                actual.setArriveTime(actual.getStartTime() + 1);
            } else {
                actual.setArriveTime(actual.getStartTime() + 1.2);
            }
            // Explorar nodos vecinos
            for (NodePosition vecino : obtenerNodosVecinos(actual, bloqueos, anio, mes, gridGraph, objetivo, pedido)) {

                if (nodosVisitados.contains(vecino)) {
                    continue; // Saltar nodos ya procesados
                }
                // Calcular costo del camino hasta el vecino
                double nuevoCosto = costoRealAcumulado.get(actual) + calcularHeuristica(actual, vecino);

                if (!costoRealAcumulado.containsKey(vecino) || nuevoCosto < costoRealAcumulado.get(vecino)) {
                    // Actualizar información del vecino
                    costoRealAcumulado.put(vecino, nuevoCosto);
                    double heuristica = calcularHeuristica(vecino, objetivo);
                    double costoTotal = nuevoCosto + heuristica;
                    vecino.setCostoTotal(costoTotal);
                    vecino.setAntecesor(actual);
                    vecino.setStartTime(actual.getArriveTime());
                    colaPrioridad.add(vecino);
                }
            }
        }
        return;
    }
    // Reconstruye el camino óptimo desde el objetivo hasta el inicio siguiendo los antecesores
    private void reconstruirCamino(NodePosition objetivo, Camion vehiculo, Pedido pedido, int anio,
                                   int mes, int tipoSimulacion) {
        List<NodePosition> camino = new ArrayList<>();
        NodePosition actual = objetivo;
        // Reconstruir camino siguiendo antecesores
        while (actual != null) {
            camino.add(actual);
            actual = actual.getAntecesor();
        }

        Collections.reverse(camino); // Invertir para obtener orden correcto
        int i = 0;
        for (NodePosition caminoVehiculo : camino) {
            // Verificar si el nodo corresponde a un pedido específico
            if (pedido != null) {
                if (pedido.getPosX() == caminoVehiculo.getX() && pedido.getPosY() == caminoVehiculo.getY()) {
                    vehiculo.asignarPedido(pedido);
                    NodePosition ruta = new NodePosition(caminoVehiculo.getId(), caminoVehiculo.getX(),
                            caminoVehiculo.getY(),
                            caminoVehiculo.isDepot());
                    // Configurar tiempos del primer nodo
                    if (i == 0) {
                        ruta.setStartTime(caminoVehiculo.getStartTime());
                        ruta.setArriveTime(caminoVehiculo.getStartTime());
                    } else {
                        // Configurar tiempos según tipo de simulación
                        ruta.setStartTime(caminoVehiculo.getStartTime());
                        if(tipoSimulacion == 1){
                            ruta.setArriveTime(caminoVehiculo.getStartTime() + 1);
                        }else{
                            ruta.setArriveTime(caminoVehiculo.getStartTime() + 1.2);
                        }
                    }
                    ruta.setPedidoRuta(pedido);
                    ruta.setPedido(true);
                    ruta.setAnio(anio);
                    ruta.setMes(mes);
                    vehiculo.asignarPosicion(ruta);
                    break; // Terminar al encontrar el destino del pedido
                }
            }
            caminoVehiculo.setAnio(anio);
            caminoVehiculo.setMes(mes);
            NodePosition ruta = new NodePosition(caminoVehiculo.getId(), caminoVehiculo.getX(), caminoVehiculo.getY(),
                    caminoVehiculo.isDepot());
            ruta.setArriveTime(caminoVehiculo.getArriveTime());
            ruta.setStartTime(caminoVehiculo.getStartTime());
            ruta.setAnio(anio);
            ruta.setMes(mes);
            vehiculo.asignarPosicion(ruta);
            i++;
        }
    }

    private double calcularHeuristica(NodePosition desde, NodePosition hasta) {
        return Math.sqrt(Math.pow(hasta.getX() - desde.getX(), 2) + Math.pow(hasta.getY() - desde.getY(), 2));
    }

    // Obtiene los nodos vecinos de una posición actual, excluyendo bloqueos y el nodo anterior
    public List<NodePosition> obtenerNodosVecinos(NodePosition posicionActual, List<Bloqueo> bloqueos, int anio,
                                                  int mes, Mapa gridGraph, NodePosition objetivo, Pedido pedido) {
        List<NodePosition> vecinos = new ArrayList<>();
        int x = posicionActual.getX();
        int y = posicionActual.getY();
        // Calcular tiempo de llegada proyectado (2.4 minutos por movimiento)
        double arriveTime = posicionActual.getStartTime() + 2.4;
        // Configurar calendario para validación de bloqueos en tiempo futuro

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, anio);
        calendar.set(Calendar.MONTH, mes - 1); // Mes es 0-based en Calendar
        calendar.set(Calendar.DAY_OF_MONTH, (int) arriveTime / 1440);
        calendar.set(Calendar.HOUR_OF_DAY, (int) (arriveTime % 1440) / 60);
        calendar.set(Calendar.MINUTE, (int) arriveTime % 60);
        // Obtener bloqueos activos para el tiempo proyectado
        List<Bloqueo> bloqueoFuturo = Bloqueo.bloqueosActivos(bloqueos, calendar);
        List<Nodo> nodosBloqueados = Bloqueo.NodosBloqueados(bloqueoFuturo);

        // Generar candidatos de movimiento en las cuatro direcciones cardinales
        // Movimiento hacia la izquierda (oeste)
        if (x > 0 && x <= 70) {
            vecinos.add(gridGraph.getGrid()[x - 1][y]);
        }
        // Movimiento hacia la derecha (este)
        if (x >= 0 && x < 70) {
            vecinos.add(gridGraph.getGrid()[x + 1][y]);
        }
        // Movimiento hacia arriba (norte)
        if (y > 0 && y <= 50) {
            vecinos.add(gridGraph.getGrid()[x][y - 1]);
        }
        // Movimiento hacia abajo (sur)
        if (y >= 0 && y < 50) {
            vecinos.add(gridGraph.getGrid()[x][y + 1]);
        }
        // Filtrar vecinos que estén bloqueados en el tiempo futuro
        List<NodePosition> vecinosItera = new ArrayList<>(vecinos);
        for (Nodo nodo : nodosBloqueados) {
            for (NodePosition vecino : vecinosItera) {
                if (nodo.getX() == objetivo.getX() && nodo.getY() == objetivo.getY()) {
                    pedido.setIsbloqueo(true);
                    continue;
                } else {
                    if (vecino.getX() == nodo.getX() && vecino.getY() == nodo.getY()) {
                        vecinos.remove(vecino);
                    } else {
                        if (vecino.getX() == posicionActual.getNodoAnteriorX()
                                && vecino.getY() == posicionActual.getNodoAnteriorY()) {
                            vecinos.remove(vecino);
                        }
                    }
                }
            }
        }

        return vecinos;
    }
}