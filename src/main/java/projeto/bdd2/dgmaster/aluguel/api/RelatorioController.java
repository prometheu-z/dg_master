package projeto.bdd2.dgmaster.aluguel.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * VIEW-01 / RNF08: expõe diretamente as views relacionais e a trilha de
 * auditoria do MySQL, sem reagregação na aplicação.
 */
@RestController
@RequestMapping("/api/gerente/relatorios")
@PreAuthorize("hasRole('GERENTE')")
public class RelatorioController {

    private final JdbcTemplate jdbcTemplate;

    public RelatorioController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/alugueis")
    public List<Map<String, Object>> relatorioAlugueis() {
        return jdbcTemplate.queryForList("SELECT * FROM vw_relatorio_alugueis");
    }

    @GetMapping("/jogos-mais-alugados")
    public List<Map<String, Object>> jogosMaisAlugados() {
        return jdbcTemplate.queryForList("SELECT * FROM vw_jogos_mais_alugados");
    }

    @GetMapping("/faturamento-categoria")
    public List<Map<String, Object>> faturamentoCategoria() {
        return jdbcTemplate.queryForList("SELECT * FROM vw_faturamento_categoria");
    }

    @GetMapping("/penalidades-pendentes")
    public List<Map<String, Object>> penalidadesPendentes() {
        return jdbcTemplate.queryForList("SELECT * FROM vw_penalidades_pendentes");
    }

    @GetMapping("/auditoria")
    public List<Map<String, Object>> auditoria() {
        return jdbcTemplate.queryForList(
                "SELECT * FROM log_auditoria ORDER BY id_log DESC LIMIT 200");
    }
}
