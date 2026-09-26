package projeto.bdd2.dgmaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.repository.AluguelReservaRepository;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.JogoRepository;
import projeto.bdd2.dgmaster.security.UserPrincipal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AluguelControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private JogoRepository jogoRepository;

    @Autowired
    private AluguelReservaRepository aluguelReservaRepository;

    @Test
    void reservationUsesAuthenticatedCustomerInsteadOfCpfFromRequest() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setCpf("321654987" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
        cliente.setNome("Cliente API");
        cliente.setEmail("api." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha-hash");
        cliente.setDataNascimento(LocalDate.of(1990, 1, 1));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);

        Jogo jogo = new Jogo();
        jogo.setNome("Jogo API " + UUID.randomUUID());
        jogo.setPrecoLocacao(new BigDecimal("25.00"));
        jogo.setValorReposicao(new BigDecimal("100.00"));
        jogo.setQuantidadeEstoque(1);
        jogoRepository.save(jogo);

        UserPrincipal principal = new UserPrincipal(
                cliente.getEmail(), cliente.getSenha(), cliente.getNome(), cliente.getCpf(), "CLIENTE");
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        mockMvc.perform(post("/api/alugueis")
                        .with(authentication(authentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpfCliente\":\"00000000000\",\"idsJogos\":[" + jogo.getCodigoJogo() + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVADO"))
                .andExpect(jsonPath("$.jogos[0]").value(jogo.getNome()))
                .andExpect(jsonPath("$.clienteCpf").doesNotExist());

        assertThat(aluguelReservaRepository.findByClienteCpf(cliente.getCpf())).hasSize(1);
        assertThat(aluguelReservaRepository.findByClienteCpf("00000000000")).isEmpty();
    }

    @Test
    void customerCanReadOnlyTheirOwnRentalHistory() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setCpf("654987321" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
        cliente.setNome("Cliente Histórico");
        cliente.setEmail("historico." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha-hash");
        cliente.setDataNascimento(LocalDate.of(1990, 1, 1));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);
        UserPrincipal principal = new UserPrincipal(
                cliente.getEmail(), cliente.getSenha(), cliente.getNome(), cliente.getCpf(), "CLIENTE");
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/clientes/{cpf}/alugueis", cliente.getCpf())
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/clientes/{cpf}/alugueis", "00000000000")
                        .with(authentication(authentication)))
                .andExpect(status().isForbidden());
    }
}