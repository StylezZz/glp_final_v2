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
    // Atributos de la clase del algoritmo Genetico
    private List<Pedido> pedidos;
    private List<Pedido> originalPedidos;
    private List<Pedido> pedidosNuevos;
    private List<Camion> camiones;
    private Mapa gridGraph;
    // Parametros del algoritmo genetico
    private double[][][][] matrizFeromonas;
    private double q0 = 0.3;
    private double Q = 1;
    // Parametros de la simulacion
    private int cantidadPdidosInicial = 0;
    private boolean inicio;
    private List<Bloqueo> bloqueosActivos;
    private double MIN_DISTANCE = 0.00000000009;

    public Genetico() {
    }

    // Metodo para iniciar la simulacion
    public List<Camion> simulacionRuteo(int anio, int mes, int dia, int hora, int minuto, int minutosPorIteracion,
                                        List<Pedido> pedidosDia, List<Bloqueo> bloqueos, List<Pedido> pedidoOriginal, int primeraVez,
                                        double timer, int tipoSimulacion) {

        // Filtrar pedidos del dia
        getPedidosDia(anio, mes, dia, hora, minuto, pedidosDia, minutosPorIteracion, pedidoOriginal, timer,
                tipoSimulacion);

        // Configuracion del calendario para validar los bloqueos
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, anio);
        // El mes en Calendar comienza desde 0, por lo que se resta 1 (Enero es 0, Febrero es 1, etc.)
        calendar.set(Calendar.MONTH, mes - 1);
        calendar.set(Calendar.DAY_OF_MONTH, dia);
        calendar.set(Calendar.HOUR_OF_DAY, hora);
        calendar.set(Calendar.MINUTE, minuto);

        // Identificar los bloqueos activos
        bloqueosActivos = Bloqueo.bloqueosActivos(bloqueos, calendar);

        // Ejecutar el algoritmo
        run(anio, mes, dia, hora, minuto, bloqueos, primeraVez, timer, tipoSimulacion);

        // Validar entregas completadas
        validarTiempoRuta(anio, mes, dia, hora, minuto, minutosPorIteracion, tipoSimulacion);

        return camiones;

    }

    // Filtrar los pedidos segun la fecha y hora
    public void getPedidosDia(int anio, int mes, int dia, int hora, int minuto, List<Pedido> pedidosDia,
                              int minutosPorIteracion, List<Pedido> pedidoOriginal, double timer, int tipoSimulacion) {

        this.pedidos = new ArrayList<>();

        LocalDateTime fecha = LocalDateTime.of(anio, mes, dia, hora, minuto);
        LocalDateTime fechaIteracion;

        // Simulacion diaria: momento actual
        if (tipoSimulacion == 1) {
            fechaIteracion = fecha;
        } else {
            fechaIteracion = fecha.plusMinutes(minutosPorIteracion);    // Tiempo real mas iteracion
        }

        // Filtrar pedidos segun la ventana de tiempo
        for (Pedido pedido : pedidosDia) {

            if (pedido.getFechaDeRegistro().isBefore(fechaIteracion)
                    || pedido.getFechaDeRegistro().isEqual(fechaIteracion)) {

                this.pedidos.add(pedido);

            }
        }

        // Mantener copia de pedidos originales para seguimiento de estado
        this.originalPedidos = new ArrayList<>();
        for (Pedido pedido : pedidoOriginal) {
            if (pedido.getFechaDeRegistro().isBefore(fechaIteracion)
                    || pedido.getFechaDeRegistro().isEqual(fechaIteracion)) {
                originalPedidos.add(pedido);
            }
        }
    }

    // Constructor principal que incializa el genetico con la flota de vehiculos
    public Genetico(List<Camion> camiones, int tipoSimulacion) {
        this.camiones = camiones;
        this.pedidos = new ArrayList<>();
        // Inicializar mapa
        gridGraph = new Mapa();
        gridGraph.inicializarAlmacenes(tipoSimulacion);
        gridGraph.initialGrid();
        this.inicio = true;
        this.bloqueosActivos = new ArrayList<>();
        initialPheromones();

    }

    // Identifica y prioriza pedidos con entregas urgentes o no completadas
    // Los pedidos con fecha de entrega menor o igual a la fecha actual reciben prioridad
    public void validarPedidosNoCompletados(int anio, int mes, int dia, int hora, int minuto) {

        LocalDateTime fechaActual = LocalDateTime.of(anio, mes, dia, hora, minuto);

        for (Pedido pedido : originalPedidos) {
            if (!pedido.isEntregadoCompleto()) {
                // Verificar si la fecha de entrega ya paso o esta en el momento actual
                if (pedido.getFechaEntrega().isBefore(fechaActual) || pedido.getFechaEntrega().isEqual(fechaActual)) {
                    // Actualiza la prioridad
                    for (Pedido pedido2 : pedidos) {
                        if (pedido.getId() == pedido2.getId()) {
                            pedido2.setPriodidad(-1);   // -1 es la prioridad mas alta
                            pedido2.setEntregadoCompleto(false);
                            pedido2.setEntregado(false);
                        }
                    }
                }
            }
        }
    }

    // Actualiza el estado de las rutas de los vehiculos y marca como entregados los pedidos cuyo tiempo de entrega ya paso
    // Gestiona la carga y capacidad de los camiones
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
            // Simulacion diaria
            if (tipoSimulacion == 1) {
                // Recorre cada nodo en la ruta del camion
                for (NodePosition ubicacion : camion.getRoute()) {

                    if (mes == ubicacion.getMes() && anio == ubicacion.getAnio()) {
                        double startTime = timer;
                        // Si el tiempo de inicio ya paso, marcar como entregado
                        if (Math.round(ubicacion.getStartTime()) < startTime) {
                            distanciaRecorrida += 1;
                            camion.setUbicacionActual(ubicacion);
                            camion.setDistanciaRecorrida(distanciaRecorrida);
                            camion.getUbicacionActual().setRoute(false);
                            // Procesar entrega de pedidos
                            if (ubicacion.isPedido()) {
                                if (ubicacion.getMes() <= mes && ubicacion.getAnio() <= anio) {
                                    ubicacion.getPedidoRuta().setEntregado(true);
                                    ubicacion.getPedidoRuta().setHoraDeInicio((int) ubicacion.getStartTime());
                                    ubicacion.getPedidoRuta().setTiempoLlegada((int) ubicacion.getArriveTime());
                                    camion.asignarPedido(ubicacion.getPedidoRuta());
                                }
                            }
                            // Procesar llegada a almacen
                            if (ubicacion.isDepot()) {

                                for (Almacen deposito2 : gridGraph.getAlmacenes()) {

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
                                ubicacion.setStartTime(startTime);
                                ubicacion.setArriveTime(startTime + 1);

                            }

                        } else {
                            // Actualizar la ubicacion actual del camion si el tiempo de inicio no ha pasado
                            if (bandera) {
                                camion.setUbicacionActual(ubicacion);
                                bandera = false;
                            }
                            // Resetear estado del pedido si no se ha entregado
                            if (ubicacion.isPedido()) {
                                if (ubicacion.getMes() <= mes && ubicacion.getAnio() <= anio) {
                                    ubicacion.getPedidoRuta().setEntregado(false);
                                    ubicacion.getPedidoRuta().setAsignado(false);
                                    ubicacion.getPedidoRuta().setIdCamion(null);
                                }
                            }
                            // Manejar almacenes no alcanzados
                            if (ubicacion.isDepot()) {
                                if (Math.round(ubicacion.getStartTime()) > startTime) {
                                    if (i == 0) {
                                        camion.setUbicacionActual(ubicacion);
                                        continue;
                                    }
                                }
                            }
                        }
                        i++;
                    }

                    // Mantener carga anterior del camion
                    if (camion.getCargaAsignada() == 0.0) {
                        camion.setCargaAsignada(camion.getCargaAnterior());
                    }
                }
                i++;
            } else {
                // Simulacion timpo real
                for (NodePosition ubicacion : camion.getRoute()) {

                    if (mes == ubicacion.getMes() && anio == ubicacion.getAnio()) {
                        double startTime = timer;

                        if ((int) ubicacion.getArriveTime() <= startTime) {
                            distanciaRecorrida += 1;
                            camion.setUbicacionActual(ubicacion);
                            camion.setDistanciaRecorrida(distanciaRecorrida);
                            camion.getUbicacionActual().setRoute(false);
                            // Procesar entrega de pedidos
                            if (ubicacion.isPedido()) {
                                if (ubicacion.getMes() <= mes && ubicacion.getAnio() <= anio) {
                                    ubicacion.getPedidoRuta().setEntregado(true);
                                    ubicacion.getPedidoRuta().setHoraDeInicio((int) ubicacion.getStartTime());
                                    ubicacion.getPedidoRuta().setTiempoLlegada((int) ubicacion.getArriveTime());
                                    camion.asignarPedido(ubicacion.getPedidoRuta());
                                }
                            }
                            // Procesar llegada a almacen
                            if (ubicacion.isDepot()) {

                                for (Almacen deposito2 : gridGraph.getAlmacenes()) {
                                    // Reiniciar capacidad disponible al inicio del día
                                    if (hora == 0 && minuto >= 0) {
                                        deposito2.setCapacidadDisponible(deposito2.getCapacidad());
                                    }
                                    // Verificar si el camión está en este almacén
                                    if (deposito2.getUbicacion().getX() == camion.getUbicacionActual().getX()
                                            && deposito2.getUbicacion().getY() == camion.getUbicacionActual().getY()) {
                                        // Almacén central (12,8) - recarga completa
                                        if (deposito2.getUbicacion().getX() == 12 && deposito2.getUbicacion().getY() == 8) {
                                            camion.setCargaAsignada(0);
                                            camion.setGlpDisponible(camion.getCarga());
                                            camion.setDistanciaRecorrida(0.0);
                                        } else {
                                            // Almacenes secundarios - verificar capacidad disponible
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
                                ubicacion.setStartTime(startTime);
                                ubicacion.setArriveTime(startTime + 1.2);
                            }
                        } else {
                            // Resetear estado del pedido si no se ha entregado
                            if (ubicacion.isPedido()) {
                                if (ubicacion.getMes() <= mes && ubicacion.getAnio() <= anio) {
                                    ubicacion.getPedidoRuta().setEntregado(false);
                                    ubicacion.getPedidoRuta().setAsignado(false);
                                    ubicacion.getPedidoRuta().setIdCamion(null);
                                }
                            }
                            // Manejar almacenes no alcanzados
                            if (ubicacion.isDepot()) {
                                if ((int) ubicacion.getArriveTime() > startTime) {
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
                i++;
            }
        }
        // Ordenar pedidos originales por tiempo de entrega
        Comparator<Pedido> comparadorPorTiempoDeLlegada = Comparator.comparing(Pedido::getFechaEntrega);
        originalPedidos.sort(comparadorPorTiempoDeLlegada);

    }

    // Gestiona las averías de los camiones, actualizando su estado y ubicación
    public void validarAverias(int anio, int mes, int dia, int hora, int minuto, List<Bloqueo> bloqueos,
                               int tipoSimulacion) {

        double arriveTime = dia * 1440 + hora * 60 + minuto;
        for (Camion camion : camiones) {
            double tiempoDetenido = camion.getTiempoDetenido();
            // Reactivar el camion si termino el periodo de detencion
            if ((camion.isDetenido()) && (tiempoDetenido <= arriveTime)) {
                camion.setDetenido(false);
            }
            // Manejar averías tipo 2 y 3 - retorno automático a almacén central
            if ((camion.getTipoAveria() == 2 || camion.getTipoAveria() == 3) && !camion.isDetenido()
                    && camion.isEnAveria()) {
                NodePosition nodoCentral = new NodePosition(0, 12, 8, true);
                nodoCentral.setStartTime(camion.getTiempoFinAveria());
                // Configurar tiempo de llegada según tipo de simulación
                if (tipoSimulacion == 1) {
                    nodoCentral.setArriveTime(camion.getTiempoFinAveria() + 1);
                } else {
                    nodoCentral.setArriveTime(camion.getTiempoFinAveria() + 1.2);

                }
                nodoCentral.setMes(mes);
                nodoCentral.setAnio(anio);
                camion.setUbicacionActual(nodoCentral);
            }
            // Finalizar estado de avería cuando se cumple el tiempo
            if (camion.isEnAveria() && camion.getTiempoFinAveria() <= arriveTime) {
                camion.setEnAveria(false);
            }
            // Gestionar rescate de camiones en avería
            if (camion.isEnAveria()) {
                double menorDistancia = Double.POSITIVE_INFINITY;
                Camion camionMenorDistancia = new Camion();
                // Limpiar ruta y pedidos del camión averiado
                camion.setRoute(new ArrayList<>());
                camion.setPedidosAsignados(new ArrayList<>());
                // Asignar ubicación por defecto si no tiene una
                if (camion.getUbicacionActual() == null) {
                    NodePosition posicionDefault = new NodePosition(0, 12, 8, true);
                    posicionDefault.setAnio(anio);
                    posicionDefault.setMes(mes);
                    camion.setUbicacionActual(posicionDefault);
                }
                // Encontrar el camión más cercano para rescate
                for (Camion camionRescate : camiones) {
                    if (camion.getCodigo().equals(camionRescate.getCodigo())) {
                        continue;
                    } else {
                        // Asignar ubicación por defecto al camión de rescate si es necesario
                        if (camionRescate.getUbicacionActual() == null) {
                            NodePosition posicionDefault = new NodePosition(0, 12, 8, true);
                            posicionDefault.setAnio(anio);
                            posicionDefault.setMes(mes);
                            camionRescate.setUbicacionActual(posicionDefault);
                        }
                        // Calcular la distancia al camión en avería
                        double distancia = camion.getUbicacionActual().distance(camionRescate.getUbicacionActual());
                        if (distancia < menorDistancia) {
                            menorDistancia = distancia;
                            camionMenorDistancia = camionRescate;
                        }
                    }
                }
                // Calcular ruta de rescate
                Astar aestar = new Astar();
                aestar.encontrarCamino(gridGraph, camionMenorDistancia.getUbicacionActual(),
                        camion.getUbicacionActual(), bloqueos, anio, mes, camionMenorDistancia, null, inicio, tipoSimulacion);
            }

        }

    }
    // Validar entregas completas de pedidos
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

    // Algoritmo principal que coordina la ejecución de las tareas de ruteo y entrega
    public void run(int anio, int mes, int dia, int hora, int minuto, List<Bloqueo> bloqueos, int primeraVez,
                    double timer, int tipoSimulacion) {

        // FASE 1: Limpieza y preparación
        gridGraph.limpiarPedidos();

        // FASE 2: Validación de estado actual
        validarPedidosNoEntregado(anio, mes, dia, hora, minuto, timer, tipoSimulacion);
        entregadosCompletos();
        clearRoute();   // Limpiar rutas anteriores

        // FASE 3: Reorganización de pedidos
        if (tipoSimulacion == 2) {
            eliminarPedidosEntregadosYReorganizar();
        }

        validarAverias(anio, mes, dia, hora, minuto, bloqueos, tipoSimulacion);
        validarPedidosNoCompletados(anio, mes, dia, hora, minuto);

        // FASE 4: Priorización de pedidos
        LocalDateTime fechaActual = LocalDateTime.of(anio, mes, dia, hora, minuto);
        Comparator<Pedido> comparadorPorProximidad = Comparator
                .comparing(pedido -> Duration.between(fechaActual, pedido.getFechaEntrega()).abs());
        pedidos.sort(comparadorPorProximidad);

        // FASE 5: Asignación de pedidos a camiones
        if (primeraVez == 0) {
            // Primera ejecución: inicializar todos los camiones en almacen central
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
            // Siguientes ejecuciones: verificar rutas y pedidos asignados
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

        // FASE 6: Planificacion segun tipo de simulacion
        if (tipoSimulacion == 1) {
            planificarPedidosDiaria(primeraVez, dia, hora, minuto, mes, anio, bloqueos, timer);
        } else {
            planificarPedidos(primeraVez, dia, hora, minuto, mes, anio, bloqueos, timer);
        }

        // FASE 7: Resetear estado de entregas para nueva planificación
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

        // FASE 8: Construcción final de rutas y retorno a almacenes
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
            // Construir ruta óptima para pedidos asignados
            construirSolucion(camion, posicionActual, anio, mes, dia, hora, minuto, bloqueos, primeraVez, tipoSimulacion);
            // Programar retorno al almacén más conveniente
            volverAlmacen(camion, bloqueos, anio, mes, dia, hora, minuto, tipoSimulacion);
        }

    }
    // Elimina pedidos entregados de las asignaciones y reordena los vehículos priorizando
    // aquellos con pedidos pendientes para mejorar la eficiencia de asignación.
    public void eliminarPedidosEntregadosYReorganizar() {
        List<Camion> camionesConPedidos = new ArrayList<>();
        List<Camion> camionesSinPedidos = new ArrayList<>();

        // Clasificar vehículos según estado de asignación
        for (Camion camion : camiones) {
            if (camion.getPedidosAsignados() != null && !camion.getPedidosAsignados().isEmpty()) {
                camionesConPedidos.add(camion);
            } else {
                camionesSinPedidos.add(camion);
            }
        }

        // Limpiar pedidos entregados de vehículos con asignaciones
        for (Camion camion : camionesConPedidos) {
            List<Pedido> pedidosAsignados = camion.getPedidosAsignados();
            Iterator<Pedido> iterator = pedidosAsignados.iterator();

            while (iterator.hasNext()) {
                Pedido pedido = iterator.next();
                if (pedido.isEntregado()) {
                    iterator.remove(); // Eliminar pedidos completados
                }
            }
        }

        // Reorganizar lista: vehículos con pedidos primero, luego vehículos libres
        camiones.clear();
        camiones.addAll(camionesConPedidos);
        camiones.addAll(camionesSinPedidos);
    }

    // Valida y actualiza el estado de entregas basado en los tiempos de ruta y pedidos asignados.
    // Actualiza capacidades de los almacenes y gestiona la entrega de pedidos según el tipo de simulación.
    public void validarTiempoRuta(int anio, int mes, int dia, int hora, int minuto, int minutosPorIteracion,
                                  int tipoSimulacion) {

        double arriveTime = dia * 1440 + hora * 60 + minuto + minutosPorIteracion;
        for (Camion camion : camiones) {
            camion.setCargaAsignada(0);
            for (NodePosition posicion : camion.getRoute()) {
                if (anio == posicion.getAnio() && mes == posicion.getMes()) {
                    // Validar si la entrega/llegada ya ocurrió
                    if ((int) posicion.getArriveTime() <= arriveTime) {
                        // Procesar entrega de pedido
                        if (posicion.isPedido()) {
                            camion.setCargaAsignada(
                                    camion.getCargaAsignada() + posicion.getPedidoRuta().getCantidadGLP());
                            // Sincronizar con pedidos originales
                            for (Pedido pedidoOriginal : originalPedidos) {
                                if (pedidoOriginal.getId() == posicion.getPedidoRuta().getId()) {
                                    posicion.getPedidoRuta().setEntregado(true);
                                    pedidoOriginal
                                            .setCantidadGLPAsignada(pedidoOriginal.getCantidadGLPAsignada() +
                                                    posicion.getPedidoRuta().getCantidadGLP());
                                    // Validar entrega completa según tipo de simulación
                                    if (tipoSimulacion == 1) {
                                        // Simulación diaria - verificar cantidad total
                                        if (pedidoOriginal.getCantidadGLP() <= pedidoOriginal
                                                .getCantidadGLPAsignada()) {
                                            posicion.getPedidoRuta().setEntregadoCompleto(true);
                                            pedidoOriginal.setEntregadoCompleto(true);
                                            pedidoOriginal.setEntregado(true);
                                        }
                                    } else {
                                        // Tiempo real - marcar como completo inmediatamente
                                        if (pedidoOriginal.getId() == posicion.getPedidoRuta().getId()) {
                                            posicion.getPedidoRuta().setEntregadoCompleto(true);
                                            pedidoOriginal.setEntregadoCompleto(true);
                                            pedidoOriginal.setEntregado(true);
                                        }
                                    }
                                }
                            }
                        }
                        // Procesar llegada a almacén
                        if (posicion.isDepot()) {
                            for (Almacen deposito : gridGraph.getAlmacenes()) {
                                if (deposito.getUbicacion().getX() == posicion.getX()
                                        && deposito.getUbicacion().getY() == posicion.getY()) {
                                    // Reducir capacidad disponible (excepto almacén central)
                                    if (deposito.getUbicacion().getX() != 12 && deposito.getUbicacion().getY() != 8) {
                                        deposito.setCapacidadDisponible(
                                                deposito.getCapacidadDisponible() - 2);
                                    }
                                }
                            }

                        }
                    } else {
                        // Entrega/llegada aún no ocurre - mantener estado pendiente
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
                                    // Restaurar capacidad para llegadas no completadas
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

    // Gestiona el retorno automático del camión al almacén más conveniente al finalizar las entregas.
    public void volverAlmacen(Camion camion, List<Bloqueo> bloqueos, int anio, int mes, int dia, int hora,
                              int minuto, int tipoSimulacion) {
        // Seleccionar almacén más conveniente según estrategia de simulación
        NodePosition deposito = new NodePosition(0, 12, 8, true);
        deposito = selecionarAlmacen(camion, tipoSimulacion);

        // Calcular ruta optima
        Astar astar = new Astar();
        for (NodePosition node : camion.getRoute()) {
            node.setAntecesor(null);    // Limpiar referencias
        }
        NodePosition posicionActual = camion.getUbicacionActual();
        NodePosition nodoDeposito = new NodePosition(0, deposito.getX(), deposito.getY(), true);
        astar.encontrarCamino(gridGraph, posicionActual, nodoDeposito, bloqueos,
                anio, mes, camion, null, inicio, tipoSimulacion);
        // Configurar tiempos de llegada según tipo de simulación
        if(tipoSimulacion == 1){
            camion.getUbicacionActual().setArriveTime(camion.getUbicacionActual().getStartTime() + 1);
        }else{
            camion.getUbicacionActual().setArriveTime(camion.getUbicacionActual().getStartTime() + 1.2);
        }
        // Marcar ubicación como almacén
        camion.getUbicacionActual().setDepot(true);
        posicionActual = camion.getUbicacionActual();
        // Sincronizar tiempos de ruta
        posicionActual.setStartTime(camion.ubicacionActual.getStartTime());
        posicionActual.setArriveTime(camion.ubicacionActual.getArriveTime());
        // Limpiar antecesores para próxima planificación
        for (NodePosition node : camion.getRoute()) {
            node.setAntecesor(null);
        }
        // Resetear estado del vehículo al llegar al almacén
        camion.setCargaAsignada(0.0);
        camion.setDistanciaRecorrida(0.0);

    }

    // Seleccionar el almacen optimo para retorno de vehículos.
    public NodePosition selecionarAlmacen(Camion camion, int tipoSimulacion) {
        NodePosition deposito = new NodePosition(0, 12, 8, true);
        if (tipoSimulacion == 1) {
            return deposito; // Simulación diaria siempre usa almacén central
        } else {
            // Simulación tiempo real - buscar almacén más cercano disponible
            double distanciaMinima = Double.MAX_VALUE;
            for (int x = 0; x < gridGraph.getRows(); x++) {
                for (int y = 0; y < gridGraph.getColumns(); y++) {
                    if (gridGraph.getGrid()[x][y].isDepot()) {
                        NodePosition nodoDeposito = gridGraph.getGrid()[x][y];
                        double distancia = camion.getUbicacionActual().distance(nodoDeposito);
                        if (distancia < distanciaMinima) {
                            distanciaMinima = distancia;
                            deposito = nodoDeposito;
                        }

                    }
                }
            }
            // Actualizar capacidad del almacén seleccionado
            for (Almacen depositoP : gridGraph.getAlmacenes()) {
                if (depositoP.getUbicacion().getX() == deposito.getX() && depositoP.getUbicacion().getY() == deposito.getY()) {
                    if (depositoP.getUbicacion().getX() != 12 && depositoP.getUbicacion().getY() != 8) {
                        // Reducir capacidad solo en almacenes secundarios
                        if (depositoP.getCapacidadDisponible() - 2 <= 0) {
                            depositoP.setCapacidadDisponible(0);

                        } else {
                            depositoP.setCapacidadDisponible(depositoP.getCapacidadDisponible() - 2);
                        }

                    }
                }
            }
            // Verificar disponibilidad y redirigir a central si está lleno
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

    // Construye la ruta óptima para un vehículo visitando todos sus pedidos asignados.
    public void construirSolucion(Camion camion, NodePosition posicionActual, int anio, int mes, int dia, int hora,
                                  int minuto, List<Bloqueo> bloqueos, int primeraVez, int tipoSimulacion) {

        if (camion.getPedidosAsignados() == null)
            return;
        for (Pedido pedido : camion.getPedidosAsignados()) {
            NodePosition nodoPedido = new NodePosition(0, pedido.getPosX(), pedido.getPosY(), false);
            // Validar temporización de pedidos
            LocalDateTime fechaTimer = LocalDateTime.of(anio, mes, dia, hora, minuto);
            LocalDateTime fechaPedido = pedido.getFechaDeRegistro();
            if (fechaPedido.isBefore(fechaTimer)) {
                // Pedido ya registrado - mantener tiempo actual
                posicionActual.setStartTime(posicionActual.getStartTime());
                inicio = false;

            } else {
                // Pedido futuro - configurar tiempo según contexto
                if (inicio) {
                    // Primera planificación del día
                    posicionActual.setStartTime(pedido.getHoraDeInicio());
                    inicio = false;
                } else {
                    // Planificacion posterior
                    posicionActual.setStartTime(posicionActual.getArriveTime());
                }
            }
            // Calcular ruta óptima hacia el pedido
            Astar astar = new Astar();
            for (NodePosition node : camion.getRoute()) {
                node.setAntecesor(null); // Limpiar referencias
            }
            astar.encontrarCamino(gridGraph, posicionActual, nodoPedido, bloqueos,
                    anio, mes, camion, pedido, inicio, tipoSimulacion);
            // Actualizar posición para siguiente pedido
            posicionActual = new NodePosition(0, camion.getUbicacionActual().getX(),
                    camion.getUbicacionActual().getY(), false);
            // Sincronizar tiempos de ruta
            posicionActual.setStartTime(camion.ubicacionActual.getStartTime());
            posicionActual.setArriveTime(camion.ubicacionActual.getArriveTime());
            // Limpiar antecesores para próxima búsqueda
            for (NodePosition node : camion.getRoute()) {
                node.setAntecesor(null);
            }
        }
        inicio = true; // Resetear flag para próxima ejecución
    }

    // Marca todos los pedidos asignados como entregados para verificacion momentánea.
    public void verificacionMomentanea() {
        for (Camion camion : camiones) {
            if (camion.getPedidosAsignados() == null)
                continue;
            for (Pedido pedido : camion.getPedidosAsignados()) {
                pedido.setEntregado(true);
            }
        }
    }

    // Asigna pedidos únicamente a vehiculos que no tienen asignaciones previas.
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
                // Asignar pedidos
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
                // Gestionar ubicación según contexto de ejecución
                if (primeraVez == 0) {
                    camion.setUbicacionActual(null); // Primera vez - no necesita ubicación previa
                } else {
                    camion.setUbicacionActual(posicionActual); // Mantener ubicación actual
                }
            }
        }
    }
    // Reasigna pedidos a todos los vehículos disponibles, limpiando asignaciones previas.
    public void asignarPedidosACamionesLlenos(int primeraVez, int dia, int hora, int minuto, int mes, int anio,
                                              List<Bloqueo> bloqueos, double timer) {
        NodePosition posicionActual = null;
        for (Camion camion : camiones) {

            if (camion.isEnAveria()) {
                continue;   // Saltar vehículos averiados
            }
            // Limpiar asignaciones previas para redistribuir
            camion.setPedidosAsignados(new ArrayList<>());
            posicionActual = camion.getUbicacionActual();
            // Reasignar pedidos óptimos
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
            // Gestionar ubicación según contexto
            if (primeraVez == 0) {
                camion.setUbicacionActual(null); // Primera planificación
            } else {
                camion.setUbicacionActual(posicionActual);
            }
        }
    }

    // Planifica los pedidos asignados a los camiones según su estado y disponibilidad.
    public void planificarPedidos(int primeraVez, int dia, int hora, int minuto, int mes, int anio,
                                  List<Bloqueo> bloqueos, double timer) {
        // Detectar si hay vehículos disponibles
        boolean hayCamionVacio = false;
        for (Camion camion : camiones) {
            if (camion.getPedidosAsignados() == null || camion.isEnAveria()
                    || camion.getPedidosAsignados().isEmpty()) {
                hayCamionVacio = true;
                break;
            }
        }
        verificacionMomentanea(); // Sincronizar estado de entregas
        // Aplicar estrategia de asignación
        if (hayCamionVacio) {
            asignarPedidosACamionesVacios(primeraVez, dia, hora, minuto, mes, anio, bloqueos, timer);
        } else {
            asignarPedidosACamionesLlenos(primeraVez, dia, hora, minuto, mes, anio, bloqueos, timer);
        }

    }
    //  Planificador específico para simulación diaria que asigna todos los pedidos sin restricciones de capacidad
    public void planificarPedidosDiaria(int primeraVez, int dia, int hora, int minuto, int mes, int anio,
                                        List<Bloqueo> bloqueos, double timer) {
        NodePosition posicionActual = null;
        for (Camion camion : camiones) {
            if (camion.isEnAveria()) {
                continue;
            }
            camion.setPedidosAsignados(new ArrayList<>());
            posicionActual = camion.getUbicacionActual();
            // Asignar pedidos sin restricción de capacidad para cobertura completa
            for (int i = 0; i < pedidos.size(); i++) {
                Pedido pedidoMax = proximoPedidoFD(camion, dia, hora, minuto, mes, anio, bloqueos, timer, 1);
                if (pedidoMax == null) {
                    break;
                }
                // En simulación diaria, asignar sin verificar capacidad
                camion.asignarPedido(pedidoMax);
                camion.getPedidosAsignados().add(pedidoMax);
                pedidoMax.setEntregado(true);
            }
            // Gestionar ubicación
            if (primeraVez == 0) {
                camion.setUbicacionActual(null); // Primera planificación del día
            } else {
                camion.setUbicacionActual(posicionActual);
            }
        }
    }

    // Busca el próximo pedido que puede ser atendido por un camión, considerando restricciones de tiempo y capacidad
    public Pedido proximoPedidoFD(Camion camion, int dia, int hora, int minuto, int mes, int anio,
                                  List<Bloqueo> bloqueos, double timer, int tipoSimulacion) {
        // SIMULACIÓN DIARIA - Priorizar por distancia mínima
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

                    }

                }
            }
            return pedidoMax;
        } else {
            // SIMULACIÓN TIEMPO REAL - Priorizar por ventanas de entrega
            double max = Double.POSITIVE_INFINITY;
            Pedido pedidoMax = null;
            for (Pedido pedido : pedidos) {
                if (!pedido.isEntregado()) {
                    double distance = camion.getUbicacionActual().distance(new NodePosition(0, pedido.getPosX(),
                            pedido.getPosY(), false));
                    double tiempoViaje = (distance / camion.getVelocidad()) * 60;
                    double tiempoEntrega = pedido.getTiempoLlegada();
                    double tiempoActual = timer;

                    // Restricción crítica: debe llegar dentro de ventana de entrega
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

    public void initialPheromones() {
        this.matrizFeromonas = new double[gridGraph.getRows() + 1][gridGraph.getColumns() + 1][gridGraph.getRows()
                + 1][gridGraph
                .getColumns() + 1];
        for (int i = 0; i <= gridGraph.getRows(); i++) {
            for (int j = 0; j <= gridGraph.getColumns(); j++) {
                for (int k = 0; k <= gridGraph.getRows(); k++) {
                    for (int l = 0; l <= gridGraph.getColumns(); l++) {
                        matrizFeromonas[i][j][k][l] = 0.1;
                    }
                }
            }
        }
    }

    // Limpia todas las rutas asignadas a los vehículos para nueva planificacion
    public void clearRoute() {
        for (Camion camion : camiones) {
            camion.setRoute(new ArrayList<>());
        }
    }

}
