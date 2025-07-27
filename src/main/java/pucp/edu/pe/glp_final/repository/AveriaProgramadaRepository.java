package pucp.edu.pe.glp_final.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pucp.edu.pe.glp_final.models.AveriaProgramada;
import java.util.List;

@Repository
public interface AveriaProgramadaRepository extends JpaRepository<AveriaProgramada, Long> {
    List<AveriaProgramada> findByTurnoAveria(Integer turno);
    List<AveriaProgramada> findByCodigoCamion(String codigoCamion);
}

