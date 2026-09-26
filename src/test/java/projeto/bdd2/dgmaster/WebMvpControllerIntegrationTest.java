package projeto.bdd2.dgmaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Dependente;
import projeto.bdd2.dgmaster.entity.Gerente;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.repository.AluguelReservaRepository;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.DependenteRepository;
import projeto.bdd2.dgmaster.repository.GerenteRepository;
import projeto.bdd2.dgmaster.repository.JogoRepository;
import projeto.bdd2.dgmaster.repository.PenalidadeRepository;
import projeto.bdd2.dgmaster.security.UserPrincipal;
import projeto.bdd2.dgmaster.service.AluguelService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class WebMvpControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private DependenteRepository dependenteRepository;

    @Autowired
    private GerenteRepository gerenteRepository;

    @Autowired
    private JogoRepository jogoRepository;

    @Autowired
    private AluguelReservaRepository aluguelReservaRepository;

    @Autowired
    private AluguelService aluguelService;

    @Autowired
    private PenalidadeRepository penalidadeRepository;

    @Test
    void visitorCanOpenCatalogAndLoginPages() throws Exception {
        mockMvc.perform(get("/catalogo"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalogo"));

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

            @Test
            void customerCanReserveForDependentAndOpenTheirDashboard() throws Exception {
            Cliente cliente = criarCliente();
            Dependente dependente = new Dependente();
            dependente.setNome("Perfil Filho " + UUID.randomUUID());
            dependente.setDataNascimento(LocalDate.of(2014, 5, 10));
            dependente.setCliente(cliente);
            dependenteRepository.save(dependente);

            Jogo jogo = new Jogo();
            jogo.setNome("Jogo de Mesa " + UUID.randomUUID());
            jogo.setPrecoLocacao(new BigDecimal("25.00"));
            jogo.setValorReposicao(new BigDecimal("100.00"));
            jogo.setQuantidadeEstoque(1);
            jogoRepository.save(jogo);

            UserPrincipal principal = new UserPrincipal(
                cliente.getEmail(), cliente.getSenha(), cliente.getNome(), cliente.getCpf(), "CLIENTE");
            var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            mockMvc.perform(get("/catalogo").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(dependente.getNome())));

            mockMvc.perform(post("/catalogo/reservar")
                    .with(authentication(auth))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("idsJogos", jogo.getCodigoJogo().toString())
                    .param("idDependente", dependente.getIdDependente().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente"));

            var reserva = aluguelReservaRepository.findByClienteCpf(cliente.getCpf()).get(0);
            org.assertj.core.api.Assertions.assertThat(reserva.getDependente().getIdDependente())
                .isEqualTo(dependente.getIdDependente());
            mockMvc.perform(get("/cliente").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(jogo.getNome())));
            mockMvc.perform(get("/gerente").with(authentication(auth)))
                .andExpect(status().isForbidden());
            }

            @Test
            void managerCanCreateGameAndOpenOperationsDashboard() throws Exception {
            Gerente gerente = new Gerente();
            gerente.setNome("Gerente Web");
            gerente.setEmail("gerente.web." + UUID.randomUUID() + "@teste.com");
            gerente.setSenha("senha-hash");
            gerenteRepository.save(gerente);
            UserPrincipal principal = new UserPrincipal(gerente.getEmail(), gerente.getSenha(), gerente.getNome(),
                String.valueOf(gerente.getIdGerente()), "GERENTE");
            var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            String nomeJogo = "Jogo Gerencial " + UUID.randomUUID();

            mockMvc.perform(post("/gerente/jogos")
                    .with(authentication(auth))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("nome", nomeJogo)
                    .param("categoria", "Estrategia")
                    .param("genero", "Familia")
                    .param("faixaEtariaRecomendada", "10")
                    .param("precoLocacao", "25.50")
                    .param("valorReposicao", "100.00")
                    .param("quantidadeEstoque", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gerente"));

            mockMvc.perform(get("/gerente").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(nomeJogo)));
            }

            @Test
            void managerCanSeeAndRegisterFinePaymentFromDashboard() throws Exception {
            Cliente cliente = criarCliente();
            Jogo jogo = new Jogo();
            jogo.setNome("Jogo com multa " + UUID.randomUUID());
            jogo.setPrecoLocacao(new BigDecimal("25.00"));
            jogo.setValorReposicao(new BigDecimal("100.00"));
            jogo.setQuantidadeEstoque(1);
            jogoRepository.save(jogo);
            var reserva = aluguelService.reservar(cliente.getCpf(), java.util.List.of(jogo.getCodigoJogo()));
            LocalDateTime retirada = LocalDateTime.now().withNano(0).plusSeconds(2);
            aluguelService.retirarReserva(reserva.getIdAluguel(), cliente.getCpf(), retirada);
            aluguelService.devolver(reserva.getIdAluguel(), retirada.plusDays(8));
            var multa = penalidadeRepository.findByAluguelReservaIdAluguel(reserva.getIdAluguel()).get(0);

            Gerente gerente = new Gerente();
            gerente.setNome("Gerente Baixa");
            gerente.setEmail("gerente.baixa." + UUID.randomUUID() + "@teste.com");
            gerente.setSenha("senha-hash");
            gerenteRepository.save(gerente);
            UserPrincipal principal = new UserPrincipal(gerente.getEmail(), gerente.getSenha(), gerente.getNome(),
                String.valueOf(gerente.getIdGerente()), "GERENTE");
            var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

            mockMvc.perform(get("/gerente").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(cliente.getNome())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Registrar pagamento")));

            mockMvc.perform(post("/gerente/penalidades/{id}/pagamento", multa.getIdPenalidade())
                    .with(authentication(auth))
                    .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gerente"));

            org.assertj.core.api.Assertions.assertThat(penalidadeRepository.findById(multa.getIdPenalidade())
                .orElseThrow().isMultaPaga()).isTrue();
            }

            private Cliente criarCliente() {
            Cliente cliente = new Cliente();
            cliente.setCpf("912345678" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
            cliente.setNome("Cliente Web");
            cliente.setEmail("cliente.web." + UUID.randomUUID() + "@teste.com");
            cliente.setSenha("senha-hash");
            cliente.setDataNascimento(LocalDate.of(1990, 1, 1));
            cliente.setStatusConta(true);
            return clienteRepository.save(cliente);
            }
}