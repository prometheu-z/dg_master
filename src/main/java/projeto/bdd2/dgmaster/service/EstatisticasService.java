package projeto.bdd2.dgmaster.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projeto.bdd2.dgmaster.aluguel.api.EstatisticasResponse;
import projeto.bdd2.dgmaster.repository.EstatisticaJogoProjection;
import projeto.bdd2.dgmaster.repository.RelatorioRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RF10 / VIEW-01: os relatórios são calculados diretamente pelas views
 * relacionais {@code vw_jogos_mais_alugados} e derivados, sem reagregação
 * pesada na aplicação.
 */
@Service
public class EstatisticasService {

    private final RelatorioRepository relatorioRepository;

    public EstatisticasService(RelatorioRepository relatorioRepository) {
        this.relatorioRepository = relatorioRepository;
    }

    @Transactional(readOnly = true)
    public EstatisticasResponse gerarEstatisticasCompletas() {
        List<EstatisticasResponse.JogoEstatistica> maisAlugados =
                relatorioRepository.jogosMaisAlugados().stream()
                        .map(this::mapearEstatistica)
                        .collect(Collectors.toList());

        List<EstatisticasResponse.JogoEstatistica> menosAlugados =
                relatorioRepository.jogosMenosAlugados().stream()
                        .map(this::mapearEstatistica)
                        .collect(Collectors.toList());

        List<EstatisticasResponse.JogoRentabilidade> maisRentaveis =
                relatorioRepository.jogosMaisRentaveis().stream()
                        .map(this::mapearRentabilidade)
                        .collect(Collectors.toList());

        List<EstatisticasResponse.JogoRentabilidade> menosRentaveis =
                relatorioRepository.jogosMenosRentaveis().stream()
                        .map(this::mapearRentabilidade)
                        .collect(Collectors.toList());

        return new EstatisticasResponse(maisAlugados, menosAlugados, maisRentaveis, menosRentaveis);
    }

    private EstatisticasResponse.JogoEstatistica mapearEstatistica(EstatisticaJogoProjection projection) {
        return new EstatisticasResponse.JogoEstatistica(
                projection.getIdJogo(),
                projection.getTitulo(),
                projection.getNomeCategoria(),
                projection.getTotalLocacoes() == null ? 0L : projection.getTotalLocacoes(),
                projection.getUnidadesAlugadas() == null ? 0L : projection.getUnidadesAlugadas());
    }

    private EstatisticasResponse.JogoRentabilidade mapearRentabilidade(EstatisticaJogoProjection projection) {
        BigDecimal receitaTotal = projection.getReceitaBrutaGerada() == null
                ? BigDecimal.ZERO
                : projection.getReceitaBrutaGerada();
        long totalLocacoes = projection.getTotalLocacoes() == null ? 0L : projection.getTotalLocacoes();
        BigDecimal receitaMedia = totalLocacoes > 0
                ? receitaTotal.divide(BigDecimal.valueOf(totalLocacoes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new EstatisticasResponse.JogoRentabilidade(
                projection.getIdJogo(),
                projection.getTitulo(),
                projection.getNomeCategoria(),
                receitaTotal,
                totalLocacoes,
                receitaMedia);
    }
}
