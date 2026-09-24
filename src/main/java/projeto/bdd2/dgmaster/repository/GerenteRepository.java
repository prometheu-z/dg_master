package projeto.bdd2.dgmaster.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projeto.bdd2.dgmaster.entity.Gerente;
import java.util.Optional;

public interface GerenteRepository extends JpaRepository<Gerente, Integer> {
    Optional<Gerente> findByEmail(String email);
    boolean existsByEmail(String email);
}
