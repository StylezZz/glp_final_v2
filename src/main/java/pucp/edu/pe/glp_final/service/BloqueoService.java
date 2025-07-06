package pucp.edu.pe.glp_final.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pucp.edu.pe.glp_final.models.Bloqueo;
import pucp.edu.pe.glp_final.repository.BloqueoRepository;


@Service
public class BloqueoService {

    @Autowired
    private BloqueoRepository bloqueoRepository;

    //sube los archivos y id automaticamente, en teroia xDD
    public List<Bloqueo> saveBloqueoArchivo(MultipartFile file) {
        String nameFile = file.getOriginalFilename();
        String anio = nameFile.substring(0, 4);
        String mes = nameFile.substring(4, 6);
        List<Bloqueo> bloqueos = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String registro;
            while ((registro = reader.readLine()) != null && !registro.isBlank()) {
                Bloqueo bloqueo = Bloqueo.leerBloqueo(registro, Integer.parseInt(anio), Integer.parseInt(mes));
                bloqueos.add(bloqueoRepository.save(bloqueo));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bloqueos;
    }

    public List<Bloqueo> obtenerBloqueosPorAnioMes(int anio, int mes) {
        return bloqueoRepository.findByAnioAndMes(anio, mes);
    }


    public List<Bloqueo> leerArchivoBloqueo(Path file) {
        List<Bloqueo> bloqueos = new ArrayList<>();
        String nombreArchivo = file.getFileName().toString();
        int anio = Integer.parseInt(nombreArchivo.substring(0, 4)); // Extraer los primeros 4 dígitos como año
        int mes = Integer.parseInt(nombreArchivo.substring(4, 6)); // Extraer los siguientes 2 dígitos como mes
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(file)))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                Bloqueo bloqueo = Bloqueo.leerBloqueo(linea, anio, mes);
                bloqueos.add(bloqueo);
            }
        } catch (IOException e) {
            System.out.println("Error al leer el archivo");
        }
        return bloqueos;
    }

    //Dado una Lista de pedidos devolver los pedidos de una semana dada el dia de inicio
    public List<Bloqueo> getBloqueosSemanal(List<Bloqueo> bloqueos, int dia, int mes, int anio, int hora, int minuto) {
        List<Bloqueo> bloqueosSemanal = new ArrayList<>();
        LocalDateTime fechaInicio = LocalDateTime.of(anio, mes, dia, hora, minuto);
        LocalDateTime fechaFin = fechaInicio.plusHours(168);

        for (Bloqueo bloqueo : bloqueos) {
            LocalDateTime fechaBloqueo = bloqueo.getFechaInicio();
            if (!fechaBloqueo.isBefore(fechaInicio) && !fechaBloqueo.isAfter(fechaFin)) {
                bloqueosSemanal.add(bloqueo);
            }
        }
        return bloqueosSemanal;
    }

    public List<String> obtenerNombresArchivosSubidos() {
        return bloqueoRepository.findDistintosArchivosNames();
    }

}
