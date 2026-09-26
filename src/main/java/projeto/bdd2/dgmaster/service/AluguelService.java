package projeto.bdd2.dgmaster.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projeto.bdd2.dgmaster.entity.AluguelReserva;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.entity.StatusAluguel;
import projeto.bdd2.dgmaster.repository.AluguelReservaRepository;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.JogoRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AluguelService {

    private static final int LIMITE_JOGOS_POR_CLIENTE = 4;

    private final AluguelReservaRepository aluguelReservaRepository;
    private final ClienteRepository clienteRepository;
    private final JogoRepository jogoRepository;

    public AluguelService(AluguelReservaRepository aluguelReservaRepository,
                          ClienteRepository clienteRepository,
                          JogoRepository jogoRepository) {
        this.aluguelReservaRepository = aluguelReservaRepository;
        this.clienteRepository = clienteRepository;
        this.jogoRepository = jogoRepository;
    }

    @Transactional
    public AluguelReserva reservar(String cpfCliente, List<Integer> idsJogos) {
        Cliente cliente = clienteRepository.findById(cpfCliente)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + cpfCliente));

        if (idsJogos == null || idsJogos.isEmpty()) {
            throw new IllegalArgumentException("É necessário selecionar pelo menos um jogo");
        }

        if (idsJogos.size() > LIMITE_JOGOS_POR_CLIENTE) {
            throw new IllegalArgumentException("Limite máximo de 4 jogos por cliente");
        }

        List<Jogo> jogos = new ArrayList<>();
        for (Integer idJogo : idsJogos) {
            Jogo jogo = jogoRepository.findById(idJogo)
                    .orElseThrow(() -> new IllegalArgumentException("Jogo não encontrado: " + idJogo));
            if (jogo.getQuantidadeEstoque() <= 0) {
                throw new IllegalArgumentException("Jogo indisponível no estoque: " + jogo.getNome());
            }
            jogos.add(jogo);
        }

        AluguelReserva aluguel = new AluguelReserva();
        aluguel.setCliente(cliente);
        aluguel.setDataHoraReserva(LocalDateTime.now());
        aluguel.setDataLimiteRetirada(LocalDateTime.now().plusHours(24));
        aluguel.setStatus(StatusAluguel.RESERVADO);
        aluguel.setValorTotal(0.0);
        aluguel.setJogos(jogos);
        aluguel.setQuantidadeRenovacoes(0);

        for (Jogo jogo : jogos) {
            jogo.setQuantidadeEstoque(jogo.getQuantidadeEstoque() - 1);
            jogo.setStatusDisponibilidade(jogo.getQuantidadeEstoque() > 0);
        }

        return aluguelReservaRepository.save(aluguel);
    }
}
