package projeto.bdd2.dgmaster.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import projeto.bdd2.dgmaster.service.AluguelService;

@Component
public class AtrasoJob {

    private final AluguelService aluguelService;

    public AtrasoJob(AluguelService aluguelService) {
        this.aluguelService = aluguelService;
    }

    @Scheduled(cron = "${dgmaster.atrasos.cron:0 0 1 * * *}")
    public void registrarAtrasos() {
        aluguelService.registrarAtrasos();
    }
}
