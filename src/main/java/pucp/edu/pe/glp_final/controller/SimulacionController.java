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
@RestController
@RequestMapping("/api/simulacion")
public class SimulacionController {

    private List<Bloqueo> bloqueos;
    private List<Pedido> pedidos;

    @Autowired
    private SimulacionService simulacionService;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private BloqueoService bloqueoService;

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
        List<Pedido> pedidosTotales = pedidoService.obtenerTodos();
        this.pedidos = pedidoService.getPedidosSemana(pedidosTotales, dia, mes, anio, hora, minuto);
        List<Pedido> pedidosOrdenados = this.pedidos.stream()
                .sorted((p1, p2) -> Integer.compare(p1.getId(), p2.getId()))
                .toList();
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
