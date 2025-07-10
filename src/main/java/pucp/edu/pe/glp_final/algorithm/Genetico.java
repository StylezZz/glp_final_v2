package pucp.edu.pe.glp_final.algorithm;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Component;
import pucp.edu.pe.glp_final.models.*;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@NoArgsConstructor
public class Genetico {

    private Mapa mapa;
    private List<Camion> camiones;
    private List<Pedido> pedidos;
    private List<Pedido> originalPedidos;
    private List<Pedido> pedidosNuevos;
    private List<Bloqueo> bloqueosActivos;
    private double[][][][] matrizPoblacion;
    private boolean inicio;

    public Genetico(
            List<Camion> camiones,
            int tipoSimulacion
    ) {
        this.bloqueosActivos = new ArrayList<>();
        this.mapa = new Mapa(tipoSimulacion);
        this.pedidos = new ArrayList<>();
        this.camiones = camiones;
        this.inicio = true;
        inicializarPoblacion();
    }

    public List<Camion> simulacionRuteo(
            int anio,
            int mes,
            int dia,
            int hora,
            int minuto,
            int minutosPorIteracion,
            List<Pedido> pedidosDia,
            List<Bloqueo> bloqueos,
            List<Pedido> pedidoOriginal,
            int primeraVez,
            double momento,
            int tipoSimulacion
    ) {
        getPedidosDia(anio, mes, dia, hora, minuto, pedidosDia, minutosPorIteracion, pedidoOriginal, tipoSimulacion);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, anio);
        calendar.set(Calendar.MONTH, mes - 1);
        calendar.set(Calendar.DAY_OF_MONTH, dia);
        calendar.set(Calendar.HOUR_OF_DAY, hora);
        calendar.set(Calendar.MINUTE, minuto);

        bloqueosActivos = Bloqueo.bloqueosActivos(bloqueos, calendar);
        ejecutar(anio, mes, dia, hora, minuto, bloqueos, primeraVez, momento, tipoSimulacion);
        validarTiempoRuta(anio, mes, dia, hora, minuto, minutosPorIteracion, tipoSimulacion);

