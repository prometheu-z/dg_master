package projeto.bdd2.dgmaster.aluguel.api;

import projeto.bdd2.dgmaster.entity.Penalidade;

import java.time.LocalDateTime;

public record PenalidadeResponse(
        Integer idPenalidade,
        String tipoPenalidade,
        Double valorMulta,
        boolean multaPaga,
        LocalDateTime dataFimSuspensao) {

    public static PenalidadeResponse from(Penalidade penalidade) {
        return new PenalidadeResponse(
                penalidade.getIdPenalidade(),
                penalidade.getTipoPenalidade(),
                penalidade.getValorMulta(),
                penalidade.isMultaPaga(),
                penalidade.getDataFimSuspensao());
    }
}