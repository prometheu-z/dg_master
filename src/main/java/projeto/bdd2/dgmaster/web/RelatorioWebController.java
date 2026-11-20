package projeto.bdd2.dgmaster.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import projeto.bdd2.dgmaster.service.RelatorioService;

@Controller
@RequestMapping("/gerente/relatorios")
@PreAuthorize("hasRole('GERENTE')")
public class RelatorioWebController {

    private final RelatorioService relatorioService;

    public RelatorioWebController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping
    public String relatorios(Model model) {
        model.addAttribute("alugueis", relatorioService.listarAlugueis());
        model.addAttribute("faturamentoCategorias", relatorioService.listarFaturamentoPorCategoria());
        model.addAttribute("auditoria", relatorioService.listarAuditoria());
        return "relatorios";
    }
}
