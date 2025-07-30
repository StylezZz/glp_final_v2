package pucp.edu.pe.glp_final.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;

import pucp.edu.pe.glp_final.service.SimulacionService;
import pucp.edu.pe.glp_final.service.BloqueoService;
import pucp.edu.pe.glp_final.service.PedidoService;
import pucp.edu.pe.glp_final.models.Bloqueo;
import pucp.edu.pe.glp_final.models.Pedido;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// SOLUTION 1: Add efficient refresh method to SimulacionController.java

@RestController
@RequestMapping("/api/simulacion")
public class SimulacionController {

    private List<Bloqueo> bloqueos;
    private List<Pedido> pedidos;
    private LocalDateTime ultimaActualizacionPedidos; // Track last update time

    @Autowired
    private SimulacionService simulacionService;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private BloqueoService bloqueoService;

    // ADD: Method to get fresh orders efficiently
    public List<Pedido> getPedidosFrescos(int anio, int mes, int dia, int hora, int minuto) {
        System.out.println("=== GETTING FRESH ORDERS ===");
        System.out.println("Requested time: " + anio + "-" + mes + "-" + dia + " " + hora + ":" + minuto);

        LocalDateTime tiempoActual = LocalDateTime.of(anio, mes, dia, hora, minuto);

        System.out.println("*** REFRESHING ORDERS FROM DATABASE ***");
        System.out.println("Last update: " + ultimaActualizacionPedidos);

        // Get fresh data from database
        List<Pedido> pedidosTotales = pedidoService.obtenerPorSemanaMasMenosUnDia(anio, mes, dia);
        this.pedidos = pedidoService.getPedidosSemana(pedidosTotales, dia, mes, anio, hora, minuto);
        this.ultimaActualizacionPedidos = LocalDateTime.now();

        System.out.println("Refreshed orders count: " + this.pedidos.size());

        // Log recent orders for debugging
        List<Pedido> pedidosRecientes = this.pedidos.stream()
                .sorted((p1, p2) -> Integer.compare(p2.getId(), p1.getId())) // Descending by ID
                .limit(10)
                .toList();

        System.out.println("=== LATEST 10 ORDERS AFTER REFRESH ===");
        for (Pedido p : pedidosRecientes) {
            java.time.Duration tiempoRestante = java.time.Duration.between(tiempoActual, p.getFechaEntrega());
            System.out.println("  FRESH ORDER: ID=" + p.getId() +
                    ", Pos=" + p.getPosX() + "," + p.getPosY() +
                    ", EntregadoCompleto=" + p.isEntregadoCompleto() +
                    ", TiempoRestante=" + tiempoRestante.toMinutes() + "min" +
                    ", Urgente=" + (tiempoRestante.toHours() < 4));
        }


        System.out.println("=== RETURNING ORDERS ===");
        System.out.println("Total orders returned: " + this.pedidos.size());
        return this.pedidos;
    }

    // KEEP existing method for backward compatibility but add refresh
    public List<Pedido> getPedidos() {
        System.out.println("*** WARNING: Using deprecated getPedidos() - should use getPedidosFrescos() ***");

        // If we don't have current time context, return cached or empty
        if (this.pedidos == null) {
            System.out.println("*** NO CACHED ORDERS - RETURNING EMPTY LIST ***");
            return new ArrayList<>();
        }

        System.out.println("*** RETURNING CACHED ORDERS: " + this.pedidos.size() + " ***");
        return this.pedidos.stream()
                .sorted((p1, p2) -> Integer.compare(p1.getId(), p2.getId()))
                .toList();
    }

