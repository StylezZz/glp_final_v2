package pucp.edu.pe.glp_final.algorithm;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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

    // Id
    private long id; // Identificador único de la simulación
    private Mapa mapa;
    private List<Camion> camiones;
    private List<Pedido> pedidos;
    private List<Pedido> pedidosCompletos;
    private List<Pedido> pedidosNuevos;
    private List<Bloqueo> bloqueosActivos;
    private double[][][][] matrizPoblacion;
    private boolean inicio;
    private LocalDateTime fechaBaseSimulacion;
    private String SIM;

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
        if (tipoSimulacion == 1) {
            this.SIM = "DIA";
        } else if (tipoSimulacion == 2) {
            this.SIM = "SEMANA";
        } else {
            this.SIM = "COLAPSO";
        }

        // Generamos el id
        this.id = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toEpochSecond(java.time.ZoneOffset.UTC);

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
        System.out.println("\n+++ SIMULACION RUTEO START +++");
        System.out.println("Parámetros:");
        System.out.println("  - Fecha: " + anio + "-" + mes + "-" + dia + " " + hora + ":" + minuto);
        System.out.println("  - MinutosPorIteracion: " + minutosPorIteracion);
        System.out.println("  - PedidosDia: " + pedidosDia.size());
        System.out.println("  - Bloqueos: " + bloqueos.size());
        System.out.println("  - PedidoOriginal: " + pedidoOriginal.size());
        System.out.println("  - PrimeraVez: " + primeraVez);
        System.out.println("  - Momento: " + momento);
        System.out.println("  - TipoSimulacion: " + tipoSimulacion);

        getPedidosDia(anio, mes, dia, hora, minuto, pedidosDia, minutosPorIteracion, pedidoOriginal, tipoSimulacion);

        System.out.println("Después de getPedidosDia:");
        System.out.println("  - this.pedidos.size(): " + this.pedidos.size());
        System.out.println("  - this.pedidosCompletos.size(): " + this.pedidosCompletos.size());

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, anio);
        calendar.set(Calendar.MONTH, mes - 1);
        calendar.set(Calendar.DAY_OF_MONTH, dia);
        calendar.set(Calendar.HOUR_OF_DAY, hora);
        calendar.set(Calendar.MINUTE, minuto);

        bloqueosActivos = Bloqueo.obtenerBloqueosActivos(bloqueos, calendar);

        System.out.println("Llamando a ejecutar...");
        ejecutar(anio, mes, dia, hora, minuto, bloqueos, primeraVez, momento, tipoSimulacion);

        System.out.println("Después de ejecutar, llamando a validarTiempoRuta...");
        validarTiempoRuta(anio, mes, dia, hora, minuto, minutosPorIteracion, tipoSimulacion);

        System.out.println("+++ SIMULACION RUTEO END +++\n");
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
                    if (
                            (mes == ubicacion.getMes() && anio == ubicacion.getAnio())
                                    || (mes == ubicacion.getMes() + 1 && anio == ubicacion.getAnio())
                                    || (mes == 1 && ubicacion.getMes() == 12 && anio == ubicacion.getAnio() + 1)
                    ) {
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
                    if (
                            (mes == ubicacion.getMes() && anio == ubicacion.getAnio())
                                    || (mes == ubicacion.getMes() + 1 && anio == ubicacion.getAnio())
                                    || (mes == 1 && ubicacion.getMes() == 12 && anio == ubicacion.getAnio() + 1)
                    ) {
                        double startTime = timer;
                        if ((int) ubicacion.getTiempoFin() <= startTime) { // ACA
                            distanciaRecorrida = getDistanciaRecorrida(anio, mes, camion, distanciaRecorrida, ubicacion);
                            if (ubicacion.isEsAlmacen()) {
                                for (Almacen deposito2 : mapa.getAlmacenes()) {
                                    gestionarCapacidadesAlmacen(hora, minuto, camion, deposito2);
                                }
                                ubicacion.setTiempoInicio(startTime);
                                ubicacion.setTiempoFin(startTime + 1);
                            }

                        } else {
                            if (ubicacion.isEsPedido()) {
                                LocalDateTime fechaActual = LocalDateTime.of(anio,mes,
                                        ((int)timer/1440),(((int)timer%1440)/60),((int)timer%60));
                                LocalDateTime fechaLimite = ubicacion.getPedido().getFechaEntrega();
                                if(fechaActual.isAfter(fechaLimite)){
                                    ubicacion.getPedido().setEntregado(false);
                                    ubicacion.getPedido().setAsignado(false);
                                    ubicacion.getPedido().setIdCamion(null);
                                }
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

    private boolean esUbicacionAnteriorOIgual(NodoMapa ubicacion, int anioActual, int mesActual) {
        // Si año anterior, siempre true
        if (ubicacion.getAnio() < anioActual) return true;

        // Si año posterior, siempre false
        if (ubicacion.getAnio() > anioActual) return false;

        // Mismo año: comparar meses
        return ubicacion.getMes() <= mesActual;
    }

    private double getDistanciaRecorrida(int anio, int mes, Camion camion, double distanciaRecorrida, NodoMapa ubicacion) {
        distanciaRecorrida += 1;
        camion.setUbicacionActual(ubicacion);
        camion.setDistanciaRecorrida(distanciaRecorrida);
        camion.getUbicacionActual().setEsRuta(false);
        if (ubicacion.isEsPedido()) {
            if (esUbicacionAnteriorOIgual(ubicacion, anio, mes)) {
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
                    nodoCentral.setTiempoFin(camion.getTiempoFinAveria() + 1);
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
                        dia,
                        hora,
                        minuto,
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
        System.out.println("=== EJECUTAR START ===");
        System.out.println("Timer: " + timer + ", TipoSim: " + tipoSimulacion + ", PrimeraVez: " + primeraVez);
        System.out.println("Fecha actual: " + anio + "-" + mes + "-" + dia + " " + hora + ":" + minuto);
        System.out.println("Total pedidos disponibles: " + pedidos.size());
        // Log current truck assignments
        for (Camion camion : camiones) {
            System.out.println("Camión " + camion.getCodigo() + ":");
            System.out.println("  - En avería: " + camion.isEnAveria());
            System.out.println("  - Ubicación actual: " + (camion.getUbicacionActual() != null ?
                    camion.getUbicacionActual().getX() + "," + camion.getUbicacionActual().getY() : "null"));
            System.out.println("  - Pedidos asignados: " + (camion.getPedidosAsignados() != null ?
                    camion.getPedidosAsignados().size() : "null"));
            if (camion.getPedidosAsignados() != null) {
                for (Pedido p : camion.getPedidosAsignados()) {
                    System.out.println("    * Pedido " + p.getId() + " en " + p.getPosX() + "," + p.getPosY() +
                            " - Entrega: " + p.getFechaEntrega() + " - Entregado: " + p.isEntregado());
                }
            }
            System.out.println("  - Ruta actual: " + (camion.getRoute() != null ? camion.getRoute().size() + " nodos" : "null"));
        }

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

        System.out.println("=== ANTES DEL SORTING ===");
        for (Pedido p : pedidos) {
            Duration tiempoRestante = Duration.between(fechaActual, p.getFechaEntrega());
            System.out.println("Pedido " + p.getId() + " en " + p.getPosX() + "," + p.getPosY() +
                    " - Tiempo restante: " + tiempoRestante.toMinutes() + " min - Entregado: " + p.isEntregado() +
                    " - Asignado: " + p.isAsignado());
        }

//        Comparator<Pedido> comparadorPorProximidad = Comparator
//                .comparing(pedido -> Duration.between(fechaActual, pedido.getFechaEntrega()).abs());
//        pedidos.sort(comparadorPorProximidad);

        Comparator<Pedido> comparadorPorUrgencia = Comparator
                .comparing((Pedido pedido) -> {
                    Duration tiempoRestante = Duration.between(fechaActual, pedido.getFechaEntrega());
                    // If delivery time has passed, give maximum priority (negative value)
                    if (tiempoRestante.isNegative()) {
                        return tiempoRestante.toMinutes(); // Most negative first
                    }
                    // Otherwise, sort by time remaining (ascending - less time remaining = higher priority)
                    return tiempoRestante.toMinutes();
                })
                .thenComparing(pedido -> pedido.getPriodidad()); // Secondary sort by priority if same urgency

        pedidos.sort(comparadorPorUrgencia);

        System.out.println("=== DESPUÉS DEL SORTING ===");
        for (Pedido p : pedidos) {
            Duration tiempoRestante = Duration.between(fechaActual, p.getFechaEntrega());
            System.out.println("Pedido " + p.getId() + " en " + p.getPosX() + "," + p.getPosY() +
                    " - Tiempo restante: " + tiempoRestante.toMinutes() + " min - Entregado: " + p.isEntregado() +
                    " - Asignado: " + p.isAsignado());
        }

        // 4. ADD THIS LOG BEFORE REROUTING CHECK
        System.out.println("=== CHECKING REROUTING CONDITIONS ===");
        System.out.println("TipoSimulacion: " + tipoSimulacion + ", PrimeraVez: " + primeraVez);

        if (tipoSimulacion == 2 && primeraVez != 0) {
            System.out.println("*** CALLING REASIGNAR PEDIDOS URGENTES ***");
            reasignarPedidosUrgentes(fechaActual, tipoSimulacion);
        } else {
            System.out.println("*** SKIPPING REROUTING: tipoSim=" + tipoSimulacion + ", primeraVez=" + primeraVez + " ***");
        }

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

        System.out.println("=== EJECUTAR END SUMMARY ===");
        for (Camion camion : camiones) {
            System.out.println("Camión " + camion.getCodigo() + " resultado final:");
            System.out.println("  - Pedidos asignados: " + (camion.getPedidosAsignados() != null ?
                    camion.getPedidosAsignados().size() : "null"));
            if (camion.getPedidosAsignados() != null) {
                for (Pedido p : camion.getPedidosAsignados()) {
                    System.out.println("    * Pedido " + p.getId() + " en " + p.getPosX() + "," + p.getPosY());
                }
            }
            System.out.println("  - Nodos en ruta: " + (camion.getRoute() != null ?
                    camion.getRoute().size() : "null"));
        }
        System.out.println("=========================\n");

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
                if (pedido.isEntregado() && pedido.isEntregadoCompleto()) {
                    iterator.remove();
                }else if (pedido.isEntregado() && !pedido.isEntregadoCompleto()){
                    pedido.setEntregado(false);
                    pedido.setAsignado(false);
                    pedido.setIdCamion(null);
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
                if (
                        (anio == posicion.getAnio() && mes == posicion.getMes())
                                || (anio == posicion.getAnio() && mes == posicion.getMes() + 1)
                                || (anio == posicion.getAnio() + 1 && mes == 1 && posicion.getMes() == 12)
                ) {
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
                anio, mes, dia, hora, minuto, camion, null, tipoSimulacion, fechaBaseSimulacion);

        if (tipoSimulacion == 1) {
            camion.getUbicacionActual().setTiempoFin(camion.getUbicacionActual().getTiempoInicio() + 1);
        } else {
            camion.getUbicacionActual().setTiempoFin(camion.getUbicacionActual().getTiempoInicio() + 1);
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
                    anio, mes, dia, hora, minuto, camion, pedido, tipoSimulacion, fechaBaseSimulacion);

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
        System.out.println("=== VERIFICACION MOMENTANEA ===");
        for (Camion camion : camiones) {
            if (camion.getPedidosAsignados() == null)
                continue;
            System.out.println("Camión " + camion.getCodigo() + " - Pedidos asignados: " + camion.getPedidosAsignados().size());
            for (Pedido pedido : camion.getPedidosAsignados()) {
                // FIXED: Only mark as assigned, NOT as delivered
                pedido.setAsignado(true);
                pedido.setIdCamion(camion.getCodigo());
                // Do NOT set entregado = true here!
                System.out.println("  - Pedido " + pedido.getId() + " asignado (no entregado aún)");
            }
        }
        System.out.println("===============================");
    }

    public void asignarPedidosACamionesVacios(int primeraVez, double timer, int anio, int mes, int dia, int hora, int minuto) {
        System.out.println("=== ASIGNAR PEDIDOS A CAMIONES VACÍOS ===");

        NodoMapa posicionActual = null;
        for (Camion camion : camiones) {
            System.out.println("Procesando camión: " + camion.getCodigo());

            if (camion.isEnAveria()) {
                System.out.println("  -> SALTANDO: Camión en avería");
                continue;
            }

            if (camion.getPedidosAsignados() == null || camion.getPedidosAsignados().isEmpty()) {
                camion.setPedidosAsignados(new ArrayList<>());
                posicionActual = camion.getUbicacionActual();

                for (int i = 0; i < pedidos.size(); i++) {
                    Pedido pedidoMax = proximoPedidoFD(camion, timer, 2, anio, mes, dia, hora, minuto);

                    if (pedidoMax == null) {
                        System.out.println("  -> NO HAY MÁS PEDIDOS DISPONIBLES");
                        break;
                    }

                    if (camion.tieneCapacidad(pedidoMax.getCantidadGLP())) {
                        System.out.println("  -> ASIGNANDO PEDIDO " + pedidoMax.getId() + " (NO marcando como entregado)");

                        // FIXED: Only assign, don't mark as delivered
                        camion.asignar(pedidoMax);
                        camion.getPedidosAsignados().add(pedidoMax);

                        // CORRECT: Mark as assigned only
                        pedidoMax.setAsignado(true);
                        pedidoMax.setIdCamion(camion.getCodigo());
                        // REMOVED: pedidoMax.setEntregado(true); ← This was wrong!

                    } else {
                        System.out.println("  -> SIN CAPACIDAD para pedido " + pedidoMax.getId());
                    }
                }

                if (primeraVez == 0) {
                    camion.setUbicacionActual(null);
                } else {
                    camion.setUbicacionActual(posicionActual);
                }
            }
        }

        System.out.println("=== FIN ASIGNAR PEDIDOS A CAMIONES VACÍOS ===\n");
    }


    public void asignarPedidosACamionesLlenos(int primeraVez, double timer, int anio, int mes, int dia, int hora, int minuto) {
        System.out.println("=== ASIGNAR PEDIDOS A CAMIONES LLENOS ===");

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
                    System.out.println("  -> ASIGNANDO PEDIDO " + pedidoMax.getId() + " a camión " + camion.getCodigo());

                    // FIXED: Only assign, don't mark as delivered
                    camion.asignar(pedidoMax);
                    camion.getPedidosAsignados().add(pedidoMax);

                    // CORRECT: Mark as assigned only
                    pedidoMax.setAsignado(true);
                    pedidoMax.setIdCamion(camion.getCodigo());
                    // REMOVED: pedidoMax.setEntregado(true); ← This was wrong!
                }
            }

            if (primeraVez == 0) {
                camion.setUbicacionActual(null);
            } else {
                camion.setUbicacionActual(posicionActual);
            }
        }

        System.out.println("=== FIN ASIGNAR PEDIDOS A CAMIONES LLENOS ===\n");
    }


    public void planificarPedidos(int primeraVez, double timer, int anio, int mes, int dia, int hora, int minuto) {
        System.out.println("=== PLANIFICAR PEDIDOS START ===");
        System.out.println("PrimeraVez: " + primeraVez + ", Timer: " + timer);

        boolean camionVacio = false;
        for (Camion camion : camiones) {
            if (
                    camion.getPedidosAsignados() == null
                            || camion.isEnAveria()
                            || camion.getPedidosAsignados().isEmpty()
            ) {
                camionVacio = true;
                System.out.println("Camión vacío encontrado: " + camion.getCodigo() +
                        " (avería: " + camion.isEnAveria() +
                        ", pedidos: " + (camion.getPedidosAsignados() != null ? camion.getPedidosAsignados().size() : "null") + ")");
                break;
            }
        }

        verificacionMomentanea();

        System.out.println("Hay camión vacío: " + camionVacio);
        if (camionVacio) {
            System.out.println("*** ASIGNANDO PEDIDOS A CAMIONES VACÍOS ***");
            asignarPedidosACamionesVacios(primeraVez, timer, anio, mes, dia, hora, minuto);
        } else {
            System.out.println("*** ASIGNANDO PEDIDOS A CAMIONES LLENOS ***");
            asignarPedidosACamionesLlenos(primeraVez, timer, anio, mes, dia, hora, minuto);
        }

        System.out.println("=== PLANIFICAR PEDIDOS END ===\n");
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
        System.out.println("  [PROXIMO_PEDIDO] Camión " + camion.getCodigo() + ", Timer: " + timer +
                ", TipoSim: " + tipoSimulacion);
        System.out.println("  [PROXIMO_PEDIDO] Ubicación camión: " +
                (camion.getUbicacionActual() != null ?
                        camion.getUbicacionActual().getX() + "," + camion.getUbicacionActual().getY() : "null"));

        LocalDateTime fechaTimer = LocalDateTime.of(anio, mes, dia, hora, minuto);
        System.out.println("  [PROXIMO_PEDIDO] Fecha timer: " + fechaTimer);

        if (tipoSimulacion == 1) {
            // Daily simulation logic
            Pedido pedidoSeleccionado = null;
            double mejorScore = Double.NEGATIVE_INFINITY;

            System.out.println("  [PROXIMO_PEDIDO] Evaluando " + pedidos.size() + " pedidos para simulación diaria");

            for (Pedido pedido : pedidos) {
                System.out.println("    [EVAL] Pedido " + pedido.getId() + " en " + pedido.getPosX() + "," + pedido.getPosY());
                System.out.println("    [EVAL] Entregado: " + pedido.isEntregado() +
                        ", Asignado: " + pedido.isAsignado() +
                        ", Cantidad: " + pedido.getCantidadGLP());

                // FIXED: Check for both not delivered AND not assigned (or assigned to this truck)
                boolean disponible = !pedido.isEntregado() &&
                        (!pedido.isAsignado() || (pedido.getIdCamion() != null && pedido.getIdCamion().equals(camion.getCodigo())));

                System.out.println("    [EVAL] Disponible para este camión: " + disponible);

                if (disponible && camion.tieneCapacidad(pedido.getCantidadGLP())) {
                    double distance = camion.getUbicacionActual().calcularDistancia(new NodoMapa(0, pedido.getPosX(),
                            pedido.getPosY(), false));
                    double tiempoViaje = (distance / camion.getVelocidad()) * 60;

                    LocalDateTime fechaPedido = pedido.getFechaDeRegistro();
                    double tiempoEntrega;
                    if (fechaPedido.getMonthValue() != fechaTimer.getMonthValue() ||
                            fechaPedido.getYear() != fechaTimer.getYear()) {
                        tiempoEntrega = convertirTiempoEntregaReal(pedido);
                    } else {
                        tiempoEntrega = pedido.getTiempoLlegada();
                    }

                    double tiempoActual = timer;
                    double tiempoRestante = tiempoEntrega - tiempoActual;

                    System.out.println("    [EVAL] Distancia: " + distance + ", TiempoViaje: " + tiempoViaje);
                    System.out.println("    [EVAL] TiempoEntrega: " + tiempoEntrega + ", TiempoActual: " + tiempoActual);
                    System.out.println("    [EVAL] TiempoRestante: " + tiempoRestante);
                    System.out.println("    [EVAL] Puede llegar a tiempo: " + (tiempoActual + tiempoViaje <= tiempoEntrega));

                    if (tiempoActual + tiempoViaje <= tiempoEntrega) {
                        double scoreUrgencia = 1000.0 / (tiempoRestante + 1);
                        double scoreDistancia = 100.0 / (distance + 1);
                        double scoreFinal = (scoreUrgencia * 3) + scoreDistancia;

                        System.out.println("    [EVAL] ScoreUrgencia: " + scoreUrgencia +
                                ", ScoreDistancia: " + scoreDistancia + ", ScoreFinal: " + scoreFinal);

                        if (scoreFinal > mejorScore) {
                            mejorScore = scoreFinal;
                            pedidoSeleccionado = pedido;
                            System.out.println("    [EVAL] *** NUEVO MEJOR PEDIDO ***");
                        }
                    }
                } else {
                    System.out.println("    [EVAL] -> DESCARTADO (entregado: " + pedido.isEntregado() +
                            ", asignado: " + pedido.isAsignado() +
                            ", sin capacidad: " + !camion.tieneCapacidad(pedido.getCantidadGLP()) + ")");
                }
            }

            System.out.println("  [PROXIMO_PEDIDO] Pedido seleccionado: " +
                    (pedidoSeleccionado != null ? "Pedido " + pedidoSeleccionado.getId() : "null"));
            return pedidoSeleccionado;

        } else {
            // Weekly simulation logic
            System.out.println("  [PROXIMO_PEDIDO] Evaluando " + pedidos.size() + " pedidos para simulación semanal");

            Pedido pedidoMasUrgente = null;
            double menorTiempoRestante = Double.POSITIVE_INFINITY;

            for (Pedido pedido : pedidos) {
                System.out.println("    [EVAL] Pedido " + pedido.getId() + " en " + pedido.getPosX() + "," + pedido.getPosY());
                System.out.println("    [EVAL] Entregado: " + pedido.isEntregado() +
                        ", Asignado: " + pedido.isAsignado());

                // FIXED: Check for both not delivered AND not assigned (or assigned to this truck)
                boolean disponible = !pedido.isEntregado() &&
                        (!pedido.isAsignado() || (pedido.getIdCamion() != null && pedido.getIdCamion().equals(camion.getCodigo())));

                System.out.println("    [EVAL] Disponible para este camión: " + disponible);

                if (disponible) {
                    double distance = camion.getUbicacionActual().calcularDistancia(new NodoMapa(0, pedido.getPosX(),
                            pedido.getPosY(), false));
                    double tiempoViaje = (distance / camion.getVelocidad()) * 60;

                    LocalDateTime fechaPedido = pedido.getFechaDeRegistro();
                    double tiempoEntrega;
                    if (fechaPedido.getMonthValue() != fechaTimer.getMonthValue() ||
                            fechaPedido.getYear() != fechaTimer.getYear()) {
                        tiempoEntrega = convertirTiempoEntregaReal(pedido);
                    } else {
                        tiempoEntrega = pedido.getTiempoLlegada();
                    }

                    double tiempoActual = timer;
                    double tiempoRestante = tiempoEntrega - tiempoActual;

                    System.out.println("    [EVAL] Distancia: " + distance + ", TiempoViaje: " + tiempoViaje);
                    System.out.println("    [EVAL] TiempoEntrega: " + tiempoEntrega + ", TiempoActual: " + tiempoActual);
                    System.out.println("    [EVAL] TiempoRestante: " + tiempoRestante);
                    System.out.println("    [EVAL] Puede llegar a tiempo: " + (tiempoActual + tiempoViaje <= tiempoEntrega));

                    if (tiempoActual + tiempoViaje <= tiempoEntrega) {
                        if (tiempoRestante < menorTiempoRestante) {
                            menorTiempoRestante = tiempoRestante;
                            pedidoMasUrgente = pedido;
                            System.out.println("    [EVAL] *** NUEVO PEDIDO MÁS URGENTE ***");
                        }
                    } else {
                        System.out.println("    [EVAL] *** PEDIDO VENCIDO - RETORNANDO INMEDIATAMENTE ***");
                        return pedido;
                    }
                } else {
                    System.out.println("    [EVAL] -> DESCARTADO (entregado: " + pedido.isEntregado() +
                            ", asignado a otro: " + (pedido.isAsignado() && !camion.getCodigo().equals(pedido.getIdCamion())) + ")");
                }
            }

            System.out.println("  [PROXIMO_PEDIDO] Pedido más urgente seleccionado: " +
                    (pedidoMasUrgente != null ? "Pedido " + pedidoMasUrgente.getId() : "null"));
            return pedidoMasUrgente;
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

    // Add this new method to Genetico.java
    public void reasignarPedidosUrgentes(LocalDateTime fechaActual, int tipoSimulacion) {
        System.out.println("=== ENHANCED REASIGNAR PEDIDOS URGENTES START ===");
        System.out.println("Fecha actual: " + fechaActual);

        // Get ALL urgent orders (including newly assigned ones)
        // FIXED: Filter by delivery status, not assignment status
        List<Pedido> todosLosUrgentes = pedidos.stream()
                .filter(p -> !p.isEntregado()) // Only non-delivered (assignment doesn't matter)
                .filter(p -> {
                    Duration tiempoRestante = Duration.between(fechaActual, p.getFechaEntrega());
                    return tiempoRestante.toHours() < 4; // Urgent: < 4 hours remaining
                })
                .sorted(Comparator.comparing((Pedido p) -> Duration.between(fechaActual, p.getFechaEntrega()).toMinutes()))
                .collect(Collectors.toList());

        System.out.println("Total pedidos urgentes (entregados y no entregados): " + todosLosUrgentes.size());
        for (Pedido p : todosLosUrgentes) {
            Duration tiempoRestante = Duration.between(fechaActual, p.getFechaEntrega());
            System.out.println("  - Pedido urgente " + p.getId() + " en " + p.getPosX() + "," + p.getPosY() +
                    " - Tiempo restante: " + tiempoRestante.toMinutes() + " min - Asignado: " + p.isAsignado() +
                    " - Camión: " + p.getIdCamion() + " - Entregado: " + p.isEntregado());
        }

        if (todosLosUrgentes.isEmpty()) {
            System.out.println("*** NO HAY PEDIDOS URGENTES - SALIENDO ***");
            return;
        }

        // Find the most urgent unassigned order
        // FIXED: Check assignment status correctly
        Pedido pedidoMasUrgenteNoAsignado = todosLosUrgentes.stream()
                .filter(p -> !p.isAsignado()) // Only truly unassigned orders
                .findFirst()
                .orElse(null);

        if (pedidoMasUrgenteNoAsignado == null) {
            System.out.println("*** TODOS LOS PEDIDOS URGENTES YA ESTÁN ASIGNADOS ***");
            // Check if we need to swap assignments between trucks
            verificarIntercambiosUrgentes(todosLosUrgentes, fechaActual);
            return;
        }

        System.out.println("\n--- Procesando pedido MÁS urgente no asignado: " + pedidoMasUrgenteNoAsignado.getId() + " ---");
        Duration urgenciaMaxima = Duration.between(fechaActual, pedidoMasUrgenteNoAsignado.getFechaEntrega());
        System.out.println("Urgencia máxima: " + urgenciaMaxima.toMinutes() + " min");

        // Find truck with LEAST urgent assigned orders
        Camion mejorCandidato = null;
        Pedido pedidoMenosUrgente = null;
        Duration mayorTiempoDisponible = Duration.ZERO;

        System.out.println("Evaluando camiones para reasignación:");
        for (Camion camion : camiones) {
            System.out.println("  Camión " + camion.getCodigo() + ":");
            System.out.println("    - En avería: " + camion.isEnAveria());
            System.out.println("    - Pedidos asignados: " + (camion.getPedidosAsignados() != null ?
                    camion.getPedidosAsignados().size() : "null"));

            if (camion.isEnAveria()) {
                System.out.println("    -> DESCARTADO: En avería");
                continue;
            }

            if (camion.getPedidosAsignados() == null || camion.getPedidosAsignados().isEmpty()) {
                System.out.println("    -> DISPONIBLE PARA ASIGNACIÓN DIRECTA");
                // Truck is available, assign directly
                if (camion.tieneCapacidad(pedidoMasUrgenteNoAsignado.getCantidadGLP())) {
                    System.out.println("    -> *** ASIGNACIÓN DIRECTA ***");
                    if (camion.getPedidosAsignados() == null) {
                        camion.setPedidosAsignados(new ArrayList<>());
                    }
                    camion.getPedidosAsignados().add(pedidoMasUrgenteNoAsignado);
                    pedidoMasUrgenteNoAsignado.setAsignado(true);
                    pedidoMasUrgenteNoAsignado.setIdCamion(camion.getCodigo());
                    // FIXED: Don't mark as delivered!
                    camion.setRoute(new ArrayList<>()); // Clear route for replanning
                    System.out.println("*** ASIGNACIÓN DIRECTA COMPLETADA ***");
                    return;
                }
                continue;
            }

            // Find least urgent order assigned to this truck
            // FIXED: Only consider non-delivered orders
            Pedido menosUrgenteCamion = camion.getPedidosAsignados().stream()
                    .filter(p -> !p.isEntregado()) // Only non-delivered orders
                    .max(Comparator.comparing((Pedido p) -> Duration.between(fechaActual, p.getFechaEntrega()).toMinutes()))
                    .orElse(null);

            System.out.println("    - Pedido menos urgente: " + (menosUrgenteCamion != null ?
                    "Pedido " + menosUrgenteCamion.getId() : "ninguno"));

            if (menosUrgenteCamion != null) {
                Duration tiempoDisponible = Duration.between(fechaActual, menosUrgenteCamion.getFechaEntrega());
                System.out.println("    - Tiempo disponible: " + tiempoDisponible.toMinutes() + " min");
                System.out.println("    - Es menos urgente que pedido objetivo: " +
                        (tiempoDisponible.compareTo(urgenciaMaxima) > 0));
                System.out.println("    - Tiene más tiempo que mejor candidato actual: " +
                        (tiempoDisponible.compareTo(mayorTiempoDisponible) > 0));

                // If this truck's least urgent order is less urgent than our most urgent unassigned order
                if (tiempoDisponible.compareTo(urgenciaMaxima) > 0 &&
                        tiempoDisponible.compareTo(mayorTiempoDisponible) > 0 &&
                        camion.tieneCapacidad(pedidoMasUrgenteNoAsignado.getCantidadGLP())) {

                    mejorCandidato = camion;
                    pedidoMenosUrgente = menosUrgenteCamion;
                    mayorTiempoDisponible = tiempoDisponible;
                    System.out.println("    -> NUEVO MEJOR CANDIDATO PARA INTERCAMBIO");
                }
            }
        }

        // Execute reassignment if found
        if (mejorCandidato != null && pedidoMenosUrgente != null) {
            System.out.println("*** EJECUTANDO INTERCAMBIO ***");
            System.out.println("Camión: " + mejorCandidato.getCodigo());
            System.out.println("Removiendo pedido: " + pedidoMenosUrgente.getId());
            System.out.println("Asignando pedido urgente: " + pedidoMasUrgenteNoAsignado.getId());

            // Remove less urgent order
            mejorCandidato.getPedidosAsignados().remove(pedidoMenosUrgente);
            pedidoMenosUrgente.setAsignado(false);
            pedidoMenosUrgente.setIdCamion(null);

            // Assign urgent order
            mejorCandidato.getPedidosAsignados().add(pedidoMasUrgenteNoAsignado);
            pedidoMasUrgenteNoAsignado.setAsignado(true);
            pedidoMasUrgenteNoAsignado.setIdCamion(mejorCandidato.getCodigo());
            // FIXED: Don't mark as delivered!

            // Clear route for replanning
            mejorCandidato.setRoute(new ArrayList<>());

            System.out.println("*** INTERCAMBIO COMPLETADO ***");
        } else {
            System.out.println("*** NO SE PUDO REALIZAR INTERCAMBIO ***");
        }

        System.out.println("=== ENHANCED REASIGNAR PEDIDOS URGENTES END ===\n");
    }

    private void debugUrgentOrderDetection(LocalDateTime fechaActual) {
        System.out.println("\n=== DEBUG URGENT ORDER DETECTION ===");
        System.out.println("Fecha actual: " + fechaActual);
        System.out.println("Total pedidos en lista: " + pedidos.size());

        for (Pedido p : pedidos) {
            Duration tiempoRestante = Duration.between(fechaActual, p.getFechaEntrega());
            boolean esUrgente = tiempoRestante.toHours() < 4;
            boolean noEntregado = !p.isEntregado();
            boolean noAsignado = !p.isAsignado();

            System.out.println("Pedido " + p.getId() + ":");
            System.out.println("  - Posición: " + p.getPosX() + "," + p.getPosY());
            System.out.println("  - Fecha entrega: " + p.getFechaEntrega());
            System.out.println("  - Tiempo restante: " + tiempoRestante.toMinutes() + " min (" + tiempoRestante.toHours() + " horas)");
            System.out.println("  - Es urgente (< 4h): " + esUrgente);
            System.out.println("  - No entregado: " + noEntregado);
            System.out.println("  - No asignado: " + noAsignado);
            System.out.println("  - Califica para reasignación: " + (esUrgente && noEntregado && noAsignado));
        }
        System.out.println("=====================================\n");
    }

    // ADD this method to Genetico.java
// Method to check for swaps between trucks when all urgent orders are already assigned
    private void verificarIntercambiosUrgentes(List<Pedido> pedidosUrgentes, LocalDateTime fechaActual) {
        System.out.println("\n=== VERIFICANDO INTERCAMBIOS ENTRE CAMIONES ===");

        // Group urgent orders by assigned truck
        Map<String, List<Pedido>> pedidosPorCamion = new HashMap<>();
        for (Pedido p : pedidosUrgentes) {
            if (p.isAsignado() && p.getIdCamion() != null && !p.isEntregado()) {
                pedidosPorCamion.computeIfAbsent(p.getIdCamion(), k -> new ArrayList<>()).add(p);
            }
        }

        System.out.println("Camiones con pedidos urgentes asignados: " + pedidosPorCamion.size());

        // Look for optimization opportunities
        for (String codigoCamion : pedidosPorCamion.keySet()) {
            List<Pedido> pedidosDelCamion = pedidosPorCamion.get(codigoCamion);

            System.out.println("Camión " + codigoCamion + " tiene " + pedidosDelCamion.size() + " pedidos urgentes");

            // Find least urgent order in this truck
            Pedido menosUrgente = pedidosDelCamion.stream()
                    .max(Comparator.comparing(p -> Duration.between(fechaActual, p.getFechaEntrega()).toMinutes()))
                    .orElse(null);

            if (menosUrgente != null) {
                Duration tiempoMenosUrgente = Duration.between(fechaActual, menosUrgente.getFechaEntrega());
                System.out.println("  - Pedido menos urgente: " + menosUrgente.getId() +
                        " (tiempo: " + tiempoMenosUrgente.toMinutes() + " min)");

                // Look for more urgent orders in other trucks
                for (String otroCodigoCamion : pedidosPorCamion.keySet()) {
                    if (!otroCodigoCamion.equals(codigoCamion)) {
                        List<Pedido> pedidosDelOtroCamion = pedidosPorCamion.get(otroCodigoCamion);

                        Pedido masUrgenteDOtro = pedidosDelOtroCamion.stream()
                                .min(Comparator.comparing(p -> Duration.between(fechaActual, p.getFechaEntrega()).toMinutes()))
                                .orElse(null);

                        if (masUrgenteDOtro != null) {
                            Duration tiempoMasUrgente = Duration.between(fechaActual, masUrgenteDOtro.getFechaEntrega());

                            System.out.println("  - Comparando con camión " + otroCodigoCamion +
                                    " pedido " + masUrgenteDOtro.getId() +
                                    " (tiempo: " + tiempoMasUrgente.toMinutes() + " min)");

                            // If we can improve urgency by swapping (significant difference: >30 min)
                            long diferenciaTiempo = tiempoMenosUrgente.toMinutes() - tiempoMasUrgente.toMinutes();
                            if (diferenciaTiempo > 30) { // Only swap if significant improvement
                                System.out.println("*** OPORTUNIDAD DE INTERCAMBIO DETECTADA ***");
                                System.out.println("Diferencia de urgencia: " + diferenciaTiempo + " minutos");
                                System.out.println("Camión " + codigoCamion + " pedido " + menosUrgente.getId() +
                                        " (menos urgente: " + tiempoMenosUrgente.toMinutes() + " min)");
                                System.out.println("VS Camión " + otroCodigoCamion + " pedido " + masUrgenteDOtro.getId() +
                                        " (más urgente: " + tiempoMasUrgente.toMinutes() + " min)");

                                // Execute swap if capacity allows
                                Camion camion1 = encontrarCamionPorCodigo(codigoCamion);
                                Camion camion2 = encontrarCamionPorCodigo(otroCodigoCamion);

                                if (camion1 != null && camion2 != null) {
                                    // Check capacity constraints
                                    boolean camion1PuedeTomarPedido2 = verificarCapacidadParaIntercambio(camion1, menosUrgente, masUrgenteDOtro);
                                    boolean camion2PuedeTomarPedido1 = verificarCapacidadParaIntercambio(camion2, masUrgenteDOtro, menosUrgente);

                                    if (camion1PuedeTomarPedido2 && camion2PuedeTomarPedido1) {
                                        ejecutarIntercambio(camion1, menosUrgente, camion2, masUrgenteDOtro);
                                        return; // Only do one swap per iteration to avoid complications
                                    } else {
                                        System.out.println("*** INTERCAMBIO BLOQUEADO POR CAPACIDAD ***");
                                        System.out.println("Camión1 puede tomar pedido2: " + camion1PuedeTomarPedido2);
                                        System.out.println("Camión2 puede tomar pedido1: " + camion2PuedeTomarPedido1);
                                    }
                                }
                            } else {
                                System.out.println("  -> Diferencia insuficiente para intercambio (" + diferenciaTiempo + " min)");
                            }
                        }
                    }
                }
            }
        }

        System.out.println("=== FIN VERIFICACIÓN INTERCAMBIOS ===\n");
    }

    // ADD this helper method to Genetico.java
    private Camion encontrarCamionPorCodigo(String codigo) {
        return camiones.stream()
                .filter(c -> c.getCodigo().equals(codigo))
                .findFirst()
                .orElse(null);
    }

    // ADD this helper method to Genetico.java
    private boolean verificarCapacidadParaIntercambio(Camion camion, Pedido pedidoARemover, Pedido pedidoAAgregar) {
        // Calculate capacity if we remove one order and add another
        double capacidadLiberada = pedidoARemover.getCantidadGLP();
        double capacidadNecesaria = pedidoAAgregar.getCantidadGLP();
        double diferenciaCarga = capacidadNecesaria - capacidadLiberada;

        // Check if truck can handle the difference
        double cargaActual = camion.getCargaAsignada();
        double nuevaCarga = cargaActual + diferenciaCarga;

        boolean puedeIntercambiar = nuevaCarga <= camion.getGlpDisponible();

        System.out.println("    [CAPACIDAD_INTERCAMBIO] Camión " + camion.getCodigo() +
                " - Carga actual: " + cargaActual +
                ", Libera: " + capacidadLiberada +
                ", Necesita: " + capacidadNecesaria +
                ", Nueva carga: " + nuevaCarga +
                ", Capacidad: " + camion.getGlpDisponible() +
                ", Puede intercambiar: " + puedeIntercambiar);

        return puedeIntercambiar;
    }

    // ADD this helper method to Genetico.java
    private void ejecutarIntercambio(Camion camion1, Pedido pedido1, Camion camion2, Pedido pedido2) {
        System.out.println("*** EJECUTANDO INTERCAMBIO ENTRE CAMIONES ***");
        System.out.println("Intercambiando:");
        System.out.println("  - Camión " + camion1.getCodigo() + " cede pedido " + pedido1.getId() +
                " y recibe pedido " + pedido2.getId());
        System.out.println("  - Camión " + camion2.getCodigo() + " cede pedido " + pedido2.getId() +
                " y recibe pedido " + pedido1.getId());

        // Remove orders from their current trucks
        camion1.getPedidosAsignados().remove(pedido1);
        camion2.getPedidosAsignados().remove(pedido2);

        // Update truck loads
        camion1.setCargaAsignada(camion1.getCargaAsignada() - pedido1.getCantidadGLP() + pedido2.getCantidadGLP());
        camion2.setCargaAsignada(camion2.getCargaAsignada() - pedido2.getCantidadGLP() + pedido1.getCantidadGLP());

        // Swap assignments
        camion1.getPedidosAsignados().add(pedido2);
        camion2.getPedidosAsignados().add(pedido1);

        // Update order assignments
        pedido1.setIdCamion(camion2.getCodigo());
        pedido2.setIdCamion(camion1.getCodigo());

        // Clear routes for replanning
        camion1.setRoute(new ArrayList<>());
        camion2.setRoute(new ArrayList<>());

        System.out.println("*** INTERCAMBIO COMPLETADO ***");
        System.out.println("Resultado:");
        System.out.println("  - Camión " + camion1.getCodigo() + " nueva carga: " + camion1.getCargaAsignada());
        System.out.println("  - Camión " + camion2.getCodigo() + " nueva carga: " + camion2.getCargaAsignada());
    }

}
