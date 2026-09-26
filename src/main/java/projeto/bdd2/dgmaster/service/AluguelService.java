package projeto.bdd2.dgmaster.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projeto.bdd2.dgmaster.entity.AluguelReserva;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Dependente;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.entity.Penalidade;
import projeto.bdd2.dgmaster.entity.StatusAluguel;
import projeto.bdd2.dgmaster.repository.AluguelReservaRepository;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.DependenteRepository;
import projeto.bdd2.dgmaster.repository.JogoRepository;
import projeto.bdd2.dgmaster.repository.PenalidadeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
public class AluguelService {

    private static final int LIMITE_JOGOS_POR_CLIENTE = 4;
    private static final int PRAZO_RESERVA_HORAS = 24;
    private static final int PRAZO_ALUGUEL_DIAS = 7;
    private static final BigDecimal MULTA_POR_JOGO_DIA = new BigDecimal("5.00");
    private static final String TERMOS_CONTRATO = "Ao realizar a retirada do(s) jogo(s) descrito(s) neste aluguel, o Locatário titular assume total responsabilidade civil e financeira pela guarda, conservação e integridade de todos os componentes (tabuleiro, cartas, peças, manuais e caixa). Em caso de perda, extravio ou dano que inviabilize o uso do produto, o Locatário concorda em arcar com o valor integral de reposição do jogo de tabuleiro. A devolução fora do prazo estipulado implica em multa diária de R$ 5,00 por item e suspensão temporária da plataforma. O aceite digital deste termo possui validade legal e vinculativa para a DG Master LTDA.";

    private final AluguelReservaRepository aluguelReservaRepository;
    private final ClienteRepository clienteRepository;
    private final DependenteRepository dependenteRepository;
    private final JogoRepository jogoRepository;
    private final PenalidadeRepository penalidadeRepository;

    public AluguelService(AluguelReservaRepository aluguelReservaRepository,
                          ClienteRepository clienteRepository,
                          DependenteRepository dependenteRepository,
                          JogoRepository jogoRepository,
                          PenalidadeRepository penalidadeRepository) {
        this.aluguelReservaRepository = aluguelReservaRepository;
        this.clienteRepository = clienteRepository;
        this.dependenteRepository = dependenteRepository;
        this.jogoRepository = jogoRepository;
        this.penalidadeRepository = penalidadeRepository;
    }

    @Transactional
    public AluguelReserva reservar(String cpfCliente, List<Integer> idsJogos) {
        return solicitarReserva(cpfCliente, null, idsJogos);
    }

