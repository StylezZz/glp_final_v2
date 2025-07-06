package pucp.edu.pe.glp_final.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


import org.springframework.http.HttpStatus;
import pucp.edu.pe.glp_final.algorithm.Genetico;
import pucp.edu.pe.glp_final.algorithm.NodePosition;
import pucp.edu.pe.glp_final.models.Averia;
import pucp.edu.pe.glp_final.models.Bloqueo;
import pucp.edu.pe.glp_final.models.Camion;
import pucp.edu.pe.glp_final.models.Pedido;
import pucp.edu.pe.glp_final.models.enums.TipoIncidente;
import pucp.edu.pe.glp_final.service.CamionService;
import pucp.edu.pe.glp_final.service.PedidoService;
import pucp.edu.pe.glp_final.algorithm.NodePosition;

@RequestMapping("/api")
@RestController
// Controlador principal para la gestión del algoritmo genético de optimización de rutas.
// Coordina la ejecución de simulaciones de ruteo vehicular integrando múltiples componentes:
// flota de vehículos, pedidos de entrega, bloqueos dinámicos y averías en tiempo real.

public class GeneticoController {
    // Lista de vehículos (camiones) disponibles para la simulación.
    private List<Camion> camiones;
    // Listas para almacenar pedidos originales y pedidos divididos para simulación.
    private List<Pedido> pedidos = new ArrayList<>();;
    private List<Pedido> pedidosSimulacion = new ArrayList<>();
    // Instancia del algoritmo genético con toda la lógica de optimización
    private Genetico aco;
    private int primeraVezDiaria = 0;
    private int primeraVezSemanal = 0;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private CamionService camionService;

    @Autowired
    private SimulacionBloqueoController simulacionController;

    // Inicializa el algoritmo genetico con la flota disponible y el tipo de simulación.
    @PostMapping("/aco/inicializar")
    @ResponseBody
    public ResponseEntity<Object> inicializar(@RequestParam(required = false) int tipoSimulacion) {
        camiones = new ArrayList<>();
        camiones = camionService.findAll(); // Carga de flota desde base de datos
        System.out.println(tipoSimulacion);
        aco = new Genetico(camiones, tipoSimulacion);
        Map<String, String> okMap = new HashMap<>();
        okMap.put("mensaje", "Aco Inicializado");
        return ResponseEntity.ok(okMap);
    }

