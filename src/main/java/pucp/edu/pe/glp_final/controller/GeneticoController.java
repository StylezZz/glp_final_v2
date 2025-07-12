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
import pucp.edu.pe.glp_final.algorithm.NodoMapa;
import pucp.edu.pe.glp_final.models.Averia;
import pucp.edu.pe.glp_final.models.Bloqueo;
import pucp.edu.pe.glp_final.models.Camion;
import pucp.edu.pe.glp_final.models.Pedido;
import pucp.edu.pe.glp_final.models.enums.TipoIncidente;
import pucp.edu.pe.glp_final.service.CamionService;
import pucp.edu.pe.glp_final.service.PedidoService;

@RequestMapping("/api/aco")
@RestController
public class GeneticoController {

    private Genetico aco;
    private List<Camion> camiones;
    private List<Pedido> pedidos = new ArrayList<>();
    private List<Pedido> pedidosSimulacion = new ArrayList<>();
    private int primeraVezDiaria = 0;
    private int primeraVezSemanal = 0;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private CamionService camionService;

    @Autowired
    private SimulacionBloqueoController simulacionController;

    @PostMapping("/inicializar")
    @ResponseBody
    public ResponseEntity<Object> inicializar(
            @RequestParam(required = false) int tipoSimulacion
    ) {
        camiones = camionService.findAll();
        aco = new Genetico(camiones, tipoSimulacion);

        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Algoritmo de planificación inicializado");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/simulacionRuta/dia-dia")
    @ResponseBody
    public ResponseEntity<List<Camion>> simulacionRutaDiaria(
            @RequestParam(required = false) int anio,
            @RequestParam(required = false) int mes, @RequestParam(required = false) double timer,
            @RequestParam(required = false) int minutosPorIteracion,
            @RequestBody(required = false) List<Averia> averias
    ) {
        if (averias == null) {
            averias = new ArrayList<>();
        }
        int dia = ((int) timer / 1440);
        int hora = (((int) timer % 1440) / 60);
        int minuto = ((int) timer % 60);

        averias(averias, timer);

        if (primeraVezDiaria == 0) {
            List<Integer> dias = new ArrayList<>();
            dias.add(dia);
            pedidos = pedidoService.obtenerPedidosPorFecha(dias, anio, mes);
            pedidosSimulacion = pedidoService.dividirPedidos(pedidos, 1);
            aco.simulacionRuteo(anio, mes, dia, hora, minuto, minutosPorIteracion, pedidosSimulacion,
                    new ArrayList<>(), pedidos,
                    primeraVezDiaria, timer, 1);
            for (Camion vehiculo : camiones) {
                if (vehiculo.getRoute().isEmpty()) {
                    continue;
                } else {
                    for (NodoMapa ubicacion : vehiculo.getRoute()) {
                        if (ubicacion.isEsPedido()) {
                            if (ubicacion.getPedido().isEntregado()) {
                                for (Pedido pedido : pedidos) {
                                    if (pedido.getId() == ubicacion.getPedido().getId()) {
                                        pedido.setEntregado(true);
                                        pedido.setEntregadoCompleto(true);
                                        pedidoService.guardar(pedido);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            List<Integer> dias = new ArrayList<>();
            dias.add(dia);
            pedidos = pedidoService.obtenerPedidosPorFecha(dias, anio, mes);
            pedidosSimulacion = pedidoService.dividirPedidos(pedidos, 1);
            aco.simulacionRuteo(anio, mes, dia, hora, minuto, minutosPorIteracion, pedidosSimulacion,
                    new ArrayList<>(), pedidos,
                    primeraVezDiaria, timer, 1);
            for (Camion vehiculo : camiones) {
                if (vehiculo.getRoute().isEmpty()) {
                    continue;
                } else {
                    for (NodoMapa ubicacion : vehiculo.getRoute()) {
                        if (ubicacion.isEsPedido()) {
                            if (ubicacion.getPedido().isEntregado()) {
                                for (Pedido pedido : pedidos) {
                                    if (pedido.getId() == ubicacion.getPedido().getId()) {
                                        pedido.setEntregado(true);
                                        pedido.setEntregadoCompleto(true);
                                        pedidoService.guardar(pedido);
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

    @PostMapping("/simulacionRuta/semanal")
    @ResponseBody
    public ResponseEntity<List<Camion>> simulacionRutaSemanal(
            @RequestParam(required = false) int anio,
            @RequestParam(required = false) int mes,
            @RequestParam(required = false) double timer,
            @RequestParam(required = false) int minutosPorIteracion,
            @RequestBody(required = false) List<Averia> averias
    ) {
        if (averias == null) averias = new ArrayList<>();
        int dia = ((int) timer / 1440);
        int hora = (((int) timer % 1440) / 60);
        int minuto = ((int) timer % 60);

        averias(averias, timer);

        if (primeraVezSemanal == 0) {
            pedidos = simulacionController.getPedidos();
            pedidosSimulacion = pedidoService.dividirPedidos(pedidos, 2);
            List<Bloqueo> bloqueos = simulacionController.getBloqueos();
            aco.simulacionRuteo(anio, mes, dia, hora, minuto, minutosPorIteracion, pedidosSimulacion, bloqueos,
                    pedidos, primeraVezSemanal, timer, 2);
        } else {
            List<Bloqueo> bloqueos = simulacionController.getBloqueos();
            aco.simulacionRuteo(anio, mes, dia, hora, minuto, minutosPorIteracion, pedidosSimulacion, bloqueos,
                    pedidos, primeraVezSemanal, timer, 2);
        }

        primeraVezSemanal = 1;
        return ResponseEntity.ok(aco.getCamiones());

    }

    @GetMapping("/simulacionRuta/bloqueo")
    @ResponseBody
    public ResponseEntity<List<Bloqueo>> obtenerBloqeos() {
        return ResponseEntity.ok(aco.getBloqueosActivos());
    }

    @GetMapping("/simulacionRuta/pedido")
    @ResponseBody
    public ResponseEntity<List<Pedido>> obtenerPedidosOriginales() {
        int i = 0;
        for (Pedido pedido : aco.getOriginalPedidos()) {
            if (pedido.isEntregado()) {
                i++;
            }
        }
        System.out.println("Pedidos entregados: " + i + "/" + aco.getOriginalPedidos().size());
        return ResponseEntity.ok(aco.getOriginalPedidos());
    }

    @GetMapping("/simulacionRuta/pedidoDividido")
    @ResponseBody
    public ResponseEntity<List<Pedido>> obtenerPedidosDivididos() {

        return ResponseEntity.ok(aco.getPedidos());
    }

    @GetMapping("/simulacionRuta/almacen")
    @ResponseBody
    public ResponseEntity<Object> obtenerAlmacenes() {
        if (this.aco == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El algoritmo ACO no ha sido inicializado. Llama primero a /api/aco/inicializar.");
        }
        return ResponseEntity.ok(aco.getMapa().getAlmacenes());
    }

    @PostMapping("/simulacionRuta/reset")
    @ResponseBody
    public ResponseEntity<Object> reset() {
        try {
            // Reset de camiones y pedidos
            int camionesReseteados = resetearCamionesCompleto();
            int pedidosActualizados = resetearPedidos();
            limpiarListasMemoria();

            // Reset algoritmo y variables de control
            aco = null;
            primeraVezDiaria = primeraVezSemanal = 0;

            // Construir respuesta simplificada
            Map<String, Object> response = Map.of(
                    "mensaje", "Reset completado",
                    "detalles", Map.of(
                            "camionesReseteados", camionesReseteados,
                            "pedidosActualizados", pedidosActualizados,
                            "listasLimpiadas", "pedidos, pedidosSimulacion",
                            "variablesControl", "primeraVezDiaria, primeraVezSemanal"
                    ),
                    "estadoReset", "COMPLETO",
                    "timestamp", LocalDateTime.now(),
                    "tiempoInicial", "Día 2, 00:00:00",
                    "timerRecomendado", 1440.0, // Día 2 para evitar bug día 0
                    "resumen", String.format("Reseteados: %d camiones, %d pedidos",
                            camionesReseteados, pedidosActualizados)
            );


            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Error durante el reset: " + e.getMessage(),
                            "estadoReset", "ERROR",
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Resetea completamente todos los camiones a su estado inicial
     *
     * @return número de camiones reseteados
     */
    private int resetearCamionesCompleto() {
        int contador = 0;
        if (camiones == null || camiones.isEmpty()) {
            return contador; // No hay camiones para resetear
        }
        for (Camion camion : camiones) {
            // Estado operativo - rutas y cargas
            if (camion.getRoute() != null) {
                camion.getRoute().clear();
            } else {
                camion.inicializarRuta();
            }
            camion.setCargaAsignada(0);
            camion.setDistanciaRecorrida(0.0);
            camion.setTiempoViaje(0);
            camion.setUbicacionActual(null);
            camion.setCapacidadCompleta(false);
            camion.setGlpDisponible(camion.getCarga()); // Volver a capacidad original
            camion.setCargaAnterior(0);

            // Estado de averías - CRÍTICO para timer
            camion.setEnAveria(false);
            camion.setTipoAveria(0);
            camion.setTiempoInicioAveria(null);
            camion.setTiempoFinAveria(null);
            camion.setDetenido(false);
            camion.setTiempoDetenido(0);

            // Pedidos asignados
            if (camion.getPedidosAsignados() != null) {
                camion.getPedidosAsignados().clear();
            } else {
                camion.setPedidosAsignados(new ArrayList<>());
            }

            contador++;
        }
        return contador;
    }

    /**
     * Resetea el estado de todos los pedidos en memoria y base de datos
     *
     * @return número de pedidos actualizados
     */
    private int resetearPedidos() {
        int contador = 0;
        if (pedidos != null) {
            for (Pedido pedido : pedidos) {
                pedido.setEntregado(false);
                pedido.setEntregadoCompleto(false);
                pedidoService.guardar(pedido); // Persistir en BD
                contador++;
            }
        }
        return contador;
    }

    /**
     * Limpia todas las listas en memoria para evitar memory leaks
     */
    private void limpiarListasMemoria() {
        if (pedidos != null) {
            pedidos.clear();
        } else {
            pedidos = new ArrayList<>();
        }

        if (pedidosSimulacion != null) {
            pedidosSimulacion.clear();
        } else {
            pedidosSimulacion = new ArrayList<>();
        }
    }

    public void averias(List<Averia> averias, double timer) {
        for (Averia averia : averias) {
            for (Camion vehiculo : camiones) {
                if (averia.getCodigoCamion().equals(vehiculo.getCodigo())) {
                    int dia = ((int) timer / 1440);
                    vehiculo.setEnAveria(true);
                    vehiculo.setTiempoInicioAveria(timer);
                    if (averia.getTipoAveria() == TipoIncidente.LEVE) {

                        vehiculo.setTipoAveria(1);
                        vehiculo.setDetenido(true);
                        vehiculo.setTiempoDetenido(vehiculo.getTiempoInicioAveria() + 120);
                        vehiculo.setTiempoFinAveria(vehiculo.getTiempoInicioAveria() + 120);

                    } else {
                        if (averia.getTipoAveria() == TipoIncidente.MODERADO) {
                            vehiculo.setTipoAveria(2);
                            vehiculo.setDetenido(true);
                            vehiculo.setTiempoDetenido(vehiculo.getTiempoInicioAveria() + 120);
                            if (averia.getTurnoAveria() == 1) {
                                vehiculo.setTiempoFinAveria((double) (dia * 1440 + 960));

                            } else {
                                if (averia.getTurnoAveria() == 2) {

                                    vehiculo.setTiempoFinAveria((double) (dia * 1440 + 1440));

                                } else {
                                    if (averia.getTurnoAveria() == 3) {
                                        vehiculo.setTiempoFinAveria((double) (dia * 1440 + 1440 + 480));
                                    }
                                }
                            }

                        } else {
                            if (averia.getTipoAveria() == TipoIncidente.GRAVE) {
                                vehiculo.setDetenido(true);
                                vehiculo.setTiempoDetenido(vehiculo.getTiempoInicioAveria() + 240);
                                vehiculo.setTipoAveria(3);
                                vehiculo.setTiempoFinAveria((double) (dia * 1440 + 1440 + 960));
                            } else if (averia.getTipoAveria() == TipoIncidente.MANTENIMIENTO) {
                                vehiculo.setTipoAveria(4);
                                vehiculo.setDetenido(true);
                                // Mantenimiento: 1 día completo (1440 minutos)
                                vehiculo.setTiempoDetenido(vehiculo.getTiempoInicioAveria());
                                vehiculo.setTiempoFinAveria(vehiculo.getTiempoInicioAveria() + 1440);
                            }
                        }
                    }

                }
            }
        }
    }

    @GetMapping("/camiones")
    @ResponseBody
    public ResponseEntity<List<Camion>> getcamiones() {
        return ResponseEntity.ok(camiones);
    }

    // ENDPOINTS DE LOS REPORTES
    @GetMapping("/reportes/eficiencia-camiones")
    @ResponseBody
    public ResponseEntity<Object> reporteEficienciaCamiones() {
        if (this.aco == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Primero ejecuta una simulación ACO");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Map<String, Object> reporte = new HashMap<>();
        List<Map<String, Object>> estadisticasCamiones = new ArrayList<>();

        for (Camion camion : camiones) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("codigo", camion.getCodigo());
            stats.put("tipo", camion.getCodigo().substring(0, 2));
            stats.put("distanciaRecorrida", camion.getDistanciaRecorrida() != null ? camion.getDistanciaRecorrida() : 0.0);
            stats.put("cargaAsignada", camion.getCargaAsignada());
            stats.put("capacidadTotal", camion.getCarga());
            stats.put("utilizacionCapacidad", (camion.getCargaAsignada() / camion.getCarga()) * 100);
            stats.put("tiempoViaje", camion.getTiempoViaje());
            stats.put("pedidosEnRuta", camion.getRoute() != null ? contarPedidosEnRuta(camion) : 0);
            stats.put("consumoCombustible", calcularConsumoCombustible(camion));
            stats.put("enAveria", camion.isEnAveria());
            stats.put("tipoAveria", camion.getTipoAveria());

            estadisticasCamiones.add(stats);
        }

        reporte.put("camiones", estadisticasCamiones);
        reporte.put("totalCamiones", camiones.size());
        reporte.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(reporte);
    }

    @GetMapping("/reportes/cumplimiento-entregas")
    @ResponseBody
    public ResponseEntity<Object> reporteCumplimientoEntregas() {
        if (this.aco == null || pedidos == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Primero ejecuta una simulación ACO");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Map<String, Object> reporte = new HashMap<>();

        int totalPedidos = pedidos.size();
        int pedidosEntregados = 0;
        int pedidosPendientes = 0;
        int pedidosRetrasados = 0;
        double tiempoPromedioEntrega = 0;

        for (Pedido pedido : pedidos) {
            if (pedido.isEntregado()) {
                pedidosEntregados++;
            } else {
                pedidosPendientes++;
            }
        }

        reporte.put("totalPedidos", totalPedidos);
        reporte.put("pedidosEntregados", pedidosEntregados);
        reporte.put("pedidosPendientes", pedidosPendientes);
        reporte.put("pedidosRetrasados", pedidosRetrasados);
        reporte.put("porcentajeCumplimiento", totalPedidos > 0 ? (double) pedidosEntregados / totalPedidos * 100 : 0);
        reporte.put("tiempoPromedioEntrega", tiempoPromedioEntrega);
        reporte.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(reporte);
    }

    @GetMapping("/reportes/utilizacion-flota")
    @ResponseBody
    public ResponseEntity<Object> reporteUtilizacionFlota() {
        if (this.aco == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Primero ejecuta una simulación ACO");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Map<String, Object> reporte = new HashMap<>();
        Map<String, Object> estadisticasPorTipo = new HashMap<>();

        // Agrupar por tipo de camión
        Map<String, List<Camion>> camionsPorTipo = new HashMap<>();
        for (Camion camion : camiones) {
            String tipo = camion.getCodigo().substring(0, 2);
            camionsPorTipo.computeIfAbsent(tipo, k -> new ArrayList<>()).add(camion);
        }

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

        reporte.put("estadisticasPorTipo", estadisticasPorTipo);
        reporte.put("totalFlota", camiones.size());
        reporte.put("flotaActiva", camiones.stream().mapToInt(c -> c.getRoute() != null && !c.getRoute().isEmpty() ? 1 : 0).sum());
        reporte.put("flotaEnAveria", camiones.stream().mapToInt(c -> c.isEnAveria() ? 1 : 0).sum());
        reporte.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(reporte);
    }

    @GetMapping("/reportes/incidentes")
    @ResponseBody
    public ResponseEntity<Object> reporteIncidentes() {
        if (this.aco == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Primero ejecuta una simulación ACO");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Map<String, Object> reporte = new HashMap<>();
        List<Map<String, Object>> incidentes = new ArrayList<>();

        int averiasLeves = 0; // TI1
        int averiasModeradas = 0; // TI2
        int averiasGraves = 0; // TI3

        for (Camion camion : camiones) {
            if (camion.isEnAveria()) {
                Map<String, Object> incidente = new HashMap<>();
                incidente.put("codigoCamion", camion.getCodigo());
                incidente.put("tipoAveria", camion.getTipoAveria());
                incidente.put("tiempoInicioAveria", camion.getTiempoInicioAveria());
                incidente.put("tiempoFinAveria", camion.getTiempoFinAveria());
                incidente.put("detenido", camion.isDetenido());
                incidente.put("tiempoDetenido", camion.getTiempoDetenido());

                // Clasificar por tipo
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

        reporte.put("incidentes", incidentes);
        reporte.put("totalIncidentes", incidentes.size());
        reporte.put("averiasLeves", averiasLeves);
        reporte.put("averiasModeradas", averiasModeradas);
        reporte.put("averiasGraves", averiasGraves);
        reporte.put("impactoOperativo", calcularImpactoOperativo());
        reporte.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(reporte);
    }

    @GetMapping("/reportes/dashboard")
    @ResponseBody
    public ResponseEntity<Object> reporteDashboard() {
        if (this.aco == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Primero ejecuta una simulación ACO");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Map<String, Object> dashboard = new HashMap<>();

        // KPIs principales
        Map<String, Object> kpis = new HashMap<>();
        kpis.put("totalCamiones", camiones.size());
        kpis.put("camionesActivos", camiones.stream().mapToInt(c -> c.getRoute() != null && !c.getRoute().isEmpty() ? 1 : 0).sum());
        kpis.put("camionesEnAveria", camiones.stream().mapToInt(c -> c.isEnAveria() ? 1 : 0).sum());

        if (pedidos != null) {
            kpis.put("totalPedidos", pedidos.size());
            kpis.put("pedidosEntregados", pedidos.stream().mapToInt(p -> p.isEntregado() ? 1 : 0).sum());
            kpis.put("porcentajeCumplimiento",
                    pedidos.size() > 0 ? (double) pedidos.stream().mapToInt(p -> p.isEntregado() ? 1 : 0).sum() / pedidos.size() * 100 : 0);
        }

        // Capacidad total vs utilizada
        double capacidadTotal = camiones.stream().mapToDouble(Camion::getCarga).sum();
        double capacidadUtilizada = camiones.stream().mapToDouble(Camion::getCargaAsignada).sum();
        kpis.put("capacidadTotal", capacidadTotal);
        kpis.put("capacidadUtilizada", capacidadUtilizada);
        kpis.put("porcentajeUtilizacionFlota", capacidadTotal > 0 ? (capacidadUtilizada / capacidadTotal) * 100 : 0);

        // Consumo total estimado
        double consumoTotal = camiones.stream().mapToDouble(this::calcularConsumoCombustible).sum();
        kpis.put("consumoTotalCombustible", consumoTotal);

        dashboard.put("kpis", kpis);
        dashboard.put("ultimaActualizacion", LocalDateTime.now());
        dashboard.put("estadoSimulacion", "ACTIVA");

        // Alertas
        List<String> alertas = new ArrayList<>();
        if (camiones.stream().anyMatch(Camion::isEnAveria)) {
            alertas.add("⚠️ Camiones con averías detectadas");
        }
        if (capacidadTotal > 0 && (capacidadUtilizada / capacidadTotal) > 0.9) {
            alertas.add("🚨 Utilización de flota superior al 90%");
        }
        if (pedidos != null && pedidos.stream().anyMatch(p -> !p.isEntregado())) {
            alertas.add("📦 Pedidos pendientes de entrega");
        }

        dashboard.put("alertas", alertas);

        return ResponseEntity.ok(dashboard);
    }

    // Métodos auxiliares para los reportes
    private int contarPedidosEnRuta(Camion camion) {
        if (camion.getRoute() == null) return 0;
        return (int) camion.getRoute().stream().filter(node -> node.isEsPedido()).count();
    }

    private double calcularConsumoCombustible(Camion camion) {
        if (camion.getDistanciaRecorrida() == null || camion.getDistanciaRecorrida() == 0) {
            return 0.0;
        }
        // Fórmula: Consumo = Distancia(Km) x Peso(Ton) / 180
        return camion.getDistanciaRecorrida() * camion.getPeso() / 180;
    }

    private String calcularImpactoOperativo() {
        long camionesEnAveria = camiones.stream().filter(Camion::isEnAveria).count();
        if (camionesEnAveria == 0) return "NINGUNO";
        if (camionesEnAveria <= 2) return "BAJO";
        if (camionesEnAveria <= 5) return "MODERADO";
        return "ALTO";
    }

}
