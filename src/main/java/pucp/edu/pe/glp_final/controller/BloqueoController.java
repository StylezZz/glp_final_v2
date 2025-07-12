package pucp.edu.pe.glp_final.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import pucp.edu.pe.glp_final.service.BloqueoService;
import pucp.edu.pe.glp_final.models.Bloqueo;
import lombok.Getter;
import lombok.Setter;

@RestController
@RequestMapping("/api/bloqueos")
@Getter
@Setter
public class BloqueoController {

    @Autowired
    private BloqueoService bloqueoService;

    @GetMapping("/meses")
    @ResponseBody
    public ResponseEntity<?> obtenerNombreBloqueosArchivo() {
        Map<String, Object> response = new HashMap<>();
        List<String> nombres = bloqueoService.obtenerNombresArchivosSubidos();
        response.put("Mensaje", "Nombres de archivos");
        response.put("nombresBloqueos", nombres);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/leer-bloqueos", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<Object> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Bloqueo> bloqueos = bloqueoService.saveBloqueoArchivo(file);
            response.put("mensaje", "Se procesaron " + bloqueos.size() + " bloqueos correctamente");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            response.put("mensaje", "Error al procesar el archivo de bloqueos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
