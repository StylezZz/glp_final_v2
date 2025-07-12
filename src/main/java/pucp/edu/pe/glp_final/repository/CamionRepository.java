package pucp.edu.pe.glp_final.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pucp.edu.pe.glp_final.models.Camion;


public interface CamionRepository extends JpaRepository<Camion, String> {
    Camion findByCodigo(String Codigo);
}
