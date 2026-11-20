package projeto.bdd2.dgmaster.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projeto.bdd2.dgmaster.entity.Aluguel;
import projeto.bdd2.dgmaster.entity.StatusAluguel;

import java.util.Collection;
import java.util.List;

public interface AluguelRepository extends JpaRepository<Aluguel, Integer> {
    List<Aluguel> findByClienteCpf(String cpf);
    List<Aluguel> findByClienteCpfAndStatusAluguelIn(String cpf, Collection<StatusAluguel> status);
    List<Aluguel> findByStatusAluguel(StatusAluguel status);
    List<Aluguel> findByStatusAluguelIn(Collection<StatusAluguel> status);
}
