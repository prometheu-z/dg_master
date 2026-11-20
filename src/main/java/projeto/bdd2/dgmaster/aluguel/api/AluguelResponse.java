package projeto.bdd2.dgmaster.aluguel.api;

import projeto.bdd2.dgmaster.entity.Aluguel;
import projeto.bdd2.dgmaster.entity.ItemAluguel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record AluguelResponse(
        Integer idAluguel,
        String status,
        LocalDateTime dataReserva,
        LocalDateTime dataExpiracaoReserva,
        LocalDateTime dataRetirada,
        LocalDate devolucaoPrevista,
        LocalDate devolucaoEfetiva,
        BigDecimal valorDesconto,
        BigDecimal valorTotal,
        Integer qtdRenovacoes,
        String clienteCpf,
        List<String> jogos) {

    public static AluguelResponse from(Aluguel aluguel) {
        List<String> nomesJogos = new ArrayList<>();
        for (ItemAluguel item : aluguel.getItensAluguel()) {
            nomesJogos.add(item.getJogo().getTitulo());
        }
        return new AluguelResponse(
                aluguel.getIdAluguel(),
                aluguel.getStatusAluguel().name(),
                aluguel.getDataReserva(),
                aluguel.getDataExpiracaoReserva(),
                aluguel.getDataRetirada(),
                aluguel.getDevolucaoPrevista(),
                aluguel.getDevolucaoEfetiva(),
                aluguel.getValorDesconto(),
                aluguel.getValorTotal(),
                aluguel.getQtdRenovacoes(),
                aluguel.getCliente().getCpf(),
                nomesJogos);
    }
}
