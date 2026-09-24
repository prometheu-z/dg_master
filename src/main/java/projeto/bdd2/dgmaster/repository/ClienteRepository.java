package projeto.bdd2.dgmaster.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projeto.bdd2.dgmaster.entity.Cliente;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, String> {
    Optional<Cliente> findByEmail(String email);
    boolean existsByEmail(String email);
}
