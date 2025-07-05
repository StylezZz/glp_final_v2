package pucp.edu.pe.glp_final.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pucp.edu.pe.glp_final.models.Averia;


@Service
public class AveriaService {

    public List<Averia> lecturaArchivo(MultipartFile file) {

        List<Averia> averias = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                Averia averia = Averia.leerRegistro(linea);
                averias.add(averia);
            }
            return averias;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

}

