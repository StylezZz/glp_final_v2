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

@RequestMapping("/api/genetico")
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
    private SimulacionController simulacionController;

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

    @PostMapping("/dia-dia")
    @ResponseBody
    public ResponseEntity<List<Camion>> ejecutarDia(
            @RequestParam(required = false) int anio,
            @RequestParam(required = false) int mes, @RequestParam(required = false) double timer,
            @RequestParam(required = false) int minutosPorIteracion,
            @RequestBody(required = false) List<Averia> averias
    ) {
        if (averias == null) averias = new ArrayList<>();
        int dia = ((int) timer / 1440);
        int hora = (((int) timer % 1440) / 60);
        int minuto = ((int) timer % 60);

        gestionarAverias(averias, timer);
        gestionarPedidos(anio, mes, timer, minutosPorIteracion, dia, hora, minuto);
        primeraVezDiaria = 1;
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
                primeraVezDiaria,
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
        if (averias == null) averias = new ArrayList<>();
        int dia = ((int) timer / 1440);
        int hora = (((int) timer % 1440) / 60);
        int minuto = ((int) timer % 60);

        gestionarAverias(averias, timer);

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
        return ResponseEntity.ok(aco.getMapa().getAlmacenes());
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
            this.primeraVezDiaria = primeraVezSemanal = 0;

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
        for (Camion camion : camiones) {
            if (camion.getRoute() != null) {
                camion.getRoute().clear();
            } else {
                camion.crearRuta();
            }
            camion.setCargaAsignada(0);
            camion.setDistanciaRecorrida(0.0);
            camion.setTiempoViaje(0);
            camion.setUbicacionActual(null);
            camion.setCapacidadCompleta(false);
            camion.setGlpDisponible(camion.getCarga());
            camion.setCargaAnterior(0);
            camion.setEnAveria(false);
            camion.setTipoAveria(0);
            camion.setTiempoInicioAveria(null);
            camion.setTiempoFinAveria(null);
            camion.setDetenido(false);
            camion.setTiempoDetenido(0);

            if (camion.getPedidosAsignados() != null)
                camion.getPedidosAsignados().clear();
            else
                camion.setPedidosAsignados(new ArrayList<>());
        }
    }

    private void limpiarPedidos() {
        if (pedidos != null) {
            for (Pedido pedido : pedidos) {
                pedido.setEntregado(false);
                pedido.setEntregadoCompleto(false);
                pedidoService.guardar(pedido);
            }
        }
    }

    public void gestionarAverias(List<Averia> averias, double momento) {
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

    @GetMapping("/reportes/pedidoDividido")
    @ResponseBody
    public ResponseEntity<List<Pedido>> obtenerPedidosDivididos() {
        return ResponseEntity.ok(aco.getPedidos());
    }

    private String calcularImpactoOperativo() {
        long camionesEnAveria = camiones.stream().filter(Camion::isEnAveria).count();
        if (camionesEnAveria == 0) return "NINGUNO";
        if (camionesEnAveria <= 2) return "BAJO";
        if (camionesEnAveria <= 5) return "MODERADO";
        return "ALTO";
    }
}
