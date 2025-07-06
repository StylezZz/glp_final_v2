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
    // Lista de bloqueos cargados para la simulación actual
    private List<Bloqueo> bloqueos;
    // Lista de pedidos cargados para la simulación actual
    private List<Pedido> pedidos;

    @Autowired
    private SimulacionBloqueoService simulacionService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private BloqueoService bloqueoService;

    // Detiene la simulación de bloqueos y libera los recursos asociados.
    @GetMapping("/simulacion/detener")
    public ResponseEntity<Object> detenerSimulacion() {
        simulacionService.detenerSimulacion();
        Map<String, String> okMap = new HashMap<>();
        okMap.put("mensaje", "Simulacion de Bloqueos Detenida");
        return ResponseEntity.ok(okMap);
    }

    // Obtiene la lista de bloqueos activos en la simulación actual
    @GetMapping("/simulacion/bloqueosActivos")
    public ResponseEntity<List<Bloqueo>> obtenerBloqueosActivos() {
        List<Bloqueo> bloqueosActivos = simulacionService.obtenerBloqueosActivos();
        return ResponseEntity.ok(bloqueosActivos);
    }

    // Inicializa nueva simulación de bloqueos con los parámetros proporcionados
    @PostMapping("/simulacion/inicializar-simulacion")
    public ResponseEntity<Object> iniciarSimulacion(
            @RequestParam("horaInicial") int horaInicial,
            @RequestParam("minutoInicial") int minutoInicial,
            @RequestParam("anio") int anio,
            @RequestParam("mes") int mes,
            @RequestParam("dia") int dia,
            @RequestParam("minutosPorIteracion") int minutosPorIteracion,
            @RequestParam("timerSimulacion") int timerSimulacion) throws IOException {

        // INICIALIZACIÓN DE COLECCIÓN DE BLOQUEOS
        List<Bloqueo> bloqueos = new ArrayList<>();
        Bloqueo prueba = new Bloqueo();
        // OBTENCIÓN DE ARCHIVOS DE BLOQUEOS DISPONIBLES
        List<String> nombresArchivos = prueba.obtenerNombresDeArchivosDeBloqueos();

        // CARGA AUTOMÁTICA DEL PRIMER ARCHIVO DISPONIBLE
        if (!nombresArchivos.isEmpty()) {
            String primerArchivo = nombresArchivos.get(0);
            System.out.println("Leyendo solo el primer archivo de bloqueos: " + primerArchivo);
            bloqueos = prueba.leerArchivoBloqueo(primerArchivo);
        } else {
            System.out.println("No se encontraron archivos de bloqueos");
        }
        // INICIALIZACIÓN DE SIMULACIÓN CON PARÁMETROS CONFIGURADOS
        simulacionService.iniciarSimulacion(bloqueos, horaInicial, minutoInicial, anio, mes, dia, minutosPorIteracion, timerSimulacion);
        // CONSTRUCCIÓN DE RESPUESTA INFORMATIVA
        Map<String, String> okMap = new HashMap<>();
        okMap.put("mensaje", "Simulacion de Bloqueos Inicializada" + (!nombresArchivos.isEmpty() ? " con archivo: " + nombresArchivos.get(0) : " sin archivos"));
        return ResponseEntity.ok(okMap);
    }

    // Establece la velocidad de avance temporal de la simulación activa.
    @PostMapping("/simulacion/establecervelocidad")
    public ResponseEntity<String> establecerVelocidad(@RequestParam int minutosPorIteracion) {
        // APLICACIÓN INMEDIATA DE NUEVA VELOCIDAD TEMPORAL
        simulacionService.establecerVelocidad(minutosPorIteracion);
        return ResponseEntity.ok("Velocidad establecida a " + minutosPorIteracion + " minutos por iteración.");
    }

    // Consulta la velocidad actual de avance temporal de la simulación.
    @GetMapping("/simulacion/obtenervelocidad")
    public ResponseEntity<Integer> obtenerVelocidad() {
        // CONSULTA DE VELOCIDAD ACTUAL CONFIGURADA
        int velocidad = simulacionService.obtenerVelocidad();
        return ResponseEntity.ok(velocidad);
    }

    //  * Obtiene el estado temporal completo actual de la simulación en ejecución.
    @GetMapping("/simulacion/tiempo-actual")
    public ResponseEntity<Map<String, Object>> obtenerTiempoActual() {
        // VALIDACIÓN DE ESTADO DE SIMULACIÓN ACTIVA
        if (simulacionService == null || simulacionService.getSimulacion() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        // CONSTRUCCIÓN DE RESPUESTA CON ESTADO TEMPORAL COMPLETO
        Map<String, Object> tiempoActual = new HashMap<>();
        tiempoActual.put("horaActual", simulacionService.getSimulacion().getHoraActual());
        tiempoActual.put("minutoActual", simulacionService.getSimulacion().getMinutoActual());
        tiempoActual.put("diaActual", simulacionService.getSimulacion().getDiaActual());
        tiempoActual.put("mesActual", simulacionService.getSimulacion().getMesActual());
        tiempoActual.put("anioActual", simulacionService.getSimulacion().getAnioActual());
        tiempoActual.put("timer", simulacionService.getSimulacion().getTimerSimulacion());

        return ResponseEntity.ok(tiempoActual);
    }

    // Obtiene pedidos filtrados por semana según los parámetros de fecha y hora proporcionados.
    @GetMapping("/simulacion/pedidos/semanal")
    @ResponseBody
    public ResponseEntity<List<Pedido>> obtenerPedidosSemanal(
            @RequestParam(required = false) int anio,
            @RequestParam(required = false) int mes,
            @RequestParam(required = false) int dia,
            @RequestParam(required = false) int hora,
            @RequestParam(required = false) int minuto) {

        // CARGA COMPLETA DE PEDIDOS DESDE BASE DE DATOS
        List<Pedido> todosLosPedidos = pedidoService.findAll();

        // APLICACIÓN DE FILTRO SEMANAL CON PARÁMETROS TEMPORALES
        this.pedidos = pedidoService.getPedidosSemana(todosLosPedidos, dia, mes, anio, hora, minuto);

        // ORDENAMIENTO AUTOMÁTICO POR IDENTIFICADOR PARA CONSISTENCIA
        List<Pedido> pedidosOrdenados = this.pedidos.stream()
                .sorted((p1, p2) -> Integer.compare(p1.getId(), p2.getId()))
                .toList();

        return ResponseEntity.ok(pedidosOrdenados);
    }

    // Obtiene bloqueos filtrados por período semanal específico desde archivos
    @GetMapping("/simulacion/bloqueos/semanal")
    @ResponseBody
    public ResponseEntity<List<Bloqueo>> obtenerBloqueosSemanal(
            @RequestParam(required = false) int anio,
            @RequestParam(required = false) int mes,
            @RequestParam(required = false) int dia,
            @RequestParam(required = false) int hora,
            @RequestParam(required = false) int minuto) {
        // RESOLUCIÓN DE ARCHIVO DE BLOQUEOS POR CRITERIOS TEMPORALES
        Path fileBloqueo = fileStorageService.getFileBloqueo(anio, mes);
        // INICIALIZACIÓN DE COLECCIONES TEMPORALES
        List<Bloqueo> bloqueosArchivo = new ArrayList<>();
        bloqueos = new ArrayList<>();
        // CARGA COMPLETA DE BLOQUEOS DESDE ARCHIVO ESPECÍFICO
        bloqueosArchivo = bloqueoService.leerArchivoBloqueo(fileBloqueo);
        // APLICACIÓN DE FILTRO SEMANAL CON PARÁMETROS TEMPORALES
        bloqueos = bloqueoService.getBloqueosSemanal(bloqueosArchivo, dia, mes, anio, hora, minuto);

        return ResponseEntity.ok(this.bloqueos);
    }

    // Obtiene pedidos filtrados por día específico según los parámetros de fecha y hora proporcionados.
    @GetMapping("/simulacion/pedidos/dia-dia")
    public ResponseEntity<List<Pedido>> obtenerPedidosDiario(
            @RequestParam(required = false) int anio,
            @RequestParam(required = false) int mes,
            @RequestParam(required = false) int dia,
            @RequestParam(required = false) int hora,
            @RequestParam(required = false) int minuto) {
        // CONSTRUCCIÓN DE VENTANA TEMPORAL DE 24 HORAS
        LocalDateTime fechaInicio = LocalDateTime.of(anio, mes, dia, hora, minuto);
        LocalDateTime fechaFin = fechaInicio.plusHours(24);
        // INICIALIZACIÓN DE COLECCIÓN DE PEDIDOS
        this.pedidos = new ArrayList<>();
        // CONSULTA DIRECTA CON RANGO TEMPORAL ESPECÍFICO
        this.pedidos = pedidoService.findByFechaPedidoBetween(fechaInicio, fechaFin);

        return ResponseEntity.ok(this.pedidos);
    }
}
