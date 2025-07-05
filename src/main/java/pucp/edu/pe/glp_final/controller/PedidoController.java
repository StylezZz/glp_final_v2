package pucp.edu.pe.glp_final.controller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import pucp.edu.pe.glp_final.service.FileStorageService;
import pucp.edu.pe.glp_final.service.PedidoService;

@RestController
@RequestMapping("/api")
@Getter
@Setter
public class PedidoController {

    private List<Pedido> pedidos;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private FileStorageService storageService;

    @GetMapping("/pedidos")
    @ResponseBody
    public ResponseEntity<Object> findAll(@RequestParam(required = false) List<Integer> dias,
                                          @RequestParam(required = false) Integer anio, @RequestParam(required = false) Integer mesPedido) {
        List<Pedido> pedidos;
        Map<String, Object> response = new HashMap<>();

        try {
            if (dias != null && anio != null && mesPedido != null) {
                // Si se proporcionan los parámetros, filtra los pedidos
                pedidos = pedidoService.findByDiaInAndAnioMes(dias, anio, mesPedido);
            } else {
                // Si no se proporcionan parámetros, devuelve todos los pedidos
                pedidos = pedidoService.findAll();
            }

            // Ordenar por id antes de responder
            pedidos = pedidos.stream()
                    .sorted((p1, p2) -> Integer.compare(p1.getId(), p2.getId()))
                    .toList();

            if (pedidos.isEmpty()) {
                response.put("mensaje", "No se encontraron pedidos");
                response.put("pedidos", new ArrayList<>());
                return ResponseEntity.ok(response);
            }

            response.put("mensaje", "Se encontraron " + pedidos.size() + " pedidos con éxito");
            response.put("pedidos", pedidos);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("mensaje", "Error al obtener los pedidos");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/pedidos/{id}")
    @ResponseBody
    public Pedido findById(@PathVariable int id) {
        return pedidoService.findById(id);
    }

    //Lectura de archivos de pedidos  SE GUARDA EN BD
    @PostMapping(value = "/pedidos/leer-pedidos", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<Object> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Primero guardamos el archivo
            //storageService.saveFile(file);

            // Procesamos el archivo para contar los pedidos
            //List<Pedido> pedidos = pedidoService.procesarArchivo(file);
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

    @GetMapping("/pedidos/nombre-pedidos-archivos")
    @ResponseBody
    public ResponseEntity<?> obtenerNombrePedidosArchivo(){
        Map<String,Object> response = new HashMap<>();
        List<String> nombres = pedidoService.obtenerNombresArchivosSubidos();
        response.put("Mensaje","Nombres de archivos");
        response.put("nombresPedidos",nombres);
        return ResponseEntity.ok(response);
    }

    //Lectura de archivos de pedidos, donde se guarda en la base de datos
    @PostMapping(value = "/pedidos/upload/diario", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<Object> uploadFileDiario(@RequestParam("files") MultipartFile file) {
        try {
            // Procesar directamente el archivo sin guardarlo
            List<Pedido> pedidos = pedidoService.procesarArchivo(file);

            // Guardar los pedidos en la base de datos
            for (Pedido pedido : pedidos) {
                pedidoService.save(pedido);
            }

            Map<String, String> mapa = new HashMap<String, String>();
            mapa.put("Mensaje", "Se procesaron " + pedidos.size() + " pedidos correctamente");
            return ResponseEntity.status(HttpStatus.OK).body(mapa);
        } catch (Exception e) {
            Map<String, String> mapa = new HashMap<String, String>();
            mapa.put("Error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapa);
        }
    }


    @PostMapping("/pedidos")
    @ResponseBody
    public Pedido save(@RequestBody Pedido pedido) {

        pedido.setHoraDeInicio(pedido.getDia() * 1440 + pedido.getHora() * 60 + pedido.getMinuto());
        pedido.setTiempoLlegada(pedido.getHoraDeInicio() + pedido.getHorasLimite() * 60);
        pedido.setFechaDeRegistro(LocalDateTime.of(pedido.getAnio(), pedido.getMesPedido(), pedido.getDia(),
                pedido.getHora(), pedido.getMinuto()));
        LocalDateTime fechaEntrega = pedido.getFechaDeRegistro().plusHours(pedido.getHorasLimite());
        pedido.setFechaEntrega(fechaEntrega);
        pedido.setFecDia(LocalDateTime.now());

        return pedidoService.save(pedido);
    }

    @DeleteMapping("/pedidos/{id}")
    @ResponseBody
    public void deleteById(@PathVariable int id) {
        pedidoService.deleteById(id);
    }

    @PutMapping("/pedidos/{id}")
    @ResponseBody
    public Pedido update(@RequestBody Pedido pedido) {
        return pedidoService.save(pedido);
    }

    @DeleteMapping("/pedidos")
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

    @GetMapping("/pedidos/count")
    @ResponseBody
    public ResponseEntity<Object> count() {
        Map<String, Object> response = new HashMap<>();
        try {
            long totalPedidos = pedidoService.count();
            response.put("mensaje", "Conteo realizado con éxito");
            response.put("total", totalPedidos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("mensaje", "Error al contar pedidos");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
