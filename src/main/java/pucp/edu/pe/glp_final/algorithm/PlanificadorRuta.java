package pucp.edu.pe.glp_final.algorithm;

import pucp.edu.pe.glp_final.models.Bloqueo;
import pucp.edu.pe.glp_final.models.Camion;
import pucp.edu.pe.glp_final.models.Pedido;
import pucp.edu.pe.glp_final.models.Nodo;

import java.util.*;

public class PlanificadorRuta {

    public void encontrarCamino(Mapa gridGraph, NodoMapa nodoInicial, NodoMapa objetivo,
                                List<Bloqueo> bloqueos, int anio, int mes, Camion vehiculo, Pedido pedido,
                                boolean inicio, int tipoSimulacion) {
        PriorityQueue<NodoMapa> colaPrioridad = new PriorityQueue<>(
                Comparator.comparingDouble(n -> n.getCostoTotal()));
        Map<NodoMapa, Double> costoRealAcumulado = new HashMap<>();
        Set<NodoMapa> nodosVisitados = new HashSet<>();

        colaPrioridad.add(nodoInicial);
        costoRealAcumulado.put(nodoInicial, 0.0);

        while (!colaPrioridad.isEmpty()) {
            NodoMapa actual = colaPrioridad.poll();

            if (actual.getX() == objetivo.getX() && actual.getY() == objetivo.getY()) {
                // Se encontró el camino, reconstruir y devolver

                if (pedido != null && actual.getPedido() == pedido) {
                    actual.setPedido(pedido);
                    actual.getPedido().setEntregado(true);
                    reconstruirCamino(actual, vehiculo, pedido, anio, mes, tipoSimulacion);

                    break;
                }
                reconstruirCamino(actual, vehiculo, pedido, anio, mes, tipoSimulacion);

                if (pedido != null && pedido.isIsbloqueo()) {
                    NodoMapa nodoAnterior = new NodoMapa(0, actual.getNodoPrevio().getX(),
                            actual.getNodoPrevio().getY(), false);
                    nodoAnterior.setNodoPrevio(actual);
                    nodoAnterior.setTiempoInicio(vehiculo.getUbicacionActual().getTiempoFin());
                    if (tipoSimulacion == 1) {
                        nodoAnterior.setTiempoFin(vehiculo.getUbicacionActual().getTiempoFin() + 1);
                    } else {
                        nodoAnterior.setTiempoFin(vehiculo.getUbicacionActual().getTiempoFin() + 1.2);
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
                actual.setTiempoFin(actual.getTiempoInicio() + 1);
            } else {
                actual.setTiempoFin(actual.getTiempoInicio() + 1.2);
            }
            for (NodoMapa vecino : obtenerNodosVecinos(actual, bloqueos, anio, mes, gridGraph, objetivo, pedido)) {

                if (nodosVisitados.contains(vecino)) {
                    continue; // Nodo ya visitado o bloqueado
                }

                double nuevoCosto = costoRealAcumulado.get(actual) + calcularHeuristica(actual, vecino);

                if (!costoRealAcumulado.containsKey(vecino) || nuevoCosto < costoRealAcumulado.get(vecino)) {

                    costoRealAcumulado.put(vecino, nuevoCosto);
                    double heuristica = calcularHeuristica(vecino, objetivo);
                    double costoTotal = nuevoCosto + heuristica;
                    vecino.setCostoTotal(costoTotal);
                    vecino.setNodoPrevio(actual);
                    vecino.setTiempoInicio(actual.getTiempoFin());
                    colaPrioridad.add(vecino);
                }
            }
        }

        // No se encontró un camino
        return;
    }

    private void reconstruirCamino(NodoMapa objetivo, Camion vehiculo, Pedido pedido, int anio,
                                   int mes, int tipoSimulacion) {
        List<NodoMapa> camino = new ArrayList<>();
        NodoMapa actual = objetivo;

        while (actual != null) {
            camino.add(actual);
            actual = actual.getNodoPrevio();
        }

        Collections.reverse(camino);
        int i = 0;
        for (NodoMapa caminoVehiculo : camino) {

            if (pedido != null) {
                if (pedido.getPosX() == caminoVehiculo.getX() && pedido.getPosY() == caminoVehiculo.getY()) {
                    vehiculo.asignarPedido(pedido);
                    NodoMapa ruta = new NodoMapa(caminoVehiculo.getId(), caminoVehiculo.getX(),
                            caminoVehiculo.getY(),
                            caminoVehiculo.isEsAlmacen());
                    if (i == 0) {
                        ruta.setTiempoInicio(caminoVehiculo.getTiempoInicio());
                        ruta.setTiempoFin(caminoVehiculo.getTiempoInicio());
                    } else {
                        ruta.setTiempoInicio(caminoVehiculo.getTiempoInicio());
                        if(tipoSimulacion == 1){
                            ruta.setTiempoFin(caminoVehiculo.getTiempoInicio() + 1);
                        }else{
                            ruta.setTiempoFin(caminoVehiculo.getTiempoInicio() + 1.2);
                        }
                    }
                    ruta.setPedido(pedido);
                    ruta.setEsPedido(true);
                    ruta.setAnio(anio);
                    ruta.setMes(mes);
                    vehiculo.asignarPosicion(ruta);
                    break;
                }
            }
            caminoVehiculo.setAnio(anio);
            caminoVehiculo.setMes(mes);
            NodoMapa ruta = new NodoMapa(caminoVehiculo.getId(), caminoVehiculo.getX(), caminoVehiculo.getY(),
                    caminoVehiculo.isEsAlmacen());
            ruta.setTiempoFin(caminoVehiculo.getTiempoFin());
            ruta.setTiempoInicio(caminoVehiculo.getTiempoInicio());
            ruta.setAnio(anio);
            ruta.setMes(mes);
            vehiculo.asignarPosicion(ruta);
            i++;
        }
    }

    private double calcularHeuristica(NodoMapa desde, NodoMapa hasta) {
        // Puedes usar la distancia euclidiana u otras heurísticas según tu problema
        return Math.sqrt(Math.pow(hasta.getX() - desde.getX(), 2) + Math.pow(hasta.getY() - desde.getY(), 2));
    }

    public List<NodoMapa> obtenerNodosVecinos(NodoMapa posicionActual, List<Bloqueo> bloqueos, int anio,
                                              int mes, Mapa gridGraph, NodoMapa objetivo, Pedido pedido) {
        List<NodoMapa> vecinos = new ArrayList<>();
        int x = posicionActual.getX();
        int y = posicionActual.getY();
        // 1440 + 1 -> 1441(proximo nodo)
        double arriveTime = posicionActual.getTiempoInicio() + 2.4;

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
            vecinos.add(gridGraph.getMapa()[x - 1][y]); // Nodo izquierda
        }
        if (x >= 0 && x < 70) {
            vecinos.add(gridGraph.getMapa()[x + 1][y]); // Nodo derecha
        }
        if (y > 0 && y <= 50) {
            vecinos.add(gridGraph.getMapa()[x][y - 1]); // Nodo abajo
        }
        if (y >= 0 && y < 50) {
            vecinos.add(gridGraph.getMapa()[x][y + 1]); // Nodo arriba
        }

        // List<NodoMapa> vecinosBloqueos = Bloqueo.NodosBloqueados(bloqueoFuturo);
        // vecinos.add(posicionActual); // Agregar el nodo actual a la lista de vecinos

        List<NodoMapa> vecinosItera = new ArrayList<>(vecinos);
        for (Nodo nodo : nodosBloqueados) {
            for (NodoMapa vecino : vecinosItera) {
                if (nodo.getX() == objetivo.getX() && nodo.getY() == objetivo.getY()) {
                    // Si el objetivo esta en el bloqueo, no se quita
                    pedido.setIsbloqueo(true);
                    continue;
                } else {
                    if (vecino.getX() == nodo.getX() && vecino.getY() == nodo.getY()) {
                        vecinos.remove(vecino);
                    } else {
                        if (vecino.getX() == posicionActual.getXPrevio()
                                && vecino.getY() == posicionActual.getYPrevio()) {
                            vecinos.remove(vecino);
                        }
                    }
                }
            }
        }

        return vecinos;
    }
}