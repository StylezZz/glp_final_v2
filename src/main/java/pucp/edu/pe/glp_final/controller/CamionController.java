package pucp.edu.pe.glp_final.controller;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import pucp.edu.pe.glp_final.service.CamionService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import pucp.edu.pe.glp_final.models.Camion;


@RestController
@RequestMapping("/api/camiones")
public class CamionController {

    @Autowired
    private CamionService camionService;

    @GetMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllCamion() {
        try {
            List<Camion> camiones = camionService.findAll();
            return ResponseEntity.ok(buildSuccessResponse(camiones));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse(e.getMessage()));
        }
    }

    @PostMapping
    @ResponseBody
    public Camion save(@RequestBody Camion vehiculo) {
        return camionService.save(vehiculo);
    }

    @GetMapping("/{codigo}")
    @ResponseBody
    public Camion findByCodigo(@PathVariable String codigo) {
        return camionService.findCamionByCodigo(codigo);
    }

    private Map<String, Object> buildSuccessResponse(List<Camion> camiones) {
        Map<String, Object> response = new HashMap<>();

        if (camiones.isEmpty()) {
            response.put("mensaje", "No hay vehículos disponibles en el sistema");
            response.put("camiones", Collections.emptyList());
        } else {
            response.put("mensaje", String.format("Consulta exitosa: %d vehículos encontrados",
                    camiones.size()));
            response.put("camiones", camiones);
        }

        return response;
    }

    private Map<String, Object> buildErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Ha ocurrido un problema al consultar los vehículos");
        response.put("error", errorMessage);
        return response;
    }
}
