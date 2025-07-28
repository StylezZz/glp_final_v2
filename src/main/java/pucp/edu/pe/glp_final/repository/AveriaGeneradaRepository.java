package pucp.edu.pe.glp_final.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pucp.edu.pe.glp_final.models.AveriaGenerada;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AveriaGeneradaRepository extends JpaRepository<AveriaGenerada, Long> {
    List<AveriaGenerada> findBySimulacionIdOrderByMomentoGeneracionDesc(Long simulacionId);
    List<AveriaGenerada> findByFechaBaseSimulacion(LocalDateTime fechaBase);
    List<AveriaGenerada> findByCodigoCamion(String codigoCamion);
}