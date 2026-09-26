package projeto.bdd2.dgmaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.JogoRepository;
import projeto.bdd2.dgmaster.service.AluguelService;

import java.time.LocalDate;
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
        jogo1.setQuantidadeEstoque(2);
        jogoRepository.save(jogo1);

        Jogo jogo2 = new Jogo();
        jogo2.setNome("Azul");
        jogo2.setCategoria("Estratégia");
        jogo2.setGenero("Família");
        jogo2.setFaixaEtariaRecomendada(8);
        jogo2.setQuantidadeEstoque(3);
        jogoRepository.save(jogo2);

        var reserva = aluguelService.reservar(cliente.getCpf(), List.of(jogo1.getCodigoJogo(), jogo2.getCodigoJogo()));

        assertThat(reserva.getIdAluguel()).isNotNull();
        assertThat(reserva.getStatus()).isEqualTo(projeto.bdd2.dgmaster.entity.StatusAluguel.RESERVADO);
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
            jogo.setQuantidadeEstoque(2);
            jogoRepository.save(jogo);
        }

        List<Integer> ids = jogoRepository.findAll().stream().map(Jogo::getCodigoJogo).toList();

        assertThatThrownBy(() -> aluguelService.reservar(cliente.getCpf(), ids))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limite máximo");
    }
}
