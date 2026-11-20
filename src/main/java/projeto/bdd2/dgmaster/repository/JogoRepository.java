package projeto.bdd2.dgmaster.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projeto.bdd2.dgmaster.entity.Jogo;

import java.util.List;

public interface JogoRepository extends JpaRepository<Jogo, Integer> {
    List<Jogo> findByCategoriaNomeCategoriaIgnoreCase(String nomeCategoria);
    List<Jogo> findByFaixaEtariaMinimaLessThanEqual(Integer idade);
    List<Jogo> findByTituloContainingIgnoreCase(String titulo);
}
