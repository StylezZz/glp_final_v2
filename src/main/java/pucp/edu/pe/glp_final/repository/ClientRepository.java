package pucp.edu.pe.glp_final.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pucp.edu.pe.glp_final.models.Cliente;

public interface ClientRepository extends JpaRepository<Cliente,String> {
}
