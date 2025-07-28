package pucp.edu.pe.glp_final.controller;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
import pucp.edu.pe.glp_final.models.*;
import pucp.edu.pe.glp_final.models.enums.TipoIncidente;
import pucp.edu.pe.glp_final.service.AveriaProgramadaService;
import pucp.edu.pe.glp_final.service.CamionService;
import pucp.edu.pe.glp_final.service.PedidoService;
import pucp.edu.pe.glp_final.service.ReplanificacionDiariaService;

@RequestMapping("/api/genetico")
@RestController
public class GeneticoController {

    private Genetico aco;
    private List<Camion> camiones;
    private List<Pedido> pedidos = new ArrayList<>();
    private List<Pedido> pedidosSimulacion = new ArrayList<>();
    private int primeraEjecucionDia = 0;
    private int primeraEjecucionSemanal = 0;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private CamionService camionService;

    @Autowired
    private SimulacionController simulacionController;

    @Autowired
    private AveriaProgramadaService averiaProgramadaService;

    @Autowired
    private ReplanificacionDiariaService replanificacionDiariaService;

    @PostMapping("/inicializar")
    @ResponseBody
    public ResponseEntity<Object> inicializar(
            @RequestParam(required = false) int tipoSimulacion,
            @RequestParam(required = false) int dia,
            @RequestParam(required = false) int mes,
            @RequestParam(required = false) int anio,
            @RequestParam(required = false) int hora,
            @RequestParam(required = false) int minuto

    ) {
        camiones = camionService.findAll();
        LocalDateTime fechaBaseSimulacion = LocalDateTime.of(anio, mes, dia, hora, minuto);
        aco = new Genetico(camiones, tipoSimulacion, fechaBaseSimulacion);

        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Algoritmo de planificación inicializado para la fecha " + fechaBaseSimulacion);
        response.put("simulacionId", String.valueOf(aco.getId()));
        response.put("fechaBase", fechaBaseSimulacion.toString());
        response.put("tipoSimulacion", String.valueOf(tipoSimulacion));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/dia")
    @ResponseBody
    public ResponseEntity<List<Camion>> ejecutarDia(
            @RequestParam(required = false) int anio,
            @RequestParam(required = false) int mes,
            @RequestParam(required = false) double timer,
            @RequestParam(required = false) int minutosPorIteracion,
            @RequestBody(required = false) List<Averia> averias
    ) {
        System.out.println("Ejecutando simulación DIARIA - Timer: " + timer);
        if (averias == null) averias = new ArrayList<>();
        int dia = ((int) timer / 1440);
        int hora = (((int) timer % 1440) / 60);
        int minuto = ((int) timer % 60);

        int mesAjustado = mes;
        int anioAjustado = anio;
        int diaAjustado = dia;

        while (diaAjustado > getDaysInMonth(anioAjustado, mesAjustado)) {
            diaAjustado -= getDaysInMonth(anioAjustado, mesAjustado);
            mesAjustado++;
            if (mesAjustado > 12) {
                mesAjustado = 1;
                anioAjustado++;
            }
        }

        gestionarAverias(averias, new ArrayList<>(), timer);
        List<Pedido> pedidosNuevos = obtenerPedidosNuevosDiarios(anioAjustado,mesAjustado,timer,minutosPorIteracion);

        if (!pedidosNuevos.isEmpty() && requiereReplanificacionCritica(pedidosNuevos, timer)) {
            System.out.println("🚨 DETECTADOS PEDIDOS CRÍTICOS - Ejecutando replanificación urgente...");

            replanificacionDiariaService.replanificarDiaria(
                    pedidosNuevos,
                    camiones,
                    aco.getMapa(),
                    new ArrayList<>(), // Sin bloqueos en simulación diaria
                    timer,
                    anioAjustado, mesAjustado, diaAjustado, hora, minuto,
                    aco.getFechaBaseSimulacion()
            );

            // Actualizar listas de pedidos
            actualizarPedidosDiarios(pedidosNuevos);

        } else {
            // 📋 PLANIFICACIÓN NORMAL DIARIA
            System.out.println("📋 Ejecutando planificación normal diaria");
            gestionarPedidosDiariosNormal(anioAjustado, mesAjustado, timer, minutosPorIteracion, diaAjustado, hora, minuto);
        }
        //gestionarPedidos(anioAjustado, mesAjustado, timer, minutosPorIteracion, diaAjustado, hora, minuto);
        primeraEjecucionDia = 1;
        return ResponseEntity.ok(aco.getCamiones());
    }

    private List<Pedido> obtenerPedidosNuevosDiarios(int anio, int mes, double timer, int minutosPorIteracion) {
        LocalDateTime fechaActual = LocalDateTime.of(anio, mes, 1, 0, 0).plusMinutes((long) timer);
        LocalDateTime fechaAnterior = fechaActual.minusMinutes(minutosPorIteracion);

        List<Integer> dias = Arrays.asList((int) (timer / 1440));
        List<Pedido> pedidosDelDia = pedidoService.obtenerPedidosPorFecha(dias, anio, mes);

        // Filtrar solo pedidos que aparecieron en este intervalo
        return pedidosDelDia.stream()
                .filter(p -> !p.getFechaDeRegistro().isBefore(fechaAnterior))
                .filter(p -> !p.getFechaDeRegistro().isAfter(fechaActual))
                .filter(p -> !p.isEntregado())
                .collect(Collectors.toList());
    }

    /**
     * 🆕 Evalúa si los nuevos pedidos requieren replanificación crítica
     */
    private boolean requiereReplanificacionCritica(List<Pedido> pedidosNuevos, double timer) {
        // Caso específico del profesor: pedidos al punto más lejano (69,49) con poco tiempo
        boolean hayPedidosLejanos = pedidosNuevos.stream()
                .anyMatch(p -> p.getPosX() > 60 && p.getPosY() > 40); // Zona lejana

        boolean hayPedidosCriticos = pedidosNuevos.stream()
                .anyMatch(p -> esPedidoCriticoDiario(p, timer));

        // También verificar si todos los camiones están ocupados con rutas largas
        boolean camionesOcupados = camiones.stream()
                .filter(c -> !c.isEnAveria())
                .allMatch(c -> tieneRutaLarga(c, timer));

        return (hayPedidosLejanos || hayPedidosCriticos) && camionesOcupados;
    }

    /**
     * 🆕 Verifica si un pedido es crítico en simulación diaria
     */
    private boolean esPedidoCriticoDiario(Pedido pedido, double timer) {
        double tiempoRestante = pedido.getTiempoLlegada() - timer;
        double distanciaDesdeAlmacen = Math.sqrt(
                Math.pow(pedido.getPosX() - 12, 2) + Math.pow(pedido.getPosY() - 8, 2)
        );
        double tiempoViajeMinimo = distanciaDesdeAlmacen; // 1 minuto por unidad

        // CRÍTICO: Menos de 2 veces el tiempo de viaje o menos de 3 horas
        return tiempoRestante < (tiempoViajeMinimo * 2) || tiempoRestante < 180;
    }

    /**
     * 🆕 Verifica si un camión tiene una ruta larga comprometida
     */
    private boolean tieneRutaLarga(Camion camion, double timer) {
        if (camion.getRoute() == null || camion.getRoute().isEmpty()) {
            return false;
        }

        // Verificar si tiene pedidos asignados lejos del almacén
        if (camion.getPedidosAsignados() != null) {
            return camion.getPedidosAsignados().stream()
                    .anyMatch(p -> {
                        double distancia = Math.sqrt(
                                Math.pow(p.getPosX() - 12, 2) + Math.pow(p.getPosY() - 8, 2)
                        );
                        return distancia > 50; // Ruta considerada "larga"
                    });
        }

        return false;
    }

    /**
     * 🆕 Actualiza las listas de pedidos con los nuevos pedidos críticos
     */
    private void actualizarPedidosDiarios(List<Pedido> pedidosNuevos) {
        if (pedidos == null) pedidos = new ArrayList<>();
        if (pedidosSimulacion == null) pedidosSimulacion = new ArrayList<>();

        pedidos.addAll(pedidosNuevos);
        List<Pedido> pedidosNuevosDivididos = pedidoService.dividirPedidos(pedidosNuevos, 1);
        pedidosSimulacion.addAll(pedidosNuevosDivididos);

        System.out.println("📦 Agregados " + pedidosNuevos.size() + " pedidos nuevos críticos");
    }

    /**
     * 🆕 Gestión normal de pedidos diarios (sin replanificación crítica)
     */
    private void gestionarPedidosDiariosNormal(int anio, int mes, double timer, int minutosPorIteracion,
                                               int dia, int hora, int minuto) {
        List<Integer> dias = new ArrayList<>();
        dias.add(dia);
        pedidos = pedidoService.obtenerPedidosPorFecha(dias, anio, mes);
        pedidosSimulacion = pedidoService.dividirPedidos(pedidos, 1);

        aco.simulacionRuteo(
                anio, mes, dia, hora, minuto, minutosPorIteracion,
                pedidosSimulacion, new ArrayList<>(), pedidos,
                primeraEjecucionDia, timer, 1
        );

        // Actualizar estado de entrega
        actualizarEstadoEntregaDiaria();
    }

    /**
     * 🆕 Actualiza el estado de entrega de pedidos en simulación diaria
     */
    private void actualizarEstadoEntregaDiaria() {
        for (Camion vehiculo : camiones) {
            if (!vehiculo.getRoute().isEmpty()) {
                for (NodoMapa ubicacion : vehiculo.getRoute()) {
                    if (ubicacion.isEsPedido() && ubicacion.getPedido().isEntregado()) {
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

    /**
     * 🆕 Endpoint para forzar replanificación crítica (caso del profesor)
     */
    @PostMapping("/replanificar-critico")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> forzarReplanificacionCritica(
            @RequestParam double timer,
            @RequestBody List<Pedido> pedidosCriticos
    ) {
        try {
            int dia = ((int) timer / 1440);
            int hora = (((int) timer % 1440) / 60);
            int minuto = ((int) timer % 60);

            System.out.println("🚨 FORZANDO REPLANIFICACIÓN CRÍTICA - " + pedidosCriticos.size() + " pedidos");

            replanificacionDiariaService.replanificarDiaria(
                    pedidosCriticos, camiones, aco.getMapa(), new ArrayList<>(), timer,
                    2024, 1, dia, hora, minuto, aco.getFechaBaseSimulacion()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Replanificación crítica ejecutada exitosamente");
            response.put("pedidosCriticos", pedidosCriticos.size());
            response.put("camionesAfectados", contarCamionesAfectados());
            response.put("timestamp", LocalDateTime.now());
            response.put("tipoSimulacion", "DIARIA");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error en replanificación crítica: " + e.getMessage());
            errorResponse.put("tipoError", "REPLANIFICACION_CRITICA");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🆕 Cuenta camiones afectados por la replanificación
     */
    private int contarCamionesAfectados() {
        return (int) camiones.stream()
                .filter(c -> c.getPedidosAsignados() != null && !c.getPedidosAsignados().isEmpty())
                .count();
    }

    /**
     * 🆕 Endpoint para obtener estado de criticidad específico para simulación diaria
     */
    @GetMapping("/criticidad-diaria")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerCriticidadDiaria(
            @RequestParam double timer
    ) {
        Map<String, Object> estadoGeneral = new HashMap<>();
        List<Map<String, Object>> estadoPedidos = new ArrayList<>();

        int pedidosCriticos = 0;
        int pedidosUrgentes = 0;

        for (Pedido pedido : pedidos) {
            if (!pedido.isEntregado()) {
                Map<String, Object> estado = new HashMap<>();
                estado.put("pedidoId", pedido.getId());
                estado.put("cliente", pedido.getIdCliente());
                estado.put("posicion", Map.of("x", pedido.getPosX(), "y", pedido.getPosY()));

                double tiempoRestante = pedido.getTiempoLlegada() - timer;
                double distancia = Math.sqrt(
                        Math.pow(pedido.getPosX() - 12, 2) + Math.pow(pedido.getPosY() - 8, 2)
                );

                estado.put("tiempoRestante", tiempoRestante);
                estado.put("distanciaAlmacen", distancia);
                estado.put("tiempoViajeMinimo", distancia);

                boolean esCritico = esPedidoCriticoDiario(pedido, timer);
                boolean esUrgente = tiempoRestante < 60; // Menos de 1 hora

                estado.put("esCritico", esCritico);
                estado.put("esUrgente", esUrgente);
                estado.put("nivelCriticidad", calcularNivelCriticidad(pedido, timer));

                if (esCritico) pedidosCriticos++;
                if (esUrgente) pedidosUrgentes++;

                estadoPedidos.add(estado);
            }
        }

        // Información general
        estadoGeneral.put("totalPedidos", pedidos.size());
        estadoGeneral.put("pedidosCriticos", pedidosCriticos);
        estadoGeneral.put("pedidosUrgentes", pedidosUrgentes);
        estadoGeneral.put("requiereReplanificacion", pedidosCriticos > 0);
        estadoGeneral.put("estadoCamiones", obtenerEstadoCamiones(timer));
        estadoGeneral.put("pedidos", estadoPedidos);
        estadoGeneral.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(estadoGeneral);
    }

    /**
     * 🆕 Calcula nivel de criticidad específico para simulación diaria
     */
    private double calcularNivelCriticidad(Pedido pedido, double timer) {
        double tiempoRestante = pedido.getTiempoLlegada() - timer;
        double tiempoViajeMinimo = Math.sqrt(
                Math.pow(pedido.getPosX() - 12, 2) + Math.pow(pedido.getPosY() - 8, 2)
        );

        if (tiempoRestante <= tiempoViajeMinimo) {
            return 1.0; // Máxima criticidad
        }

        // Criticidad inversamente proporcional al tiempo de holgura
        double holgura = tiempoRestante - tiempoViajeMinimo;
        return Math.max(0.0, 1.0 - (holgura / 240)); // 4 horas de referencia para simulación diaria
    }

    /**
     * 🆕 Obtiene estado actual de todos los camiones
     */
    private List<Map<String, Object>> obtenerEstadoCamiones(double timer) {
        List<Map<String, Object>> estadoCamiones = new ArrayList<>();

        for (Camion camion : camiones) {
            Map<String, Object> estado = new HashMap<>();
            estado.put("codigo", camion.getCodigo());
            estado.put("enAveria", camion.isEnAveria());
            estado.put("detenido", camion.isDetenido());

            if (camion.getUbicacionActual() != null) {
                estado.put("posicionActual", Map.of(
                        "x", camion.getUbicacionActual().getX(),
                        "y", camion.getUbicacionActual().getY()
                ));
                estado.put("distanciaAlAlmacen", Math.sqrt(
                        Math.pow(camion.getUbicacionActual().getX() - 12, 2) +
                                Math.pow(camion.getUbicacionActual().getY() - 8, 2)
                ));
            }

            int pedidosAsignados = (camion.getPedidosAsignados() != null) ?
                    camion.getPedidosAsignados().size() : 0;
            estado.put("pedidosAsignados", pedidosAsignados);
            estado.put("cargaAsignada", camion.getCargaAsignada());
            estado.put("capacidadDisponible", camion.getCarga() - camion.getCargaAsignada());

            // Estado de disponibilidad
            boolean disponible = !camion.isEnAveria() && !camion.isDetenido() &&
                    (pedidosAsignados == 0 || camion.getCargaAsignada() < camion.getCarga());
            estado.put("disponible", disponible);

            estadoCamiones.add(estado);
        }

        return estadoCamiones;
    }

    /**
     * 🆕 Endpoint para simular el caso específico del profesor
     */
    @PostMapping("/simular-caso-profesor")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> simularCasoProfesor(
            @RequestParam double timer,
            @RequestParam(defaultValue = "12") int horasHolgura
    ) {
        try {
            System.out.println("👨‍🏫 SIMULANDO CASO DEL PROFESOR");

            // 1. Crear pedidos que fuercen a todos los camiones a ir al punto lejano (69,49)
            List<Pedido> pedidosLejanos = crearPedidosLejanos(timer, horasHolgura);

            // 2. Asignar estos pedidos (simulación inicial)
            asignarPedidosIniciales(pedidosLejanos);

            // 3. Esperar 1 hora (simulado)
            double timerDespues = timer + 60; // +1 hora

            // 4. Crear pedidos críticos que requieren replanificación
            List<Pedido> pedidosCriticos = crearPedidosCriticos(timerDespues);

            // 5. Ejecutar replanificación crítica
            replanificacionDiariaService.replanificarDiaria(
                    pedidosCriticos, camiones, aco.getMapa(), new ArrayList<>(), timerDespues,
                    2024, 1, 1, 12, 0, aco.getFechaBaseSimulacion()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Caso del profesor simulado exitosamente");
            response.put("pedidosLejanos", pedidosLejanos.size());
            response.put("pedidosCriticos", pedidosCriticos.size());
            response.put("timerInicial", timer);
            response.put("timerDespues", timerDespues);
            response.put("camionesReasignados", contarCamionesReasignados());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error simulando caso del profesor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🆕 Crea pedidos lejanos con holgura (caso inicial del profesor)
     */
    private List<Pedido> crearPedidosLejanos(double timer, int horasHolgura) {
        List<Pedido> pedidosLejanos = new ArrayList<>();

        // Crear 20 pedidos al punto más lejano (69,49) con holgura de 12h
        String[] clientes = {"c-999", "c-998", "c-997", "c-996", "c-995",
                "c-994", "c-993", "c-992", "c-991", "c-990",
                "c-989", "c-988", "c-987", "c-986", "c-985",
                "c-984", "c-983", "c-982", "c-981", "c-980"};

        int[] cantidades = {25, 25, 15, 15, 15, 15, 10, 10, 10, 10,
                5, 5, 5, 5, 5, 5, 5, 5, 5, 5};

        for (int i = 0; i < 20; i++) {
            Pedido pedido = new Pedido();
            pedido.setId(9000 + i); // IDs únicos
            pedido.setPosX(69);
            pedido.setPosY(49);
            pedido.setIdCliente(clientes[i]);
            pedido.setCantidadGLP(cantidades[i]);
            pedido.setHorasLimite(horasHolgura);
            pedido.setHoraDeInicio((int) timer);
            pedido.setTiempoLlegada((int) (timer + horasHolgura * 60));
            pedido.setEntregado(false);
            pedido.setAsignado(false);

            pedidosLejanos.add(pedido);
        }

        return pedidosLejanos;
    }

    /**
     * 🆕 Crea pedidos críticos que requieren replanificación
     */
    private List<Pedido> crearPedidosCriticos(double timer) {
        List<Pedido> pedidosCriticos = new ArrayList<>();

        // Crear pedidos críticos cerca del almacén con poco tiempo
        int[][] posicionesCriticas = {
                {15, 10}, {10, 15}, {20, 5}, {8, 20}, {25, 15}
        };

        for (int i = 0; i < posicionesCriticas.length; i++) {
            Pedido pedido = new Pedido();
            pedido.setId(8000 + i); // IDs únicos
            pedido.setPosX(posicionesCriticas[i][0]);
            pedido.setPosY(posicionesCriticas[i][1]);
            pedido.setIdCliente("c-critico-" + (i + 1));
            pedido.setCantidadGLP(10);
            pedido.setHorasLimite(2); // Solo 2 horas!
            pedido.setHoraDeInicio((int) timer);
            pedido.setTiempoLlegada((int) (timer + 120)); // 2 horas
            pedido.setEntregado(false);
            pedido.setAsignado(false);

            pedidosCriticos.add(pedido);
        }

        return pedidosCriticos;
    }

    /**
     * 🆕 Asigna pedidos iniciales a todos los camiones
     */
    private void asignarPedidosIniciales(List<Pedido> pedidosLejanos) {
        int pedidoIndex = 0;

        for (Camion camion : camiones) {
            if (!camion.isEnAveria() && pedidoIndex < pedidosLejanos.size()) {
                camion.setPedidosAsignados(new ArrayList<>());

                // Asignar múltiples pedidos según capacidad
                while (pedidoIndex < pedidosLejanos.size() &&
                        camion.tieneCapacidad(pedidosLejanos.get(pedidoIndex).getCantidadGLP())) {

                    Pedido pedido = pedidosLejanos.get(pedidoIndex);
                    camion.getPedidosAsignados().add(pedido);
                    camion.asignar(pedido);
                    pedido.setEntregado(true);
                    pedido.setIdCamion(camion.getCodigo());

                    pedidoIndex++;
                }

                System.out.println("🚛 Camión " + camion.getCodigo() +
                        " asignado con " + camion.getPedidosAsignados().size() +
                        " pedidos lejanos");
            }
        }
    }

    /**
     * 🆕 Cuenta camiones que fueron reasignados
     */
    private int contarCamionesReasignados() {
        return (int) camiones.stream()
                .filter(c -> c.getPedidosAsignados() != null)
                .filter(c -> c.getPedidosAsignados().stream()
                        .anyMatch(p -> p.getIdCliente().startsWith("c-critico")))
                .count();
    }

    private void gestionarPedidos(@RequestParam(required = false) int anio, @RequestParam(required = false) int mes, @RequestParam(required = false) double timer, @RequestParam(required = false) int minutosPorIteracion, int dia, int hora, int minuto) {
        List<Integer> dias = new ArrayList<>();
        dias.add(dia);
        pedidos = pedidoService.obtenerPedidosPorFecha(dias, anio, mes);
        pedidosSimulacion = pedidoService.dividirPedidos(pedidos, 1);
        aco.simulacionRuteo(
                anio,
                mes,
                dia,
                hora,
                minuto,
                minutosPorIteracion,
                pedidosSimulacion,
                new ArrayList<>(),
                pedidos,
                primeraEjecucionDia,
                timer,
                1
        );
        for (Camion vehiculo : camiones) {
            if (!vehiculo.getRoute().isEmpty()) {
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

    @PostMapping("/semanal")
    @ResponseBody
    public ResponseEntity<List<Camion>> ejecutarSemanal(
            @RequestParam(required = false) int anio,
            @RequestParam(required = false) int mes,
            @RequestParam(required = false) double timer,
            @RequestParam(required = false) int minutosPorIteracion,
            @RequestBody(required = false) List<Averia> averias
    ) {
        System.out.println("Ejecutando semanal con timer: " + timer);

        if (averias == null) averias = new ArrayList<>();

        int dia = ((int) timer / 1440);
        int hora = (((int) timer % 1440) / 60);
        int minuto = ((int) timer % 60);

        int mesAjustado = mes;
        int anioAjustado = anio;
        int diaAjustado = dia;

        while (diaAjustado > getDaysInMonth(anioAjustado, mesAjustado)) {
            diaAjustado -= getDaysInMonth(anioAjustado, mesAjustado);
            mesAjustado++;
            if (mesAjustado > 12) {
                mesAjustado = 1;
                anioAjustado++;
            }
        }

        int turnoActual = calcularTurnoActual(hora);
        List<Averia> averiasProgramadas = averiaProgramadaService.generarAveriasProbabilisticas(
                aco.getId(),
                aco.getFechaBaseSimulacion(),
                turnoActual,
                camiones,
                timer
        );
        List<Averia> todasLasAverias = new ArrayList<>(averias);
        todasLasAverias.addAll(averiasProgramadas);
        System.out.println("Averias a generar: " + todasLasAverias.size());
        for (Averia averia : todasLasAverias) {
            System.out.println("Averia: " + averia.getCodigoCamion() + " - Tipo: " + averia.getTipoAveria() + " - Turno: " + averia.getTurnoAveria());
        }

        gestionarAverias(todasLasAverias, averias, timer);

        if (primeraEjecucionSemanal == 0) {
            pedidos = simulacionController.getPedidos();
            pedidosSimulacion = pedidoService.dividirPedidos(pedidos, 2);
            List<Bloqueo> bloqueos = simulacionController.getBloqueos();
            aco.simulacionRuteo(
                    anioAjustado,
                    mesAjustado,
                    diaAjustado,
                    hora,
                    minuto,
                    minutosPorIteracion,
                    pedidosSimulacion,
                    bloqueos,
                    pedidos,
                    primeraEjecucionSemanal,
                    timer,
                    2
            );
        } else {
            List<Bloqueo> bloqueos = simulacionController.getBloqueos();
            aco.simulacionRuteo(anioAjustado, mesAjustado, diaAjustado, hora, minuto, minutosPorIteracion,
                    pedidosSimulacion, bloqueos, pedidos, primeraEjecucionSemanal, timer, 2);
        }

        primeraEjecucionSemanal = 1;
        return ResponseEntity.ok(aco.getCamiones());

    }

    private int getDaysInMonth(int year, int month) {
        return java.time.YearMonth.of(year, month).lengthOfMonth();
    }

    @GetMapping("/bloqueos")
    @ResponseBody
    public ResponseEntity<List<Bloqueo>> obtenerBloqeos() {
        return ResponseEntity.ok(aco.getBloqueosActivos());
    }

    @GetMapping("/pedidos")
    @ResponseBody
    public ResponseEntity<List<Pedido>> obtenerPedidosCompletos() {
        int i = 0;
        for (Pedido pedido : aco.getPedidosCompletos()) {
            if (pedido.isEntregado()) {
                i++;
            }
        }
        System.out.println("Pedidos entregados: " + i + "/" + aco.getPedidosCompletos().size());
        return ResponseEntity.ok(aco.getPedidosCompletos());
    }

    @GetMapping("/almacen")
    @ResponseBody
    public ResponseEntity<Object> obtenerAlmacenes() {
        if (this.aco == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El algoritmo ACO no ha sido inicializado. Llama primero a /api/aco/inicializar.");
        }
        ArrayList<Almacen> almacenes = new ArrayList<>(aco.getMapa().getAlmacenes());
        if (aco.getSIM().equals("DIA")) {
            almacenes.removeIf(almacen -> almacen.getId() == 2 || almacen.getId() == 3);
        } else {
            almacenes.removeIf(almacen -> almacen.getId() == 1);
        }

        return ResponseEntity.ok(almacenes);
    }

    @PostMapping("/reiniciar")
    @ResponseBody
    public ResponseEntity<Object> reiniciar() {
        try {
            limpiarCamiones();
            limpiarPedidos();
            this.pedidos = new ArrayList<>();
            this.pedidosSimulacion = new ArrayList<>();
            this.aco = null;
            this.primeraEjecucionDia = primeraEjecucionSemanal = 0;

            Map<String, Object> response = Map.of(
                    "mensaje", "Reset completado",
                    "timestamp", LocalDateTime.now()
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

    private void limpiarCamiones() {
        if (camiones == null || camiones.isEmpty()) return;
        camiones.forEach(Camion::reiniciar);
    }

    private void limpiarPedidos() {
        if (pedidos != null && !pedidos.isEmpty()) {
            pedidoService.reiniciarPedidos(pedidos);

            // Actualizar objetos en memoria también
            pedidos.forEach(pedido -> {
                pedido.setEntregado(false);
                pedido.setEntregadoCompleto(false);
            });
        }
    }

    public void gestionarAverias(List<Averia> averias, List<Averia> averiasManuales, double momento) {
        for (Averia averia : averiasManuales) {
            // Para registrar averias desde front
            if (aco != null && aco.getFechaBaseSimulacion() != null) {
                averiaProgramadaService.guardarAveriaManual(
                        aco.getId(),
                        aco.getFechaBaseSimulacion(),
                        averia.getCodigoCamion(),
                        averia.getTipoAveria(),
                        averia.getTurnoAveria(),
                        "Avería manual desde plataforma - " + averia.getTipoAveria() +
                                " (" + averia.getDescripcion() + ")",
                        momento
                );
            }
        }

        for (Averia averia : averias) {
            for (Camion vehiculo : camiones) {
                if (averia.getCodigoCamion().equals(vehiculo.getCodigo())) {
                    int dia = ((int) momento / 1440);
                    vehiculo.setEnAveria(true);
                    vehiculo.setTiempoInicioAveria(momento);
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

    private int calcularTurnoActual(int hora) {
        if (hora >= 0 && hora < 8) {
            return 1;
        } else if (hora >= 8 && hora < 16) {
            return 2;
        } else {
            return 3;
        }
    }

    @GetMapping("/averias-generadas")
    @ResponseBody
    public ResponseEntity<List<AveriaGenerada>> obtenerAveriasGeneradas(
            @RequestParam(required = false) Long simulacionId
    ) {
        try {
            List<AveriaGenerada> averias = averiaProgramadaService.obtenerAveriasPorSimulacion(simulacionId);
            return ResponseEntity.ok(averias);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ArrayList<>());
        }
    }

}

