package projeto.bdd2.dgmaster.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projeto.bdd2.dgmaster.entity.Jogo;

import java.util.List;

public interface RelatorioRepository extends JpaRepository<Jogo, Integer> {

    @Query(value = "SELECT id_jogo AS idJogo, titulo, nome_categoria AS nomeCategoria, "
            + "total_locacoes AS totalLocacoes, unidades_alugadas AS unidadesAlugadas, "
            + "receita_bruta_gerada AS receitaBrutaGerada "
            + "FROM vw_jogos_mais_alugados ORDER BY unidades_alugadas DESC, receita_bruta_gerada DESC LIMIT 10",
            nativeQuery = true)
    List<EstatisticaJogoProjection> jogosMaisAlugados();

    @Query(value = "SELECT id_jogo AS idJogo, titulo, nome_categoria AS nomeCategoria, "
            + "total_locacoes AS totalLocacoes, unidades_alugadas AS unidadesAlugadas, "
            + "receita_bruta_gerada AS receitaBrutaGerada "
            + "FROM vw_jogos_mais_alugados ORDER BY unidades_alugadas ASC, receita_bruta_gerada ASC LIMIT 10",
            nativeQuery = true)
    List<EstatisticaJogoProjection> jogosMenosAlugados();

    @Query(value = "SELECT id_jogo AS idJogo, titulo, nome_categoria AS nomeCategoria, "
            + "total_locacoes AS totalLocacoes, unidades_alugadas AS unidadesAlugadas, "
            + "receita_bruta_gerada AS receitaBrutaGerada "
            + "FROM vw_jogos_mais_alugados ORDER BY receita_bruta_gerada DESC, unidades_alugadas DESC LIMIT 10",
            nativeQuery = true)
    List<EstatisticaJogoProjection> jogosMaisRentaveis();

    @Query(value = "SELECT id_jogo AS idJogo, titulo, nome_categoria AS nomeCategoria, "
            + "total_locacoes AS totalLocacoes, unidades_alugadas AS unidadesAlugadas, "
            + "receita_bruta_gerada AS receitaBrutaGerada "
            + "FROM vw_jogos_mais_alugados ORDER BY receita_bruta_gerada ASC, unidades_alugadas ASC LIMIT 10",
            nativeQuery = true)
    List<EstatisticaJogoProjection> jogosMenosRentaveis();

    @Query(value = "SELECT COUNT(*) FROM aluguel WHERE id_cliente = :idCliente "
            + "AND status_aluguel IN ('RESERVADO','RETIRADO')", nativeQuery = true)
    Long contarAlugueisAtivos(@Param("idCliente") Integer idCliente);
}
