package pucp.edu.pe.glp_final.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import pucp.edu.pe.glp_final.models.Camion;
import pucp.edu.pe.glp_final.service.CamionService;


@RestController
@RequestMapping("/api")
public class CamionController {

    @Autowired
    private CamionService vehiculoService;

    // Obtiene la lista completa de camiones registrados
    @GetMapping("/camiones")
    @ResponseBody
    public ResponseEntity<Object> getVehiculos() {
        try {
            List<Camion> camiones = vehiculoService.findAll();
            Map<String, Object> response = new HashMap<>();

            if (camiones.isEmpty()) {
                // Caso sin vehículos registrados
                response.put("mensaje", "No existen camiones cargados en la base de datos");
                response.put("camiones", new ArrayList<>());
            } else {
                // Retornar flota disponible con estadísticas
                response.put("mensaje", "Se encontraron " + camiones.size() + " camiones");
                response.put("camiones", camiones);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Manejo de errores de base de datos
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Error al obtener los camiones");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Registra un nuevo camión en la base de datos
    @PostMapping("/camiones")
    @ResponseBody
    public Camion save(@RequestBody Camion vehiculo) {
        return vehiculoService.save(vehiculo);
    }

    // Elimina todos los camiones registrados
    @DeleteMapping("/camiones")
    @ResponseBody
    public ResponseEntity<Object> deleteAll() {
        try {
            vehiculoService.deleteAll();
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Se eliminaron todos los camiones correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Manejo de errores durante eliminación masiva
            Map<String, String> response = new HashMap<>();
            response.put("error", "Error al eliminar los camiones: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Carga una flota predefinida de camiones
    @PostMapping("/camiones/cargar-archivo")
    @ResponseBody
    public ResponseEntity<Object> cargarCamionesHardcodeados() {
        try {
            List<Camion> vehiculos = new ArrayList<>();

            // Vehículos tipo TD - Distribución urbana pequeña
            // Ideales para entregas rápidas en zonas de alta densidad
            for (int i = 0; i < 10; i++) {
                Camion vehiculo = new Camion("TD", i + 1, 1, 5, 2.5, 3.5, 25, 50);
                vehiculos.add(vehiculo);
            }
            // Vehículos tipo TC - Distribución urbana mediana
            // Balance entre capacidad y maniobrabilidad para rutas mixtas
            for (int i = 0; i < 4; i++) {
                Camion vehiculo = new Camion("TC", i + 1, 1.5, 10, 5, 6.5, 25, 50);
                vehiculos.add(vehiculo);
            }
            // Vehículos tipo TB - Distribución de alto volumen
            // Para pedidos grandes y rutas con múltiples entregas consolidadas
            for (int i = 0; i < 4; i++) {
                Camion vehiculo = new Camion("TB", i + 1, 2, 15, 7.5, 9.5, 25, 50);
                vehiculos.add(vehiculo);
            }
            // Vehículos tipo TA - Distribución masiva
            // Para cargas especiales y rutas de larga distancia con alta capacidad
            for (int i = 0; i < 2; i++) {
                Camion vehiculo = new Camion("TA", i + 1, 2.5, 25, 12.5, 15, 25, 50);
                vehiculos.add(vehiculo);
            }

            // Persistir flota completa en base de datos
            List<Camion> camionesGuardados = vehiculoService.saveAll(vehiculos);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Se cargaron " + camionesGuardados.size() + " camiones en la base de datos");
            response.put("camiones", camionesGuardados);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Manejo de errores durante carga masiva
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Error al cargar camiones");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Busca un camión específico por su código identificador
    @GetMapping("/camiones/{codigo}")
    @ResponseBody
    public Camion findByCodigo(@PathVariable String codigo){
        return vehiculoService.findByVehiculo(codigo);
    }
}
