package projeto.bdd2.dgmaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import projeto.bdd2.dgmaster.aluguel.api.EstatisticasResponse;
import projeto.bdd2.dgmaster.entity.AluguelReserva;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.ItemAluguel;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.entity.StatusAluguel;
import projeto.bdd2.dgmaster.repository.AluguelReservaRepository;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.JogoRepository;
import projeto.bdd2.dgmaster.service.EstatisticasService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EstatisticasServiceIntegrationTest {

    @Autowired
    private EstatisticasService estatisticasService;

    @Autowired
    private JogoRepository jogoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AluguelReservaRepository aluguelReservaRepository;

    @Test
    void deveGerarEstatisticasComDadosReais() {
        Cliente cliente = new Cliente();
        cliente.setCpf("12345678901");
        cliente.setNome("Cliente Teste");
        cliente.setEmail("teste" + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha-hash");
        cliente.setDataNascimento(LocalDate.of(1990, 1, 1));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);

        Jogo jogo1 = criarJogo("Jogo Popular 1", "Estratégia", new BigDecimal("50.00"), new BigDecimal("150.00"));
        Jogo jogo2 = criarJogo("Jogo Popular 2", "Estratégia", new BigDecimal("40.00"), new BigDecimal("120.00"));
        Jogo jogo3 = criarJogo("Jogo Raro 1", "RPG", new BigDecimal("60.00"), new BigDecimal("200.00"));

        jogoRepository.save(jogo1);
        jogoRepository.save(jogo2);
        jogoRepository.save(jogo3);

        AluguelReserva aluguel1 = criarAluguel(cliente, StatusAluguel.DEVOLVIDO);
        AluguelReserva aluguel2 = criarAluguel(cliente, StatusAluguel.DEVOLVIDO);
        AluguelReserva aluguel3 = criarAluguel(cliente, StatusAluguel.DEVOLVIDO);

        aluguelReservaRepository.save(aluguel1);
        aluguelReservaRepository.save(aluguel2);
        aluguelReservaRepository.save(aluguel3);

        ItemAluguel item1 = new ItemAluguel(jogo1, aluguel1, new BigDecimal("45.00"));
        ItemAluguel item2 = new ItemAluguel(jogo1, aluguel2, new BigDecimal("45.00"));
        ItemAluguel item3 = new ItemAluguel(jogo2, aluguel3, new BigDecimal("36.00"));
        ItemAluguel item4 = new ItemAluguel(jogo3, aluguel1, new BigDecimal("54.00"));

        aluguel1.getItensAluguel().add(item1);
        aluguel2.getItensAluguel().add(item2);
        aluguel3.getItensAluguel().add(item3);
        aluguel1.getItensAluguel().add(item4);

        aluguelReservaRepository.save(aluguel1);
        aluguelReservaRepository.save(aluguel2);
        aluguelReservaRepository.save(aluguel3);

        EstatisticasResponse estatisticas = estatisticasService.gerarEstatisticasCompletas();

        assertThat(estatisticas).isNotNull();
        assertThat(estatisticas.getJogosMaisAlugados()).isNotEmpty();
        assertThat(estatisticas.getJogosMenosAlugados()).isNotEmpty();
        assertThat(estatisticas.getJogosMaisRentaveis()).isNotEmpty();
        assertThat(estatisticas.getJogosMenosRentaveis()).isNotEmpty();

        assertThat(estatisticas.getJogosMaisAlugados().get(0).getTotalAlugueis()).isGreaterThan(0);
        assertThat(estatisticas.getJogosMaisRentaveis().get(0).getReceitaTotal()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void deveRetornarListasVaziasQuandoSemDados() {
        EstatisticasResponse estatisticas = estatisticasService.gerarEstatisticasCompletas();

        assertThat(estatisticas).isNotNull();
        assertThat(estatisticas.getJogosMaisAlugados()).isNotNull();
        assertThat(estatisticas.getJogosMenosAlugados()).isNotNull();
        assertThat(estatisticas.getJogosMaisRentaveis()).isNotNull();
        assertThat(estatisticas.getJogosMenosRentaveis()).isNotNull();
    }

    private Jogo criarJogo(String nome, String categoria, BigDecimal precoLocacao, BigDecimal valorReposicao) {
        Jogo jogo = new Jogo();
        jogo.setNome(nome);
        jogo.setCategoria(categoria);
        jogo.setPrecoLocacao(precoLocacao);
        jogo.setValorReposicao(valorReposicao);
        jogo.setQuantidadeEstoque(5);
        jogo.setStatusDisponibilidade(true);
        jogo.setAtivoCatalogo(true);
        return jogo;
    }

    private AluguelReserva criarAluguel(Cliente cliente, StatusAluguel status) {
        AluguelReserva aluguel = new AluguelReserva();
        aluguel.setCliente(cliente);
        aluguel.setDataHoraReserva(LocalDateTime.now());
        aluguel.setDataLimiteRetirada(LocalDateTime.now().plusHours(24));
        aluguel.setStatus(status);
        aluguel.setValorTotal(100.0);
        aluguel.setDataDevolucaoReal(LocalDateTime.now());
        return aluguel;
    }
}
