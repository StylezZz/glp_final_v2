package pucp.edu.pe.glp_final.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pucp.edu.pe.glp_final.models.Bloqueo;

@Repository
public interface BloqueoRepository extends JpaRepository<Bloqueo, Integer> {
    @Query("SELECT DISTINCT b FROM Bloqueo b LEFT JOIN FETCH b.tramo WHERE b.anio = :anio AND b.mes = :mes")
    List<Bloqueo> findByAnioAndMesWithTramos(@Param("anio") int anio, @Param("mes") int mes);

    @Query("SELECT DISTINCT CONCAT('Bloqueos de ', CASE b.mes " +
            "WHEN 1 THEN 'enero' " +
            "WHEN 2 THEN 'febrero' " +
            "WHEN 3 THEN 'marzo' " +
            "WHEN 4 THEN 'abril' " +
            "WHEN 5 THEN 'mayo' " +
            "WHEN 6 THEN 'junio' " +
            "WHEN 7 THEN 'julio' " +
            "WHEN 8 THEN 'agosto' " +
            "WHEN 9 THEN 'septiembre' " +
            "WHEN 10 THEN 'octubre' " +
            "WHEN 11 THEN 'noviembre' " +
            "WHEN 12 THEN 'diciembre' " +
            "ELSE 'mes_desconocido' END, ' ', CAST(b.anio AS string))" +
            "FROM Bloqueo b")
    List<String> getMesesBloqueo();
}
