package projeto.bdd2.dgmaster.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projeto.bdd2.dgmaster.entity.Penalidade;
import java.util.List;

public interface PenalidadeRepository extends JpaRepository<Penalidade, Integer> {
    List<Penalidade> findByAluguelReservaIdAluguel(Integer idAluguel);
}
