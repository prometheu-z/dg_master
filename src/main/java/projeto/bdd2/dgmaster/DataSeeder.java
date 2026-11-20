package projeto.bdd2.dgmaster;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import projeto.bdd2.dgmaster.entity.Aluguel;
import projeto.bdd2.dgmaster.service.AluguelService;

import java.util.List;

/**
 * Dados de demonstração alinhados ao modelo físico do RFC-003.
 * A criação de reservas/itens usa as procedures oficiais para que as triggers
 * calculem prazos, descontos, valores e movimentação de estoque.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String SENHA_DEMO = "Senha@123";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final AluguelService aluguelService;

    public DataSeeder(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, AluguelService aluguelService) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.aluguelService = aluguelService;
    }

    @Override
    public void run(String... args) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM jogo", Integer.class);
        if (count != null && count > 0) {
            return;
        }

        jdbc.update("INSERT INTO categoria (nome_categoria) VALUES ('Estratégia'), ('Cooperativo'), ('Família'), ('Criatividade')");

        String hash = passwordEncoder.encode(SENHA_DEMO);
        jdbc.update("INSERT INTO cliente (nome, cpf, telefone, senha, data_nascimento, email) VALUES (?,?,?,?,?,?)",
                "Maria Silva", "11144477735", "(11) 99999-0001", hash, "1990-05-15", "maria.silva@email.com");
        jdbc.update("INSERT INTO cliente (nome, cpf, telefone, senha, data_nascimento, email) VALUES (?,?,?,?,?,?)",
                "João Santos", "52998224725", "(11) 99999-0002", hash, "1985-11-22", "joao.santos@email.com");
        jdbc.update("INSERT INTO cliente (nome, cpf, telefone, senha, data_nascimento, email) VALUES (?,?,?,?,?,?)",
                "Ana Costa", "12345678909", "(11) 99999-0003", hash, "1995-08-30", "ana.costa@email.com");
        jdbc.update("INSERT INTO cliente (nome, cpf, telefone, senha, data_nascimento, email) VALUES (?,?,?,?,?,?)",
                "Pedro Lima", "39053344705", "(11) 99999-0004", hash, "1988-01-05", "pedro.lima@email.com");

        jdbc.update("INSERT INTO dependente (id_cliente, nome, data_nascimento, grau_parentesco) "
                        + "SELECT id_cliente, ?, ?, ? FROM cliente WHERE cpf = ?",
                "Lucas Silva", "2015-03-10", "FILHO", "11144477735");

        jdbc.update("INSERT INTO gerente (nome, email, senha, matricula) VALUES (?,?,?,?)",
                "Carlos Gerente", "carlos.gerente@email.com", hash, "GER001");

        semearJogos();

        Integer gerenteId = jdbc.queryForObject(
                "SELECT id_gerente FROM gerente WHERE matricula = 'GER001'", Integer.class);

        // Reserva em aberto (RN3: expira em 24h)
        Aluguel reservaMaria = aluguelService.solicitarReserva(
                "11144477735", null,
                List.of(idJogo("Azul"), idJogo("Dixit")));

        // Aluguel retirado e devolvido (FINALIZADO) — exercita sp_retirar/sp_finalizar
        Aluguel finalizado = aluguelService.solicitarReserva(
                "52998224725", null,
                List.of(idJogo("Catan"), idJogo("Splendor")));
        aluguelService.retirarAluguel(finalizado.getIdAluguel(), gerenteId);
        aluguelService.finalizarAluguel(finalizado.getIdAluguel(), java.time.LocalDate.now());

        // Aluguel retirado pelo gerente (RETIRADO)
        Aluguel retirado = aluguelService.solicitarReserva(
                "12345678909", null, List.of(idJogo("Pandemic")));
        aluguelService.retirarAluguel(retirado.getIdAluguel(), gerenteId);

        // Reserva cancelada (CANCELADO) — trigger devolve o estoque
        Aluguel cancelado = aluguelService.solicitarReserva(
                "39053344705", null, List.of(idJogo("Uno")));
        aluguelService.cancelarReserva(cancelado.getIdAluguel(), "39053344705");

        // Mantém a variável em uso explícito para facilitar depuração do seed.
        if (reservaMaria.getIdAluguel() == null) {
            throw new IllegalStateException("Falha ao semear a reserva de demonstração");
        }
    }

    private void semearJogos() {
        inserirJogo("Azul", "Estratégia", "Abstrato de draft e coleção de padrões.", 8, "25.00", "120.00", 3);
        inserirJogo("Ticket to Ride", "Estratégia", "Construção de rotas ferroviárias.", 8, "35.00", "150.00", 2);
        inserirJogo("Catan", "Estratégia", "Colonização e negociação de recursos.", 10, "40.00", "180.00", 4);
        inserirJogo("Dixit", "Criatividade", "Narrativa e associação de imagens.", 8, "30.00", "130.00", 1);
        inserirJogo("Carcassonne", "Estratégia", "Colocação de tiles e controle de áreas.", 7, "28.00", "110.00", 3);
        inserirJogo("Pandemic", "Cooperativo", "Contenção de pandemias em equipe.", 13, "45.00", "200.00", 2);
        inserirJogo("Codinomes", "Criatividade", "Comunicação por pistas curtas.", 14, "32.00", "140.00", 2);
        inserirJogo("7 Wonders", "Estratégia", "Civilizações e draft de cartas.", 10, "38.00", "160.00", 1);
        inserirJogo("Splendor", "Estratégia", "Motor de gemas e compras.", 10, "30.00", "120.00", 3);
        inserirJogo("King of Tokyo", "Família", "Disputa de monstros em Tóquio.", 8, "35.00", "150.00", 2);
        inserirJogo("Love Letter", "Família", "Dedução leve com poucas cartas.", 10, "22.00", "90.00", 5);
        inserirJogo("Mysterium", "Cooperativo", "Mistério cooperativo de fantasmas.", 10, "42.00", "170.00", 1);
        inserirJogo("Dobble", "Família", "Reação e reconhecimento de símbolos.", 6, "20.00", "85.00", 4);
        inserirJogo("Uno", "Família", "Clássico de combinação de cores e números.", 7, "18.00", "70.00", 6);
        inserirJogo("Magic: The Gathering Starter", "Estratégia", "Duelo de cartas colecionáveis.", 13, "50.00", "220.00", 1);
        inserirJogo("Root", "Estratégia", "Facciosidade em floresta assimétrica.", 10, "55.00", "240.00", 2);
        inserirJogo("Gloomhaven", "Cooperativo", "Campanha tática de fantasia.", 14, "120.00", "500.00", 1);
        inserirJogo("Terraforming Mars", "Estratégia", "Terraformação econômica de Marte.", 12, "80.00", "350.00", 1);
        inserirJogo("Wingspan", "Família", "Coleção de aves e motor de recursos.", 10, "48.00", "190.00", 2);
        inserirJogo("Brass: Birmingham", "Estratégia", "Rede industrial e econômica.", 14, "95.00", "380.00", 1);
    }

    private void inserirJogo(String titulo, String categoria, String descricao, int faixaEtaria,
                             String valorDiaria, String valorReposicao, int estoque) {
        jdbc.update("INSERT INTO jogo (id_categoria, titulo, descricao, faixa_etaria_minima, "
                        + "valor_diaria, valor_reposicao, quantidade_estoque) "
                        + "SELECT id_categoria, ?, ?, ?, ?, ?, ? FROM categoria WHERE nome_categoria = ?",
                titulo, descricao, faixaEtaria, valorDiaria, valorReposicao, estoque, categoria);
    }

    private Integer idJogo(String titulo) {
        return jdbc.queryForObject("SELECT id_jogo FROM jogo WHERE titulo = ?", Integer.class, titulo);
    }
}