    // Ejecuta simulación diaria del algoritmo con manejo de averías
    @PostMapping("/aco/simulacionRuta/dia-dia")
    @ResponseBody
    public ResponseEntity<List<Camion>> simulacionRutaDiaria(@RequestParam(required = false) int anio,
                                                             @RequestParam(required = false) int mes, @RequestParam(required = false) double timer,
                                                             @RequestParam(required = false) int minutosPorIteracion,
                                                             @RequestBody(required = false) List<Averia> averias,
                                                             @RequestParam(required = false) LocalDateTime fechaFin) {

        if (averias == null) {
            averias = new ArrayList<>();
        }
        // Conversión de timer a componentes temporales para simulación
        int dia = ((int) timer / 1440);
        int hora = (((int) timer % 1440) / 60);
        int minuto = ((int) timer % 60);

        // Aplicar averías activas a la flota
        averias(averias, timer);

        if (primeraVezDiaria == 0) {
            // Primera iteración: carga inicial de pedidos y configuración
            List<Integer> dias = new ArrayList<>();
            dias.add(dia);
            pedidos = pedidoService.findByDiaInAndAnioMes(dias, anio, mes);
            pedidosSimulacion = pedidoService.dividirPedidos(pedidos, 1);
            aco.simulacionRuteo(anio, mes, dia, hora, minuto, minutosPorIteracion, pedidosSimulacion,
                    new ArrayList<>(), pedidos,
                    primeraVezDiaria, timer, 1);
            for (Camion vehiculo : camiones) {
                if (vehiculo.getRoute().isEmpty()) {
                    continue;
                } else {
                    for (NodePosition ubicacion : vehiculo.getRoute()) {
                        if (ubicacion.isPedido()) {
                            if (ubicacion.getPedidoRuta().isEntregado()) {
                                for (Pedido pedido : pedidos) {
                                    if (pedido.getId() == ubicacion.getPedidoRuta().getId()) {
                                        pedido.setEntregado(true);
                                        pedido.setEntregadoCompleto(true);
                                        pedidoService.save(pedido);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Iteraciones posteriores: reutiliza pedidos y bloqueos existentes
            List<Integer> dias = new ArrayList<>();
            dias.add(dia);
            pedidos = pedidoService.findByDiaInAndAnioMes(dias, anio, mes);
            pedidosSimulacion = pedidoService.dividirPedidos(pedidos,1);
            aco.simulacionRuteo(anio, mes, dia, hora, minuto, minutosPorIteracion, pedidosSimulacion,
                    new ArrayList<>(), pedidos,
                    primeraVezDiaria, timer, 1);
            for (Camion vehiculo : camiones) {
                if (vehiculo.getRoute().isEmpty()) {
                    continue;
                } else {
                    for (NodePosition ubicacion : vehiculo.getRoute()) {
                        if (ubicacion.isPedido()) {
                            if (ubicacion.getPedidoRuta().isEntregado()) {
                                for (Pedido pedido : pedidos) {
                                    if (pedido.getId() == ubicacion.getPedidoRuta().getId()) {
                                        pedido.setEntregado(true);
                                        pedido.setEntregadoCompleto(true);
                                        pedidoService.save(pedido);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        primeraVezDiaria = 1;
        return ResponseEntity.ok(aco.getCamiones());

    }

    // Ejecuta simulación semanal del algoritmo con manejo de averías
    @PostMapping("aco/simulacionRuta/semanal")
    @ResponseBody
    public ResponseEntity<List<Camion>> simulacionRutaSemanal(@RequestParam(required = false) int anio,
                                                              @RequestParam(required = false) int mes,
                                                              @RequestParam(required = false) double timer,
                                                              @RequestParam(required = false) int minutosPorIteracion,
                                                              @RequestBody(required = false) List<Averia> averias) {

        if (averias == null) {
            averias = new ArrayList<>();
        }
        // Conversión temporal para ciclo semanal
        int dia = ((int) timer / 1440);
        int hora = (((int) timer % 1440) / 60);
        int minuto = ((int) timer % 60);
        // Aplicar impacto de averías en flota
        averias(averias, timer);

        if (primeraVezSemanal == 0) {
            // Inicialización para operación semanal extendida
            pedidos = simulacionController.getPedidos();
            pedidosSimulacion = pedidoService.dividirPedidos(pedidos,2);
            List<Bloqueo> bloqueos = simulacionController.getBloqueos();
            aco.simulacionRuteo(anio, mes, dia, hora, minuto, minutosPorIteracion, pedidosSimulacion, bloqueos,
                    pedidos, primeraVezSemanal, timer, 2);
        } else {
            // Optimización continua para horizonte semanal
            List<Bloqueo> bloqueos = simulacionController.getBloqueos();
            aco.simulacionRuteo(anio, mes, dia, hora, minuto, minutosPorIteracion, pedidosSimulacion, bloqueos,
                    pedidos, primeraVezSemanal, timer, 2);
        }

        primeraVezSemanal = 1;
        return ResponseEntity.ok(aco.getCamiones());

    }

    // Obtiene la lista de bloqueos activos que afectan la navegación
    @GetMapping("aco/simulacionRuta/bloqueo")
    @ResponseBody
    public ResponseEntity<List<Bloqueo>> obtenerBloqeos() {
        return ResponseEntity.ok(aco.getBloqueosActivos());
    }

    // Obtiene la lista de pedidos originales y pedidos divididos para simulación
    @GetMapping("aco/simulacionRuta/pedido")
    @ResponseBody
    public ResponseEntity<List<Pedido>> obtenerPedidosOriginales() {
        int i = 0;
        for (Pedido pedido : aco.getOriginalPedidos()) {
            if (pedido.isEntregado()) {
                // Conteo de pedidos entregados para métricas
                i++;
            }
        }
        System.out.println("Pedidos entregados: " + i + "/" + aco.getOriginalPedidos().size());
        return ResponseEntity.ok(aco.getOriginalPedidos());
    }

    // Obtiene la lista de pedidos divididos para simulación
    @GetMapping("aco/simulacionRuta/pedidoDividido")
    @ResponseBody
    public ResponseEntity<List<Pedido>> obtenerPedidosDivididos() {

        return ResponseEntity.ok(aco.getPedidos());
    }

    // Obtiene informacion de almacenes disponibles para ruteo
    @GetMapping("aco/simulacionRuta/almacen")
    @ResponseBody
    public ResponseEntity<Object> obtenerAlmacenes() {
        if (this.aco == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El algoritmo ACO no ha sido inicializado. Llama primero a /api/aco/inicializar.");
        }
        return ResponseEntity.ok(aco.getGridGraph().getAlmacenes());
    }

    // Resetea completamente el estado del sistema para nueva simulación.
    @PostMapping("aco/simulacionRuta/reset")
    @ResponseBody
    public ResponseEntity<Object> reset() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. LIMPIEZA DEL ALGORITMO GENÉTICO
            aco = null;

            // 2. RESETEO COMPLETO DE FLOTA
            int camionesReseteados = 0;
            if (camiones != null && !camiones.isEmpty()) {
                camionesReseteados = resetearCamionesCompleto();
            }

            // 3. RESETEO DE PEDIDOS (MEMORIA Y PERSISTENCIA)
            int pedidosActualizados = resetearPedidos();

            // 4. LIMPIEZA DE ESTRUCTURAS TEMPORALES
            limpiarListasMemoria();

            // 5. REINICIALIZACIÓN DE VARIABLES DE CONTROL
            primeraVezDiaria = 0;
            primeraVezSemanal = 0;

            // 6. CONSTRUCCIÓN DE RESPUESTA DETALLADA
            response.put("mensaje", "Reset completo realizado exitosamente");
            response.put("detalles", Map.of(
                    "camionesReseteados", camionesReseteados,
                    "pedidosActualizados", pedidosActualizados,
                    "listasLimpiadas", "pedidos, pedidosSimulacion",
                    "variablesControl", "primeraVezDiaria, primeraVezSemanal"
            ));
            response.put("timerRecomendado", 1440.0); // Día 2 para evitar bug día 0
            response.put("tiempoInicial", "Día 2, 00:00:00");
            response.put("estadoReset", "COMPLETO");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Manejo de errores durante proceso de reset
            response.put("error", "Error durante el reset: " + e.getMessage());
            response.put("estadoReset", "ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Resetea el estado de todos los camiones en memoria y base de datos
    private int resetearCamionesCompleto() {
        int contador = 0;
        for (Camion camion : camiones) {
            // LIMPIEZA DE RUTAS Y UBICACIÓN
            if (camion.getRoute() != null) {
                camion.getRoute().clear();
            } else {
                camion.inicializarRuta();
            }
            camion.setCargaAsignada(0);
            camion.setDistanciaRecorrida(0.0);
            camion.setTiempoViaje(0);
            camion.setUbicacionActual(null);
            // RESETEO DE CAPACIDADES Y CARGAS
            camion.setCapacidadCompleta(false);
            camion.setGlpDisponible(camion.getCarga());
            camion.setCargaAnterior(0);

            // LIMPIEZA DE ESTADOS DE AVERÍA - CRÍTICO PARA TIMER
            camion.setEnAveria(false);
            camion.setTipoAveria(0);
            camion.setTiempoInicioAveria(null);
            camion.setTiempoFinAveria(null);
            camion.setDetenido(false);
            camion.setTiempoDetenido(0);

            // LIMPIEZA DE PEDIDOS ASIGNADOS
            if (camion.getPedidosAsignados() != null) {
                camion.getPedidosAsignados().clear();
            } else {
                camion.setPedidosAsignados(new ArrayList<>());
            }

            contador++;
        }
        return contador;
    }

    // Resetea el estado de todos los pedidos en memoria y base de datos
    private int resetearPedidos() {
        int contador = 0;
        if (pedidos != null) {
            for (Pedido pedido : pedidos) {
                // Resetear estado de entrega para nueva simulación
                pedido.setEntregado(false);
                pedido.setEntregadoCompleto(false);
                // Persistir cambios en base de datos para consistencia
                pedidoService.save(pedido);
                contador++;
            }
        }
        return contador;
    }

    // Limpia las listas de pedidos y pedidosSimulacion en memoria
    private void limpiarListasMemoria() {
        if (pedidos != null) {
            pedidos.clear();
        } else {
            pedidos = new ArrayList<>();
        }
        // Limpieza segura de lista de pedidos divididos para simulación
        if (pedidosSimulacion != null) {
            pedidosSimulacion.clear();
        } else {
            pedidosSimulacion = new ArrayList<>();
        }
    }

    // Maneja las averías de los camiones según el tipo de incidente y tiempo
    // Tipos de averías manejadas:
    // - TI1: Averías leves (30-60 minutos)
    // - TI2: Averías moderadas (60-120 minutos)
    // - TI3: Averías graves (120+ minutos)
    public void averias(List<Averia> averias, double timer) {
        for (Averia averia : averias) {
            for (Camion vehiculo : camiones) {
                // Verificar correspondencia entre código de avería y vehículo específico
                if (averia.getCodigoCamion().equals(vehiculo.getCodigo())) {
                    // Calcular día actual para determinar fin de turno en averías moderadas/graves
                    int dia = ((int) timer / 1440);
                    // Marcar vehículo como en estado de avería
                    vehiculo.setEnAveria(true);
                    vehiculo.setTiempoInicioAveria(timer);
                    if (averia.getTipoAveria() == TipoIncidente.LEVE) {
                        // AVERÍA LEVE - Duración fija de 120 minutos
                        // Impacto: Detención temporal con recuperación automática
                        vehiculo.setTipoAveria(1);
                        vehiculo.setDetenido(true);
                        vehiculo.setTiempoDetenido(vehiculo.getTiempoInicioAveria() + 120);
                        vehiculo.setTiempoFinAveria(vehiculo.getTiempoInicioAveria() + 120);

                    } else {
                        if (averia.getTipoAveria() == TipoIncidente.MODERADO) {
                            // AVERÍA MODERADA - Duración hasta fin de turno
                            // Considera el turno en que ocurre la avería para calcular recuperación
                            vehiculo.setTipoAveria(2);
                            vehiculo.setDetenido(true);
                            vehiculo.setTiempoDetenido(vehiculo.getTiempoInicioAveria() + 120);
                            if (averia.getTurnoAveria() == 1) {
                                // Turno 1 (00:00-16:00): Recuperación a las 16:00 del día actual
                                vehiculo.setTiempoFinAveria((double) (dia * 1440 + 960));
                            } else {
                                // Turno 2 (16:00-24:00): Recuperación a las 00:00 del día siguiente
                                if (averia.getTurnoAveria() == 2) {
                                    vehiculo.setTiempoFinAveria((double) (dia * 1440 + 1440));
                                } else {
                                    // Turno 3 u otros: Recuperación al inicio del siguiente turno 1
                                    if (averia.getTurnoAveria() == 3) {
                                        vehiculo.setTiempoFinAveria((double) (dia * 1440 + 1440 + 480));
                                    }
                                }
                            }

                        } else {
                            if (averia.getTipoAveria() == TipoIncidente.GRAVE) {
                                // AVERÍA GRAVE - Fuera de servicio extendido
                                // Duración de múltiples días según severidad del incidente
                                vehiculo.setDetenido(true);
                                vehiculo.setTiempoDetenido(vehiculo.getTiempoInicioAveria() + 240);
                                vehiculo.setTipoAveria(3);
                                vehiculo.setTiempoFinAveria((double) (dia * 1440 + 1440 + 960));
                            } else if (averia.getTipoAveria() == TipoIncidente.MANTENIMIENTO) {
                                vehiculo.setTipoAveria(4);
                                vehiculo.setDetenido(true);
                                vehiculo.setTiempoDetenido(vehiculo.getTiempoInicioAveria());
                                vehiculo.setTiempoFinAveria(vehiculo.getTiempoInicioAveria() + 1440);
                            }
                        }
                    }

                }
            }
        }
    }

    // Obtiene la lista de camiones disponibles para ruteo
    @GetMapping("/aco/camiones")
    @ResponseBody
    public ResponseEntity<List<Camion>> getcamiones() {
        return ResponseEntity.ok(camiones);
    }

    // Genera reportes detallados detallado de eficiencia de camiones
    @GetMapping("aco/reportes/eficiencia-camiones")
    @ResponseBody
    public ResponseEntity<Object> reporteEficienciaCamiones() {
        // Validar que el algoritmo genético esté inicializado
        if (this.aco == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Primero ejecuta una simulación ACO");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Map<String, Object> reporte = new HashMap<>();
        List<Map<String, Object>> estadisticasCamiones = new ArrayList<>();
        // Generar estadísticas individuales para cada vehículo de la flota
        for (Camion camion : camiones) {
            Map<String, Object> stats = new HashMap<>();
            // INFORMACIÓN BÁSICA DEL VEHÍCULO
            stats.put("codigo", camion.getCodigo());
            stats.put("tipo", camion.getCodigo().substring(0, 2));

            // MÉTRICAS DE MOVIMIENTO Y DISTANCIA
            stats.put("distanciaRecorrida", camion.getDistanciaRecorrida() != null ? camion.getDistanciaRecorrida() : 0.0);
            stats.put("cargaAsignada", camion.getCargaAsignada());

            // MÉTRICAS DE CAPACIDAD Y UTILIZACIÓN
            stats.put("capacidadTotal", camion.getCarga());
            stats.put("utilizacionCapacidad", (camion.getCargaAsignada() / camion.getCarga()) * 100);
            stats.put("tiempoViaje", camion.getTiempoViaje());

            // MÉTRICAS DE PRODUCTIVIDAD
            stats.put("pedidosEnRuta", camion.getRoute() != null ? contarPedidosEnRuta(camion) : 0);

            // MÉTRICAS DE COSTOS OPERACIONALES
            stats.put("consumoCombustible", calcularConsumoCombustible(camion));

            // ESTADO OPERACIONAL Y DISPONIBILIDAD
            stats.put("enAveria", camion.isEnAveria());
            stats.put("tipoAveria", camion.getTipoAveria());

            estadisticasCamiones.add(stats);
        }
        // CONSTRUCCIÓN DE RESPUESTA CONSOLIDADA
        reporte.put("camiones", estadisticasCamiones);
        reporte.put("totalCamiones", camiones.size());
        reporte.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(reporte);
    }

    // Genera reportes de cumplimiento de entregas y estado de pedidos
    @GetMapping("aco/reportes/cumplimiento-entregas")
    @ResponseBody
    public ResponseEntity<Object> reporteCumplimientoEntregas() {
        // Validar que el algoritmo genético y pedidos estén inicializados
        if (this.aco == null || pedidos == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Primero ejecuta una simulación ACO");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        Map<String, Object> reporte = new HashMap<>();

        // CONTADORES PARA MÉTRICAS DE CUMPLIMIENTO
        int totalPedidos = pedidos.size();
        int pedidosEntregados = 0;
        int pedidosPendientes = 0;
        int pedidosRetrasados = 0;
        double tiempoPromedioEntrega = 0;

        // ANÁLISIS ESTADO POR ESTADO DE CADA PEDIDO
        for (Pedido pedido : pedidos) {
            if (pedido.isEntregado()) {
                // Incrementar contador de entregas exitosas
                pedidosEntregados++;
            } else {
                // Incrementar contador de pedidos aún no completados
                pedidosPendientes++;
            }
        }
        // CONSTRUCCIÓN DE RESPUESTA CON MÉTRICAS CALCULADAS
        reporte.put("totalPedidos", totalPedidos);
        reporte.put("pedidosEntregados", pedidosEntregados);
        reporte.put("pedidosPendientes", pedidosPendientes);
        reporte.put("pedidosRetrasados", pedidosRetrasados);
        reporte.put("porcentajeCumplimiento", totalPedidos > 0 ? (double) pedidosEntregados / totalPedidos * 100 : 0);
        reporte.put("tiempoPromedioEntrega", tiempoPromedioEntrega);
        reporte.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(reporte);
    }

    // Genera reportes de utilización de flota agrupados por tipo de camión
    @GetMapping("aco/reportes/utilizacion-flota")
    @ResponseBody
    public ResponseEntity<Object> reporteUtilizacionFlota() {
        // Validar inicialización del algoritmo genético
        if (this.aco == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Primero ejecuta una simulación ACO");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Map<String, Object> reporte = new HashMap<>();
        Map<String, Object> estadisticasPorTipo = new HashMap<>();

        // AGRUPACIÓN DE VEHÍCULOS POR TIPO PARA ANÁLISIS SEGMENTAD
        Map<String, List<Camion>> camionsPorTipo = new HashMap<>();
        for (Camion camion : camiones) {
            String tipo = camion.getCodigo().substring(0, 2);
            camionsPorTipo.computeIfAbsent(tipo, k -> new ArrayList<>()).add(camion);
        }

        // CÁLCULO DE ESTADÍSTICAS POR TIPO DE VEHÍCULO
        for (Map.Entry<String, List<Camion>> entry : camionsPorTipo.entrySet()) {
            String tipo = entry.getKey();
            List<Camion> camionsTipo = entry.getValue();

            Map<String, Object> stats = new HashMap<>();
            stats.put("cantidadCamiones", camionsTipo.size());
            stats.put("camionesActivos", camionsTipo.stream().mapToInt(c -> c.getRoute() != null && !c.getRoute().isEmpty() ? 1 : 0).sum());
            stats.put("camionesEnAveria", camionsTipo.stream().mapToInt(c -> c.isEnAveria() ? 1 : 0).sum());
            stats.put("capacidadTotal", camionsTipo.stream().mapToDouble(Camion::getCarga).sum());
            stats.put("capacidadUtilizada", camionsTipo.stream().mapToDouble(Camion::getCargaAsignada).sum());
            stats.put("porcentajeUtilizacion",
                    camionsTipo.stream().mapToDouble(Camion::getCarga).sum() > 0 ?
                            (camionsTipo.stream().mapToDouble(Camion::getCargaAsignada).sum() /
                                    camionsTipo.stream().mapToDouble(Camion::getCarga).sum()) * 100 : 0);

            estadisticasPorTipo.put(tipo, stats);
        }

        // MÉTRICAS GLOBALES DE LA FLOTA CONSOLIDADA
        reporte.put("estadisticasPorTipo", estadisticasPorTipo);
        reporte.put("totalFlota", camiones.size());
        reporte.put("flotaActiva", camiones.stream().mapToInt(c -> c.getRoute() != null && !c.getRoute().isEmpty() ? 1 : 0).sum());
        reporte.put("flotaEnAveria", camiones.stream().mapToInt(c -> c.isEnAveria() ? 1 : 0).sum());
        reporte.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(reporte);
    }

    // Genera reportes de incidentes y averías en la flota
    @GetMapping("aco/reportes/incidentes")
    @ResponseBody
    public ResponseEntity<Object> reporteIncidentes() {
        // Validar inicialización del algoritmo genético
        if (this.aco == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Primero ejecuta una simulación ACO");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Map<String, Object> reporte = new HashMap<>();
        List<Map<String, Object>> incidentes = new ArrayList<>();

        int averiasLeves = 0;
        int averiasModeradas = 0;
        int averiasGraves = 0;

        // ANÁLISIS DE VEHÍCULOS EN ESTADO DE AVERÍA
        for (Camion camion : camiones) {
            if (camion.isEnAveria()) {
                Map<String, Object> incidente = new HashMap<>();
                incidente.put("codigoCamion", camion.getCodigo());
                incidente.put("tipoAveria", camion.getTipoAveria());
                incidente.put("tiempoInicioAveria", camion.getTiempoInicioAveria());
                incidente.put("tiempoFinAveria", camion.getTiempoFinAveria());
                incidente.put("detenido", camion.isDetenido());
                incidente.put("tiempoDetenido", camion.getTiempoDetenido());

                // Clasificar tipo de incidente según código de severidad
                switch (camion.getTipoAveria()) {
                    case 1:
                        averiasLeves++;
                        incidente.put("descripcion", "TI1 - Llanta baja (2h inmovilización)");
                        break;
                    case 2:
                        averiasModeradas++;
                        incidente.put("descripcion", "TI2 - Motor ahogado (2h + 1 turno taller)");
                        break;
                    case 3:
                        averiasGraves++;
                        incidente.put("descripcion", "TI3 - Choque (4h + 1 día taller)");
                        break;
                }

                incidentes.add(incidente);
            }
        }
        // CONSTRUCCIÓN DE RESPUESTA CON ANÁLISIS DE IMPACTO
        reporte.put("incidentes", incidentes);
        reporte.put("totalIncidentes", incidentes.size());
        reporte.put("averiasLeves", averiasLeves);
        reporte.put("averiasModeradas", averiasModeradas);
        reporte.put("averiasGraves", averiasGraves);
        reporte.put("impactoOperativo", calcularImpactoOperativo());
        reporte.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(reporte);
    }

    // Genera un dashboard consolidado con KPIs y alertas operativas
    @GetMapping("aco/reportes/dashboard")
    @ResponseBody
    public ResponseEntity<Object> reporteDashboard() {
        // Validar inicialización del algoritmo genético
        if (this.aco == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Primero ejecuta una simulación ACO");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Map<String, Object> dashboard = new HashMap<>();

        // KPIs PRINCIPALES DE LA FLOTA
        Map<String, Object> kpis = new HashMap<>();
        kpis.put("totalCamiones", camiones.size());
        kpis.put("camionesActivos", camiones.stream().mapToInt(c -> c.getRoute() != null && !c.getRoute().isEmpty() ? 1 : 0).sum());
        kpis.put("camionesEnAveria", camiones.stream().mapToInt(c -> c.isEnAveria() ? 1 : 0).sum());

        // MÉTRICAS DE CUMPLIMIENTO DE PEDIDOS
        if (pedidos != null) {
            kpis.put("totalPedidos", pedidos.size());
            kpis.put("pedidosEntregados", pedidos.stream().mapToInt(p -> p.isEntregado() ? 1 : 0).sum());
            kpis.put("porcentajeCumplimiento",
                    pedidos.size() > 0 ? (double) pedidos.stream().mapToInt(p -> p.isEntregado() ? 1 : 0).sum() / pedidos.size() * 100 : 0);
        }

        double capacidadTotal = camiones.stream().mapToDouble(Camion::getCarga).sum();
        double capacidadUtilizada = camiones.stream().mapToDouble(Camion::getCargaAsignada).sum();
        kpis.put("capacidadTotal", capacidadTotal);
        kpis.put("capacidadUtilizada", capacidadUtilizada);
        kpis.put("porcentajeUtilizacionFlota", capacidadTotal > 0 ? (capacidadUtilizada / capacidadTotal) * 100 : 0);

        double consumoTotal = camiones.stream().mapToDouble(this::calcularConsumoCombustible).sum();
        kpis.put("consumoTotalCombustible", consumoTotal);

        dashboard.put("kpis", kpis);
        dashboard.put("ultimaActualizacion", LocalDateTime.now());
        dashboard.put("estadoSimulacion", "ACTIVA");

        List<String> alertas = new ArrayList<>();
        // Alerta de averías en la flota
        if (camiones.stream().anyMatch(Camion::isEnAveria)) {
            alertas.add("⚠️ Camiones con averías detectadas");
        }
        // Alerta de sobrecarga operacional (>90% capacidad utilizada)
        if (capacidadTotal > 0 && (capacidadUtilizada / capacidadTotal) > 0.9) {
            alertas.add("🚨 Utilización de flota superior al 90%");
        }
        // Alerta de pedidos pendientes de entrega
        if (pedidos != null && pedidos.stream().anyMatch(p -> !p.isEntregado())) {
            alertas.add("📦 Pedidos pendientes de entrega");
        }

        dashboard.put("alertas", alertas);

        return ResponseEntity.ok(dashboard);
    }

    // Cuenta el numero de pedidos en la ruta de un camión
    private int contarPedidosEnRuta(Camion camion) {
        // Validar existencia de ruta asignada al vehículo
        if (camion.getRoute() == null) return 0;
        return (int) camion.getRoute().stream().filter(node -> node.isPedido()).count();
    }

    // Calcula el consumo de combustible de un camión basado en su distancia recorrida y peso
    private double calcularConsumoCombustible(Camion camion) {
        if (camion.getDistanciaRecorrida() == null || camion.getDistanciaRecorrida() == 0) {
            return 0.0;
        }
        return camion.getDistanciaRecorrida() * camion.getPeso() / 180;
    }

    // Calcula el consumo de combustible de un camión basado en su distancia recorrida y peso
    private String calcularImpactoOperativo() {
        long camionesEnAveria = camiones.stream().filter(Camion::isEnAveria).count();
        if (camionesEnAveria == 0) return "NINGUNO";
        if (camionesEnAveria <= 2) return "BAJO";
        if (camionesEnAveria <= 5) return "MODERADO";
        return "ALTO";
    }

}
