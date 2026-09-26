package projeto.bdd2.dgmaster.repository;

import java.math.BigDecimal;

public interface EstatisticaRentabilidadeProjection {
    Integer getCodigoJogo();
    String getNome();
    String getCategoria();
    BigDecimal getReceitaTotal();
    Long getTotalAlugueis();
}