        return camiones;
    }

    public void getPedidosDia(
            int anio,
            int mes,
            int dia,
            int hora,
            int minuto,
            List<Pedido> pedidosDia,
            int minutosPorIteracion,
            List<Pedido> pedidoOriginal,
            int tipoSimulacion
    ) {
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
                    || pedido.getFechaDeRegistro().isEqual(fechaIteracion)
            ) {
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
    public void validarPedidosNoEntregado(int anio, int mes, int hora, int minuto, double timer,
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
                for (NodoMapa ubicacion : camion.getRoute()) {

                    if (mes == ubicacion.getMes() && anio == ubicacion.getAnio()) {
                        double startTime = timer;
                        if (Math.round(ubicacion.getTiempoInicio()) < startTime) {
                            distanciaRecorrida += 1;
                            camion.setUbicacionActual(ubicacion);
                            camion.setDistanciaRecorrida(distanciaRecorrida);
                            camion.getUbicacionActual().setEsRuta(false);
                            if (ubicacion.isEsPedido()) {
                                if (ubicacion.getMes() <= mes && ubicacion.getAnio() <= anio) {
                                    ubicacion.getPedido().setEntregado(true);
                                    ubicacion.getPedido().setHoraDeInicio((int) ubicacion.getTiempoInicio());
                                    ubicacion.getPedido().setTiempoLlegada((int) ubicacion.getTiempoFin());
                                    camion.asignarPedido(ubicacion.getPedido());
                                }
                            }
                            if (ubicacion.isEsAlmacen()) {

                                for (Almacen deposito2 : mapa.getAlmacenes()) {
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
                                                camion.setGlpDisponible(camion.getCarga());

                                            }
                                        }
                                    }

                                    camion.setCargaAnterior(0);
                                }
                                ubicacion.setTiempoInicio(startTime);
                                ubicacion.setTiempoFin(startTime + 1);
                            }

                        } else {
                            if (bandera) {
                                camion.setUbicacionActual(ubicacion);
                                bandera = false;
                            }
                            if (ubicacion.isEsPedido()) {
                                if (ubicacion.getMes() <= mes && ubicacion.getAnio() <= anio) {
                                    ubicacion.getPedido().setEntregado(false);
                                    ubicacion.getPedido().setAsignado(false);
                                    ubicacion.getPedido().setIdCamion(null);
                                }
                            }
                            if (ubicacion.isEsAlmacen()) {
                                if (Math.round(ubicacion.getTiempoInicio()) > startTime) {
                                    if (i == 0) {
                                        camion.setUbicacionActual(ubicacion);
                                        continue;
                                    }
                                }
                            }
                        }
                        i++;
                    }

                    if (camion.getCargaAsignada() == 0.0) {
                        camion.setCargaAsignada(camion.getCargaAnterior());
                    }
                }
            } else {
                for (NodoMapa ubicacion : camion.getRoute()) {

                    if (mes == ubicacion.getMes() && anio == ubicacion.getAnio()) {
                        double startTime = timer;
                        if ((int) ubicacion.getTiempoFin() <= startTime) {
                            distanciaRecorrida += 1;
                            camion.setUbicacionActual(ubicacion);
                            camion.setDistanciaRecorrida(distanciaRecorrida);
                            camion.getUbicacionActual().setEsRuta(false);
                            if (ubicacion.isEsPedido()) {
                                if (ubicacion.getMes() <= mes && ubicacion.getAnio() <= anio) {
                                    ubicacion.getPedido().setEntregado(true);
                                    ubicacion.getPedido().setHoraDeInicio((int) ubicacion.getTiempoInicio());
                                    ubicacion.getPedido().setTiempoLlegada((int) ubicacion.getTiempoFin());
                                    camion.asignarPedido(ubicacion.getPedido());
                                }
                            }
                            if (ubicacion.isEsAlmacen()) {

                                for (Almacen deposito2 : mapa.getAlmacenes()) {
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
                                                camion.setGlpDisponible(camion.getCarga());

                                            }
                                        }
                                    }

                                }
                                ubicacion.setTiempoInicio(startTime);
                                ubicacion.setTiempoFin(startTime + 1.2);
                            }

                        } else {
                            if (ubicacion.isEsPedido()) {
                                if (ubicacion.getMes() <= mes && ubicacion.getAnio() <= anio) {
                                    ubicacion.getPedido().setEntregado(false);
                                    ubicacion.getPedido().setAsignado(false);
                                    ubicacion.getPedido().setIdCamion(null);
                                }
                            }
                            if (ubicacion.isEsAlmacen()) {
                                if ((int) ubicacion.getTiempoFin() > startTime) {
                                    if (i == 0) {
                                        camion.setUbicacionActual(ubicacion);
                                        continue;
                                    }
                                }
                            }
                        }

                        i++;
                    }

                }
            }
            i++;
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
                NodoMapa nodoCentral = new NodoMapa(0, 12, 8, true);
                nodoCentral.setTiempoInicio(camion.getTiempoFinAveria());
                if (tipoSimulacion == 1) {
                    nodoCentral.setTiempoFin(camion.getTiempoFinAveria() + 1);
                } else {
                    nodoCentral.setTiempoFin(camion.getTiempoFinAveria() + 1.2);

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
                    NodoMapa posicionDefault = new NodoMapa(0, 12, 8, true);
                    posicionDefault.setAnio(anio);
                    posicionDefault.setMes(mes);
                    camion.setUbicacionActual(posicionDefault);
                }

                for (Camion camionRescate : camiones) {
                    if (camion.getCodigo().equals(camionRescate.getCodigo())) {
                        continue;
                    } else {
                        if (camionRescate.getUbicacionActual() == null) {
                            NodoMapa posicionDefault = new NodoMapa(0, 12, 8, true);
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

                PlanificadorRuta aestar = new PlanificadorRuta();

                aestar.encontrarCamino(mapa, camionMenorDistancia.getUbicacionActual(),
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

    public void ejecutar(
            int anio,
            int mes,
            int dia,
            int hora,
            int minuto,
            List<Bloqueo> bloqueos,
            int primeraVez,
            double timer,
            int tipoSimulacion
    ) {
        mapa.removerPedidos();
        validarPedidosNoEntregado(anio, mes, hora, minuto, timer, tipoSimulacion);
        entregadosCompletos();
        vaciarRutas();

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
                NodoMapa posicionActual = camion.getUbicacionActual();
                if (posicionActual == null) {
                    posicionActual = new NodoMapa(0, 12, 8, true);
                    posicionActual.setAnio(anio);
                    posicionActual.setMes(mes);
                    camion.setUbicacionActual(posicionActual);
                    mapa.getMapa()[12][8].setEsRuta(true);
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
                for (NodoMapa ubicacion : camion.getRoute()) {
                    if (ubicacion.isEsPedido()) {
                        ubicacion.getPedido().setEntregado(false);
                        ubicacion.getPedido().setEntregadoCompleto(false);
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
            NodoMapa posicionActual = camion.getUbicacionActual();
            if (posicionActual == null) {
                posicionActual = new NodoMapa(0, 12, 8, true);
                posicionActual.setAnio(anio);
                posicionActual.setMes(mes);
                posicionActual.setTiempoInicio(timer);
                camion.setUbicacionActual(posicionActual);
                mapa.getMapa()[12][8].setEsRuta(true);
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
            for (NodoMapa posicion : camion.getRoute()) {
                if (anio == posicion.getAnio() && mes == posicion.getMes()) {
                    if ((int) posicion.getTiempoFin() <= arriveTime) {
                        if (posicion.isEsPedido()) {
                            camion.setCargaAsignada(
                                    camion.getCargaAsignada() + posicion.getPedido().getCantidadGLP());
                            for (Pedido pedidoOriginal : originalPedidos) {
                                if (pedidoOriginal.getId() == posicion.getPedido().getId()) {
                                    posicion.getPedido().setEntregado(true);
                                    pedidoOriginal
                                            .setCantidadGLPAsignada(pedidoOriginal.getCantidadGLPAsignada() +
                                                    posicion.getPedido().getCantidadGLP());
                                    if (tipoSimulacion == 1) {
                                        if (pedidoOriginal.getCantidadGLP() <= pedidoOriginal
                                                .getCantidadGLPAsignada()) {
                                            posicion.getPedido().setEntregadoCompleto(true);
                                            pedidoOriginal.setEntregadoCompleto(true);
                                            pedidoOriginal.setEntregado(true);
                                        }
                                    } else {
                                        if (pedidoOriginal.getId() == posicion.getPedido().getId()) {
                                            posicion.getPedido().setEntregadoCompleto(true);
                                            pedidoOriginal.setEntregadoCompleto(true);
                                            pedidoOriginal.setEntregado(true);
                                        }
                                    }
                                }
                            }
                        }
                        if (posicion.isEsAlmacen()) {
                            for (Almacen deposito : mapa.getAlmacenes()) {
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

                        if (posicion.isEsPedido()) {
                            camion.setCargaAsignada(
                                    camion.getCargaAsignada() + posicion.getPedido().getCantidadGLP());
                            posicion.getPedido().setEntregado(false);
                            posicion.getPedido().setEntregadoCompleto(false);
                        }
                        if (posicion.isEsAlmacen()) {
                            for (Almacen deposito : mapa.getAlmacenes()) {
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

        NodoMapa deposito = new NodoMapa(0, 12, 8, true);

        deposito = selecionarAlmacen(camion, tipoSimulacion);

        PlanificadorRuta planificadorRuta = new PlanificadorRuta();
        for (NodoMapa node : camion.getRoute()) {
            node.setNodoPrevio(null);
        }
        NodoMapa posicionActual = camion.getUbicacionActual();
        NodoMapa nodoDeposito = new NodoMapa(0, deposito.getX(), deposito.getY(), true);
        planificadorRuta.encontrarCamino(mapa, posicionActual, nodoDeposito, bloqueos,
                anio, mes, camion, null, inicio, tipoSimulacion);

        if (tipoSimulacion == 1) {
            camion.getUbicacionActual().setTiempoFin(camion.getUbicacionActual().getTiempoInicio() + 1);
        } else {
            camion.getUbicacionActual().setTiempoFin(camion.getUbicacionActual().getTiempoInicio() + 1.2);
        }
        camion.getUbicacionActual().setEsAlmacen(true);
        posicionActual = camion.getUbicacionActual();

        posicionActual.setTiempoInicio(camion.ubicacionActual.getTiempoInicio());
        posicionActual.setTiempoFin(camion.ubicacionActual.getTiempoFin());
        for (NodoMapa node : camion.getRoute()) {
            node.setNodoPrevio(null);
        }

        camion.setCargaAsignada(0.0);
        camion.setDistanciaRecorrida(0.0);

    }

    // Seleccionar el almacen mas cercano para el camion
    public NodoMapa selecionarAlmacen(Camion camion, int tipoSimulacion) {
        /* double distanciaMinima = Double.MAX_VALUE; */
        NodoMapa deposito = new NodoMapa(0, 12, 8, true);
        if (tipoSimulacion == 1) {
            return deposito;
        } else {
            double distanciaMinima = Double.MAX_VALUE;
            for (int x = 0; x < mapa.getRows(); x++) {
                for (int y = 0; y < mapa.getColumns(); y++) {
                    if (mapa.getMapa()[x][y].isEsAlmacen()) {
                        NodoMapa nodoDeposito = mapa.getMapa()[x][y];
                        double distancia = camion.getUbicacionActual().distance(nodoDeposito);
                        if (distancia < distanciaMinima) {
                            distanciaMinima = distancia;
                            deposito = nodoDeposito; // Norte , Central o al este
                        }

                    }
                }

            }

            // Actualizar la carga de los Almacenes

            for (Almacen depositoP : mapa.getAlmacenes()) {
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

            for (Almacen deposito2 : mapa.getAlmacenes()) {
                if (deposito.getX() == deposito2.getUbicacion().getX() && deposito.getY() == deposito2.getUbicacion().getY()) {
                    if (deposito2.getCapacidadDisponible() == 0) {
                        deposito = new NodoMapa(0, 12, 8, true);
                    }
                }
            }
        }

        return deposito;
    }

    // Construir la solucion de rutas para el camion
    public void construirSolucion(Camion camion, NodoMapa posicionActual, int anio, int mes, int dia, int hora,
                                  int minuto, List<Bloqueo> bloqueos, int primeraVez, int tipoSimulacion) {

        if (camion.getPedidosAsignados() == null)
            return;
        for (Pedido pedido : camion.getPedidosAsignados()) {
            NodoMapa nodoPedido = new NodoMapa(0, pedido.getPosX(), pedido.getPosY(), false);
            LocalDateTime fechaTimer = LocalDateTime.of(anio, mes, dia, hora, minuto);
            LocalDateTime fechaPedido = pedido.getFechaDeRegistro();
            if (fechaPedido.isBefore(fechaTimer)) {

                posicionActual.setTiempoInicio(posicionActual.getTiempoInicio());
                inicio = false;

            } else {
                if (inicio) {
                    posicionActual.setTiempoInicio(pedido.getHoraDeInicio());
                    inicio = false;
                } else {
                    posicionActual.setTiempoInicio(posicionActual.getTiempoFin());
                }
            }
            PlanificadorRuta planificadorRuta = new PlanificadorRuta();
            for (NodoMapa node : camion.getRoute()) {
                node.setNodoPrevio(null);
            }
            planificadorRuta.encontrarCamino(mapa, posicionActual, nodoPedido, bloqueos,
                    anio, mes, camion, pedido, inicio, tipoSimulacion);

            posicionActual = new NodoMapa(0, camion.getUbicacionActual().getX(),
                    camion.getUbicacionActual().getY(), false);

            posicionActual.setTiempoInicio(camion.ubicacionActual.getTiempoInicio());
            posicionActual.setTiempoFin(camion.ubicacionActual.getTiempoFin());
            for (NodoMapa node : camion.getRoute()) {
                node.setNodoPrevio(null);
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
        NodoMapa posicionActual = null;
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
        NodoMapa posicionActual = null;
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
        NodoMapa posicionActual = null;
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
                    double distance = camion.getUbicacionActual().distance(new NodoMapa(0, pedido.getPosX(),
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
                    double distance = camion.getUbicacionActual().distance(new NodoMapa(0, pedido.getPosX(),
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

    public void inicializarPoblacion() {
        this.matrizPoblacion = new double[mapa.getRows() + 1][mapa.getColumns() + 1][mapa.getRows() + 1][mapa.getColumns() + 1];
        for (int i = 0; i <= mapa.getRows(); i++) {
            for (int j = 0; j <= mapa.getColumns(); j++) {
                for (int k = 0; k <= mapa.getRows(); k++) {
                    for (int l = 0; l <= mapa.getColumns(); l++) {
                        // Asignar un valor inicial de feromonas
                        matrizPoblacion[i][j][k][l] = 0.1; // Puedes elegir un valor adecuado
                    }
                }
            }
        }
    }

    public void vaciarRutas() {
        for (Camion camion : camiones)
            camion.setRoute(new ArrayList<>());
    }

}
