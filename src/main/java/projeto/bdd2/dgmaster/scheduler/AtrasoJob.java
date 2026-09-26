package projeto.bdd2.dgmaster.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import projeto.bdd2.dgmaster.service.AluguelService;
import projeto.bdd2.dgmaster.service.PenalidadeService;

import java.time.LocalDateTime;

@Component
public class AtrasoJob {

    private final AluguelService aluguelService;
    private final PenalidadeService penalidadeService;

    public AtrasoJob(AluguelService aluguelService, PenalidadeService penalidadeService) {
        this.aluguelService = aluguelService;
        this.penalidadeService = penalidadeService;
    }

    @Scheduled(cron = "${dgmaster.atrasos.cron:0 0 1 * * *}")
    public void verificarAtrasosEReativarContas() {
        LocalDateTime agora = LocalDateTime.now();
        aluguelService.verificarAtrasos(agora);
        penalidadeService.reativarContasElegiveis(agora);
    }
}