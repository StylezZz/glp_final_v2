package pucp.edu.pe.glp_final.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pucp.edu.pe.glp_final.repository.CamionRepository;
import pucp.edu.pe.glp_final.models.Camion;

@Service
public class CamionService {

    @Autowired
    private CamionRepository camionRepository;

    public List<Camion> findAll() {
        return camionRepository.findAll();
    }

    public Camion save(Camion vehiculo) {
        return camionRepository.save(vehiculo);
    }

    public Camion findCamionByCodigo(String Codigo) {
        return camionRepository.findByCodigo(Codigo);
    }

}
