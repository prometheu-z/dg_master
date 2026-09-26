package projeto.bdd2.dgmaster.aluguel.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projeto.bdd2.dgmaster.service.EstatisticasService;

@RestController
@RequestMapping("/api/estatisticas")
public class EstatisticasController {

    private final EstatisticasService estatisticasService;

    public EstatisticasController(EstatisticasService estatisticasService) {
        this.estatisticasService = estatisticasService;
    }

    @PreAuthorize("hasRole('GERENTE')")
    @GetMapping
    public EstatisticasResponse obterEstatisticasCompletas() {
        return estatisticasService.gerarEstatisticasCompletas();
    }
}
