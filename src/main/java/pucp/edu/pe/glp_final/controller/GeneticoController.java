package pucp.edu.pe.glp_final.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        gestionarPedidos(anioAjustado, mesAjustado, timer, minutosPorIteracion, diaAjustado, hora, minuto);
        primeraEjecucionDia = 1;
        return ResponseEntity.ok(aco.getCamiones());

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
        System.out.println("\n##########################################");
        System.out.println("### /SEMANAL ENDPOINT CALLED ###");
        System.out.println("##########################################");
        System.out.println("Parámetros recibidos:");
        System.out.println("  - Año: " + anio);
        System.out.println("  - Mes: " + mes);
        System.out.println("  - Timer: " + timer);
        System.out.println("  - Minutos por iteración: " + minutosPorIteracion);
        System.out.println("  - Averías: " + (averias != null ? averias.size() : "null"));
        System.out.println("  - primeraEjecucionSemanal: " + primeraEjecucionSemanal);

        if (averias == null) averias = new ArrayList<>();

        int dia = ((int) timer / 1440);
        int hora = (((int) timer % 1440) / 60);
        int minuto = ((int) timer % 60);

        System.out.println("Cálculos de tiempo:");
        System.out.println("  - Día calculado: " + dia);
        System.out.println("  - Hora calculada: " + hora);
        System.out.println("  - Minuto calculado: " + minuto);

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

        System.out.println("Fecha ajustada: " + anioAjustado + "-" + mesAjustado + "-" + diaAjustado);

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
        List<Bloqueo> bloqueos = simulacionController.getBloqueos();

        // Log current pedidos state BEFORE getting new ones
        System.out.println("=== ESTADO ANTES DE OBTENER NUEVOS PEDIDOS ===");
        System.out.println("Pedidos actuales en memoria: " + pedidos.size());
        for (Pedido p : pedidos) {
            System.out.println("  - Pedido " + p.getId() + " en " + p.getPosX() + "," + p.getPosY() +
                    " - EntregadoCompleto: " + p.isEntregadoCompleto() + " - Entregado: " + p.isEntregado());
        }

        if (primeraEjecucionSemanal == 0) {
            System.out.println("*** PRIMERA EJECUCIÓN SEMANAL ***");
            pedidos = simulacionController.getPedidosFrescos(anioAjustado, mesAjustado, diaAjustado, hora, minuto);
            pedidosSimulacion = pedidoService.dividirPedidos(pedidos, 2);
            System.out.println("Pedidos obtenidos del simulador: " + pedidos.size());
            System.out.println("Pedidos para simulación: " + pedidosSimulacion.size());
        } else {
            System.out.println("*** EJECUCIÓN SEMANAL POSTERIOR ***");
            List<Pedido> pedidosActualizados = simulacionController.getPedidosFrescos(anioAjustado, mesAjustado, diaAjustado, hora, minuto);
            System.out.println("Pedidos actualizados del simulador: " + pedidosActualizados.size());

            // Log ALL orders from simulation controller
            System.out.println("=== TODOS LOS PEDIDOS DEL SIMULADOR ===");
            for (Pedido p : pedidosActualizados) {
                System.out.println("  - Pedido " + p.getId() + " en " + p.getPosX() + "," + p.getPosY() +
                        " - EntregadoCompleto: " + p.isEntregadoCompleto() + " - Entregado: " + p.isEntregado() +
                        " - Asignado: " + p.isAsignado() + " - FechaEntrega: " + p.getFechaEntrega());
            }

            // Show sample of fresh orders
            System.out.println("=== SAMPLE OF FRESH ORDERS ===");
            LocalDateTime tiempoSimulacion = LocalDateTime.of(anioAjustado, mesAjustado, diaAjustado, hora, minuto);

            pedidosActualizados.stream()
                    .sorted((p1, p2) -> Integer.compare(p2.getId(), p1.getId())) // Latest first
                    .limit(10)
                    .forEach(p -> {
                        Duration tiempoRestante = Duration.between(tiempoSimulacion, p.getFechaEntrega());
                        System.out.println("  ORDER: ID=" + p.getId() +
                                ", Pos=" + p.getPosX() + "," + p.getPosY() +
                                ", EntregadoCompleto=" + p.isEntregadoCompleto() +
                                ", TiempoRestante=" + tiempoRestante.toMinutes() + "min" +
                                ", Urgente=" + (tiempoRestante.toHours() < 4));
                    });

//            this.pedidos = pedidosActualizados;

            // Show urgent orders count
            long urgentCount = pedidos.stream()
                    .filter(p -> {
                        Duration tiempoRestante = Duration.between(tiempoSimulacion, p.getFechaEntrega());
                        return tiempoRestante.toHours() < 4;
                    })
                    .count();

            System.out.println("*** URGENT ORDERS DETECTED: " + urgentCount + " ***");


            System.out.println("Pedidos filtrados (no entregados): " + pedidos.size());
            System.out.println("=== PEDIDOS FILTRADOS ===");
            for (Pedido p : pedidos) {
                System.out.println("  - Pedido " + p.getId() + " en " + p.getPosX() + "," + p.getPosY() +
                        " - EntregadoCompleto: " + p.isEntregadoCompleto() + " - Entregado: " + p.isEntregado() +
                        " - Asignado: " + p.isAsignado() + " - FechaEntrega: " + p.getFechaEntrega());
            }

            pedidosSimulacion = pedidoService.dividirPedidos(pedidos,2);
            System.out.println("Pedidos divididos para simulación: " + pedidosSimulacion.size());
        }

        System.out.println("=== ANTES DE LLAMAR A SIMULACION RUTEO ===");
        System.out.println("Parámetros para simulacionRuteo:");
        System.out.println("  - anioAjustado: " + anioAjustado);
        System.out.println("  - mesAjustado: " + mesAjustado);
        System.out.println("  - diaAjustado: " + diaAjustado);
        System.out.println("  - hora: " + hora);
        System.out.println("  - minuto: " + minuto);
        System.out.println("  - minutosPorIteracion: " + minutosPorIteracion);
        System.out.println("  - pedidosSimulacion.size(): " + pedidosSimulacion.size());
        System.out.println("  - bloqueos.size(): " + bloqueos.size());
        System.out.println("  - pedidos.size(): " + pedidos.size());
        System.out.println("  - primeraEjecucionSemanal: " + primeraEjecucionSemanal);
        System.out.println("  - timer: " + timer);
        System.out.println("  - tipoSimulacion: 2");

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

        System.out.println("=== DESPUÉS DE SIMULACION RUTEO ===");
        // Log final truck states
        for (Camion camion : aco.getCamiones()) {
            System.out.println("Camión " + camion.getCodigo() + " resultado:");
            System.out.println("  - Pedidos asignados: " + (camion.getPedidosAsignados() != null ?
                    camion.getPedidosAsignados().size() : "null"));
            if (camion.getPedidosAsignados() != null) {
                for (Pedido p : camion.getPedidosAsignados()) {
                    System.out.println("    * Pedido " + p.getId() + " en " + p.getPosX() + "," + p.getPosY());
                }
            }
            System.out.println("  - Ruta: " + (camion.getRoute() != null ? camion.getRoute().size() + " nodos" : "null"));
            if (camion.getRoute() != null && !camion.getRoute().isEmpty()) {
                System.out.println("  - Destinos en ruta:");
                for (NodoMapa nodo : camion.getRoute()) {
                    if (nodo.isEsPedido()) {
                        System.out.println("    -> Pedido en " + nodo.getX() + "," + nodo.getY());
                    } else if (nodo.isEsAlmacen()) {
                        System.out.println("    -> Almacén en " + nodo.getX() + "," + nodo.getY());
                    } else {
                        System.out.println("    -> Punto en " + nodo.getX() + "," + nodo.getY());
                    }
                }
            }
        }

        primeraEjecucionSemanal = 1;

        System.out.println("##########################################");
        System.out.println("### /SEMANAL ENDPOINT COMPLETED ###");
        System.out.println("##########################################\n");

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

