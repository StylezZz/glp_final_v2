package pucp.edu.pe.glp_final.algorithm;

import org.springframework.cglib.core.Local;
import pucp.edu.pe.glp_final.models.Bloqueo;
import pucp.edu.pe.glp_final.models.Camion;
import pucp.edu.pe.glp_final.models.Pedido;
import pucp.edu.pe.glp_final.models.Nodo;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PlanificadorRuta {

    public void encontrarCamino(
            Mapa gridGraph,
            NodoMapa nodoInicial,
            NodoMapa objetivo,
            List<Bloqueo> bloqueos,
            int anio,
            int mes,
            int dia,
            int hora,
            int minuto,
            Camion vehiculo,
            Pedido pedido,
            int tipoSimulacion,
            LocalDateTime fechaBaseSimulacion
    ) {
        PriorityQueue<NodoMapa> colaPrioridad = new PriorityQueue<>(
                Comparator.comparingDouble(n -> n.getCostoTotal()));
        Map<NodoMapa, Double> costoRealAcumulado = new HashMap<>();
        Set<NodoMapa> nodosVisitados = new HashSet<>();

        colaPrioridad.add(nodoInicial);
        costoRealAcumulado.put(nodoInicial, 0.0);

        while (!colaPrioridad.isEmpty()) {
            NodoMapa actual = colaPrioridad.poll();

            if (actual.getX() == objetivo.getX() && actual.getY() == objetivo.getY()) {
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
                        nodoAnterior.setTiempoFin(vehiculo.getUbicacionActual().getTiempoFin() + 1);
                    }
                    nodoAnterior.setAnio(anio);
                    nodoAnterior.setMes(mes);
                    vehiculo.posicionar(nodoAnterior);

                    pedido.setIsbloqueo(false);
                }
                break;
            }

            nodosVisitados.add(actual);
            if (tipoSimulacion == 1) {
                actual.setTiempoFin(actual.getTiempoInicio() + 1);
            } else {
                actual.setTiempoFin(actual.getTiempoInicio() + 1);
            }
            for (NodoMapa vecino : obtenerNodosVecinos(actual, bloqueos, anio, mes, dia, hora, minuto, gridGraph, objetivo, pedido, fechaBaseSimulacion)) {

                if (nodosVisitados.contains(vecino)) continue;
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
                    vehiculo.asignar(pedido);
                    NodoMapa ruta = new NodoMapa(caminoVehiculo.getId(), caminoVehiculo.getX(),
                            caminoVehiculo.getY(),
                            caminoVehiculo.isEsAlmacen());
                    if (i == 0) {
                        ruta.setTiempoInicio(caminoVehiculo.getTiempoInicio());
                        ruta.setTiempoFin(caminoVehiculo.getTiempoInicio());
                    } else {
                        ruta.setTiempoInicio(caminoVehiculo.getTiempoInicio());
                        if (tipoSimulacion == 1) {
                            ruta.setTiempoFin(caminoVehiculo.getTiempoInicio() + 1);
                        } else {
                            ruta.setTiempoFin(caminoVehiculo.getTiempoInicio() + 1);
                        }
                    }
                    ruta.setPedido(pedido);
                    ruta.setEsPedido(true);
                    ruta.setAnio(anio);
                    ruta.setMes(mes);
                    vehiculo.posicionar(ruta);
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
            vehiculo.posicionar(ruta);
            i++;
        }
    }

    private double calcularHeuristica(NodoMapa desde, NodoMapa hasta) {
        return Math.sqrt(Math.pow(hasta.getX() - desde.getX(), 2) + Math.pow(hasta.getY() - desde.getY(), 2));
    }

    public List<NodoMapa> obtenerNodosVecinos(
            NodoMapa posicionActual,
            List<Bloqueo> bloqueos,
            int anio,
            int mes,
            int dia,
            int hora,
            int minuto,
            Mapa mapa,
            NodoMapa objetivo,
            Pedido pedido,
            LocalDateTime fechaBaseSimulacion
    ) {
        List<NodoMapa> vecinos = new ArrayList<>();
        int x = posicionActual.getX();
        int y = posicionActual.getY();
        double arriveTime = posicionActual.getTiempoInicio() + 2.4;

        // ✅ NUEVA LÓGICA: Usar fechaBaseSimulacion si está disponible
        Calendar calendar = Calendar.getInstance();
        if (fechaBaseSimulacion != null) {
            LocalDateTime fechaEsperada = LocalDateTime.of(anio, mes, dia, hora, minuto);
            long tiempoEsperado = ChronoUnit.MINUTES.between(fechaBaseSimulacion, fechaEsperada);

            // Si arriveTime está muy lejos de tiempoEsperado, hay problema de escala
            if (Math.abs(arriveTime - tiempoEsperado) > 1440) { // Más de 1 día de diferencia
                // Usar tiempo esperado en lugar del arriveTime problemático
                arriveTime = tiempoEsperado + 2.4;
            }

            LocalDateTime fechaCalculada = fechaBaseSimulacion.plusMinutes((long) arriveTime);
            calendar.set(Calendar.YEAR, fechaCalculada.getYear());
            calendar.set(Calendar.MONTH, fechaCalculada.getMonthValue() - 1);
            calendar.set(Calendar.DAY_OF_MONTH, fechaCalculada.getDayOfMonth());
            calendar.set(Calendar.HOUR_OF_DAY, fechaCalculada.getHour());
            calendar.set(Calendar.MINUTE, fechaCalculada.getMinute());
        } else {
            // ✅ FALLBACK al método actual con ajuste de mes
            int diaCalculado = (int) arriveTime / 1440;
            int mesAjustado = mes;
            int anioAjustado = anio;

            while (diaCalculado > getDaysInMonth(anioAjustado, mesAjustado)) {
                diaCalculado -= getDaysInMonth(anioAjustado, mesAjustado);
                mesAjustado++;
                if (mesAjustado > 12) {
                    mesAjustado = 1;
                    anioAjustado++;
                }
            }

            calendar.set(Calendar.YEAR, anioAjustado);
            calendar.set(Calendar.MONTH, mesAjustado - 1);
            calendar.set(Calendar.DAY_OF_MONTH, diaCalculado);
            calendar.set(Calendar.HOUR_OF_DAY, (int) (arriveTime % 1440) / 60);
            calendar.set(Calendar.MINUTE, (int) arriveTime % 60);
        }

        List<Bloqueo> bloqueoFuturo = Bloqueo.obtenerBloqueosActivos(bloqueos, calendar);
        List<Nodo> nodosBloqueados = Bloqueo.NodosBloqueados(bloqueoFuturo);
        if (x > 0 && x <= 70) {
            vecinos.add(mapa.getMapa()[x - 1][y]);
        }
        if (x >= 0 && x < 70) {
            vecinos.add(mapa.getMapa()[x + 1][y]);
        }
        if (y > 0 && y <= 50) {
            vecinos.add(mapa.getMapa()[x][y - 1]);
        }
        if (y >= 0 && y < 50) {
            vecinos.add(mapa.getMapa()[x][y + 1]);
        }

        List<NodoMapa> vecinosItera = new ArrayList<>(vecinos);
        for (Nodo nodo : nodosBloqueados) {
            for (NodoMapa vecino : vecinosItera) {
                if (nodo.getX() == objetivo.getX() && nodo.getY() == objetivo.getY()) {
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

    private int getDaysInMonth(int year, int month) {
        return java.time.YearMonth.of(year, month).lengthOfMonth();
    }
}