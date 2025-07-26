package pucp.edu.pe.glp_final.algorithm;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.springframework.cglib.core.Local;
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
    private List<Pedido> pedidosCompletos;
    private List<Pedido> pedidosNuevos;
    private List<Bloqueo> bloqueosActivos;
    private double[][][][] matrizPoblacion;
    private boolean inicio;
    private LocalDateTime fechaBaseSimulacion;

    public Genetico(
            List<Camion> camiones,
            int tipoSimulacion,
            LocalDateTime fechaBaseSimulacion
    ) {
        this.bloqueosActivos = new ArrayList<>();
        this.mapa = new Mapa(tipoSimulacion);
        this.pedidos = new ArrayList<>();
        this.camiones = camiones;
        this.inicio = true;
        inicializarPoblacion();
        this.fechaBaseSimulacion = fechaBaseSimulacion;
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

        bloqueosActivos = Bloqueo.obtenerBloqueosActivos(bloqueos, calendar);
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
        this.pedidosCompletos = new ArrayList<>();
        for (Pedido pedido : pedidoOriginal) {
            if (pedido.getFechaDeRegistro().isBefore(fechaIteracion)
                    || pedido.getFechaDeRegistro().isEqual(fechaIteracion)) {
                pedidosCompletos.add(pedido);
            }
        }
    }

    public void verificarPedidosNoCompletados(
            int anio,
            int mes,
            int dia,
            int hora,
            int minuto
    ) {
        LocalDateTime fechaActual = LocalDateTime.of(anio, mes, dia, hora, minuto);
        for (Pedido pedido : pedidosCompletos) {
            if (!pedido.isEntregadoCompleto()) {
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

    public void validarPedidosNoEntregado(
            int anio,
            int mes,
            int hora,
            int minuto,
            double timer,
            int tipoSimulacion
    ) {
        for (Camion camion : camiones) {
            int i = 0;
            if (camion.getRoute() == null)
                continue;
            boolean bandera = true;
            double distanciaRecorrida = 0.0;
            camion.setCargaAsignada(0);
            if (tipoSimulacion == 1) {
                for (NodoMapa ubicacion : camion.getRoute()) {
                    if (mes == ubicacion.getMes() && anio == ubicacion.getAnio()) {
                        double startTime = timer;
                        if (Math.round(ubicacion.getTiempoInicio()) < startTime) {
                            distanciaRecorrida = getDistanciaRecorrida(anio, mes, camion, distanciaRecorrida, ubicacion);
                            if (ubicacion.isEsAlmacen()) {
                                for (Almacen deposito2 : mapa.getAlmacenes()) {
                                    gestionarCapacidadesAlmacen(hora, minuto, camion, deposito2);

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
                    if (camion.getCargaAsignada() == 0.0)
                        camion.setCargaAsignada(camion.getCargaAnterior());
                }
            } else {
                for (NodoMapa ubicacion : camion.getRoute()) {

                    if (mes == ubicacion.getMes() && anio == ubicacion.getAnio()) {
                        double startTime = timer;
                        if ((int) ubicacion.getTiempoInicio() <= startTime) { // ACA
                            distanciaRecorrida = getDistanciaRecorrida(anio, mes, camion, distanciaRecorrida, ubicacion);
                            if (ubicacion.isEsAlmacen()) {
                                for (Almacen deposito2 : mapa.getAlmacenes()) {
                                    gestionarCapacidadesAlmacen(hora, minuto, camion, deposito2);
                                }
                                ubicacion.setTiempoInicio(startTime);
                                ubicacion.setTiempoFin(startTime + 1.2);
                            }

                        } else {
                            if (ubicacion.isEsPedido()) {
                                ubicacion.getPedido().setEntregado(false);
                                ubicacion.getPedido().setAsignado(false);
                                ubicacion.getPedido().setIdCamion(null);
                            }
                            if (ubicacion.isEsAlmacen()) {
                                if ((int) ubicacion.getTiempoInicio() > startTime) { // ACA
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
        pedidosCompletos.sort(comparadorPorTiempoDeLlegada);
    }

    private void gestionarCapacidadesAlmacen(int hora, int minuto, Camion camion, Almacen deposito2) {
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

    private double getDistanciaRecorrida(int anio, int mes, Camion camion, double distanciaRecorrida, NodoMapa ubicacion) {
        distanciaRecorrida += 1;
        camion.setUbicacionActual(ubicacion);
        camion.setDistanciaRecorrida(distanciaRecorrida);
        camion.getUbicacionActual().setEsRuta(false);
        if (ubicacion.isEsPedido()) {
            if (ubicacion.getMes() <= mes && ubicacion.getAnio() <= anio) {
                ubicacion.getPedido().setEntregado(true);
                ubicacion.getPedido().setHoraDeInicio((int) ubicacion.getTiempoInicio());
                ubicacion.getPedido().setTiempoLlegada((int) ubicacion.getTiempoFin());
                camion.asignar(ubicacion.getPedido());
            }
        }
        return distanciaRecorrida;
    }

    public void verificarAverias(
            int anio,
            int mes,
            int dia,
            int hora,
            int minuto,
            List<Bloqueo> bloqueos,
            int tipoSimulacion
    ) {
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

                        double distancia = camion.getUbicacionActual().calcularDistancia(camionRescate.getUbicacionActual());
                        if (distancia < menorDistancia) {
                            menorDistancia = distancia;
                            camionMenorDistancia = camionRescate;
                        }
                    }
                }
                PlanificadorRuta planificador = new PlanificadorRuta();
                planificador.encontrarCamino(
                        mapa,
                        camionMenorDistancia.getUbicacionActual(),
                        camion.getUbicacionActual(),
                        bloqueos,
                        anio,
                        mes,
                        camionMenorDistancia,
                        null,
                        tipoSimulacion,
                        fechaBaseSimulacion
                );
            }

        }

    }

    public void entregadosCompletos() {
        for (Pedido pedido : pedidosCompletos) {
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

        verificarAverias(anio, mes, dia, hora, minuto, bloqueos, tipoSimulacion);
        verificarPedidosNoCompletados(anio, mes, dia, hora, minuto);

        LocalDateTime fechaActual = LocalDateTime.of(anio, mes, dia, hora, minuto);

        Comparator<Pedido> comparadorPorProximidad = Comparator
                .comparing(pedido -> Duration.between(fechaActual, pedido.getFechaEntrega()).abs());

        pedidos.sort(comparadorPorProximidad);
        if (primeraVez == 0) {
            for (Camion camion : camiones) {
                camion.crearRuta();
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
                    camion.crearRuta();
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
            planificarPedidosDiaria(primeraVez, timer, anio, mes, dia, hora, minuto);
        } else {
            planificarPedidos(primeraVez, timer, anio, mes, dia, hora, minuto);
        }
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
                camion.crearRuta();
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
            generarSolucion(camion, posicionActual, anio, mes, dia, hora, minuto, bloqueos, tipoSimulacion);
            volverAlmacen(camion, bloqueos, anio, mes, dia, hora, minuto, tipoSimulacion);
        }

    }

    public void eliminarPedidosEntregadosYReorganizar() {
        List<Camion> camionesConPedidos = new ArrayList<>();
        List<Camion> camionesSinPedidos = new ArrayList<>();

        for (Camion camion : camiones) {
            if (camion.getPedidosAsignados() != null && !camion.getPedidosAsignados().isEmpty()) {
                camionesConPedidos.add(camion);
            } else {
                camionesSinPedidos.add(camion);
            }
        }

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
                            for (Pedido pedidoOriginal : pedidosCompletos) {
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
                anio, mes, camion, null, tipoSimulacion, fechaBaseSimulacion);

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

    public NodoMapa selecionarAlmacen(Camion camion, int tipoSimulacion) {
        NodoMapa deposito = new NodoMapa(0, 12, 8, true);
        if (tipoSimulacion == 1) {
            return deposito;
        } else {
            double distanciaMinima = Double.MAX_VALUE;
            for (int x = 0; x < mapa.getRows(); x++) {
                for (int y = 0; y < mapa.getColumns(); y++) {
                    if (mapa.getMapa()[x][y].isEsAlmacen()) {
                        NodoMapa nodoDeposito = mapa.getMapa()[x][y];
                        double distancia = camion.getUbicacionActual().calcularDistancia(nodoDeposito);
                        if (distancia < distanciaMinima) {
                            distanciaMinima = distancia;
                            deposito = nodoDeposito; // Norte , Central o al este
                        }

                    }
                }

            }

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

    public void generarSolucion(Camion camion, NodoMapa posicionActual, int anio, int mes, int dia, int hora,
                                int minuto, List<Bloqueo> bloqueos, int tipoSimulacion) {

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
                    // ✅ NUEVA LÓGICA: Solo usar conversión si cruza meses
                    if (fechaPedido.getMonthValue() != fechaTimer.getMonthValue() ||
                            fechaPedido.getYear() != fechaTimer.getYear()) {
                        posicionActual.setTiempoInicio(convertirTiempoReal(pedido));
                    } else {
                        posicionActual.setTiempoInicio(pedido.getHoraDeInicio());
                    }
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
                    anio, mes, camion, pedido, tipoSimulacion, fechaBaseSimulacion);

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

    public void asignarPedidosACamionesVacios(int primeraVez, double timer, int anio, int mes, int dia, int hora, int minuto) {
        NodoMapa posicionActual = null;
        for (Camion camion : camiones) {
            if (camion.isEnAveria()) continue;
            if (camion.getPedidosAsignados() == null || camion.getPedidosAsignados().isEmpty()) {
                camion.setPedidosAsignados(new ArrayList<>());
                posicionActual = camion.getUbicacionActual();
                for (int i = 0; i < pedidos.size(); i++) {
                    Pedido pedidoMax = proximoPedidoFD(camion, timer, 2, anio, mes, dia, hora, minuto);
                    if (pedidoMax == null) {
                        break;
                    }
                    if (camion.tieneCapacidad(pedidoMax.getCantidadGLP())) {
                        camion.asignar(pedidoMax);
                        camion.getPedidosAsignados().add(pedidoMax);
                        pedidoMax.setEntregado(true);
                    }

                }
                if (primeraVez == 0) {
                    camion.setUbicacionActual(null);
                } else {
                    camion.setUbicacionActual(posicionActual);
                }
            }
        }
    }

    public void asignarPedidosACamionesLlenos(int primeraVez, double timer, int anio, int mes, int dia, int hora, int minuto) {
        NodoMapa posicionActual;
        for (Camion camion : camiones) {

            if (camion.isEnAveria()) {
                continue;
            }

            camion.setPedidosAsignados(new ArrayList<>());
            posicionActual = camion.getUbicacionActual();
            for (int i = 0; i < pedidos.size(); i++) {
                Pedido pedidoMax = proximoPedidoFD(camion, timer, 2, anio, mes, dia, hora, minuto);
                if (pedidoMax == null) {
                    break;
                }
                if (camion.tieneCapacidad(pedidoMax.getCantidadGLP())) {

                    camion.asignar(pedidoMax);
                    camion.getPedidosAsignados().add(pedidoMax);
                    pedidoMax.setEntregado(true);
                }

            }
            if (primeraVez == 0) {
                camion.setUbicacionActual(null);
            } else {
                camion.setUbicacionActual(posicionActual);
            }
        }
    }

    public void planificarPedidos(int primeraVez, double timer, int anio, int mes, int dia, int hora, int minuto) {
        boolean camionVacio = false;
        for (Camion camion : camiones) {
            if (
                    camion.getPedidosAsignados() == null
                            || camion.isEnAveria()
                            || camion.getPedidosAsignados().isEmpty()
            ) {
                camionVacio = true;
                break;
            }
        }
        verificacionMomentanea();

        if (camionVacio)
            asignarPedidosACamionesVacios(primeraVez, timer, anio, mes, dia, hora, minuto);
        else
            asignarPedidosACamionesLlenos(primeraVez, timer, anio, mes, dia, hora, minuto);

    }

    public void planificarPedidosDiaria(int primeraVez, double timer, int anio, int mes, int dia, int hora, int minuto) {
        NodoMapa posicionActual;
        for (Camion camion : camiones) {
            if (camion.isEnAveria()) {
                continue;
            }
            camion.setPedidosAsignados(new ArrayList<>());
            posicionActual = camion.getUbicacionActual();
            for (int i = 0; i < pedidos.size(); i++) {
                Pedido pedidoMax = proximoPedidoFD(camion, timer, 1, anio, mes, dia, hora, minuto);
                if (pedidoMax == null) {
                    break;
                }
                camion.asignar(pedidoMax);
                camion.getPedidosAsignados().add(pedidoMax);
                pedidoMax.setEntregado(true);
            }
            if (primeraVez == 0) {
                camion.setUbicacionActual(null);
            } else {
                camion.setUbicacionActual(posicionActual);
            }
        }
    }

    public Pedido proximoPedidoFD(Camion camion, double timer, int tipoSimulacion,
                                  int anio, int mes, int dia, int hora, int minuto) {
        if (tipoSimulacion == 1) {
            double max = Double.POSITIVE_INFINITY;
            Pedido pedidoMax = null;
            for (Pedido pedido : pedidos) {
                if (!pedido.isEntregado()) {
                    double distance = camion.getUbicacionActual().calcularDistancia(new NodoMapa(0, pedido.getPosX(),
                            pedido.getPosY(), false));
                    double tiempoViaje = (distance / camion.getVelocidad()) * 60;
                    // USAR función de conversión
                    LocalDateTime fechaTimer = LocalDateTime.of(anio, mes, dia, hora, minuto);
                    LocalDateTime fechaPedido = pedido.getFechaDeRegistro();

                    double tiempoEntrega;
                    if (fechaPedido.getMonthValue() != fechaTimer.getMonthValue() ||
                            fechaPedido.getYear() != fechaTimer.getYear()) {
                        tiempoEntrega = convertirTiempoEntregaReal(pedido);
                    } else {
                        tiempoEntrega = pedido.getTiempoLlegada();
                    }
                    double tiempoActual = timer;
                    if (camion.tieneCapacidad(pedido.getCantidadGLP())) {
                        if (tiempoActual + tiempoViaje <= tiempoEntrega) {
                            if (distance < max) {
                                max = distance;
                                pedidoMax = pedido;
                            }
                        }
                    }
                }
            }
            return pedidoMax;
        } else {
            double max = Double.POSITIVE_INFINITY;
            Pedido pedidoMax = null;
            for (Pedido pedido : pedidos) {
                if (!pedido.isEntregado()) {
                    double distance = camion.getUbicacionActual().calcularDistancia(new NodoMapa(0, pedido.getPosX(),
                            pedido.getPosY(), false));
                    double tiempoViaje = (distance / camion.getVelocidad()) * 60;
                    // Misma lógica de conversión
                    LocalDateTime fechaTimer = LocalDateTime.of(anio, mes, dia, hora, minuto);
                    LocalDateTime fechaPedido = pedido.getFechaDeRegistro();

                    double tiempoEntrega;
                    if (fechaPedido.getMonthValue() != fechaTimer.getMonthValue() ||
                            fechaPedido.getYear() != fechaTimer.getYear()) {
                        tiempoEntrega = convertirTiempoEntregaReal(pedido);
                    } else {
                        tiempoEntrega = pedido.getTiempoLlegada();
                    }
                    double tiempoActual = timer;

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
                        matrizPoblacion[i][j][k][l] = 0.1;
                    }
                }
            }
        }
    }

    public void vaciarRutas() {
        for (Camion camion : camiones)
            camion.setRoute(new ArrayList<>());
    }

    /* Funciones añadidas por desincronización al pasar a un siguiente mes */
    private long convertirTiempoReal(Pedido pedido) {
        if (fechaBaseSimulacion == null) {
            // Fallback al cálculo original si no se configuró
            return pedido.getHoraDeInicio();
        }
        return ChronoUnit.MINUTES.between(fechaBaseSimulacion, pedido.getFechaDeRegistro());
    }

    private long convertirTiempoEntregaReal(Pedido pedido) {
        if (fechaBaseSimulacion == null) {
            return pedido.getTiempoLlegada();
        }
        return ChronoUnit.MINUTES.between(fechaBaseSimulacion, pedido.getFechaEntrega());
    }
}
