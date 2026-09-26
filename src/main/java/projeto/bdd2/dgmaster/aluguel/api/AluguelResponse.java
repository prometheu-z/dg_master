package projeto.bdd2.dgmaster.aluguel.api;

import projeto.bdd2.dgmaster.entity.AluguelReserva;
import projeto.bdd2.dgmaster.entity.Jogo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record AluguelResponse(
        Integer idAluguel,
        String status,
        LocalDateTime dataHoraReserva,
        LocalDateTime dataLimiteRetirada,
        LocalDateTime dataLimiteDevolucao,
        LocalDateTime dataDevolucaoReal,
        Double valorTotal,
        BigDecimal creditoAplicado,
        List<String> jogos,
                String termosContrato) {

        public static AluguelResponse from(AluguelReserva reserva) {
                List<String> nomesJogos = new ArrayList<>();
                for (Jogo jogo : reserva.getJogos()) {
                        nomesJogos.add(jogo.getNome());
                }
                return new AluguelResponse(
                                reserva.getIdAluguel(),
                                reserva.getStatus().name(),
                                reserva.getDataHoraReserva(),
                                reserva.getDataLimiteRetirada(),
                                reserva.getDataLimiteDevolucao(),
                                reserva.getDataDevolucaoReal(),
                                reserva.getValorTotal(),
                                reserva.getCreditoAplicado(),
                                nomesJogos,
                                reserva.getTermosContrato());
        }
}