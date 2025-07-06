package pucp.edu.pe.glp_final.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;
import pucp.edu.pe.glp_final.models.Bloqueo;
import pucp.edu.pe.glp_final.service.BloqueoService;
import pucp.edu.pe.glp_final.service.FileStorageService;

@RestController
@RequestMapping("/api/bloqueos")
@Getter
@Setter
public class BloqueoController {

    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private BloqueoService bloqueoService;

    @PostMapping(value = "/", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<List<Bloqueo>> cargarBloqueos(@RequestParam("file") MultipartFile file) {
        try {
            // Guardamos el archivo
            fileStorageService.saveFile(file);

            // Leemos los bloqueos del archivo y los retornamos directamente
            List<Bloqueo> bloqueos = Bloqueo.leerArchivoBloqueo(file.getOriginalFilename());
            return ResponseEntity.ok(bloqueos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getBloqueoById(@PathVariable int id) {
        try {
            List<Bloqueo> todosLosBloqueos = new ArrayList<>();
            List<String> archivosBloqueos = Bloqueo.obtenerNombresDeArchivosDeBloqueos();

            // Buscamos en todos los archivos
            for (String nombreArchivo : archivosBloqueos) {
                List<Bloqueo> bloqueosArchivo = Bloqueo.leerArchivoBloqueo(nombreArchivo);
                todosLosBloqueos.addAll(bloqueosArchivo);
            }

            // Buscamos el bloqueo con el ID especificado
            Bloqueo bloqueoEncontrado = todosLosBloqueos.stream()
                    .filter(b -> b.getId() == id)
                    .findFirst()
                    .orElse(null);

            if (bloqueoEncontrado != null) {
                return ResponseEntity.ok(bloqueoEncontrado);
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("mensaje", "No se encontró un bloqueo con el ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Error al buscar el bloqueo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/nombre-bloqueos-archivos")
    @ResponseBody
    public ResponseEntity<?> obtenerNombreBloqueosArchivo() {
        Map<String, Object> response = new HashMap<>();
        List<String> nombres = bloqueoService.obtenerNombresArchivosSubidos();
        response.put("Mensaje", "Nombres de archivos");
        response.put("nombresBloqueos", nombres);
        return ResponseEntity.ok(response);
    }

    //Lectura de archivos de bloqueos
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
