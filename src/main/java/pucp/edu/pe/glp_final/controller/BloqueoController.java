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
@RequestMapping("/api")
@Getter
@Setter
public class BloqueoController {

    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private BloqueoService bloqueoService;

    // Endpoint para cargar bloqueos desde archivo
    @PostMapping(value = "/bloqueos", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<List<Bloqueo>> cargarBloqueos(@RequestParam("file") MultipartFile file) {
        try {
            // Persistir archivo en sistema de archivos para referencia futura
            fileStorageService.saveFile(file);

            // Procesar archivo y extraer datos de bloqueos estructurados
            List<Bloqueo> bloqueos = Bloqueo.leerArchivoBloqueo(file.getOriginalFilename());
            // Retornar bloqueos procesados para confirmación y uso inmediato
            return ResponseEntity.ok(bloqueos);

        } catch (Exception e) {
            // Manejar errores de procesamiento y retornar lista vacía
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }
    // Busca un bloqueo por ID en todos los archivos de bloqueos
    @GetMapping("/bloqueos/{id}")
    public ResponseEntity<Object> getBloqueoById(@PathVariable int id) {
        try {
            List<Bloqueo> todosLosBloqueos = new ArrayList<>();
            // Obtener lista de archivos de bloqueos disponibles
            List<String> archivosBloqueos = Bloqueo.obtenerNombresDeArchivosDeBloqueos();
            // Consolidar bloqueos de todos los archivos cargados
            for (String nombreArchivo : archivosBloqueos) {
                List<Bloqueo> bloqueosArchivo = Bloqueo.leerArchivoBloqueo(nombreArchivo);
                todosLosBloqueos.addAll(bloqueosArchivo);
            }

            // Buscar bloqueo específico por ID usando stream
            Bloqueo bloqueoEncontrado = todosLosBloqueos.stream()
                    .filter(b -> b.getId() == id)
                    .findFirst()
                    .orElse(null);

            if (bloqueoEncontrado != null) {
                return ResponseEntity.ok(bloqueoEncontrado);
            } else {
                // Construir respuesta de error cuando no se encuentra el bloqueo
                Map<String, String> response = new HashMap<>();
                response.put("mensaje", "No se encontró un bloqueo con el ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            // Manejar errores de sistema durante la búsqueda
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Error al buscar el bloqueo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Obtiene la lista de nombres de archivos de bloqueos subidos
    @GetMapping("/bloqueos/nombre-bloqueos-archivos")
    @ResponseBody
    public ResponseEntity<?> obtenerNombreBloqueosArchivo() {
        Map<String, Object> response = new HashMap<>();
        // Obtener lista de archivos de bloqueos desde el servicio
        List<String> nombres = bloqueoService.obtenerNombresArchivosSubidos();
        // Construir respuesta estructurada
        response.put("Mensaje", "Nombres de archivos");
        response.put("nombresBloqueos", nombres);
        return ResponseEntity.ok(response);
    }

     // Endpoint alternativo para lectura y procesamiento de archivos de bloqueos.
    @PostMapping(value = "/bloqueo/leer-bloqueos", consumes = "multipart/form-data")
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
