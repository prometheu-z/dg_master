package projeto.bdd2.dgmaster.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projeto.bdd2.dgmaster.entity.Penalidade;
import projeto.bdd2.dgmaster.entity.StatusPagamento;
import projeto.bdd2.dgmaster.repository.PenalidadeRepository;

@Service
public class PenalidadeService {

    private final PenalidadeRepository penalidadeRepository;

    public PenalidadeService(PenalidadeRepository penalidadeRepository) {
        this.penalidadeRepository = penalidadeRepository;
    }

    /**
     * Baixa manual da multa recebida no balcão. A existência/suspensão da conta
     * não é mais controlada em Java: o schema físico do RFC-003 não possui
     * colunas de suspensão; o pagamento fica auditado por trigger.
     */
    @Transactional
    public Penalidade registrarPagamentoMulta(Integer idPenalidade) {
        Penalidade penalidade = penalidadeRepository.findById(idPenalidade)
                .orElseThrow(() -> new IllegalArgumentException("Penalidade não encontrada: " + idPenalidade));

        if (penalidade.getStatusPagamento() == StatusPagamento.PAGO) {
            throw new IllegalStateException("Penalidade já está quitada");
        }

        penalidade.setStatusPagamento(StatusPagamento.PAGO);
        return penalidadeRepository.save(penalidade);
    }
}
