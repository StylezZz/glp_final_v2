package pucp.edu.pe.glp_final.repository;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pucp.edu.pe.glp_final.models.Pedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    @Modifying
    @Transactional
    @Query("UPDATE Pedido p SET p.entregado = false, p.entregadoCompleto = false WHERE p.id IN :ids")
    void reiniciarPedidosPorIds(@Param("ids") List<Integer> ids);

    @Override
    @Query("SELECT p FROM Pedido p ORDER BY p.id")
    List<Pedido> findAll();

    List<Pedido> findByDiaInAndAnioAndMesPedidoOrderById(
            List<Integer> dias,
            Integer anio,
            Integer mes_pedido
    );

    List<Pedido> findByfechaDeRegistroBetween(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    @Query("SELECT DISTINCT CONCAT('Pedidos de ', CASE p.mesPedido " +
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
            "ELSE 'mes_desconocido' END, ' ', CAST(p.anio AS string))" +
            "FROM Pedido p")
    List<String> getMesesPedido();
}
