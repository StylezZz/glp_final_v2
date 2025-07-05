package pucp.edu.pe.glp_final.algorithm;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Component;


import lombok.Getter;
import lombok.Setter;
import pucp.edu.pe.glp_final.models.*;

@Getter
@Setter
@Component
public class Genetico {

    private List<Pedido> pedidos;
    private List<Pedido> originalPedidos;
    private List<Pedido> pedidosNuevos;
    private List<Camion> camiones;
    private Mapa gridGraph;
    private double[][][][] matrizFeromonas;
    private double q0 = 0.3;
    private double Q = 1;
    private int cantidadPdidosInicial = 0;
    private boolean inicio;
    private List<Bloqueo> bloqueosActivos;
    private double MIN_DISTANCE = 0.00000000009;

    public Genetico() {
    }

    // Funcion para comenzar la simulacion
    public List<Camion> simulacionRuteo(int anio, int mes, int dia, int hora, int minuto, int minutosPorIteracion,
                                        List<Pedido> pedidosDia, List<Bloqueo> bloqueos, List<Pedido> pedidoOriginal, int primeraVez,
                                        double timer, int tipoSimulacion) {

        getPedidosDia(anio, mes, dia, hora, minuto, pedidosDia, minutosPorIteracion, pedidoOriginal, timer,
                tipoSimulacion);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, anio);
        calendar.set(Calendar.MONTH, mes - 1); // Nota: El mes es 0-based (enero es 0)
        calendar.set(Calendar.DAY_OF_MONTH, dia);
        calendar.set(Calendar.HOUR_OF_DAY, hora);
        calendar.set(Calendar.MINUTE, minuto);

        bloqueosActivos = Bloqueo.bloqueosActivos(bloqueos, calendar);
        run(anio, mes, dia, hora, minuto, bloqueos, primeraVez, timer, tipoSimulacion);

        validarTiempoRuta(anio, mes, dia, hora, minuto, minutosPorIteracion, tipoSimulacion);

