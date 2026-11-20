package projeto.bdd2.dgmaster.repository;

import java.math.BigDecimal;

/**
 * Projeção nativa alimentada pelas views gerenciais (RFC-003, VIEW-01).
 */
public interface EstatisticaJogoProjection {
    Integer getIdJogo();
    String getTitulo();
    String getNomeCategoria();
    Long getTotalLocacoes();
    Long getUnidadesAlugadas();
    BigDecimal getReceitaBrutaGerada();
}
