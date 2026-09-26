package projeto.bdd2.dgmaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Dependente;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.entity.Penalidade;
import projeto.bdd2.dgmaster.repository.AluguelReservaRepository;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.DependenteRepository;
import projeto.bdd2.dgmaster.repository.JogoRepository;
import projeto.bdd2.dgmaster.repository.PenalidadeRepository;
import projeto.bdd2.dgmaster.service.AluguelService;
import projeto.bdd2.dgmaster.service.PenalidadeService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AluguelReservaIntegrationTest {

    @Autowired
    private AluguelService aluguelService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private DependenteRepository dependenteRepository;

    @Autowired
    private AluguelReservaRepository aluguelReservaRepository;

    @Autowired
    private PenalidadeRepository penalidadeRepository;

    @Autowired
    private PenalidadeService penalidadeService;

    @Autowired
    private JogoRepository jogoRepository;

    @Test
    void shouldReserveGamesForClientWithinLimit() {
        Cliente cliente = new Cliente();
        cliente.setCpf("333444555" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
        cliente.setNome("Cliente Reserva");
        cliente.setEmail("reserva." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha123");
        cliente.setDataNascimento(LocalDate.of(1995, 2, 14));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);

        Jogo jogo1 = new Jogo();
        jogo1.setNome("Carcassonne");
        jogo1.setCategoria("Estratégia");
        jogo1.setGenero("Família");
        jogo1.setFaixaEtariaRecomendada(10);
        jogo1.setPrecoLocacao(new BigDecimal("40.00"));
        jogo1.setValorReposicao(new BigDecimal("100.00"));
        jogo1.setQuantidadeEstoque(2);
        jogoRepository.save(jogo1);

        Jogo jogo2 = new Jogo();
        jogo2.setNome("Azul");
        jogo2.setCategoria("Estratégia");
        jogo2.setGenero("Família");
        jogo2.setFaixaEtariaRecomendada(8);
        jogo2.setPrecoLocacao(new BigDecimal("25.00"));
        jogo2.setValorReposicao(new BigDecimal("100.00"));
        jogo2.setQuantidadeEstoque(3);
        jogoRepository.save(jogo2);

        var reserva = aluguelService.reservar(cliente.getCpf(), List.of(jogo1.getCodigoJogo(), jogo2.getCodigoJogo()));

        assertThat(reserva.getIdAluguel()).isNotNull();
        assertThat(reserva.getStatus()).isEqualTo(projeto.bdd2.dgmaster.entity.StatusAluguel.RESERVADO);
        assertThat(reserva.getTermosContrato()).contains("Locatário titular");
        assertThat(reserva.getValorTotal()).isPositive();
    }

    @Test
    void shouldApplyProgressiveDiscountForMultipleGames() {
        Cliente cliente = new Cliente();
        cliente.setCpf("444555666" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
        cliente.setNome("Cliente Desconto");
        cliente.setEmail("desconto." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha123");
        cliente.setDataNascimento(LocalDate.of(1992, 8, 9));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        List<Integer> ids = new java.util.ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            Jogo jogo = new Jogo();
            jogo.setNome("Jogo Desconto " + uniqueSuffix + " " + i);
            jogo.setCategoria("Estratégia");
            jogo.setGenero("Família");
            jogo.setFaixaEtariaRecomendada(12);
            jogo.setPrecoLocacao(new BigDecimal(i == 1 ? "10.50" : i == 2 ? "20.25" : "34.25"));
            jogo.setValorReposicao(new BigDecimal("100.00"));
            jogo.setQuantidadeEstoque(4);
            jogoRepository.save(jogo);
            ids.add(jogo.getCodigoJogo());
        }

        var reserva = aluguelService.reservar(cliente.getCpf(), ids);

        assertThat(reserva.getValorTotal()).isEqualTo(52.00);
    }

    @Test
    void shouldRejectReservationWhenGameLimitExceeded() {
        Cliente cliente = new Cliente();
        cliente.setCpf("666777888" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
        cliente.setNome("Cliente Limite");
        cliente.setEmail("limite." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha123");
        cliente.setDataNascimento(LocalDate.of(1993, 5, 21));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);

        for (int i = 1; i <= 5; i++) {
            Jogo jogo = new Jogo();
            jogo.setNome("Jogo " + i);
            jogo.setCategoria("Lógica");
            jogo.setGenero("Indefinido");
            jogo.setFaixaEtariaRecomendada(10);
            jogo.setPrecoLocacao(new BigDecimal("35.00"));
            jogo.setValorReposicao(new BigDecimal("100.00"));
            jogo.setQuantidadeEstoque(2);
            jogoRepository.save(jogo);
        }

        List<Integer> ids = new java.util.ArrayList<>();
        for (Jogo jogo : jogoRepository.findAll()) {
            ids.add(jogo.getCodigoJogo());
        }

        assertThatThrownBy(() -> aluguelService.reservar(cliente.getCpf(), ids))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limite máximo");
    }

    @Test
    void shouldExpireUnclaimedReservationAndRestoreGameStock() {
        Cliente cliente = new Cliente();
        cliente.setCpf("777888999" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
        cliente.setNome("Cliente Expiração");
        cliente.setEmail("expiracao." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha123");
        cliente.setDataNascimento(LocalDate.of(1990, 1, 1));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);

        Jogo jogo = new Jogo();
        jogo.setNome("Jogo Expiração " + UUID.randomUUID());
        jogo.setPrecoLocacao(new BigDecimal("30.00"));
        jogo.setValorReposicao(new BigDecimal("100.00"));
        jogo.setQuantidadeEstoque(1);
        jogoRepository.save(jogo);

        var reserva = aluguelService.reservar(cliente.getCpf(), List.of(jogo.getCodigoJogo()));
        LocalDateTime depoisDoPrazo = reserva.getDataLimiteRetirada().plusSeconds(1);

        int expiradas = aluguelService.expirarReservasVencidas(depoisDoPrazo);

        assertThat(expiradas).isGreaterThanOrEqualTo(1);
        assertThat(aluguelReservaRepository.findById(reserva.getIdAluguel()).orElseThrow().getStatus())
                .isEqualTo(projeto.bdd2.dgmaster.entity.StatusAluguel.EXPIRADO);
        assertThat(jogoRepository.findById(jogo.getCodigoJogo()).orElseThrow().getQuantidadeEstoque()).isEqualTo(1);
    }

    @Test
    void shouldCancelReservationAndRestoreGameStock() {
        Cliente cliente = new Cliente();
        cliente.setCpf("888999000" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
        cliente.setNome("Cliente Cancelamento");
        cliente.setEmail("cancelamento." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha123");
        cliente.setDataNascimento(LocalDate.of(1991, 3, 1));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);

        Jogo jogo = new Jogo();
        jogo.setNome("Jogo Cancelamento " + UUID.randomUUID());
        jogo.setPrecoLocacao(new BigDecimal("30.00"));
        jogo.setValorReposicao(new BigDecimal("100.00"));
        jogo.setQuantidadeEstoque(1);
        jogoRepository.save(jogo);

        var reserva = aluguelService.reservar(cliente.getCpf(), List.of(jogo.getCodigoJogo()));

        aluguelService.cancelarReserva(reserva.getIdAluguel(), cliente.getCpf());

        assertThat(aluguelReservaRepository.findById(reserva.getIdAluguel()).orElseThrow().getStatus())
                .isEqualTo(projeto.bdd2.dgmaster.entity.StatusAluguel.CANCELADO);
        assertThatThrownBy(() -> aluguelService.cancelarReserva(reserva.getIdAluguel(), cliente.getCpf()))
            .isInstanceOf(IllegalStateException.class);
        assertThat(jogoRepository.findById(jogo.getCodigoJogo()).orElseThrow().getQuantidadeEstoque()).isEqualTo(1);
    }

    @Test
    void shouldMarkReservationAsPickedUpWithoutChangingStock() {
        Cliente cliente = new Cliente();
        cliente.setCpf("999000111" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
        cliente.setNome("Cliente Retirada");
        cliente.setEmail("retirada." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha123");
        cliente.setDataNascimento(LocalDate.of(1989, 4, 1));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);

        Jogo jogo = new Jogo();
        jogo.setNome("Jogo Retirada " + UUID.randomUUID());
        jogo.setPrecoLocacao(new BigDecimal("30.00"));
        jogo.setValorReposicao(new BigDecimal("100.00"));
        jogo.setQuantidadeEstoque(2);
        jogoRepository.save(jogo);

        var reserva = aluguelService.reservar(cliente.getCpf(), List.of(jogo.getCodigoJogo()));

        aluguelService.retirarReserva(reserva.getIdAluguel(), cliente.getCpf());

        assertThat(aluguelReservaRepository.findById(reserva.getIdAluguel()).orElseThrow().getStatus())
                .isEqualTo(projeto.bdd2.dgmaster.entity.StatusAluguel.RETIRADO);
        assertThat(jogoRepository.findById(jogo.getCodigoJogo()).orElseThrow().getQuantidadeEstoque()).isEqualTo(1);
        assertThatThrownBy(() -> aluguelService.cancelarReserva(reserva.getIdAluguel(), cliente.getCpf()))
                .isInstanceOf(IllegalStateException.class);
    }

        @Test
        void shouldSetSevenDayRentalPeriodAndAllowOnlyOneRenewal() {
        Cliente cliente = criarCliente("prazo");
        Jogo jogo = criarJogo("Prazo", new BigDecimal("30.00"), new BigDecimal("100.00"), 1);
        var reserva = aluguelService.reservar(cliente.getCpf(), List.of(jogo.getCodigoJogo()));
        LocalDateTime retirada = LocalDateTime.now().withNano(0).plusSeconds(1);

        aluguelService.retirarReserva(reserva.getIdAluguel(), cliente.getCpf(), retirada);

        var aluguelRetirado = aluguelReservaRepository.findById(reserva.getIdAluguel()).orElseThrow();
        assertThat(aluguelRetirado.getDataLimiteDevolucao()).isEqualTo(retirada.plusDays(7));

        var aluguelRenovado = aluguelService.renovarAluguel(reserva.getIdAluguel(), cliente.getCpf(), retirada.plusDays(2));

        assertThat(aluguelRenovado.getDataLimiteDevolucao()).isEqualTo(retirada.plusDays(14));
        assertThat(aluguelRenovado.getQuantidadeRenovacoes()).isEqualTo(1);
        assertThatThrownBy(() -> aluguelService.renovarAluguel(reserva.getIdAluguel(), cliente.getCpf(), retirada.plusDays(3)))
            .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void shouldCapLateFeePerGameAndReactivateOnlyAfterSuspensionAndPayment() {
        Cliente cliente = criarCliente("multa");
        Jogo jogoAbaixoDoTeto = criarJogo("Teto baixo", new BigDecimal("30.00"), new BigDecimal("8.00"), 1);
        Jogo jogoAcimaDoTeto = criarJogo("Teto alto", new BigDecimal("30.00"), new BigDecimal("100.00"), 1);
        var reserva = aluguelService.reservar(cliente.getCpf(),
            List.of(jogoAbaixoDoTeto.getCodigoJogo(), jogoAcimaDoTeto.getCodigoJogo()));
        LocalDateTime retirada = LocalDateTime.now().withNano(0).plusSeconds(1);
        aluguelService.retirarReserva(reserva.getIdAluguel(), cliente.getCpf(), retirada);
        LocalDateTime prazoDevolucao = retirada.plusDays(7);
        aluguelService.verificarAtrasos(prazoDevolucao.plusHours(1));
        assertThat(aluguelReservaRepository.findById(reserva.getIdAluguel()).orElseThrow().getStatus())
            .isEqualTo(projeto.bdd2.dgmaster.entity.StatusAluguel.ATRASADO);
        LocalDateTime devolucaoAtrasada = prazoDevolucao.plusDays(2);

        var devolvido = aluguelService.devolver(reserva.getIdAluguel(), devolucaoAtrasada);
        Penalidade multa = penalidadeRepository.findByAluguelReservaIdAluguel(reserva.getIdAluguel()).get(0);

        assertThat(devolvido.getStatus()).isEqualTo(projeto.bdd2.dgmaster.entity.StatusAluguel.DEVOLVIDO);
        assertThat(devolvido.getDataDevolucaoReal()).isEqualTo(devolucaoAtrasada);
        assertThat(multa.getValorMulta()).isEqualTo(18.00);
        assertThat(multa.getDataFimSuspensao()).isEqualTo(devolucaoAtrasada.plusDays(4));
        assertThat(clienteRepository.findById(cliente.getCpf()).orElseThrow().isStatusConta()).isFalse();

        penalidadeService.registrarPagamentoMulta(multa.getIdPenalidade());
        penalidadeService.reativarContasElegiveis(devolucaoAtrasada.plusDays(3));
        assertThat(clienteRepository.findById(cliente.getCpf()).orElseThrow().isStatusConta()).isFalse();

        penalidadeService.reativarContasElegiveis(devolucaoAtrasada.plusDays(4));
        assertThat(clienteRepository.findById(cliente.getCpf()).orElseThrow().isStatusConta()).isTrue();
        }

        @Test
        void shouldConvertExpiredReservationToWalletCreditAndUseItOnNextRental() {
        Cliente cliente = criarCliente("carteira");
        Jogo jogo = criarJogo("Carteira", new BigDecimal("30.00"), new BigDecimal("100.00"), 1);
        var primeiraReserva = aluguelService.reservar(cliente.getCpf(), List.of(jogo.getCodigoJogo()));

        aluguelService.expirarReservasVencidas(primeiraReserva.getDataLimiteRetirada().plusSeconds(1));

        assertThat(clienteRepository.findById(cliente.getCpf()).orElseThrow().getCreditoCarteira())
            .isEqualByComparingTo("30.00");

        var segundaReserva = aluguelService.reservar(cliente.getCpf(), List.of(jogo.getCodigoJogo()));

        assertThat(segundaReserva.getValorTotal()).isEqualTo(0.0);
        assertThat(clienteRepository.findById(cliente.getCpf()).orElseThrow().getCreditoCarteira())
            .isEqualByComparingTo("0.00");
        }

        @Test
        void shouldRequireTitularApprovalForDependentReservation() {
        Cliente cliente = criarCliente("dependente");
        Dependente dependente = new Dependente();
        dependente.setNome("Dependente Teste");
        dependente.setDataNascimento(LocalDate.of(2012, 1, 1));
        dependente.setCliente(cliente);
        dependenteRepository.save(dependente);
        Jogo jogo = criarJogo("Aprovação", new BigDecimal("30.00"), new BigDecimal("100.00"), 1);

        var solicitacao = aluguelService.solicitarReserva(cliente.getCpf(), dependente.getIdDependente(),
            List.of(jogo.getCodigoJogo()));

        assertThat(solicitacao.getStatus())
            .isEqualTo(projeto.bdd2.dgmaster.entity.StatusAluguel.AGUARDANDO_APROVACAO);
        assertThat(jogoRepository.findById(jogo.getCodigoJogo()).orElseThrow().getQuantidadeEstoque()).isEqualTo(1);
        aluguelService.expirarReservasVencidas(LocalDateTime.now().plusDays(2));
        assertThat(aluguelReservaRepository.findById(solicitacao.getIdAluguel()).orElseThrow().getStatus())
            .isEqualTo(projeto.bdd2.dgmaster.entity.StatusAluguel.AGUARDANDO_APROVACAO);

        assertThatThrownBy(() -> aluguelService.aprovarReserva(solicitacao.getIdAluguel(), "00000000000"))
            .isInstanceOf(IllegalArgumentException.class);
        var aprovada = aluguelService.aprovarReserva(solicitacao.getIdAluguel(), cliente.getCpf());

        assertThat(aprovada.getStatus()).isEqualTo(projeto.bdd2.dgmaster.entity.StatusAluguel.RESERVADO);
        assertThat(aprovada.getDataLimiteRetirada()).isNotNull();
        assertThat(jogoRepository.findById(jogo.getCodigoJogo()).orElseThrow().getQuantidadeEstoque()).isZero();
        assertThatThrownBy(() -> aluguelService.retirarReserva(aprovada.getIdAluguel(), "00000000000"))
            .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldCountExistingRentalsTowardTheFourGameLimit() {
        Cliente cliente = criarCliente("limite-acumulado");
        List<Jogo> jogos = List.of(
            criarJogo("Limite 1", new BigDecimal("30.00"), new BigDecimal("100.00"), 1),
            criarJogo("Limite 2", new BigDecimal("30.00"), new BigDecimal("100.00"), 1),
            criarJogo("Limite 3", new BigDecimal("30.00"), new BigDecimal("100.00"), 1),
            criarJogo("Limite 4", new BigDecimal("30.00"), new BigDecimal("100.00"), 1),
            criarJogo("Limite 5", new BigDecimal("30.00"), new BigDecimal("100.00"), 1));

        aluguelService.reservar(cliente.getCpf(), idsDosJogos(jogos.subList(0, 3)));

        assertThatThrownBy(() -> aluguelService.reservar(cliente.getCpf(),
            idsDosJogos(jogos.subList(3, 5))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Limite máximo");
        }

        private Cliente criarCliente(String prefixo) {
        Cliente cliente = new Cliente();
        cliente.setCpf("123456789" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
        cliente.setNome("Cliente " + prefixo);
        cliente.setEmail(prefixo + "." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha123");
        cliente.setDataNascimento(LocalDate.of(1990, 1, 1));
        cliente.setStatusConta(true);
        return clienteRepository.save(cliente);
        }

        private Jogo criarJogo(String nome, BigDecimal preco, BigDecimal reposicao, int estoque) {
        Jogo jogo = new Jogo();
        jogo.setNome(nome + " " + UUID.randomUUID());
        jogo.setPrecoLocacao(preco);
        jogo.setValorReposicao(reposicao);
        jogo.setQuantidadeEstoque(estoque);
        return jogoRepository.save(jogo);
        }

    private List<Integer> idsDosJogos(List<Jogo> jogos) {
        List<Integer> ids = new java.util.ArrayList<>();
        for (Jogo jogo : jogos) {
            ids.add(jogo.getCodigoJogo());
        }
        return ids;
    }
}
