package projeto.bdd2.dgmaster.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projeto.bdd2.dgmaster.entity.AluguelReserva;
import projeto.bdd2.dgmaster.entity.StatusAluguel;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AluguelReservaRepository extends JpaRepository<AluguelReserva, Integer> {
    List<AluguelReserva> findByClienteCpf(String cpf);
    List<AluguelReserva> findByClienteCpfAndStatusIn(String cpf, Collection<StatusAluguel> status);
    List<AluguelReserva> findByStatus(StatusAluguel status);
    List<AluguelReserva> findByStatusAndDataLimiteRetiradaBefore(StatusAluguel status, LocalDateTime limite);
}
