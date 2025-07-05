package pucp.edu.pe.glp_final.algorithm;

import pucp.edu.pe.glp_final.models.Bloqueo;
import pucp.edu.pe.glp_final.models.Camion;
import pucp.edu.pe.glp_final.models.Nodo;
import pucp.edu.pe.glp_final.models.Pedido;

import java.util.*;


public class Astar {

    public void encontrarCamino(Mapa gridGraph, NodePosition nodoInicial, NodePosition objetivo,
                                List<Bloqueo> bloqueos, int anio, int mes, Camion vehiculo, Pedido pedido,
                                boolean inicio, int tipoSimulacion) {
        PriorityQueue<NodePosition> colaPrioridad = new PriorityQueue<>(
                Comparator.comparingDouble(n -> n.getCostoTotal()));
        Map<NodePosition, Double> costoRealAcumulado = new HashMap<>();
        Set<NodePosition> nodosVisitados = new HashSet<>();

        colaPrioridad.add(nodoInicial);
        costoRealAcumulado.put(nodoInicial, 0.0);

        while (!colaPrioridad.isEmpty()) {
            NodePosition actual = colaPrioridad.poll();

            if (actual.getX() == objetivo.getX() && actual.getY() == objetivo.getY()) {
                // Se encontró el camino, reconstruir y devolver

                if (pedido != null && actual.getPedidoRuta() == pedido) {
                    actual.setPedidoRuta(pedido);
                    actual.getPedidoRuta().setEntregado(true);
                    reconstruirCamino(actual, vehiculo, pedido, anio, mes, tipoSimulacion);

                    break;
                }
                reconstruirCamino(actual, vehiculo, pedido, anio, mes, tipoSimulacion);

                if (pedido != null && pedido.isIsbloqueo()) {
                    NodePosition nodoAnterior = new NodePosition(0, actual.getAntecesor().getX(),
                            actual.getAntecesor().getY(), false);
                    nodoAnterior.setAntecesor(actual);
                    nodoAnterior.setStartTime(vehiculo.getUbicacionActual().getArriveTime());
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

            nodosVisitados.add(actual);
            if (tipoSimulacion == 1) {
                actual.setArriveTime(actual.getStartTime() + 1);
            } else {
                actual.setArriveTime(actual.getStartTime() + 1.2);
            }
            for (NodePosition vecino : obtenerNodosVecinos(actual, bloqueos, anio, mes, gridGraph, objetivo, pedido)) {

                if (nodosVisitados.contains(vecino)) {
                    continue; // Nodo ya visitado o bloqueado
                }

                double nuevoCosto = costoRealAcumulado.get(actual) + calcularHeuristica(actual, vecino);

                if (!costoRealAcumulado.containsKey(vecino) || nuevoCosto < costoRealAcumulado.get(vecino)) {

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

        // No se encontró un camino
        return;
    }

    private void reconstruirCamino(NodePosition objetivo, Camion vehiculo, Pedido pedido, int anio,
                                   int mes, int tipoSimulacion) {
        List<NodePosition> camino = new ArrayList<>();
        NodePosition actual = objetivo;

        while (actual != null) {
            camino.add(actual);
            actual = actual.getAntecesor();
        }

        Collections.reverse(camino);
        int i = 0;
        for (NodePosition caminoVehiculo : camino) {

            if (pedido != null) {
                if (pedido.getPosX() == caminoVehiculo.getX() && pedido.getPosY() == caminoVehiculo.getY()) {
                    vehiculo.asignarPedido(pedido);
                    NodePosition ruta = new NodePosition(caminoVehiculo.getId(), caminoVehiculo.getX(),
                            caminoVehiculo.getY(),
                            caminoVehiculo.isDepot());
                    if (i == 0) {
                        ruta.setStartTime(caminoVehiculo.getStartTime());
                        ruta.setArriveTime(caminoVehiculo.getStartTime());
                    } else {
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
                    break;
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
        // Puedes usar la distancia euclidiana u otras heurísticas según tu problema
        return Math.sqrt(Math.pow(hasta.getX() - desde.getX(), 2) + Math.pow(hasta.getY() - desde.getY(), 2));
    }

    public List<NodePosition> obtenerNodosVecinos(NodePosition posicionActual, List<Bloqueo> bloqueos, int anio,
                                                  int mes, Mapa gridGraph, NodePosition objetivo, Pedido pedido) {
        List<NodePosition> vecinos = new ArrayList<>();
        int x = posicionActual.getX();
        int y = posicionActual.getY();
        // 1440 + 1 -> 1441(proximo nodo)
        double arriveTime = posicionActual.getStartTime() + 2.4;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, anio);
        calendar.set(Calendar.MONTH, mes - 1); // Nota: El mes es 0-based (enero es 0)
        calendar.set(Calendar.DAY_OF_MONTH, (int) arriveTime / 1440);
        calendar.set(Calendar.HOUR_OF_DAY, (int) (arriveTime % 1440) / 60);
        calendar.set(Calendar.MINUTE, (int) arriveTime % 60);

        List<Bloqueo> bloqueoFuturo = Bloqueo.bloqueosActivos(bloqueos, calendar);

        /*
         * List<NodoBloqueado> posicionesBloqueadas =
         * obtenerNodosBloqueados(bloqueoFuturo);
         */
        List<Nodo> nodosBloqueados = Bloqueo.NodosBloqueados(bloqueoFuturo);

        // Comprobar los nodos vecinos en las cuatro direcciones: arriba, abajo,
        // izquierda y derecha
        // Asegúrate de verificar los límites de la cuadrícula para evitar
        // desbordamientos
        if (x > 0 && x <= 70) {
            vecinos.add(gridGraph.getGrid()[x - 1][y]); // Nodo izquierda
        }
        if (x >= 0 && x < 70) {
            vecinos.add(gridGraph.getGrid()[x + 1][y]); // Nodo derecha
        }
        if (y > 0 && y <= 50) {
            vecinos.add(gridGraph.getGrid()[x][y - 1]); // Nodo abajo
        }
        if (y >= 0 && y < 50) {
            vecinos.add(gridGraph.getGrid()[x][y + 1]); // Nodo arriba
        }

        // List<NodePosition> vecinosBloqueos = Bloqueo.NodosBloqueados(bloqueoFuturo);
        // vecinos.add(posicionActual); // Agregar el nodo actual a la lista de vecinos

        List<NodePosition> vecinosItera = new ArrayList<>(vecinos);
        for (Nodo nodo : nodosBloqueados) {
            for (NodePosition vecino : vecinosItera) {
                if (nodo.getX() == objetivo.getX() && nodo.getY() == objetivo.getY()) {
                    // Si el objetivo esta en el bloqueo, no se quita
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