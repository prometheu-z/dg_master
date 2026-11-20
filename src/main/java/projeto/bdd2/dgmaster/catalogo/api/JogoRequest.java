package projeto.bdd2.dgmaster.catalogo.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record JogoRequest(
        @NotBlank String titulo,
        @NotBlank String categoria,
        String descricao,
        @PositiveOrZero Integer faixaEtariaMinima,
        @NotNull @DecimalMin("0.01") BigDecimal valorDiaria,
        @NotNull @DecimalMin("0.01") BigDecimal valorReposicao,
        @NotNull @PositiveOrZero Integer quantidadeEstoque) {
}
