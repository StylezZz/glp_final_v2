package pucp.edu.pe.glp_final.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pucp.edu.pe.glp_final.models.Cliente;

@Repository
public interface ClientRepository extends JpaRepository<Cliente,String> {
}