        return camiones;

    }

    // Obtener pedidos de acuerdo a la fecha de simulacion
    public void getPedidosDia(int anio, int mes, int dia, int hora, int minuto, List<Pedido> pedidosDia,
                              int minutosPorIteracion, List<Pedido> pedidoOriginal, double timer, int tipoSimulacion) {

        this.pedidos = new ArrayList<>();

        LocalDateTime fecha = LocalDateTime.of(anio, mes, dia, hora, minuto);
        LocalDateTime fechaIteracion;
        if (tipoSimulacion == 1) {
            fechaIteracion = fecha;
        } else {
            fechaIteracion = fecha.plusMinutes(minutosPorIteracion);
        }
        for (Pedido pedido : pedidosDia) {

            if (pedido.getFechaDeRegistro().isBefore(fechaIteracion)
                    || pedido.getFechaDeRegistro().isEqual(fechaIteracion)) {

                this.pedidos.add(pedido);

            }
        }
        this.originalPedidos = new ArrayList<>();
        for (Pedido pedido : pedidoOriginal) {
            if (pedido.getFechaDeRegistro().isBefore(fechaIteracion)
                    || pedido.getFechaDeRegistro().isEqual(fechaIteracion)) {
                originalPedidos.add(pedido);
            }
        }
    }

    // Constructor de la clase Iaco con los camiones y el tipo de simulacion
    public Genetico(List<Camion> camiones, int tipoSimulacion) {
        this.camiones = camiones;
        this.pedidos = new ArrayList<>();
        gridGraph = new Mapa();
        gridGraph.inicializarAlmacenes(tipoSimulacion);
        gridGraph.initialGrid();
        this.inicio = true;
        this.bloqueosActivos = new ArrayList<>();
        initialPheromones();

    }

    // Metodo que coloque en prioridad los pedidos que no han sido entregados
    // Completamente y que su fecha de entrega sea menor a la fecha actual o menor a
    // 4 horas
    public void validarPedidosNoCompletados(int anio, int mes, int dia, int hora, int minuto) {

        LocalDateTime fechaActual = LocalDateTime.of(anio, mes, dia, hora, minuto);

        for (Pedido pedido : originalPedidos) {
            if (!pedido.isEntregadoCompleto()) {
                // Si la fecha de entrega es menor a la fecha actual o igual a la fecha actual, se coloca en prioridad
                if (pedido.getFechaEntrega().isBefore(fechaActual) || pedido.getFechaEntrega().isEqual(fechaActual)) {
                    for (Pedido pedido2 : pedidos) {
                        if (pedido.getId() == pedido2.getId()) {
                            pedido2.setPriodidad(-1);
                            pedido2.setEntregadoCompleto(false);
                            pedido2.setEntregado(false);
                        }
                    }
                }
            }
        }
    }

    // Validar el estado de los pedidos no entregados y maneja las rutas de los camiones
    public void validarPedidosNoEntregado(int anio, int mes, int dia, int hora, int minuto, double timer,
                                          int tipoSimulacion) {

        for (Camion camion : camiones) {

            int i = 0;
            if (camion.getRoute() == null) {
                continue;
            }
            boolean bandera = true;
            double distanciaRecorrida = 0.0;
            camion.setCargaAsignada(0);
            if (tipoSimulacion == 1) {
                for (NodePosition ubicacion : camion.getRoute()) {

                    if (mes == ubicacion.getMes() && anio == ubicacion.getAnio()) {
                        double startTime = timer;
                        if (Math.round(ubicacion.getStartTime()) < startTime) {
                            distanciaRecorrida += 1;
                            camion.setUbicacionActual(ubicacion);
                            camion.setDistanciaRecorrida(distanciaRecorrida);
                            camion.getUbicacionActual().setRoute(false);
                            if (ubicacion.isPedido()) {
                                if (ubicacion.getMes() <= mes && ubicacion.getAnio() <= anio) {
                                    ubicacion.getPedidoRuta().setEntregado(true);
                                    ubicacion.getPedidoRuta().setHoraDeInicio((int) ubicacion.getStartTime());
                                    ubicacion.getPedidoRuta().setTiempoLlegada((int) ubicacion.getArriveTime());
                                    camion.asignarPedido(ubicacion.getPedidoRuta());
                                }
                            }
                            if (ubicacion.isDepot()) {

                                for (Almacen deposito2 : gridGraph.getAlmacenes()) {
                                    // Si el timer esta en un rango de las 0 horas y 0 minutos se reinicia la
                                    // capacidad disponible

                                    if (hora == 0 && minuto >= 0) {
                                        deposito2.setCapacidadDisponible(deposito2.getCapacidad());
                                    }
                                    if (deposito2.getUbicacion().getX() == camion.getUbicacionActual().getX()
                                            && deposito2.getUbicacion().getY() == camion.getUbicacionActual().getY()) {
                                        if (deposito2.getUbicacion().getX() == 12 && deposito2.getUbicacion().getY() == 8) {
                                            camion.setCargaAsignada(0);
                                            camion.setGlpDisponible(camion.getCarga());
                                            camion.setDistanciaRecorrida(0.0);
                                        } else {
                                            if (deposito2.getCapacidadDisponible() - 2 <= 0) {
                                                camion.setCargaAsignada(0);
                                                camion.setGlpDisponible(camion.getCarga());
                                                camion.setDistanciaRecorrida(0.0);
                                                deposito2.setCapacidadDisponible(0);
                                            } else {
                                                camion.setCargaAsignada(0);
                                                camion.setDistanciaRecorrida(0.0);
                                                /*
                                                 * deposito2.setCapacidadDisponible(
                                                 * (int) deposito2.getCapacidadDisponible()
                                                 * - (int) camion.getCarga());
                                                 */
                                                camion.setGlpDisponible(camion.getCarga());

                                            }
                                        }
                                    }

                                    camion.setCargaAnterior(0);
                                }
                                ubicacion.setStartTime(startTime);
                                ubicacion.setArriveTime(startTime + 1);

                            }
                            // camion.getRoute().remove(ubicacion);

                        } else {
                            if (bandera) {
                                camion.setUbicacionActual(ubicacion);
                                bandera = false;
                            }
                            if (ubicacion.isPedido()) {
                                if (ubicacion.getMes() <= mes && ubicacion.getAnio() <= anio) {
                                    ubicacion.getPedidoRuta().setEntregado(false);
                                    ubicacion.getPedidoRuta().setAsignado(false);
                                    ubicacion.getPedidoRuta().setIdCamion(null);
                                }
                            }
                            if (ubicacion.isDepot()) {
                                if (Math.round(ubicacion.getStartTime()) > startTime) {
                                    if (i == 0) {
                                        camion.setUbicacionActual(ubicacion);
                                        /*
                                         * for (Deposito deposito2 : gridGraph.getDepositos()) {
                                         * if (hora == 0 && minuto == 0) {
                                         * deposito2.setCapacidadDisponible(deposito2.getCapacidad());
                                         * }
                                         * if (deposito2.getPositionX() == ubicacion.getX()
                                         * && deposito2.getPositionY() == ubicacion.getY()) {
                                         * if (deposito2.getPositionX() != 12 && deposito2.getPositionY() != 8) {
                                         *
                                         * camion.setCargaAsignada(0.0);
                                         * camion.setDistanciaRecorrida(0.0);
                                         *
                                         * deposito2.setCapacidadDisponible(
                                         * deposito2.getCapacidadDisponible()
                                         * + (int) camion.getGlpDisponible());
                                         *
                                         * }
                                         * }
                                         *
                                         * }
                                         */
                                        continue;
                                    }

                                    /*
                                     * for (Deposito deposito2 : gridGraph.getDepositos()) {
                                     * if (hora == 0 && minuto == 0) {
                                     * deposito2.setCapacidadDisponible(deposito2.getCapacidad());
                                     * }
                                     * if (deposito2.getPositionX() == ubicacion.getX()
                                     * && deposito2.getPositionY() == ubicacion.getY()) {
                                     * if (deposito2.getPositionX() != 12 && deposito2.getPositionY() != 8) {
                                     * deposito2.setCapacidadDisponible(
                                     * deposito2.getCapacidadDisponible()
                                     * + (int) camion.getGlpDisponible());
                                     *
                                     *
                                     * }
                                     * }
                                     *
                                     * }
                                     */
                                }
                            }
                        }

                        i++;
                    }

                    if (camion.getCargaAsignada() == 0.0) {
                        camion.setCargaAsignada(camion.getCargaAnterior());
                    }
                }
                i++;
            } else {
                for (NodePosition ubicacion : camion.getRoute()) {

                    if (mes == ubicacion.getMes() && anio == ubicacion.getAnio()) {
                        double startTime = timer;
                        if ((int) ubicacion.getArriveTime() <= startTime) {
                            distanciaRecorrida += 1;
                            camion.setUbicacionActual(ubicacion);
                            camion.setDistanciaRecorrida(distanciaRecorrida);
                            camion.getUbicacionActual().setRoute(false);
                            if (ubicacion.isPedido()) {
                                if (ubicacion.getMes() <= mes && ubicacion.getAnio() <= anio) {
                                    ubicacion.getPedidoRuta().setEntregado(true);
                                    ubicacion.getPedidoRuta().setHoraDeInicio((int) ubicacion.getStartTime());
                                    ubicacion.getPedidoRuta().setTiempoLlegada((int) ubicacion.getArriveTime());
                                    camion.asignarPedido(ubicacion.getPedidoRuta());
                                }
                            }
                            if (ubicacion.isDepot()) {

                                for (Almacen deposito2 : gridGraph.getAlmacenes()) {
                                    // Si el timer esta en un rango de las 0 horas y 0 minutos se reinicia la
                                    // capacidad disponible

                                    if (hora == 0 && minuto >= 0) {
                                        deposito2.setCapacidadDisponible(deposito2.getCapacidad());
                                    }
                                    if (deposito2.getUbicacion().getX() == camion.getUbicacionActual().getX()
                                            && deposito2.getUbicacion().getY() == camion.getUbicacionActual().getY()) {
                                        if (deposito2.getUbicacion().getX() == 12 && deposito2.getUbicacion().getY() == 8) {
                                            camion.setCargaAsignada(0);
                                            camion.setGlpDisponible(camion.getCarga());
                                            camion.setDistanciaRecorrida(0.0);
                                        } else {
                                            if (deposito2.getCapacidadDisponible() - 2 <= 0) {
                                                camion.setCargaAsignada(0);
                                                camion.setGlpDisponible(camion.getCarga());
                                                camion.setDistanciaRecorrida(0.0);
                                                deposito2.setCapacidadDisponible(0);
                                            } else {
                                                camion.setCargaAsignada(0);
                                                camion.setDistanciaRecorrida(0.0);
                                                /*
                                                 * deposito2.setCapacidadDisponible(
                                                 * (int) deposito2.getCapacidadDisponible()
                                                 * - (int) camion.getCarga());
                                                 */
                                                camion.setGlpDisponible(camion.getCarga());

                                            }
                                        }
                                    }

                                }
                                ubicacion.setStartTime(startTime);
                                ubicacion.setArriveTime(startTime + 1.2);

                            }

                        } else {
                            if (ubicacion.isPedido()) {
                                if (ubicacion.getMes() <= mes && ubicacion.getAnio() <= anio) {
                                    ubicacion.getPedidoRuta().setEntregado(false);
                                    ubicacion.getPedidoRuta().setAsignado(false);
                                    ubicacion.getPedidoRuta().setIdCamion(null);
                                }
                            }
                            if (ubicacion.isDepot()) {
                                if ((int) ubicacion.getArriveTime() > startTime) {
                                    if (i == 0) {
                                        camion.setUbicacionActual(ubicacion);
                                        /*
                                         * for (Deposito deposito2 : gridGraph.getDepositos()) {
                                         * if (hora == 0 && minuto == 0) {
                                         * deposito2.setCapacidadDisponible(deposito2.getCapacidad());
                                         * }
                                         * if (deposito2.getPositionX() == ubicacion.getX()
                                         * && deposito2.getPositionY() == ubicacion.getY()) {
                                         * if (deposito2.getPositionX() != 12 && deposito2.getPositionY() != 8) {
                                         *
                                         * camion.setCargaAsignada(0.0);
                                         * camion.setDistanciaRecorrida(0.0);
                                         *
                                         * deposito2.setCapacidadDisponible(
                                         * deposito2.getCapacidadDisponible()
                                         * + (int) camion.getGlpDisponible());
                                         *
                                         * }
                                         * }
                                         *
                                         * }
                                         */
                                        continue;
                                    }

                                    /*
                                     * for (Deposito deposito2 : gridGraph.getDepositos()) {
                                     * if (hora == 0 && minuto == 0) {
                                     * deposito2.setCapacidadDisponible(deposito2.getCapacidad());
                                     * }
                                     * if (deposito2.getPositionX() == ubicacion.getX()
                                     * && deposito2.getPositionY() == ubicacion.getY()) {
                                     * if (deposito2.getPositionX() != 12 && deposito2.getPositionY() != 8) {
                                     * deposito2.setCapacidadDisponible(
                                     * deposito2.getCapacidadDisponible()
                                     * + (int) camion.getGlpDisponible());
                                     *
                                     *
                                     * }
                                     * }
                                     *
                                     * }
                                     */
                                }
                            }
                        }

                        i++;
                    }

                }
                i++;
            }
        }

        Comparator<Pedido> comparadorPorTiempoDeLlegada = Comparator.comparing(Pedido::getFechaEntrega);
        originalPedidos.sort(comparadorPorTiempoDeLlegada);

    }

    // Gestionar las averias de los camiones
    // A star para encontrar el camino mas corto
    public void validarAverias(int anio, int mes, int dia, int hora, int minuto, List<Bloqueo> bloqueos,
                               int tipoSimulacion) {

        double arriveTime = dia * 1440 + hora * 60 + minuto;
        for (Camion camion : camiones) {
            double tiempoDetenido = camion.getTiempoDetenido();
            if ((camion.isDetenido()) && (tiempoDetenido <= arriveTime)) {
                camion.setDetenido(false);
            }

            if ((camion.getTipoAveria() == 2 || camion.getTipoAveria() == 3) && !camion.isDetenido()
                    && camion.isEnAveria()) {
                NodePosition nodoCentral = new NodePosition(0, 12, 8, true);
                nodoCentral.setStartTime(camion.getTiempoFinAveria());
                if (tipoSimulacion == 1) {
                    nodoCentral.setArriveTime(camion.getTiempoFinAveria() + 1);
                } else {
                    nodoCentral.setArriveTime(camion.getTiempoFinAveria() + 1.2);

                }
                nodoCentral.setMes(mes);
                nodoCentral.setAnio(anio);
                camion.setUbicacionActual(nodoCentral);
            }

            if (camion.isEnAveria() && camion.getTiempoFinAveria() <= arriveTime) {
                camion.setEnAveria(false);
            }
            if (camion.isEnAveria()) {
                double menorDistancia = Double.POSITIVE_INFINITY;
                Camion camionMenorDistancia = new Camion();
                camion.setRoute(new ArrayList<>());
                camion.setPedidosAsignados(new ArrayList<>());

                if (camion.getUbicacionActual() == null) {
                    NodePosition posicionDefault = new NodePosition(0, 12, 8, true);
                    posicionDefault.setAnio(anio);
                    posicionDefault.setMes(mes);
                    camion.setUbicacionActual(posicionDefault);
                }

                for (Camion camionRescate : camiones) {
                    if (camion.getCodigo().equals(camionRescate.getCodigo())) {
                        continue;
                    } else {
                        if (camionRescate.getUbicacionActual() == null) {
                            NodePosition posicionDefault = new NodePosition(0, 12, 8, true);
                            posicionDefault.setAnio(anio);
                            posicionDefault.setMes(mes);
                            camionRescate.setUbicacionActual(posicionDefault);
                        }

                        double distancia = camion.getUbicacionActual().distance(camionRescate.getUbicacionActual());
                        if (distancia < menorDistancia) {
                            menorDistancia = distancia;
                            camionMenorDistancia = camionRescate;
                        }
                    }
                }

                Astar aestar = new Astar();

                aestar.encontrarCamino(gridGraph, camionMenorDistancia.getUbicacionActual(),
                        camion.getUbicacionActual(), bloqueos, anio, mes, camionMenorDistancia, null, inicio, tipoSimulacion);
            }

        }

    }

    // Validar los pedidos entregados completos
    public void entregadosCompletos() {
        for (Pedido pedido : originalPedidos) {
            if (pedido.isEntregadoCompleto()) {
                for (Pedido pedido2 : pedidos) {
                    if (pedido.getId() == pedido2.getId()) {
                        pedido2.setEntregado(true);
                        pedido2.setEntregadoCompleto(true);
                    }
                }
            }
        }
    }

    // Ejecutar el algoritmo
    public void run(int anio, int mes, int dia, int hora, int minuto, List<Bloqueo> bloqueos, int primeraVez,
                    double timer, int tipoSimulacion) {

        // Limpiar los pedidos de la gráfica
        gridGraph.limpiarPedidos();

        validarPedidosNoEntregado(anio, mes, dia, hora, minuto, timer, tipoSimulacion);
        entregadosCompletos();

        clearRoute();

        if (tipoSimulacion == 2) {
            eliminarPedidosEntregadosYReorganizar();
        }

        validarAverias(anio, mes, dia, hora, minuto, bloqueos, tipoSimulacion);

        validarPedidosNoCompletados(anio, mes, dia, hora, minuto);

        LocalDateTime fechaActual = LocalDateTime.of(anio, mes, dia, hora, minuto);

        Comparator<Pedido> comparadorPorProximidad = Comparator
                .comparing(pedido -> Duration.between(fechaActual, pedido.getFechaEntrega()).abs());

        pedidos.sort(comparadorPorProximidad);
        // PlanificarPedidos en los camiones con ACO

        if (primeraVez == 0) {
            for (Camion camion : camiones) {
                camion.inicializarRuta();
                NodePosition posicionActual = camion.getUbicacionActual();
                if (posicionActual == null) {
                    posicionActual = new NodePosition(0, 12, 8, true);
                    posicionActual.setAnio(anio);
                    posicionActual.setMes(mes);
                    camion.setUbicacionActual(posicionActual);
                    gridGraph.getGrid()[12][8].setRoute(true);
                }
            }
        } else {
            for (Camion camion : camiones) {
                if (camion.getRoute().isEmpty()) {
                    camion.inicializarRuta();
                }
                if (!camion.getPedidosAsignados().isEmpty()) {
                    camion.setCargaAnterior(0);
                    for (Pedido pedidoAsignado : camion.getPedidosAsignados()) {
                        if (pedidoAsignado.isEntregado() && pedidoAsignado.isEntregadoCompleto()) {
                            camion.setCargaAnterior(camion.getCargaAnterior() + pedidoAsignado.getCantidadGLP());
                        }
                    }
                }
            }
        }
        if (tipoSimulacion == 1) {
            planificarPedidosDiaria(primeraVez, dia, hora, minuto, mes, anio, bloqueos, timer);
        } else {
            planificarPedidos(primeraVez, dia, hora, minuto, mes, anio, bloqueos, timer);
        }
        // planificarPedidos(primeraVez, dia, hora, minuto, mes, anio, bloqueos, timer);
        for (Camion camion : camiones) {
            if (camion.getRoute().isEmpty()) {
                continue;
            } else {
                for (NodePosition ubicacion : camion.getRoute()) {
                    if (ubicacion.isPedido()) {
                        ubicacion.getPedidoRuta().setEntregado(false);
                        ubicacion.getPedidoRuta().setEntregadoCompleto(false);
                    }
                }
            }
        }

        for (Camion camion : camiones) {
            if (camion.isEnAveria()) {
                continue;
            }
            if (camion.getRoute().isEmpty()) {
                camion.inicializarRuta();
            }
            NodePosition posicionActual = camion.getUbicacionActual();
            if (posicionActual == null) {
                posicionActual = new NodePosition(0, 12, 8, true);
                posicionActual.setAnio(anio);
                posicionActual.setMes(mes);
                posicionActual.setStartTime(timer);
                camion.setUbicacionActual(posicionActual);
                gridGraph.getGrid()[12][8].setRoute(true);
            }
            construirSolucion(camion, posicionActual, anio, mes, dia, hora, minuto, bloqueos, primeraVez, tipoSimulacion);
            volverAlmacen(camion, bloqueos, anio, mes, dia, hora, minuto, tipoSimulacion);
        }

    }

    public void eliminarPedidosEntregadosYReorganizar() {
        List<Camion> camionesConPedidos = new ArrayList<>();
        List<Camion> camionesSinPedidos = new ArrayList<>();

        // Separar vehículos con y sin pedidos asignados
        for (Camion camion : camiones) {
            if (camion.getPedidosAsignados() != null && !camion.getPedidosAsignados().isEmpty()) {
                camionesConPedidos.add(camion);
            } else {
                camionesSinPedidos.add(camion);
            }
        }

        // Limpiar la lista de pedidos entregados en vehículos con pedidos asignados
        for (Camion camion : camionesConPedidos) {
            List<Pedido> pedidosAsignados = camion.getPedidosAsignados();
            Iterator<Pedido> iterator = pedidosAsignados.iterator();

            while (iterator.hasNext()) {
                Pedido pedido = iterator.next();
                if (pedido.isEntregado()) {
                    iterator.remove();
                }
            }
        }

        // Reorganizar los vehículos (poner los que no tienen pedidos al final)
        camiones.clear();
        camiones.addAll(camionesConPedidos);
        camiones.addAll(camionesSinPedidos);
    }

    public void validarTiempoRuta(int anio, int mes, int dia, int hora, int minuto, int minutosPorIteracion,
                                  int tipoSimulacion) {

        double arriveTime = dia * 1440 + hora * 60 + minuto + minutosPorIteracion;
        for (Camion camion : camiones) {
            camion.setCargaAsignada(0);
            for (NodePosition posicion : camion.getRoute()) {
                if (anio == posicion.getAnio() && mes == posicion.getMes()) {
                    if ((int) posicion.getArriveTime() <= arriveTime) {
                        if (posicion.isPedido()) {
                            camion.setCargaAsignada(
                                    camion.getCargaAsignada() + posicion.getPedidoRuta().getCantidadGLP());
                            for (Pedido pedidoOriginal : originalPedidos) {
                                if (pedidoOriginal.getId() == posicion.getPedidoRuta().getId()) {
                                    posicion.getPedidoRuta().setEntregado(true);
                                    pedidoOriginal
                                            .setCantidadGLPAsignada(pedidoOriginal.getCantidadGLPAsignada() +
                                                    posicion.getPedidoRuta().getCantidadGLP());
                                    if (tipoSimulacion == 1) {
                                        if (pedidoOriginal.getCantidadGLP() <= pedidoOriginal
                                                .getCantidadGLPAsignada()) {
                                            posicion.getPedidoRuta().setEntregadoCompleto(true);
                                            pedidoOriginal.setEntregadoCompleto(true);
                                            pedidoOriginal.setEntregado(true);
                                        }
                                    } else {
                                        if (pedidoOriginal.getId() == posicion.getPedidoRuta().getId()) {
                                            posicion.getPedidoRuta().setEntregadoCompleto(true);
                                            pedidoOriginal.setEntregadoCompleto(true);
                                            pedidoOriginal.setEntregado(true);
                                        }
                                    }
                                }
                            }
                        }
                        if (posicion.isDepot()) {
                            for (Almacen deposito : gridGraph.getAlmacenes()) {
                                if (deposito.getUbicacion().getX() == posicion.getX()
                                        && deposito.getUbicacion().getY() == posicion.getY()) {
                                    if (deposito.getUbicacion().getX() != 12 && deposito.getUbicacion().getY() != 8) {
                                        deposito.setCapacidadDisponible(
                                                deposito.getCapacidadDisponible() - 2);
                                    }
                                }
                            }

                        }
                    } else {

                        if (posicion.isPedido()) {
                            camion.setCargaAsignada(
                                    camion.getCargaAsignada() + posicion.getPedidoRuta().getCantidadGLP());
                            posicion.getPedidoRuta().setEntregado(false);
                            posicion.getPedidoRuta().setEntregadoCompleto(false);
                        }
                        if (posicion.isDepot()) {
                            for (Almacen deposito : gridGraph.getAlmacenes()) {
                                if (deposito.getUbicacion().getX() == posicion.getX()
                                        && deposito.getUbicacion().getY() == posicion.getY()) {
                                    if (deposito.getUbicacion().getX() != 12 && deposito.getUbicacion().getY() != 8) {
                                        deposito.setCapacidadDisponible(
                                                deposito.getCapacidadDisponible() + 2);
                                    }
                                }
                            }
                        }

                    }
                }
            }

        }
    }

    // Retorno del camion al almacen
    public void volverAlmacen(Camion camion, List<Bloqueo> bloqueos, int anio, int mes, int dia, int hora,
                              int minuto, int tipoSimulacion) {

        NodePosition deposito = new NodePosition(0, 12, 8, true);

        deposito = selecionarAlmacen(camion, tipoSimulacion);

        Astar astar = new Astar();
        for (NodePosition node : camion.getRoute()) {
            node.setAntecesor(null);
        }
        NodePosition posicionActual = camion.getUbicacionActual();
        NodePosition nodoDeposito = new NodePosition(0, deposito.getX(), deposito.getY(), true);
        astar.encontrarCamino(gridGraph, posicionActual, nodoDeposito, bloqueos,
                anio, mes, camion, null, inicio, tipoSimulacion);

        if(tipoSimulacion == 1){
            camion.getUbicacionActual().setArriveTime(camion.getUbicacionActual().getStartTime() + 1);
        }else{
            camion.getUbicacionActual().setArriveTime(camion.getUbicacionActual().getStartTime() + 1.2);
        }
        camion.getUbicacionActual().setDepot(true);
        posicionActual = camion.getUbicacionActual();

        posicionActual.setStartTime(camion.ubicacionActual.getStartTime());
        posicionActual.setArriveTime(camion.ubicacionActual.getArriveTime());
        for (NodePosition node : camion.getRoute()) {
            node.setAntecesor(null);
        }

        camion.setCargaAsignada(0.0);
        camion.setDistanciaRecorrida(0.0);

    }

    // Seleccionar el almacen mas cercano para el camion
    public NodePosition selecionarAlmacen(Camion camion, int tipoSimulacion) {
        /* double distanciaMinima = Double.MAX_VALUE; */
        NodePosition deposito = new NodePosition(0, 12, 8, true);
        if (tipoSimulacion == 1) {
            return deposito;
        } else {
            double distanciaMinima = Double.MAX_VALUE;
            for (int x = 0; x < gridGraph.getRows(); x++) {
                for (int y = 0; y < gridGraph.getColumns(); y++) {
                    if (gridGraph.getGrid()[x][y].isDepot()) {
                        NodePosition nodoDeposito = gridGraph.getGrid()[x][y];
                        double distancia = camion.getUbicacionActual().distance(nodoDeposito);
                        if (distancia < distanciaMinima) {
                            distanciaMinima = distancia;
                            deposito = nodoDeposito; // Norte , Central o al este
                        }

                    }
                }

            }

            // Actualizar la carga de los Almacenes

            for (Almacen depositoP : gridGraph.getAlmacenes()) {
                if (depositoP.getUbicacion().getX() == deposito.getX() && depositoP.getUbicacion().getY() == deposito.getY()) {
                    if (depositoP.getUbicacion().getX() != 12 && depositoP.getUbicacion().getY() != 8) {
                        if (depositoP.getCapacidadDisponible() - 2 <= 0) {
                            depositoP.setCapacidadDisponible(0);

                        } else {
                            depositoP.setCapacidadDisponible(depositoP.getCapacidadDisponible() - 2);
                        }

                    }
                }
            }

            for (Almacen deposito2 : gridGraph.getAlmacenes()) {
                if (deposito.getX() == deposito2.getUbicacion().getX() && deposito.getY() == deposito2.getUbicacion().getY()) {
                    if (deposito2.getCapacidadDisponible() == 0) {
                        deposito = new NodePosition(0, 12, 8, true);
                    }
                }
            }
        }

        return deposito;
    }

    // Construir la solucion de rutas para el camion
    public void construirSolucion(Camion camion, NodePosition posicionActual, int anio, int mes, int dia, int hora,
                                  int minuto, List<Bloqueo> bloqueos, int primeraVez, int tipoSimulacion) {

        if (camion.getPedidosAsignados() == null)
            return;
        for (Pedido pedido : camion.getPedidosAsignados()) {
            NodePosition nodoPedido = new NodePosition(0, pedido.getPosX(), pedido.getPosY(), false);
            LocalDateTime fechaTimer = LocalDateTime.of(anio, mes, dia, hora, minuto);
            LocalDateTime fechaPedido = pedido.getFechaDeRegistro();
            if (fechaPedido.isBefore(fechaTimer)) {

                posicionActual.setStartTime(posicionActual.getStartTime());
                inicio = false;

            } else {
                if (inicio) {
                    posicionActual.setStartTime(pedido.getHoraDeInicio());
                    inicio = false;
                } else {
                    posicionActual.setStartTime(posicionActual.getArriveTime());
                }
            }
            Astar astar = new Astar();
            for (NodePosition node : camion.getRoute()) {
                node.setAntecesor(null);
            }
            astar.encontrarCamino(gridGraph, posicionActual, nodoPedido, bloqueos,
                    anio, mes, camion, pedido, inicio, tipoSimulacion);

            posicionActual = new NodePosition(0, camion.getUbicacionActual().getX(),
                    camion.getUbicacionActual().getY(), false);

            posicionActual.setStartTime(camion.ubicacionActual.getStartTime());
            posicionActual.setArriveTime(camion.ubicacionActual.getArriveTime());
            for (NodePosition node : camion.getRoute()) {
                node.setAntecesor(null);
            }
        }
        inicio = true;
    }

    public void verificacionMomentanea() {
        for (Camion camion : camiones) {
            if (camion.getPedidosAsignados() == null)
                continue;
            for (Pedido pedido : camion.getPedidosAsignados()) {
                pedido.setEntregado(true);
            }
        }
    }

    public void asignarPedidosACamionesVacios(int primeraVez, int dia, int hora, int minuto, int mes, int anio,
                                              List<Bloqueo> bloqueos, double timer) {
        NodePosition posicionActual = null;
        for (Camion camion : camiones) {
            if (camion.isEnAveria()) {
                continue;
            }
            if (camion.getPedidosAsignados() == null || camion.getPedidosAsignados().isEmpty()) {
                camion.setPedidosAsignados(new ArrayList<>());
                posicionActual = camion.getUbicacionActual();
                for (int i = 0; i < pedidos.size(); i++) {
                    Pedido pedidoMax = proximoPedidoFD(camion, dia, hora, minuto, mes, anio, bloqueos, timer, 2);
                    if (pedidoMax == null) {
                        break;
                    }
                    if (camion.CheckIfFits(pedidoMax.getCantidadGLP())) {

                        camion.asignarPedido(pedidoMax);
                        camion.getPedidosAsignados().add(pedidoMax);
                        pedidoMax.setEntregado(true);

                        // Actualizar feromonas

                    }

                }
                if (primeraVez == 0) {
                    camion.setUbicacionActual(null); // No necesito la ubicacion actual del camion para la
                    // planificacion
                    // por ser la primera vez
                } else {
                    camion.setUbicacionActual(posicionActual);
                }
            }
        }
    }

    public void asignarPedidosACamionesLlenos(int primeraVez, int dia, int hora, int minuto, int mes, int anio,
                                              List<Bloqueo> bloqueos, double timer) {
        NodePosition posicionActual = null;
        for (Camion camion : camiones) {

            if (camion.isEnAveria()) {
                continue;
            }

            camion.setPedidosAsignados(new ArrayList<>());
            posicionActual = camion.getUbicacionActual();
            for (int i = 0; i < pedidos.size(); i++) {
                Pedido pedidoMax = proximoPedidoFD(camion, dia, hora, minuto, mes, anio, bloqueos, timer, 2);
                if (pedidoMax == null) {
                    break;
                }
                if (camion.CheckIfFits(pedidoMax.getCantidadGLP())) {

                    camion.asignarPedido(pedidoMax);
                    camion.getPedidosAsignados().add(pedidoMax);
                    pedidoMax.setEntregado(true);
                }

            }
            if (primeraVez == 0) {
                camion.setUbicacionActual(null); // No necesito la ubicacion actual del camion para la planificacion
                // por ser la primera vez
            } else {
                camion.setUbicacionActual(posicionActual);
            }
        }
    }

    public void planificarPedidos(int primeraVez, int dia, int hora, int minuto, int mes, int anio,
                                  List<Bloqueo> bloqueos, double timer) {

        boolean hayCamionVacio = false;

        for (Camion camion : camiones) {
            if (camion.getPedidosAsignados() == null || camion.isEnAveria()
                    || camion.getPedidosAsignados().isEmpty()) {
                hayCamionVacio = true;
                break;
            }
        }

        verificacionMomentanea();

        if (hayCamionVacio) {
            asignarPedidosACamionesVacios(primeraVez, dia, hora, minuto, mes, anio, bloqueos, timer);
        } else {
            asignarPedidosACamionesLlenos(primeraVez, dia, hora, minuto, mes, anio, bloqueos, timer);
        }

    }

    public void planificarPedidosDiaria(int primeraVez, int dia, int hora, int minuto, int mes, int anio,
                                        List<Bloqueo> bloqueos, double timer) {
        NodePosition posicionActual = null;
        for (Camion camion : camiones) {
            if (camion.isEnAveria()) {
                continue;
            }
            camion.setPedidosAsignados(new ArrayList<>());
            posicionActual = camion.getUbicacionActual();
            for (int i = 0; i < pedidos.size(); i++) {
                Pedido pedidoMax = proximoPedidoFD(camion, dia, hora, minuto, mes, anio, bloqueos, timer, 1);
                if (pedidoMax == null) {
                    break;
                }
                camion.asignarPedido(pedidoMax);
                camion.getPedidosAsignados().add(pedidoMax);
                pedidoMax.setEntregado(true);
                /*
                 * if (camion.CheckIfFits(pedidoMax.getCantidadGLP())) {
                 *
                 * camion.asignarPedido(pedidoMax);
                 * camion.getPedidosAsignados().add(pedidoMax);
                 * pedidoMax.setEntregado(true);
                 * }
                 */

            }
            if (primeraVez == 0) {
                camion.setUbicacionActual(null); // No necesito la ubicacion actual del vehiculo para la planificacion
                // por ser la primera vez
            } else {
                camion.setUbicacionActual(posicionActual);
            }
        }
    }

    // Obtener el proximo pedido para el camion
    public Pedido proximoPedidoFD(Camion camion, int dia, int hora, int minuto, int mes, int anio,
                                  List<Bloqueo> bloqueos, double timer, int tipoSimulacion) {

        if (tipoSimulacion == 1) {
            double max = Double.POSITIVE_INFINITY;
            Pedido pedidoMax = null;
            for (Pedido pedido : pedidos) {
                if (!pedido.isEntregado()) {
                    double distance = camion.getUbicacionActual().distance(new NodePosition(0, pedido.getPosX(),
                            pedido.getPosY(), false));
                    double tiempoViaje = (distance / camion.getVelocidad()) * 60;
                    double tiempoEntrega = pedido.getTiempoLlegada();
                    double tiempoActual = timer;
                    if (camion.CheckIfFits(pedido.getCantidadGLP())) {
                        if (tiempoActual + tiempoViaje <= tiempoEntrega) {
                            if (distance < max) {
                                max = distance;
                                pedidoMax = pedido;
                            }
                        }

                        /*
                         * camion.asignarPedido(pedido);
                         * camion.getPedidosAsignados().add(pedido);
                         * pedido.setEntregado(true);
                         * pedidoMax = pedido;
                         */
                    }

                }
            }
            return pedidoMax;
        } else {
            double max = Double.POSITIVE_INFINITY;
            Pedido pedidoMax = null;
            for (Pedido pedido : pedidos) {
                if (!pedido.isEntregado()) {
                    double distance = camion.getUbicacionActual().distance(new NodePosition(0, pedido.getPosX(),
                            pedido.getPosY(), false));
                    double tiempoViaje = (distance / camion.getVelocidad()) * 60;
                    double tiempoEntrega = pedido.getTiempoLlegada();
                    double tiempoActual = timer;

                    // Crea una prioridad entre el tiempoActual + tiempoViaje sea siempre menor al
                    // tiempo de entrega
                    if (tiempoActual + tiempoViaje <= tiempoEntrega) {
                        if (distance < max) {
                            max = distance;
                            pedidoMax = pedido;
                        }
                    } else {
                        return pedido;
                    }

                }
            }
            return pedidoMax;
        }

    }

    // Inicializar las feromonas para el algoritmo
    public void initialPheromones() {
        this.matrizFeromonas = new double[gridGraph.getRows() + 1][gridGraph.getColumns() + 1][gridGraph.getRows()
                + 1][gridGraph
                .getColumns() + 1];
        for (int i = 0; i <= gridGraph.getRows(); i++) {
            for (int j = 0; j <= gridGraph.getColumns(); j++) {
                for (int k = 0; k <= gridGraph.getRows(); k++) {
                    for (int l = 0; l <= gridGraph.getColumns(); l++) {
                        // Asignar un valor inicial de feromonas
                        matrizFeromonas[i][j][k][l] = 0.1; // Puedes elegir un valor adecuado
                    }
                }
            }
        }
    }

    // Eliminar las rutas de los camiones
    public void clearRoute() {
        for (Camion camion : camiones) {
            camion.setRoute(new ArrayList<>());
        }
    }

    public List<NodoBloqueado> obtenerNodosBloqueados(List<Bloqueo> bloqueos) {
        List<NodoBloqueado> nodosBloqueados = new ArrayList<>();

        for (Bloqueo bloqueo : bloqueos) {
            List<NodoBloqueado> tramoBloqueo = bloqueo.getTramo();
            nodosBloqueados.addAll(tramoBloqueo);
        }
        return nodosBloqueados;
    }

}
