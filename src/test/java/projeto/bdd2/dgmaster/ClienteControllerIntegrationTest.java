package projeto.bdd2.dgmaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.security.UserPrincipal;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClienteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    void titularCanManageOnlyTheirOwnDependents() throws Exception {
        Cliente titular = criarCliente();
        UserPrincipal principal = new UserPrincipal(
                titular.getEmail(), titular.getSenha(), titular.getNome(), titular.getCpf(), "CLIENTE");
        var autenticacao = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        mockMvc.perform(post("/api/clientes/{cpf}/dependentes", titular.getCpf())
                        .with(authentication(autenticacao))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Dependente API\",\"dataNascimento\":\"2014-05-10\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Dependente API"));

        mockMvc.perform(get("/api/clientes/{cpf}/dependentes", titular.getCpf())
                        .with(authentication(autenticacao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Dependente API"));

        mockMvc.perform(get("/api/clientes/{cpf}/dependentes", "00000000000")
                        .with(authentication(autenticacao)))
                .andExpect(status().isForbidden());
    }

    private Cliente criarCliente() {
        Cliente cliente = new Cliente();
        cliente.setCpf("876543210" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
        cliente.setNome("Titular Dependentes");
        cliente.setEmail("titular." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha-hash");
        cliente.setDataNascimento(LocalDate.of(1990, 1, 1));
        cliente.setStatusConta(true);
        return clienteRepository.save(cliente);
    }
}