package projeto.bdd2.dgmaster.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import projeto.bdd2.dgmaster.entity.Jogo;

import java.util.List;



public interface JogoRepository extends JpaRepository<Jogo, Integer> {
    List<Jogo> findByGeneroIgnoreCase(String genero);
    List<Jogo> findByCategoriaIgnoreCase(String categoria);
    List<Jogo> findByFaixaEtariaRecomendadaLessThanEqual(Integer idade);


     @Query("SELECT j.codigoJogo AS codigoJogo, j.nome AS nome, j.categoria AS categoria, COUNT(i) AS totalAlugueis " +
           "FROM Jogo j LEFT JOIN j.itensAluguel i " +
           "GROUP BY j.codigoJogo, j.nome, j.categoria " +
           "ORDER BY COUNT(i) DESC")
    List<EstatisticaAluguelProjection> findJogosMaisAlugados(Pageable pageable);

    @Query("SELECT j.codigoJogo AS codigoJogo, j.nome AS nome, j.categoria AS categoria, COUNT(i) AS totalAlugueis " +
           "FROM Jogo j LEFT JOIN j.itensAluguel i " +
           "GROUP BY j.codigoJogo, j.nome, j.categoria " +
           "ORDER BY COUNT(i) ASC")
    List<EstatisticaAluguelProjection> findJogosMenosAlugados(Pageable pageable);

    @Query("SELECT j.codigoJogo AS codigoJogo, j.nome AS nome, j.categoria AS categoria, " +
           "SUM(COALESCE(i.valorUnitarioCobrado, 0)) AS receitaTotal, COUNT(i) AS totalAlugueis " +
           "FROM Jogo j JOIN j.itensAluguel i " +
           "GROUP BY j.codigoJogo, j.nome, j.categoria " +
           "ORDER BY SUM(COALESCE(i.valorUnitarioCobrado, 0)) DESC")
    List<EstatisticaRentabilidadeProjection> findJogosMaisRentaveis(Pageable pageable);

    @Query("SELECT j.codigoJogo AS codigoJogo, j.nome AS nome, j.categoria AS categoria, " +
           "SUM(COALESCE(i.valorUnitarioCobrado, 0)) AS receitaTotal, COUNT(i) AS totalAlugueis " +
           "FROM Jogo j JOIN j.itensAluguel i " +
           "GROUP BY j.codigoJogo, j.nome, j.categoria " +
           "ORDER BY SUM(COALESCE(i.valorUnitarioCobrado, 0)) ASC")
    List<EstatisticaRentabilidadeProjection> findJogosMenosRentaveis(Pageable pageable);
}
