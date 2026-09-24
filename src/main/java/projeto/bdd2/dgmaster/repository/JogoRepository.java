package projeto.bdd2.dgmaster.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projeto.bdd2.dgmaster.entity.Jogo;
import java.util.List;

public interface JogoRepository extends JpaRepository<Jogo, Integer> {
    List<Jogo> findByGeneroIgnoreCase(String genero);
    List<Jogo> findByCategoriaIgnoreCase(String categoria);
    List<Jogo> findByFaixaEtariaRecomendadaLessThanEqual(Integer idade);
}
