package projeto.bdd2.dgmaster.cliente.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DependenteRequest(@NotBlank String nome, @NotNull LocalDate dataNascimento) {
}