package pucp.edu.pe.glp_final.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pucp.edu.pe.glp_final.models.Camion;

@Repository
public interface CamionRepository extends JpaRepository<Camion, String> {
    Camion findByCodigo(String Codigo);
}
