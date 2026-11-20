package projeto.bdd2.dgmaster.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RelatorioService {

    private final JdbcTemplate jdbcTemplate;

    public RelatorioService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listarAlugueis() {
        return jdbcTemplate.queryForList("SELECT * FROM vw_relatorio_alugueis");
    }

    public List<Map<String, Object>> listarJogosMaisAlugados() {
        return jdbcTemplate.queryForList("SELECT * FROM vw_jogos_mais_alugados");
    }

    public List<Map<String, Object>> listarFaturamentoPorCategoria() {
        return jdbcTemplate.queryForList("SELECT * FROM vw_faturamento_categoria");
    }

    public List<Map<String, Object>> listarPenalidadesPendentes() {
        return jdbcTemplate.queryForList("SELECT * FROM vw_penalidades_pendentes");
    }

    public List<Map<String, Object>> listarAuditoria() {
        return jdbcTemplate.queryForList(
                "SELECT * FROM log_auditoria ORDER BY id_log DESC LIMIT 200");
    }
}
