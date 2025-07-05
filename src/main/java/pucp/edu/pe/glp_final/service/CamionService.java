package pucp.edu.pe.glp_final.service;

import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pucp.edu.pe.glp_final.models.Camion;
import pucp.edu.pe.glp_final.repository.VehiculoRepository;

@Service
public class CamionService {


    @Autowired
    private VehiculoRepository vehiculoRepository;

    public List<Camion> findAll() {
        return vehiculoRepository.findAll();
    }

    public Camion save(Camion vehiculo) {
        return vehiculoRepository.save(vehiculo);
    }

    public List<Camion> saveAll(List<Camion> vehiculos) {
        return vehiculoRepository.saveAll(vehiculos);
    }

    public void deleteAll() {
        vehiculoRepository.deleteAll();
    }

    public Camion findByVehiculo(String Codigo){
        return vehiculoRepository.findByCodigo(Codigo);
    }

    public List<Camion> cargarCamionesDesdeArchivo(String rutaArchivo) {
        List<Camion> vehiculos = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            boolean first = true;
            int count = 0;
            while ((linea = br.readLine()) != null) {
                if (first) { first = false; continue; } // Saltar encabezado
                if (linea.trim().isEmpty()) continue;
                String[] partes = linea.split(",");
                if (partes.length < 7) continue; // Formato incorrecto
                String tipo = partes[0];
                double tara = Double.parseDouble(partes[1]);
                double carga = Double.parseDouble(partes[2]);
                double pesoCarga = Double.parseDouble(partes[3]);
                double peso = Double.parseDouble(partes[4]);
                double velocidad = Double.parseDouble(partes[5]);
                int unidades = Integer.parseInt(partes[6]);
                // Reiniciamos el número para cada tipo de camión
                int numero = 1;
                for (int i = 0; i < unidades; i++) {
                    Camion vehiculo = new Camion(tipo, numero, tara, carga, pesoCarga, peso, 0, velocidad);
                    vehiculos.add(vehiculo);
                    numero++;
                    count++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return vehiculoRepository.saveAll(vehiculos);
    }

    @PostConstruct
    public void cargarCamionesFijosSiNoExisten() {
        if (vehiculoRepository.count() == 0) {
            cargarCamionesDesdeArchivo("src/main/java/com/plg/backend/data/camiones/camiones.txt");
        }
    }

}
