package pucp.edu.pe.glp_final.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pucp.edu.pe.glp_final.models.Bloqueo;

@Repository
public interface BloqueoRepository extends JpaRepository<Bloqueo, Integer> {
    @Query("SELECT DISTINCT CONCAT(b.anio, '', LPAD(CAST(b.mes AS string), 2, '0'), '.bloqueos.txt') FROM Bloqueo b")
    List<String> findDistintosArchivosNames();

    @Query("SELECT DISTINCT b FROM Bloqueo b LEFT JOIN FETCH b.tramo WHERE b.anio = :anio AND b.mes = :mes")
    List<Bloqueo> findByAnioAndMesWithTramos(@Param("anio") int anio, @Param("mes") int mes);
}
