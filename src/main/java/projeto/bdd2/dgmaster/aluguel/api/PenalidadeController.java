package projeto.bdd2.dgmaster.aluguel.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projeto.bdd2.dgmaster.service.PenalidadeService;

@RestController
@RequestMapping("/api/gerente/penalidades")
@PreAuthorize("hasRole('GERENTE')")
public class PenalidadeController {

    private final PenalidadeService penalidadeService;

    public PenalidadeController(PenalidadeService penalidadeService) {
        this.penalidadeService = penalidadeService;
    }

    @PutMapping("/{id}/pagamento")
    public PenalidadeResponse registrarPagamento(@PathVariable Integer id) {
        return PenalidadeResponse.from(penalidadeService.registrarPagamentoMulta(id));
    }
}