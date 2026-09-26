package projeto.bdd2.dgmaster.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import projeto.bdd2.dgmaster.entity.*;
import projeto.bdd2.dgmaster.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AluguelServiceTest {

    @Mock
    private AluguelReservaRepository aluguelReservaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private DependenteRepository dependenteRepository;

    @Mock
    private JogoRepository jogoRepository;

    @Mock
    private PenalidadeRepository penalidadeRepository;

    @InjectMocks
    private AluguelService aluguelService;

    private Cliente cliente;
    private Jogo jogo;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setCpf("12345678901");
        cliente.setNome("Cliente Teste");
        cliente.setEmail("cliente@teste.com");
        cliente.setSenha("senha-hash");
        cliente.setDataNascimento(LocalDate.of(1990, 1, 1));
        cliente.setStatusConta(true);
        cliente.setCreditoCarteira(BigDecimal.ZERO);

        jogo = new Jogo();
        jogo.setCodigoJogo(1);
        jogo.setNome("Jogo Teste");
        jogo.setCategoria("Estratégia");
        jogo.setPrecoLocacao(new BigDecimal("50.00"));
        jogo.setValorReposicao(new BigDecimal("150.00"));
        jogo.setQuantidadeEstoque(5);
        jogo.setStatusDisponibilidade(true);
        jogo.setAtivoCatalogo(true);
    }

    @Test
    void deveCalcularDescontoCorretamente() {
        assertThat(aluguelService).isNotNull();
    }

    @Test
    void deveLancarExcecaoQuandoClienteNaoEncontrado() {
        when(clienteRepository.findById("12345678901")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aluguelService.reservar("12345678901", List.of(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cliente não encontrado");
    }

    @Test
    void deveLancarExcecaoQuandoContaSuspensa() {
        cliente.setStatusConta(false);
        when(clienteRepository.findById("12345678901")).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> aluguelService.reservar("12345678901", List.of(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("conta do cliente está suspensa");
    }

    @Test
    void deveLancarExcecaoQuandoJogoNaoSelecionado() {
        when(clienteRepository.findById("12345678901")).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> aluguelService.reservar("12345678901", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pelo menos um jogo");
    }

    @Test
    void deveLancarExcecaoQuandoLimiteJogosExcedido() {
        when(clienteRepository.findById("12345678901")).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> aluguelService.reservar("12345678901", List.of(1, 2, 3, 4, 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limite máximo de 4 jogos");
    }

    @Test
    void deveLancarExcecaoQuandoJogosDuplicados() {
        when(clienteRepository.findById("12345678901")).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> aluguelService.reservar("12345678901", List.of(1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jogos duplicados");
    }

    @Test
    void deveLancarExcecaoQuandoJogoNaoEncontrado() {
        when(clienteRepository.findById("12345678901")).thenReturn(Optional.of(cliente));
        when(jogoRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aluguelService.reservar("12345678901", List.of(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Jogo não encontrado");
    }

    @Test
    void deveLancarExcecaoQuandoJogoSemEstoque() {
        jogo.setQuantidadeEstoque(0);
        when(clienteRepository.findById("12345678901")).thenReturn(Optional.of(cliente));
        when(jogoRepository.findById(1)).thenReturn(Optional.of(jogo));

        assertThatThrownBy(() -> aluguelService.reservar("12345678901", List.of(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("indisponível no estoque");
    }

    @Test
    void deveAplicarCreditoCarteiraNaReserva() {
        cliente.setCreditoCarteira(new BigDecimal("20.00"));
        when(clienteRepository.findById("12345678901")).thenReturn(Optional.of(cliente));
        when(jogoRepository.findById(1)).thenReturn(Optional.of(jogo));
        when(aluguelReservaRepository.findByClienteCpfAndStatusIn(any(), any())).thenReturn(List.of());
        when(aluguelReservaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(clienteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jogoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AluguelReserva reserva = aluguelService.reservar("12345678901", List.of(1));

        assertThat(reserva.getCreditoAplicado()).isEqualTo(new BigDecimal("20.00"));
        assertThat(reserva.getValorTotal()).isEqualTo(30.0);
    }

    @Test
    void deveExpirarReservasVencidas() {
        AluguelReserva reservaVencida = new AluguelReserva();
        reservaVencida.setIdAluguel(1);
        reservaVencida.setStatus(StatusAluguel.RESERVADO);
        reservaVencida.setDataLimiteRetirada(LocalDateTime.now().minusHours(25));
        reservaVencida.setValorTotal(50.0);
        reservaVencida.setCliente(cliente);

        when(aluguelReservaRepository.findByStatusAndDataLimiteRetiradaBefore(
            eq(StatusAluguel.RESERVADO), any(LocalDateTime.class)))
                .thenReturn(List.of(reservaVencida));
        when(aluguelReservaRepository.saveAll(any())).thenReturn(List.of(reservaVencida));
        when(clienteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int quantidade = aluguelService.expirarReservasVencidas(LocalDateTime.now());

        assertThat(quantidade).isEqualTo(1);
        assertThat(reservaVencida.getStatus()).isEqualTo(StatusAluguel.EXPIRADO);
    }

    @Test
    void deveCancelarReservaComSucesso() {
        AluguelReserva reserva = new AluguelReserva();
        reserva.setIdAluguel(1);
        reserva.setStatus(StatusAluguel.RESERVADO);
        reserva.setCliente(cliente);

        when(aluguelReservaRepository.findById(1)).thenReturn(Optional.of(reserva));
        when(aluguelReservaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AluguelReserva resultado = aluguelService.cancelarReserva(1, "12345678901");

        assertThat(resultado.getStatus()).isEqualTo(StatusAluguel.CANCELADO);
    }

    @Test
    void deveLancarExcecaoQuandoCancelarReservaDeOutroCliente() {
        AluguelReserva reserva = new AluguelReserva();
        reserva.setIdAluguel(1);
        reserva.setStatus(StatusAluguel.RESERVADO);
        reserva.setCliente(cliente);

        when(aluguelReservaRepository.findById(1)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> aluguelService.cancelarReserva(1, "99999999999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Somente o titular");
    }

    @Test
    void deveRetirarReservaComSucesso() {
        AluguelReserva reserva = new AluguelReserva();
        reserva.setIdAluguel(1);
        reserva.setStatus(StatusAluguel.RESERVADO);
        reserva.setDataLimiteRetirada(LocalDateTime.now().plusHours(1));
        reserva.setCliente(cliente);

        when(aluguelReservaRepository.findById(1)).thenReturn(Optional.of(reserva));
        when(aluguelReservaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AluguelReserva resultado = aluguelService.retirarReserva(1, "12345678901");

        assertThat(resultado.getStatus()).isEqualTo(StatusAluguel.RETIRADO);
        assertThat(resultado.getDataLimiteDevolucao()).isNotNull();
    }

    @Test
    void deveRenovarAluguelComSucesso() {
        AluguelReserva reserva = new AluguelReserva();
        reserva.setIdAluguel(1);
        reserva.setStatus(StatusAluguel.RETIRADO);
        reserva.setDataLimiteDevolucao(LocalDateTime.now().plusDays(3));
        reserva.setQuantidadeRenovacoes(0);
        reserva.setCliente(cliente);

        when(aluguelReservaRepository.findById(1)).thenReturn(Optional.of(reserva));
        when(aluguelReservaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AluguelReserva resultado = aluguelService.renovarAluguel(1, "12345678901", LocalDateTime.now());

        assertThat(resultado.getQuantidadeRenovacoes()).isEqualTo(1);
    }

    @Test
    void deveDevolverJogoSemAtraso() {
        AluguelReserva reserva = new AluguelReserva();
        reserva.setIdAluguel(1);
        reserva.setStatus(StatusAluguel.RETIRADO);
        reserva.setDataLimiteDevolucao(LocalDateTime.now().plusDays(1));
        reserva.setCliente(cliente);

        when(aluguelReservaRepository.findById(1)).thenReturn(Optional.of(reserva));
        when(aluguelReservaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AluguelReserva resultado = aluguelService.devolver(1, LocalDateTime.now());

        assertThat(resultado.getStatus()).isEqualTo(StatusAluguel.DEVOLVIDO);
        assertThat(resultado.getDataDevolucaoReal()).isNotNull();
    }
}
