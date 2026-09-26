package projeto.bdd2.dgmaster.cliente.api;

import projeto.bdd2.dgmaster.entity.Dependente;

import java.time.LocalDate;

public record DependenteResponse(Integer idDependente, String nome, LocalDate dataNascimento) {

    public static DependenteResponse from(Dependente dependente) {
        return new DependenteResponse(
                dependente.getIdDependente(), dependente.getNome(), dependente.getDataNascimento());
    }
}