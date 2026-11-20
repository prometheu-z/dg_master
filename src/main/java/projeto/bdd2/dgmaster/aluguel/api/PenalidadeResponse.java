package projeto.bdd2.dgmaster.aluguel.api;

import projeto.bdd2.dgmaster.entity.Penalidade;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PenalidadeResponse(
        Integer idPenalidade,
        Integer idAluguel,
        String motivoPenalidade,
        BigDecimal valorMulta,
        String statusPagamento,
        LocalDateTime dataGeracao) {

    public static PenalidadeResponse from(Penalidade penalidade) {
        return new PenalidadeResponse(
                penalidade.getIdPenalidade(),
                penalidade.getAluguel().getIdAluguel(),
                penalidade.getMotivoPenalidade(),
                penalidade.getValorMulta(),
                penalidade.getStatusPagamento().name(),
                penalidade.getDataGeracao());
    }
}
