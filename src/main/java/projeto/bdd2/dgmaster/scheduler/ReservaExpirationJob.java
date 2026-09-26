package projeto.bdd2.dgmaster.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import projeto.bdd2.dgmaster.service.AluguelService;

import java.time.LocalDateTime;

@Component
public class ReservaExpirationJob {

    private final AluguelService aluguelService;

    public ReservaExpirationJob(AluguelService aluguelService) {
        this.aluguelService = aluguelService;
    }

    @Scheduled(cron = "${dgmaster.reservas.expiracao-cron:0 0 * * * *}")
    public void expirarReservas() {
        aluguelService.expirarReservasVencidas(LocalDateTime.now());
    }
}