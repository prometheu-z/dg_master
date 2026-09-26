package projeto.bdd2.dgmaster.aluguel.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservaAluguelRequest(
        @NotEmpty List<@NotNull @Positive Integer> idsJogos,
        @Positive Integer idDependente) {
}