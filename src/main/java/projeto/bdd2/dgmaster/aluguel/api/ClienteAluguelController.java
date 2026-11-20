package projeto.bdd2.dgmaster.aluguel.api;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projeto.bdd2.dgmaster.entity.Aluguel;
import projeto.bdd2.dgmaster.repository.AluguelRepository;
import projeto.bdd2.dgmaster.security.UserPrincipal;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteAluguelController {

    private final AluguelRepository aluguelRepository;

    public ClienteAluguelController(AluguelRepository aluguelRepository) {
        this.aluguelRepository = aluguelRepository;
    }

    @GetMapping("/{cpf}/alugueis")
    public List<AluguelResponse> listarAlugueis(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String cpf) {
        if (principal == null || !"CLIENTE".equals(principal.getTipo()) || !cpf.equals(principal.getCpf())) {
            throw new AccessDeniedException("Clientes só podem consultar o próprio histórico");
        }
        List<AluguelResponse> respostas = new ArrayList<>();
        for (Aluguel aluguel : aluguelRepository.findByClienteCpf(cpf)) {
            respostas.add(AluguelResponse.from(aluguel));
        }
        return respostas;
    }
}
