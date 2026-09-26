package projeto.bdd2.dgmaster.aluguel.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projeto.bdd2.dgmaster.entity.AluguelReserva;
import projeto.bdd2.dgmaster.service.AluguelService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/gerente")
@PreAuthorize("hasRole('GERENTE')")
public class GerenteAluguelController {

    private final AluguelService aluguelService;

    public GerenteAluguelController(AluguelService aluguelService) {
        this.aluguelService = aluguelService;
    }

    @GetMapping("/prazos")
    public List<AluguelResponse> listarPrazos() {
        List<AluguelResponse> respostas = new ArrayList<>();
        for (AluguelReserva reserva : aluguelService.listarPrazosGerenciais()) {
            respostas.add(AluguelResponse.from(reserva));
        }
        return respostas;
    }
}