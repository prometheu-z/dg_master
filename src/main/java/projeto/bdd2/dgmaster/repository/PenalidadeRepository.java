package projeto.bdd2.dgmaster.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projeto.bdd2.dgmaster.entity.Penalidade;
import java.util.List;

public interface PenalidadeRepository extends JpaRepository<Penalidade, Integer> {
    List<Penalidade> findByAluguelReservaIdAluguel(Integer idAluguel);
    java.util.Optional<Penalidade> findByAluguelReservaIdAluguelAndTipoPenalidade(Integer idAluguel, String tipoPenalidade);

    @org.springframework.data.jpa.repository.Query("select count(p) > 0 from Penalidade p where p.aluguelReserva.cliente.cpf = :cpf and p.multaPaga = false")
    boolean existsMultaNaoPagaPorCliente(@org.springframework.data.repository.query.Param("cpf") String cpf);
}
