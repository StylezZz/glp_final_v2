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
@RequestMapping("/api/camiones")
public class CamionController {

    @Autowired
    private CamionService vehiculoService;

    // Camiones cargados desde Base de datos
    @GetMapping("/")
    @ResponseBody
    public ResponseEntity<Object> getVehiculos() {
        try {
            List<Camion> camiones = vehiculoService.findAll();
            Map<String, Object> response = new HashMap<>();

            if (camiones.isEmpty()) {
                response.put("mensaje", "No existen camiones cargados en la base de datos");
                response.put("camiones", new ArrayList<>());
            } else {
                response.put("mensaje", "Se encontraron " + camiones.size() + " camiones");
                response.put("camiones", camiones);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Error al obtener los camiones");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/")
    @ResponseBody
    public Camion save(@RequestBody Camion vehiculo) {
        return vehiculoService.save(vehiculo);
    }

    @DeleteMapping("/")
    @ResponseBody
    public ResponseEntity<Object> deleteAll() {
        try {
            vehiculoService.deleteAll();
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Se eliminaron todos los camiones correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Error al eliminar los camiones: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Cargar camiones hardcodeados y guardar en BD
    @PostMapping("/cargar-archivo")
    @ResponseBody
    public ResponseEntity<Object> cargarCamionesHardcodeados() {
        try {
            List<Camion> vehiculos = new ArrayList<>();

            // Vehículos tipo TD
            for (int i = 0; i < 10; i++) {
                Camion vehiculo = new Camion("TD", i + 1, 1, 5, 2.5, 3.5, 25, 50);
                vehiculos.add(vehiculo);
            }
            // Vehículos tipo TC
            for (int i = 0; i < 4; i++) {
                Camion vehiculo = new Camion("TC", i + 1, 1.5, 10, 5, 6.5, 25, 50);
                vehiculos.add(vehiculo);
            }
            // Vehículos tipo TB
            for (int i = 0; i < 4; i++) {
                Camion vehiculo = new Camion("TB", i + 1, 2, 15, 7.5, 9.5, 25, 50);
                vehiculos.add(vehiculo);
            }
            // Vehículos tipo TA
            for (int i = 0; i < 2; i++) {
                Camion vehiculo = new Camion("TA", i + 1, 2.5, 25, 12.5, 15, 25, 50);
                vehiculos.add(vehiculo);
            }

            // Guardar todos los camiones en la base de datos
            List<Camion> camionesGuardados = vehiculoService.saveAll(vehiculos);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Se cargaron " + camionesGuardados.size() + " camiones en la base de datos");
            response.put("camiones", camionesGuardados);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Error al cargar camiones");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{codigo}")
    @ResponseBody
    public Camion findByCodigo(@PathVariable String codigo){
        return vehiculoService.findByVehiculo(codigo);
    }
}
