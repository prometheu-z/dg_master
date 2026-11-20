package projeto.bdd2.dgmaster.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projeto.bdd2.dgmaster.entity.Penalidade;
import projeto.bdd2.dgmaster.entity.StatusPagamento;

import java.util.List;
import java.util.Optional;

public interface PenalidadeRepository extends JpaRepository<Penalidade, Integer> {
    List<Penalidade> findByAluguelIdAluguel(Integer idAluguel);
    List<Penalidade> findByStatusPagamento(StatusPagamento statusPagamento);
    Optional<Penalidade> findByAluguelIdAluguelAndMotivoPenalidade(Integer idAluguel, String motivoPenalidade);
}
