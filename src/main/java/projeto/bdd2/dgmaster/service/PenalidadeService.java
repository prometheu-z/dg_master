package projeto.bdd2.dgmaster.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Penalidade;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.PenalidadeRepository;

import java.time.LocalDateTime;

@Service
public class PenalidadeService {

    private final PenalidadeRepository penalidadeRepository;
    private final ClienteRepository clienteRepository;

    public PenalidadeService(PenalidadeRepository penalidadeRepository, ClienteRepository clienteRepository) {
        this.penalidadeRepository = penalidadeRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Penalidade registrarPagamentoMulta(Integer idPenalidade) {
        Penalidade penalidade = penalidadeRepository.findById(idPenalidade)
                .orElseThrow(() -> new IllegalArgumentException("Penalidade não encontrada: " + idPenalidade));
        if (penalidade.getDataFimSuspensao() == null) {
            throw new IllegalStateException("A multa só pode ser baixada após a devolução do jogo");
        }
        penalidade.setMultaPaga(true);
        Penalidade salva = penalidadeRepository.save(penalidade);
        reativarContasElegiveis(LocalDateTime.now());
        return salva;
    }

    @Transactional
    public int reativarContasElegiveis(LocalDateTime agora) {
        int reativadas = 0;
        for (Cliente cliente : clienteRepository.findAll()) {
            if (podeReativar(cliente, agora)) {
                cliente.setStatusConta(true);
                cliente.setDataFimSuspensao(null);
                clienteRepository.save(cliente);
                reativadas++;
            }
        }
        return reativadas;
    }

    private boolean podeReativar(Cliente cliente, LocalDateTime agora) {
        if (cliente.isStatusConta() || cliente.getDataFimSuspensao() == null) {
            return false;
        }
        if (cliente.getDataFimSuspensao().isAfter(agora)) {
            return false;
        }
        return !penalidadeRepository.existsMultaNaoPagaPorCliente(cliente.getCpf());
    }
}