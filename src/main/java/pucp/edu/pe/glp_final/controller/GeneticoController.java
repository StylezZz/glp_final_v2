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
    private int primeraEjecucionDia = 0;
    private int primeraEjecucionSemanal = 0;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private CamionService camionService;

    @Autowired
    private SimulacionController simulacionController;

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

        gestionarAverias(averias, timer);
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

        gestionarAverias(averias, timer);

        if (primeraEjecucionSemanal == 0) {
            pedidos = simulacionController.getPedidos();
            pedidosSimulacion = pedidoService.dividirPedidos(pedidos, 2);
            List<Bloqueo> bloqueos = simulacionController.getBloqueos();
            aco.simulacionRuteo(anioAjustado, mesAjustado, diaAjustado, hora, minuto, minutosPorIteracion,
                    pedidosSimulacion, bloqueos, pedidos, primeraEjecucionSemanal, timer, 2);
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

}
