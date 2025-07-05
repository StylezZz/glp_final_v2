package pucp.edu.pe.glp_final.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pucp.edu.pe.glp_final.models.Pedido;


public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    //Buscar por una lista de dias y por el atributo anio_mes
    List<Pedido> findByDiaInAndAnioAndMesPedido(List<Integer> dias, Integer anio, Integer mes_pedido);


    List<Pedido> findByfechaDeRegistroBetween(@Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);

    @Query("SELECT DISTINCT CONCAT('ventas',p.anio, LPAD(CAST(p.mesPedido AS string), 2, '0'), '.txt') " +
            "FROM Pedido p")
    List<String> findDistintosArchivosNames();
}
