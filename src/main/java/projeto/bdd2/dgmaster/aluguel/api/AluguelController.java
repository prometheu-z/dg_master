package projeto.bdd2.dgmaster.aluguel.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import projeto.bdd2.dgmaster.entity.AluguelReserva;
import projeto.bdd2.dgmaster.entity.StatusAluguel;
import projeto.bdd2.dgmaster.repository.AluguelReservaRepository;
import projeto.bdd2.dgmaster.security.UserPrincipal;
import projeto.bdd2.dgmaster.service.AluguelService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alugueis")
public class AluguelController {

    private final AluguelService aluguelService;
    private final AluguelReservaRepository aluguelReservaRepository;

    public AluguelController(AluguelService aluguelService, AluguelReservaRepository aluguelReservaRepository) {
        this.aluguelService = aluguelService;
        this.aluguelReservaRepository = aluguelReservaRepository;
    }

    @PostMapping
    public ResponseEntity<AluguelResponse> reservar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReservaAluguelRequest request) {
        exigirCliente(principal);
        AluguelReserva reserva = aluguelService.solicitarReserva(
                principal.getCpf(), request.idDependente(), request.idsJogos());
        HttpStatus responseStatus = reserva.getStatus() == StatusAluguel.AGUARDANDO_APROVACAO
                ? HttpStatus.ACCEPTED
                : HttpStatus.CREATED;
        return ResponseEntity.status(responseStatus).body(AluguelResponse.from(reserva));
    }

    @PutMapping("/{id}/aprovar")
    public AluguelResponse aprovar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        exigirCliente(principal);
        return AluguelResponse.from(aluguelService.aprovarReserva(id, principal.getCpf()));
    }

    @DeleteMapping("/{id}")
    public AluguelResponse cancelar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        exigirCliente(principal);
        return AluguelResponse.from(aluguelService.cancelarReserva(id, principal.getCpf()));
    }

    @PutMapping("/{id}/retirar")
    public AluguelResponse retirar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        exigirCliente(principal);
        return AluguelResponse.from(aluguelService.retirarReserva(id, principal.getCpf()));
    }

    @PutMapping("/{id}/renovar")
    public AluguelResponse renovar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        exigirCliente(principal);
        return AluguelResponse.from(aluguelService.renovarAluguel(id, principal.getCpf(), LocalDateTime.now()));
    }

    @PreAuthorize("hasRole('GERENTE')")
    @PutMapping("/{id}/devolver")
    public AluguelResponse devolver(@PathVariable Integer id) {
        return AluguelResponse.from(aluguelService.devolver(id, LocalDateTime.now()));
    }

    @PreAuthorize("hasRole('GERENTE')")
    @GetMapping
    public List<AluguelResponse> listar(
            @RequestParam(required = false) StatusAluguel status,
            @RequestParam(required = false) String clienteCpf) {
        List<AluguelReserva> reservas;
        if (clienteCpf != null && !clienteCpf.isBlank()) {
            reservas = aluguelReservaRepository.findByClienteCpf(clienteCpf);
            if (status != null) {
                reservas.removeIf(reserva -> reserva.getStatus() != status);
            }
        } else if (status != null) {
            reservas = aluguelReservaRepository.findByStatus(status);
        } else {
            reservas = aluguelReservaRepository.findAll();
        }
        List<AluguelResponse> respostas = new java.util.ArrayList<>();
        for (AluguelReserva reserva : reservas) {
            respostas.add(AluguelResponse.from(reserva));
        }
        return respostas;
    }

    private void exigirCliente(UserPrincipal principal) {
        if (principal == null || !"CLIENTE".equals(principal.getTipo())) {
            throw new AccessDeniedException("Esta operação exige uma sessão de cliente titular");
        }
    }

}