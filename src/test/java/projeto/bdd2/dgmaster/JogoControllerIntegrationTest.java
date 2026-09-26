package projeto.bdd2.dgmaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Gerente;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.repository.AluguelReservaRepository;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.GerenteRepository;
import projeto.bdd2.dgmaster.repository.JogoRepository;
import projeto.bdd2.dgmaster.security.UserPrincipal;
import projeto.bdd2.dgmaster.service.AluguelService;
import projeto.bdd2.dgmaster.service.JogoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JogoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GerenteRepository gerenteRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private JogoRepository jogoRepository;

    @Autowired
    private AluguelReservaRepository aluguelReservaRepository;

    @Autowired
    private AluguelService aluguelService;

    @Autowired
    private JogoService jogoService;

    @Test
    void visitorCanReadCatalogWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/jogos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void visitorCannotCreateCatalogGame() throws Exception {
        mockMvc.perform(post("/api/jogos")
            .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Jogo sem gerente\",\"precoLocacao\":20,\"valorReposicao\":100,\"quantidadeEstoque\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stateChangingRequestWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/jogos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Jogo sem token\",\"precoLocacao\":20,\"valorReposicao\":100,\"quantidadeEstoque\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanCreateCatalogGameWithRentalAndReplacementPrices() throws Exception {
        Gerente gerente = criarGerente();
        UserPrincipal principal = gerentePrincipal(gerente);

        mockMvc.perform(post("/api/jogos")
                        .with(authentication(auth(principal)))
            .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Cadastro Gerente\",\"categoria\":\"Estratégia\",\"genero\":\"Família\",\"faixaEtariaRecomendada\":10,\"precoLocacao\":25.50,\"valorReposicao\":150.00,\"quantidadeEstoque\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precoLocacao").value(25.50))
                .andExpect(jsonPath("$.valorReposicao").value(150.00));
    }

    @Test
    void managerCannotRemoveGameWithActiveRental() throws Exception {
        Gerente gerente = criarGerente();
        Cliente cliente = new Cliente();
        cliente.setCpf("777666555" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
        cliente.setNome("Cliente Jogo Ativo");
        cliente.setEmail("jogoativo." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha-hash");
        cliente.setDataNascimento(LocalDate.of(1990, 1, 1));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);

        Jogo jogo = new Jogo();
        jogo.setNome("Jogo Não Excluir " + UUID.randomUUID());
        jogo.setPrecoLocacao(new BigDecimal("25.00"));
        jogo.setValorReposicao(new BigDecimal("100.00"));
        jogo.setQuantidadeEstoque(1);
        jogoRepository.save(jogo);
        var reserva = aluguelService.reservar(cliente.getCpf(), java.util.List.of(jogo.getCodigoJogo()));

        mockMvc.perform(delete("/api/jogos/{codigo}", jogo.getCodigoJogo())
                        .with(authentication(auth(gerentePrincipal(gerente))))
                        .with(csrf()))
                .andExpect(status().isConflict());

        assertThat(jogoRepository.findById(jogo.getCodigoJogo()).orElseThrow().isAtivoCatalogo()).isTrue();
        assertThat(aluguelReservaRepository.findById(reserva.getIdAluguel())).isPresent();
    }

    @Test
    void deactivatedGameCannotBeReservedByItsKnownId() {
        Cliente cliente = new Cliente();
        cliente.setCpf("111222333" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10));
        cliente.setNome("Cliente Catálogo Inativo");
        cliente.setEmail("inativo." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha-hash");
        cliente.setDataNascimento(LocalDate.of(1990, 1, 1));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);

        Jogo jogo = new Jogo();
        jogo.setNome("Jogo Desativado " + UUID.randomUUID());
        jogo.setPrecoLocacao(new BigDecimal("25.00"));
        jogo.setValorReposicao(new BigDecimal("100.00"));
        jogo.setQuantidadeEstoque(1);
        Jogo salvo = jogoRepository.save(jogo);
        jogoService.excluir(salvo.getCodigoJogo());

        assertThatThrownBy(() -> aluguelService.reservar(cliente.getCpf(), java.util.List.of(salvo.getCodigoJogo())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catálogo");
    }

    private Gerente criarGerente() {
        Gerente gerente = new Gerente();
        gerente.setNome("Gerente Catálogo");
        gerente.setEmail("gerente." + UUID.randomUUID() + "@teste.com");
        gerente.setSenha("senha-hash");
        return gerenteRepository.save(gerente);
    }

    private UserPrincipal gerentePrincipal(Gerente gerente) {
        return new UserPrincipal(gerente.getEmail(), gerente.getSenha(), gerente.getNome(),
                String.valueOf(gerente.getIdGerente()), "GERENTE");
    }

    private UsernamePasswordAuthenticationToken auth(UserPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}