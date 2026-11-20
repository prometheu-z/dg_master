package projeto.bdd2.dgmaster.aluguel.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EstatisticasResponse {
    private List<JogoEstatistica> jogosMaisAlugados;
    private List<JogoEstatistica> jogosMenosAlugados;
    private List<JogoRentabilidade> jogosMaisRentaveis;
    private List<JogoRentabilidade> jogosMenosRentaveis;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JogoEstatistica {
        private Integer idJogo;
        private String titulo;
        private String nomeCategoria;
        private Long totalLocacoes;
        private Long unidadesAlugadas;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JogoRentabilidade {
        private Integer idJogo;
        private String titulo;
        private String nomeCategoria;
        private BigDecimal receitaTotal;
        private Long totalLocacoes;
        private BigDecimal receitaMediaPorAluguel;
    }
}
