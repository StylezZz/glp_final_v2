package pucp.edu.pe.glp_final.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;
import pucp.edu.pe.glp_final.models.Pedido;
import pucp.edu.pe.glp_final.service.PedidoService;

@RestController
@RequestMapping("/api/pedidos")
@Getter
@Setter
public class PedidoController {

    private List<Pedido> pedidos;

    @Autowired
    private PedidoService pedidoService;


    @GetMapping
    @ResponseBody
    public ResponseEntity<Object> findAll(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mesPedido,
            @RequestParam(required = false) List<Integer> dias
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Pedido> pedidos = (dias == null || anio == null || mesPedido == null)
                    ? pedidoService.obtenerTodos()
                    : pedidoService.obtenerPedidosPorFecha(dias, anio, mesPedido);

            String mensaje = pedidos.isEmpty()
                    ? "Pedidos no encontrados para la fecha buscada"
                    : "Para la fecha se encontraron " + pedidos.size() + " pedidos";

            response.put("mensaje", mensaje);
            response.put("pedidos", pedidos);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("mensaje", "Error en la consulta de pedidos");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    @ResponseBody
    public Pedido findById(@PathVariable int id) {
        return pedidoService.obtenerPorId(id);
    }

    @PostMapping(value = "/cargar-archivo", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<Object> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            List<Pedido> pedidos = pedidoService.savePedidosArchivo(file);
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Se subió el archivo y se procesaron " + pedidos.size() + " pedidos correctamente");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Error al procesar el archivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(response);
        }
    }

    @GetMapping("/meses")
    @ResponseBody
    public ResponseEntity<?> obtenerMesesCargados() {
        Map<String, Object> response = new HashMap<>();
        List<String> nombres = pedidoService.getMesesPedido();
        response.put("Mensaje", "Nombres de archivos");
        response.put("nombresPedidos", nombres);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @ResponseBody
    public Pedido save(@RequestBody Pedido pedido) {

        pedido.setHoraDeInicio(pedido.getDia() * 1440 + pedido.getHora() * 60 + pedido.getMinuto());
        pedido.setTiempoLlegada(pedido.getHoraDeInicio() + pedido.getHorasLimite() * 60);
        pedido.setFechaDeRegistro(
                LocalDateTime.of(pedido.getAnio(),
                        pedido.getMesPedido(),
                        pedido.getDia(),
                        pedido.getHora(),
                        pedido.getMinuto())
        );
        LocalDateTime fechaEntrega = pedido.getFechaDeRegistro().plusHours(pedido.getHorasLimite());
        pedido.setFechaEntrega(fechaEntrega);
        pedido.setFecDia(LocalDateTime.now());

        return pedidoService.guardar(pedido);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public void deleteById(@PathVariable int id) {
        pedidoService.deleteById(id);
    }

    @PutMapping("/{id}")
    @ResponseBody
    public Pedido update(@RequestBody Pedido pedido) {
        return pedidoService.guardar(pedido);
    }

    @DeleteMapping
    @ResponseBody
    public ResponseEntity<Object> deleteAll() {
        try {
            pedidoService.deleteAll();
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Se eliminaron todos los pedidos correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Error al eliminar los pedidos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
