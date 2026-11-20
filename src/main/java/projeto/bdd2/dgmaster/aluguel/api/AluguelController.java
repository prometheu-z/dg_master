package projeto.bdd2.dgmaster.aluguel.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import projeto.bdd2.dgmaster.entity.Aluguel;
import projeto.bdd2.dgmaster.entity.StatusAluguel;
import projeto.bdd2.dgmaster.repository.AluguelRepository;
import projeto.bdd2.dgmaster.security.UserPrincipal;
import projeto.bdd2.dgmaster.service.AluguelService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/alugueis")
public class AluguelController {

    private final AluguelService aluguelService;
    private final AluguelRepository aluguelRepository;

    public AluguelController(AluguelService aluguelService, AluguelRepository aluguelRepository) {
        this.aluguelService = aluguelService;
        this.aluguelRepository = aluguelRepository;
    }

    @PostMapping
    public ResponseEntity<AluguelResponse> reservar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReservaAluguelRequest request) {
        exigirCliente(principal);
        Aluguel aluguel = aluguelService.solicitarReserva(
                principal.getCpf(), request.idDependente(), request.idsJogos());
        return ResponseEntity.status(HttpStatus.CREATED).body(AluguelResponse.from(aluguel));
    }

    @DeleteMapping("/{id}")
    public AluguelResponse cancelar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        exigirCliente(principal);
        return AluguelResponse.from(aluguelService.cancelarReserva(id, principal.getCpf()));
    }

    /**
     * BUG-05: a retirada é uma operação de balcão, exclusiva do gerente.
     * O id do gerente autenticado é repassado a sp_retirar_aluguel.
     */
    @PreAuthorize("hasRole('GERENTE')")
    @PutMapping("/{id}/retirar")
    public AluguelResponse retirar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        return AluguelResponse.from(aluguelService.retirarAluguel(id, idGerente(principal)));
    }

    @PutMapping("/{id}/renovar")
    public AluguelResponse renovar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        exigirCliente(principal);
        return AluguelResponse.from(aluguelService.renovarAluguel(id, principal.getCpf()));
    }

    @PreAuthorize("hasRole('GERENTE')")
    @PutMapping("/{id}/devolver")
    public AluguelResponse devolver(@PathVariable Integer id) {
        return AluguelResponse.from(aluguelService.finalizarAluguel(id, LocalDate.now()));
    }

    @PreAuthorize("hasRole('GERENTE')")
    @GetMapping
    public List<AluguelResponse> listar(
            @RequestParam(required = false) StatusAluguel status,
            @RequestParam(required = false) String clienteCpf) {
        List<Aluguel> alugueis;
        if (clienteCpf != null && !clienteCpf.isBlank()) {
            alugueis = aluguelRepository.findByClienteCpf(clienteCpf);
            if (status != null) {
                alugueis.removeIf(aluguel -> aluguel.getStatusAluguel() != status);
            }
        } else if (status != null) {
            alugueis = aluguelRepository.findByStatusAluguel(status);
        } else {
            alugueis = aluguelRepository.findAll();
        }
        List<AluguelResponse> respostas = new ArrayList<>();
        for (Aluguel aluguel : alugueis) {
            respostas.add(AluguelResponse.from(aluguel));
        }
        return respostas;
    }

    private void exigirCliente(UserPrincipal principal) {
        if (principal == null || !"CLIENTE".equals(principal.getTipo())) {
            throw new AccessDeniedException("Esta operação exige uma sessão de cliente titular");
        }
    }

    private Integer idGerente(UserPrincipal principal) {
        if (principal == null || !"GERENTE".equals(principal.getTipo())) {
            throw new AccessDeniedException("Esta operação exige uma sessão de gerente");
        }
        return Integer.valueOf(principal.getCpf());
    }
}
