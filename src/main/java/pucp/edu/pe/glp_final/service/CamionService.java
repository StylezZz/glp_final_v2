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

    public Camion findByVehiculo(String Codigo) {
        return vehiculoRepository.findByCodigo(Codigo);
    }

}
