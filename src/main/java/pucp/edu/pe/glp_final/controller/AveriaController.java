package pucp.edu.pe.glp_final.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pucp.edu.pe.glp_final.models.Averia;
import pucp.edu.pe.glp_final.service.AveriaService;

@RestController
@RequestMapping("/api")
public class AveriaController {

    @Autowired
    private AveriaService averiaService;
    // Lista de averias que se cargan
    private List<Averia> averias;

    // Endpoint para cargar el archivo de averias
    @PostMapping("averia/upload")
    @ResponseBody
    public List<Averia> upload(@RequestParam("file") MultipartFile file) {
        // Inicializar lista para nueva carga
        averias = new ArrayList<>();
        // Procesar archivo y extraer datos de averías
        averias = averiaService.lecturaArchivo(file);
        // Retornar averías procesadas para confirmación y uso en frontend
        return averias;
    }
}
