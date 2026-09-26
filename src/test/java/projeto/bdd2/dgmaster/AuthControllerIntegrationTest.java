package projeto.bdd2.dgmaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.repository.ClienteRepository;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void apiLoginPersistsAuthenticationForSubsequentRequests() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setCpf("102938475" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
        cliente.setNome("Cliente Login API");
        cliente.setEmail("login." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha(passwordEncoder.encode("Senha@123"));
        cliente.setDataNascimento(LocalDate.of(1990, 1, 1));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + cliente.getEmail() + "\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        assertThat(session).isNotNull();
        mockMvc.perform(get("/api/clientes/{cpf}", cliente.getCpf()).session(session))
                .andExpect(status().isOk());
    }
}