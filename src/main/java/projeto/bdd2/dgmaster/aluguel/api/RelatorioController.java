package projeto.bdd2.dgmaster.aluguel.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projeto.bdd2.dgmaster.service.RelatorioService;

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

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/alugueis")
    public List<Map<String, Object>> relatorioAlugueis() {
        return relatorioService.listarAlugueis();
    }

    @GetMapping("/jogos-mais-alugados")
    public List<Map<String, Object>> jogosMaisAlugados() {
        return relatorioService.listarJogosMaisAlugados();
    }

    @GetMapping("/faturamento-categoria")
    public List<Map<String, Object>> faturamentoCategoria() {
        return relatorioService.listarFaturamentoPorCategoria();
    }

    @GetMapping("/penalidades-pendentes")
    public List<Map<String, Object>> penalidadesPendentes() {
        return relatorioService.listarPenalidadesPendentes();
    }

    @GetMapping("/auditoria")
    public List<Map<String, Object>> auditoria() {
        return relatorioService.listarAuditoria();
    }
}
