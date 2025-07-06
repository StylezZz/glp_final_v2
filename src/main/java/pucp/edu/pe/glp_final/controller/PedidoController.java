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
    // Lista para almacenar pedidos
    private List<Pedido> pedidos;

    @Autowired
    private PedidoService pedidoService;
    @Autowired
    private FileStorageService storageService;

    // Consulta pedidos con filtrado por día, año y mes
    @GetMapping("/pedidos")
    @ResponseBody
    public ResponseEntity<Object> findAll(@RequestParam(required = false) List<Integer> dias,
                                          @RequestParam(required = false) Integer anio, @RequestParam(required = false) Integer mesPedido) {
        List<Pedido> pedidos;
        Map<String, Object> response = new HashMap<>();

        try {
            // APLICAR FILTROS TEMPORALES SI SE PROPORCIONAN CRITERIOS
            if (dias != null && anio != null && mesPedido != null) {
                // Consulta filtrada por días específicos, año y mes
                pedidos = pedidoService.findByDiaInAndAnioMes(dias, anio, mesPedido);
            } else {
                // Consulta completa sin filtros - retorna todos los pedidos
                pedidos = pedidoService.findAll();
            }

            // ORDENAMIENTO AUTOMÁTICO POR IDENTIFICADOR PARA CONSISTENCIA
            pedidos = pedidos.stream()
                    .sorted((p1, p2) -> Integer.compare(p1.getId(), p2.getId()))
                    .toList();

            // MANEJO DE RESULTADOS VACÍOS CON RESPUESTA INFORMATIVA
            if (pedidos.isEmpty()) {
                response.put("mensaje", "No se encontraron pedidos");
                response.put("pedidos", new ArrayList<>());
                return ResponseEntity.ok(response);
            }

            // CONSTRUCCIÓN DE RESPUESTA EXITOSA CON METADATA
            response.put("mensaje", "Se encontraron " + pedidos.size() + " pedidos con éxito");
            response.put("pedidos", pedidos);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // MANEJO DE ERRORES CON INFORMACIÓN DETALLADA
            response.put("mensaje", "Error al obtener los pedidos");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    //  Busca y retorna un pedido específico por su identificador único
    @GetMapping("/pedidos/{id}")
    @ResponseBody
    public Pedido findById(@PathVariable int id) {
        return pedidoService.findById(id);
    }

    // Procesa carga masiva de pedidos desde archivo con persistencia completa
    @PostMapping(value = "/pedidos/leer-pedidos", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<Object> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // PROCESAMIENTO INTEGRAL CON PERSISTENCIA COMPLETA
            // Modalidad: Archivo + BD + Registro de trazabilidad
            List<Pedido> pedidos = pedidoService.savePedidosArchivo(file);
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Se subió el archivo y se procesaron " + pedidos.size() + " pedidos correctamente");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            // MANEJO DE ERRORES EN PROCESAMIENTO MASIVO
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Error al procesar el archivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(response);
        }
    }

    // Obtiene los nombres de los archivos de pedidos subidos
    @GetMapping("/pedidos/nombre-pedidos-archivos")
    @ResponseBody
    public ResponseEntity<?> obtenerNombrePedidosArchivo(){
        Map<String,Object> response = new HashMap<>();
        List<String> nombres = pedidoService.obtenerNombresArchivosSubidos();
        response.put("Mensaje","Nombres de archivos");
        response.put("nombresPedidos",nombres);
        return ResponseEntity.ok(response);
    }

    // Procesa carga masiva diaria de pedidos sin almacenamiento de archivo.
    @PostMapping(value = "/pedidos/upload/diario", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<Object> uploadFileDiario(@RequestParam("files") MultipartFile file) {
        try {
            // PROCESAMIENTO EN MEMORIA SIN ALMACENAMIENTO DE ARCHIVO
            List<Pedido> pedidos = pedidoService.procesarArchivo(file);

            // PERSISTENCIA INDIVIDUAL DE PEDIDOS PROCESADOS
            for (Pedido pedido : pedidos) {
                pedidoService.save(pedido);
            }

            Map<String, String> mapa = new HashMap<String, String>();
            mapa.put("Mensaje", "Se procesaron " + pedidos.size() + " pedidos correctamente");
            return ResponseEntity.status(HttpStatus.OK).body(mapa);
        } catch (Exception e) {
            // MANEJO DE ERRORES EN PROCESAMIENTO DIARIO
            Map<String, String> mapa = new HashMap<String, String>();
            mapa.put("Error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapa);
        }
    }

    //  Crea un nuevo pedido con validaciones y cálculos automáticos de campos temporales
    @PostMapping("/pedidos")
    @ResponseBody
    public Pedido save(@RequestBody Pedido pedido) {
        // CÁLCULO DE HORA DE INICIO EN MINUTOS ABSOLUTOS
        // Fórmula: (día × 1440) + (hora × 60) + minutos
        pedido.setHoraDeInicio(pedido.getDia() * 1440 + pedido.getHora() * 60 + pedido.getMinuto());
        // CÁLCULO DE TIEMPO LÍMITE DE LLEGADA
        // Hora inicio + límite de entrega en minutos
        pedido.setTiempoLlegada(pedido.getHoraDeInicio() + pedido.getHorasLimite() * 60);
        // CONSTRUCCIÓN DE FECHA Y HORA COMPLETA DE REGISTRO
        pedido.setFechaDeRegistro(LocalDateTime.of(pedido.getAnio(), pedido.getMesPedido(), pedido.getDia(),
                pedido.getHora(), pedido.getMinuto()));
        // CÁLCULO DE FECHA LÍMITE DE ENTREGA
        LocalDateTime fechaEntrega = pedido.getFechaDeRegistro().plusHours(pedido.getHorasLimite());
        pedido.setFechaEntrega(fechaEntrega);
        // TIMESTAMP DE CREACIÓN DEL REGISTRO
        pedido.setFecDia(LocalDateTime.now());

        return pedidoService.save(pedido);
    }

    // Elimina un pedido específico por su identificador único
    @DeleteMapping("/pedidos/{id}")
    @ResponseBody
    public void deleteById(@PathVariable int id) {
        pedidoService.deleteById(id);
    }

    // Actualiza un pedido existente con validaciones y cálculos automáticos de campos temporales
    @PutMapping("/pedidos/{id}")
    @ResponseBody
    public Pedido update(@RequestBody Pedido pedido) {
        return pedidoService.save(pedido);
    }

    // Elimina todos los pedidos de la base de datos con manejo de errores
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

    // Cuenta el total de pedidos en la base de datos con manejo de errores
    @GetMapping("/pedidos/count")
    @ResponseBody
    public ResponseEntity<Object> count() {
        Map<String, Object> response = new HashMap<>();
        try {
            // CONTEO TOTAL DE PEDIDOS EN SISTEMA
            long totalPedidos = pedidoService.count();
            response.put("mensaje", "Conteo realizado con éxito");
            response.put("total", totalPedidos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // MANEJO DE ERRORES EN OPERACIÓN DE CONTEO
            response.put("mensaje", "Error al contar pedidos");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
