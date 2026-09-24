package projeto.bdd2.dgmaster.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projeto.bdd2.dgmaster.entity.Dependente;
import java.util.List;

public interface DependenteRepository extends JpaRepository<Dependente, Integer> {
    List<Dependente> findByClienteCpf(String cpf);
}
