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

    private List<Averia> averias;

    @PostMapping("averia/upload")
    @ResponseBody
    public List<Averia> upload(@RequestParam("file") MultipartFile file) {
        averias = new ArrayList<>();
        averias = averiaService.lecturaArchivo(file);
        return averias;
    }
}