    @Transactional
    public AluguelReserva solicitarReserva(String cpfCliente, Integer idDependente, List<Integer> idsJogos) {
        Cliente cliente = clienteRepository.findById(cpfCliente)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + cpfCliente));
        validarContaAtiva(cliente);

        if (idsJogos == null || idsJogos.isEmpty()) {
            throw new IllegalArgumentException("É necessário selecionar pelo menos um jogo");
        }

        if (idsJogos.size() > LIMITE_JOGOS_POR_CLIENTE) {
            throw new IllegalArgumentException("Limite máximo de 4 jogos por cliente");
        }
        if (Set.copyOf(idsJogos).size() != idsJogos.size()) {
            throw new IllegalArgumentException("A solicitação não pode conter jogos duplicados");
        }
        if (idDependente != null) {
            Dependente dependente = dependenteRepository.findById(idDependente)
                    .orElseThrow(() -> new IllegalArgumentException("Dependente não encontrado: " + idDependente));
            if (!dependente.getCliente().getCpf().equals(cpfCliente)) {
                throw new IllegalArgumentException("Dependente não pertence ao cliente informado");
            }
        }

        List<Jogo> jogos = new ArrayList<>();
        BigDecimal valorBruto = BigDecimal.ZERO;
        for (Integer idJogo : idsJogos) {
            Jogo jogo = jogoRepository.findById(idJogo)
                    .orElseThrow(() -> new IllegalArgumentException("Jogo não encontrado: " + idJogo));
            if (!jogo.isAtivoCatalogo()) {
                throw new IllegalArgumentException("Jogo não encontrado no catálogo: " + idJogo);
            }
            if (jogo.getQuantidadeEstoque() <= 0) {
                throw new IllegalArgumentException("Jogo indisponível no estoque: " + jogo.getNome());
            }
            if (jogo.getPrecoLocacao() == null || jogo.getPrecoLocacao().signum() <= 0) {
                throw new IllegalArgumentException("Preço de locação inválido para o jogo: " + jogo.getNome());
            }
            if (jogo.getValorReposicao() == null || jogo.getValorReposicao().signum() <= 0) {
                throw new IllegalArgumentException("Valor de reposição inválido para o jogo: " + jogo.getNome());
            }
            valorBruto = valorBruto.add(jogo.getPrecoLocacao());
            jogos.add(jogo);
        }

        validarLimiteAtivo(cpfCliente, jogos.size());
        boolean aguardandoAprovacao = idDependente != null;
        BigDecimal valorComDesconto = calcularValorComDesconto(valorBruto, jogos.size());

        AluguelReserva aluguel = new AluguelReserva();
        aluguel.setCliente(cliente);
        if (idDependente != null) {
            aluguel.setDependente(dependenteRepository.findById(idDependente).orElseThrow());
        }
        aluguel.setDataHoraReserva(LocalDateTime.now());
        aluguel.setDataLimiteRetirada(LocalDateTime.now().plusHours(PRAZO_RESERVA_HORAS));
        aluguel.setStatus(aguardandoAprovacao ? StatusAluguel.AGUARDANDO_APROVACAO : StatusAluguel.RESERVADO);
        aluguel.setValorTotal(valorComDesconto.doubleValue());
        aluguel.setCreditoAplicado(BigDecimal.ZERO);
        aluguel.setJogos(jogos);
        aluguel.setQuantidadeRenovacoes(0);
        aluguel.setTermosContrato(TERMOS_CONTRATO);

        if (!aguardandoAprovacao) {
            aplicarCreditoCarteira(cliente, aluguel, valorComDesconto);
            reservarEstoque(jogos);
        }

        AluguelReserva reservaSalva = aluguelReservaRepository.save(aluguel);
        for (Jogo jogo : jogos) {
            jogo.getAlugueis().add(reservaSalva);
            jogoRepository.save(jogo);
        }
        return reservaSalva;
    }

    @Transactional
    public AluguelReserva aprovarReserva(Integer idAluguel, String cpfTitular) {
        AluguelReserva reserva = buscarReserva(idAluguel);
        validarTitular(reserva, cpfTitular);
        validarContaAtiva(reserva.getCliente());
        if (reserva.getStatus() != StatusAluguel.AGUARDANDO_APROVACAO) {
            throw new IllegalStateException("A reserva não está aguardando aprovação");
        }
        validarLimiteAtivo(cpfTitular, reserva.getJogos().size());
        for (Jogo jogo : reserva.getJogos()) {
            if (jogo.getQuantidadeEstoque() <= 0) {
                throw new IllegalArgumentException("Jogo indisponível no estoque: " + jogo.getNome());
            }
        }

        BigDecimal valorBruto = BigDecimal.ZERO;
        for (Jogo jogo : reserva.getJogos()) {
            valorBruto = valorBruto.add(jogo.getPrecoLocacao());
        }
        BigDecimal valorComDesconto = calcularValorComDesconto(valorBruto, reserva.getJogos().size());
        aplicarCreditoCarteira(reserva.getCliente(), reserva, valorComDesconto);
        reserva.setStatus(StatusAluguel.RESERVADO);
        reserva.setDataLimiteRetirada(LocalDateTime.now().plusHours(PRAZO_RESERVA_HORAS));
        reservarEstoque(reserva.getJogos());
        return aluguelReservaRepository.save(reserva);
    }

    @Transactional
    public int expirarReservasVencidas(LocalDateTime agora) {
        List<AluguelReserva> reservasVencidas = aluguelReservaRepository
                .findByStatusAndDataLimiteRetiradaBefore(StatusAluguel.RESERVADO, agora);

        for (AluguelReserva reserva : reservasVencidas) {
            reserva.setStatus(StatusAluguel.EXPIRADO);
            BigDecimal creditoDevolvido = BigDecimal.valueOf(reserva.getValorTotal())
                    .add(reserva.getCreditoAplicado() == null ? BigDecimal.ZERO : reserva.getCreditoAplicado());
            reserva.getCliente().setCreditoCarteira(valorCarteira(reserva.getCliente()).add(creditoDevolvido));
            clienteRepository.save(reserva.getCliente());
            restaurarEstoque(reserva);
        }

        aluguelReservaRepository.saveAll(reservasVencidas);
        return reservasVencidas.size();
    }

    @Transactional
    public AluguelReserva cancelarReserva(Integer idAluguel, String cpfTitular) {
        AluguelReserva reserva = buscarReserva(idAluguel);
        validarTitular(reserva, cpfTitular);
        if (reserva.getStatus() == StatusAluguel.RESERVADO) {
            restaurarEstoque(reserva);
            devolverCreditoAplicado(reserva);
        } else if (reserva.getStatus() != StatusAluguel.AGUARDANDO_APROVACAO) {
            throw new IllegalStateException("Somente solicitações pendentes ou reservas aguardando retirada podem ser canceladas");
        }

        reserva.setStatus(StatusAluguel.CANCELADO);
        return aluguelReservaRepository.save(reserva);
    }

    @Transactional
    public AluguelReserva retirarReserva(Integer idAluguel, String cpfTitular) {
        return retirarReserva(idAluguel, cpfTitular, LocalDateTime.now());
    }

    @Transactional
    public AluguelReserva retirarReserva(Integer idAluguel, String cpfTitular, LocalDateTime retirada) {
        AluguelReserva reserva = buscarReserva(idAluguel);
        validarTitular(reserva, cpfTitular);
        if (reserva.getStatus() != StatusAluguel.RESERVADO) {
            throw new IllegalStateException("Somente reservas aguardando retirada podem ser retiradas");
        }
        if (reserva.getDataLimiteRetirada() == null || reserva.getDataLimiteRetirada().isBefore(retirada)) {
            throw new IllegalStateException("O prazo para retirada da reserva expirou");
        }

        reserva.setStatus(StatusAluguel.RETIRADO);
        reserva.setDataLimiteDevolucao(retirada.plusDays(PRAZO_ALUGUEL_DIAS));
        reserva.setDataAssinatura(retirada);
        return aluguelReservaRepository.save(reserva);
    }

    @Transactional
    public AluguelReserva renovarAluguel(Integer idAluguel, String cpfTitular, LocalDateTime agora) {
        AluguelReserva reserva = buscarReserva(idAluguel);
        validarTitular(reserva, cpfTitular);
        if (reserva.getStatus() != StatusAluguel.RETIRADO
                || reserva.getQuantidadeRenovacoes() >= 1
                || reserva.getDataLimiteDevolucao() == null
                || reserva.getDataLimiteDevolucao().isBefore(agora)) {
            throw new IllegalStateException("Este aluguel não pode ser renovado");
        }
        reserva.setDataLimiteDevolucao(reserva.getDataLimiteDevolucao().plusDays(PRAZO_ALUGUEL_DIAS));
        reserva.setQuantidadeRenovacoes(reserva.getQuantidadeRenovacoes() + 1);
        return aluguelReservaRepository.save(reserva);
    }

    @Transactional
    public AluguelReserva devolver(Integer idAluguel, LocalDateTime devolucao) {
        AluguelReserva reserva = buscarReserva(idAluguel);
        if ((reserva.getStatus() != StatusAluguel.RETIRADO && reserva.getStatus() != StatusAluguel.ATRASADO)
                || reserva.getDataLimiteDevolucao() == null) {
            throw new IllegalStateException("Somente jogos retirados podem ser devolvidos");
        }

        long diasAtraso = calcularDiasAtraso(reserva.getDataLimiteDevolucao(), devolucao);
        reserva.setDataDevolucaoReal(devolucao);
        reserva.setStatus(StatusAluguel.DEVOLVIDO);
        if (diasAtraso > 0) {
            Penalidade penalidade = atualizarPenalidadeAtraso(reserva, diasAtraso);
            penalidade.setDataFimSuspensao(devolucao.plusDays(diasAtraso * 2));
            Cliente cliente = reserva.getCliente();
            LocalDateTime dataFim = penalidade.getDataFimSuspensao();
            if (cliente.getDataFimSuspensao() != null && cliente.getDataFimSuspensao().isAfter(dataFim)) {
                dataFim = cliente.getDataFimSuspensao();
            }
            cliente.setDataFimSuspensao(dataFim);
            cliente.setStatusConta(false);
            clienteRepository.save(cliente);
        }
        restaurarEstoque(reserva);
        return aluguelReservaRepository.save(reserva);
    }

    @Transactional
    public int verificarAtrasos(LocalDateTime agora) {
        Collection<StatusAluguel> statusAtivos = List.of(StatusAluguel.RETIRADO, StatusAluguel.ATRASADO);
        List<AluguelReserva> atrasados = aluguelReservaRepository
                .findByStatusInAndDataLimiteDevolucaoBefore(statusAtivos, agora);
        for (AluguelReserva reserva : atrasados) {
            long diasAtraso = calcularDiasAtraso(reserva.getDataLimiteDevolucao(), agora);
            reserva.setStatus(StatusAluguel.ATRASADO);
            atualizarPenalidadeAtraso(reserva, diasAtraso);
        }
        aluguelReservaRepository.saveAll(atrasados);
        return atrasados.size();
    }

    @Transactional(readOnly = true)
    public List<AluguelReserva> listarPrazosGerenciais() {
        return aluguelReservaRepository.findByStatusIn(List.of(
                StatusAluguel.RESERVADO,
                StatusAluguel.RETIRADO,
                StatusAluguel.ATRASADO));
    }

    private Penalidade atualizarPenalidadeAtraso(AluguelReserva reserva, long diasAtraso) {
        BigDecimal total = BigDecimal.ZERO;
        for (Jogo jogo : reserva.getJogos()) {
            if (jogo.getValorReposicao() == null || jogo.getValorReposicao().signum() <= 0) {
                throw new IllegalStateException("Valor de reposição não configurado para o jogo: " + jogo.getNome());
            }
            BigDecimal multaPorJogo = MULTA_POR_JOGO_DIA.multiply(BigDecimal.valueOf(diasAtraso))
                    .min(jogo.getValorReposicao());
            total = total.add(multaPorJogo);
        }

        Penalidade penalidade = penalidadeRepository
                .findByAluguelReservaIdAluguelAndTipoPenalidade(reserva.getIdAluguel(), "ATRASO")
                .orElseGet(Penalidade::new);
        penalidade.setTipoPenalidade("ATRASO");
        penalidade.setValorMulta(total.doubleValue());
        penalidade.setAluguelReserva(reserva);
        reserva.getPenalidades().removeIf(item -> "ATRASO".equals(item.getTipoPenalidade()));
        reserva.getPenalidades().add(penalidade);
        return penalidadeRepository.save(penalidade);
    }

    private long calcularDiasAtraso(LocalDateTime prazo, LocalDateTime momento) {
        if (!momento.isAfter(prazo)) {
            return 0;
        }
        Duration atraso = Duration.between(prazo, momento);
        long segundosAtraso = atraso.getSeconds() + (atraso.getNano() > 0 ? 1 : 0);
        return (segundosAtraso + Duration.ofDays(1).getSeconds() - 1) / Duration.ofDays(1).getSeconds();
    }

    private void validarLimiteAtivo(String cpfCliente, int quantidadeSolicitada) {
        Collection<StatusAluguel> statusAtivos = List.of(
                StatusAluguel.RESERVADO, StatusAluguel.RETIRADO, StatusAluguel.ATRASADO);
        int quantidadeAtiva = aluguelReservaRepository.findByClienteCpfAndStatusIn(cpfCliente, statusAtivos).stream()
                .mapToInt(reserva -> reserva.getJogos().size())
                .sum();
        if (quantidadeAtiva + quantidadeSolicitada > LIMITE_JOGOS_POR_CLIENTE) {
            throw new IllegalArgumentException("Limite máximo de 4 jogos ativos por cliente e dependentes");
        }
    }

    private BigDecimal calcularValorComDesconto(BigDecimal valorBruto, int quantidadeJogos) {
        return valorBruto.multiply(BigDecimal.valueOf(1 - calcularDesconto(quantidadeJogos)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void aplicarCreditoCarteira(Cliente cliente, AluguelReserva reserva, BigDecimal valorComDesconto) {
        BigDecimal saldo = valorCarteira(cliente);
        BigDecimal creditoUsado = saldo.min(valorComDesconto);
        cliente.setCreditoCarteira(saldo.subtract(creditoUsado));
        reserva.setCreditoAplicado(creditoUsado);
        reserva.setValorTotal(valorComDesconto.subtract(creditoUsado).doubleValue());
        clienteRepository.save(cliente);
    }

    private void devolverCreditoAplicado(AluguelReserva reserva) {
        BigDecimal creditoAplicado = reserva.getCreditoAplicado() == null ? BigDecimal.ZERO : reserva.getCreditoAplicado();
        if (creditoAplicado.signum() > 0) {
            Cliente cliente = reserva.getCliente();
            cliente.setCreditoCarteira(valorCarteira(cliente).add(creditoAplicado));
            clienteRepository.save(cliente);
            reserva.setCreditoAplicado(BigDecimal.ZERO);
        }
    }

    private BigDecimal valorCarteira(Cliente cliente) {
        return cliente.getCreditoCarteira() == null ? BigDecimal.ZERO : cliente.getCreditoCarteira();
    }

    private void reservarEstoque(List<Jogo> jogos) {
        for (Jogo jogo : jogos) {
            jogo.setQuantidadeEstoque(jogo.getQuantidadeEstoque() - 1);
            jogo.setStatusDisponibilidade(jogo.getQuantidadeEstoque() > 0);
            jogoRepository.save(jogo);
        }
    }

    private AluguelReserva buscarReserva(Integer idAluguel) {
        return aluguelReservaRepository.findById(idAluguel)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada: " + idAluguel));
    }

    private void validarTitular(AluguelReserva reserva, String cpfTitular) {
        if (!reserva.getCliente().getCpf().equals(cpfTitular)) {
            throw new IllegalArgumentException("Somente o titular pode realizar esta operação");
        }
    }

    private void validarContaAtiva(Cliente cliente) {
        if (!cliente.isStatusConta()) {
            throw new IllegalStateException("A conta do cliente está suspensa");
        }
    }

    private void restaurarEstoque(AluguelReserva reserva) {
        for (Jogo jogo : reserva.getJogos()) {
            jogo.setQuantidadeEstoque(jogo.getQuantidadeEstoque() + 1);
            jogo.setStatusDisponibilidade(true);
            jogoRepository.save(jogo);
        }
    }

    private double calcularDesconto(int quantidadeJogos) {
        if (quantidadeJogos >= 4) {
            return 0.25;
        }
        if (quantidadeJogos == 3) {
            return 0.20;
        }
        if (quantidadeJogos == 2) {
            return 0.10;
        }
        return 0.0;
    }
}
