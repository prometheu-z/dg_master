package projeto.bdd2.dgmaster.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import projeto.bdd2.dgmaster.aluguel.api.EstatisticasResponse;
import projeto.bdd2.dgmaster.repository.EstatisticaRentabilidadeProjection;
import projeto.bdd2.dgmaster.repository.JogoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EstatisticasService {

    private final JogoRepository jogoRepository;

    public EstatisticasService(JogoRepository jogoRepository) {
        this.jogoRepository = jogoRepository;
    }

    public EstatisticasResponse gerarEstatisticasCompletas() {
        Pageable top10 = PageRequest.of(0, 10);

        List<EstatisticasResponse.JogoEstatistica> maisAlugados = jogoRepository.findJogosMaisAlugados(top10).stream()
                .map(p -> new EstatisticasResponse.JogoEstatistica(p.getCodigoJogo(), p.getNome(), p.getCategoria(), p.getTotalAlugueis()))
                .collect(Collectors.toList());

        List<EstatisticasResponse.JogoEstatistica> menosAlugados = jogoRepository.findJogosMenosAlugados(top10).stream()
                .map(p -> new EstatisticasResponse.JogoEstatistica(p.getCodigoJogo(), p.getNome(), p.getCategoria(), p.getTotalAlugueis()))
                .collect(Collectors.toList());

        List<EstatisticasResponse.JogoRentabilidade> maisRentaveis = jogoRepository.findJogosMaisRentaveis(top10).stream()
                .map(this::mapearParaRentabilidade)
                .collect(Collectors.toList());

        List<EstatisticasResponse.JogoRentabilidade> menosRentaveis = jogoRepository.findJogosMenosRentaveis(top10).stream()
                .map(this::mapearParaRentabilidade)
                .collect(Collectors.toList());

        return new EstatisticasResponse(maisAlugados, menosAlugados, maisRentaveis, menosRentaveis);
    }

    // Método auxiliar para evitar repetição de código no cálculo da média
    private EstatisticasResponse.JogoRentabilidade mapearParaRentabilidade(EstatisticaRentabilidadeProjection p) {
        BigDecimal receitaTotal = p.getReceitaTotal() == null ? BigDecimal.ZERO : p.getReceitaTotal();
        long totalAlugueis = p.getTotalAlugueis() == null ? 0 : p.getTotalAlugueis();
        BigDecimal receitaMedia = totalAlugueis > 0
                ? receitaTotal.divide(BigDecimal.valueOf(totalAlugueis), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new EstatisticasResponse.JogoRentabilidade(
                p.getCodigoJogo(),
                p.getNome(),
                p.getCategoria(),
                receitaTotal,
                totalAlugueis,
                receitaMedia
        );
    }
}
