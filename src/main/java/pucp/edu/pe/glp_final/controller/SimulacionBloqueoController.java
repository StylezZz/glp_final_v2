package pucp.edu.pe.glp_final.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


import lombok.Getter;
import lombok.Setter;
import pucp.edu.pe.glp_final.models.Bloqueo;
import pucp.edu.pe.glp_final.models.Pedido;
import pucp.edu.pe.glp_final.service.BloqueoService;
import pucp.edu.pe.glp_final.service.FileStorageService;
import pucp.edu.pe.glp_final.service.PedidoService;
import pucp.edu.pe.glp_final.service.SimulacionBloqueoService;

@RestController
@RequestMapping("/api")
@Getter
@Setter
public class SimulacionBloqueoController {
    private List<Bloqueo> bloqueos;
    private List<Pedido> pedidos;

    @Autowired
    private SimulacionBloqueoService simulacionService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private BloqueoService bloqueoService;

    @GetMapping("/simulacion/detener")
    public ResponseEntity<Object> detenerSimulacion() {
        simulacionService.detenerSimulacion();
        Map<String, String> okMap = new HashMap<>();
        okMap.put("mensaje", "Simulacion de Bloqueos Detenida");
        return ResponseEntity.ok(okMap);
    }

    // Endpoint para obtener los bloqueos activos en la simulación en el instante de tiempo actual.
    @GetMapping("/simulacion/bloqueosActivos")
    public ResponseEntity<List<Bloqueo>> obtenerBloqueosActivos() {
        List<Bloqueo> bloqueosActivos = simulacionService.obtenerBloqueosActivos();
        return ResponseEntity.ok(bloqueosActivos);
    }

    // Aca se carga los bloqueos
    @PostMapping("/simulacion/inicializar-simulacion")
    public ResponseEntity<Object> iniciarSimulacion(
            @RequestParam("horaInicial") int horaInicial,
            @RequestParam("minutoInicial") int minutoInicial,
            @RequestParam("anio") int anio,
            @RequestParam("mes") int mes,
            @RequestParam("dia") int dia,
            @RequestParam("minutosPorIteracion") int minutosPorIteracion,
            @RequestParam("timerSimulacion") int timerSimulacion) throws IOException {

        List<Bloqueo> bloqueos = new ArrayList<>();
        Bloqueo prueba = new Bloqueo();
        List<String> nombresArchivos = prueba.ObtenerNombresDeArchivosDeBloqueos();

        // Solo leemos el primer archivo si existe alguno
        if (!nombresArchivos.isEmpty()) {
            String primerArchivo = nombresArchivos.get(0);
            System.out.println("Leyendo solo el primer archivo de bloqueos: " + primerArchivo);
            bloqueos = prueba.leerArchivoBloqueo(primerArchivo);
        } else {
            System.out.println("No se encontraron archivos de bloqueos");
        }

        simulacionService.iniciarSimulacion(bloqueos, horaInicial, minutoInicial, anio, mes, dia, minutosPorIteracion, timerSimulacion);
        Map<String, String> okMap = new HashMap<>();
        okMap.put("mensaje", "Simulacion de Bloqueos Inicializada" + (!nombresArchivos.isEmpty() ? " con archivo: " + nombresArchivos.get(0) : " sin archivos"));
        return ResponseEntity.ok(okMap);
    }

    // Endpoint para establecer la velocidad de la simulación
    @PostMapping("/simulacion/establecervelocidad")
    public ResponseEntity<String> establecerVelocidad(@RequestParam int minutosPorIteracion) {
        simulacionService.establecerVelocidad(minutosPorIteracion);
        return ResponseEntity.ok("Velocidad establecida a " + minutosPorIteracion + " minutos por iteración.");
    }

    // Endpoint para obtener la velocidad de la simulación
    @GetMapping("/simulacion/obtenervelocidad")
    public ResponseEntity<Integer> obtenerVelocidad() {
        int velocidad = simulacionService.obtenerVelocidad();
        return ResponseEntity.ok(velocidad);
    }

    @GetMapping("/simulacion/tiempo-actual")
    public ResponseEntity<Map<String, Object>> obtenerTiempoActual() {
        if (simulacionService == null || simulacionService.getSimulacion() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        Map<String, Object> tiempoActual = new HashMap<>();
        tiempoActual.put("horaActual", simulacionService.getSimulacion().getHoraActual());
        tiempoActual.put("minutoActual", simulacionService.getSimulacion().getMinutoActual());
        tiempoActual.put("diaActual", simulacionService.getSimulacion().getDiaActual());
        tiempoActual.put("mesActual", simulacionService.getSimulacion().getMesActual());
        tiempoActual.put("anioActual", simulacionService.getSimulacion().getAnioActual());
        tiempoActual.put("timer", simulacionService.getSimulacion().getTimerSimulacion());

        return ResponseEntity.ok(tiempoActual);
    }

    @GetMapping("/simulacion/pedidos/semanal")
    @ResponseBody
    public ResponseEntity<List<Pedido>> obtenerPedidosSemanal(
            @RequestParam(required = false) int anio,
            @RequestParam(required = false) int mes,
            @RequestParam(required = false) int dia,
            @RequestParam(required = false) int hora,
            @RequestParam(required = false) int minuto) {

        // Obtener todos los pedidos de la BD primero
        List<Pedido> todosLosPedidos = pedidoService.findAll();

        // Usar el método existente para filtrar por semana
        this.pedidos = pedidoService.getPedidosSemana(todosLosPedidos, dia, mes, anio, hora, minuto);

        // Ordenar por id antes de retornar
        List<Pedido> pedidosOrdenados = this.pedidos.stream()
                .sorted((p1, p2) -> Integer.compare(p1.getId(), p2.getId()))
                .toList();

        return ResponseEntity.ok(pedidosOrdenados);
    }

    @GetMapping("/simulacion/bloqueos/semanal")
    @ResponseBody
    public ResponseEntity<List<Bloqueo>> obtenerBloqueosSemanal(
            @RequestParam(required = false) int anio,
            @RequestParam(required = false) int mes,
            @RequestParam(required = false) int dia,
            @RequestParam(required = false) int hora,
            @RequestParam(required = false) int minuto) {

        Path fileBloqueo = fileStorageService.getFileBloqueo(anio, mes);

        List<Bloqueo> bloqueosArchivo = new ArrayList<>();
        bloqueos = new ArrayList<>();

        bloqueosArchivo = bloqueoService.leerArchivoBloqueo(fileBloqueo);
        bloqueos = bloqueoService.getBloqueosSemanal(bloqueosArchivo, dia, mes, anio, hora, minuto);

        return ResponseEntity.ok(this.bloqueos);
    }

    @GetMapping("/simulacion/pedidos/dia-dia")
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