    // ADD: Force refresh method for testing
    @GetMapping("/refresh-pedidos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> forzarActualizacionPedidos(
            @RequestParam int anio,
            @RequestParam int mes,
            @RequestParam int dia,
            @RequestParam int hora,
            @RequestParam int minuto
    ) {
        System.out.println("*** FORCING ORDERS REFRESH ***");

        // Force refresh by clearing last update time
        this.ultimaActualizacionPedidos = null;

        List<Pedido> pedidosActualizados = getPedidosFrescos(anio, mes, dia, hora, minuto);

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Pedidos actualizados exitosamente");
        response.put("totalPedidos", pedidosActualizados.size());
        response.put("fechaActualizacion", LocalDateTime.now());

        // Show urgent orders count
        LocalDateTime tiempoActual = LocalDateTime.of(anio, mes, dia, hora, minuto);
        long pedidosUrgentes = pedidosActualizados.stream()
                .filter(p -> !p.isEntregadoCompleto())
                .filter(p -> {
                    java.time.Duration tiempoRestante = java.time.Duration.between(tiempoActual, p.getFechaEntrega());
                    return tiempoRestante.toHours() < 4;
                })
                .count();

        response.put("pedidosUrgentes", pedidosUrgentes);

        return ResponseEntity.ok(response);
    }

    // EXISTING METHODS REMAIN THE SAME...
    @GetMapping("/detener")
    public ResponseEntity<Object> detenerSimulacion() {
        simulacionService.parar();
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "La simulación ha sido detenida");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/inicializar-simulacion")
    public ResponseEntity<Object> iniciarSimulacion(
            @RequestParam("anio") int anio,
            @RequestParam("mes") int mes,
            @RequestParam("dia") int dia,
            @RequestParam("horaInicial") int horaInicial,
            @RequestParam("minutoInicial") int minutoInicial,
            @RequestParam("minutosPorIteracion") int minutosPorIteracion,
            @RequestParam("timerSimulacion") int timerSimulacion
    ) {
        simulacionService.empezar(
                horaInicial,
                minutoInicial,
                anio,
                mes,
                dia,
                minutosPorIteracion,
                timerSimulacion
        );
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "La simulación ha empezado");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pedidos/semanal")
    @ResponseBody
    public ResponseEntity<List<Pedido>> obtenerPedidosSemanal(
            @RequestParam(required = false) int anio,
            @RequestParam(required = false) int mes,
            @RequestParam(required = false) int dia,
            @RequestParam(required = false) int hora,
            @RequestParam(required = false) int minuto
    ) {
        // Use the new efficient method
        List<Pedido> pedidosOrdenados = getPedidosFrescos(anio, mes, dia, hora, minuto);
        return ResponseEntity.ok(pedidosOrdenados);
    }

    @GetMapping("/bloqueos/semanal")
    @ResponseBody
    public ResponseEntity<List<Bloqueo>> obtenerBloqueosSemanal(
            @RequestParam(required = false) int anio,
            @RequestParam(required = false) int mes,
            @RequestParam(required = false) int dia,
            @RequestParam(required = false) int hora,
            @RequestParam(required = false) int minuto
    ) {
        List<Bloqueo> bloqueosMes = bloqueoService.obtenerBloqueosPorAnioMes(anio, mes);
        bloqueos = bloqueoService.getBloqueosSemanal(bloqueosMes, dia, mes, anio, hora, minuto);

        return ResponseEntity.ok(this.bloqueos);
    }

    @GetMapping("/pedidos/dia-dia")
    public ResponseEntity<List<Pedido>> obtenerPedidosDiario(
            @RequestParam(required = false) int anio,
            @RequestParam(required = false) int mes,
            @RequestParam(required = false) int dia,
            @RequestParam(required = false) int hora,
            @RequestParam(required = false) int minuto) {

        LocalDateTime fechaInicio = LocalDateTime.of(anio, mes, dia, hora, minuto);
        LocalDateTime fechaFin = fechaInicio.plusHours(24);
        this.pedidos = new ArrayList<>();
        this.pedidos = pedidoService.findByFechaPedidoBetween(fechaInicio, fechaFin);

        return ResponseEntity.ok(this.pedidos);
    }
}