package projeto.bdd2.dgmaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.JogoRepository;
import projeto.bdd2.dgmaster.security.UserPrincipal;
import projeto.bdd2.dgmaster.service.AluguelService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GerenteAluguelControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private JogoRepository jogoRepository;

    @Autowired
    private AluguelService aluguelService;

    @Test
    void onlyManagerCanReadRentalDeadlines() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setCpf("654123987" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
        cliente.setNome("Cliente Prazos");
        cliente.setEmail("prazos." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha-hash");
        cliente.setDataNascimento(LocalDate.of(1990, 1, 1));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);

        Jogo jogo = new Jogo();
        jogo.setNome("Jogo Prazo " + UUID.randomUUID());
        jogo.setPrecoLocacao(new BigDecimal("20.00"));
        jogo.setValorReposicao(new BigDecimal("100.00"));
        jogo.setQuantidadeEstoque(1);
        jogoRepository.save(jogo);
        aluguelService.reservar(cliente.getCpf(), List.of(jogo.getCodigoJogo()));

        UserPrincipal gerente = new UserPrincipal("gerente@teste.com", "senha", "Gerente", "1", "GERENTE");
        var authGerente = new UsernamePasswordAuthenticationToken(gerente, null, gerente.getAuthorities());
        mockMvc.perform(get("/api/gerente/prazos").with(authentication(authGerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").exists());

        UserPrincipal titular = new UserPrincipal(
                cliente.getEmail(), cliente.getSenha(), cliente.getNome(), cliente.getCpf(), "CLIENTE");
        var authTitular = new UsernamePasswordAuthenticationToken(titular, null, titular.getAuthorities());
        mockMvc.perform(get("/api/gerente/prazos").with(authentication(authTitular)))
                .andExpect(status().isForbidden());
    }
}