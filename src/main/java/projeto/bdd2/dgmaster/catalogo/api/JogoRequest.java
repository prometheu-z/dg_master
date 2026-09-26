package projeto.bdd2.dgmaster.catalogo.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record JogoRequest(
        @NotBlank String nome,
        String categoria,
        String genero,
        @PositiveOrZero Integer faixaEtariaRecomendada,
        @NotNull @DecimalMin("0.01") BigDecimal precoLocacao,
        @NotNull @DecimalMin("0.01") BigDecimal valorReposicao,
        @NotNull @PositiveOrZero Integer quantidadeEstoque) {
}