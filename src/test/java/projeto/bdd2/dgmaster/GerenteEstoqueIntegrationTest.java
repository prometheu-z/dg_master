package projeto.bdd2.dgmaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import projeto.bdd2.dgmaster.entity.Gerente;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.repository.GerenteRepository;
import projeto.bdd2.dgmaster.service.JogoService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GerenteEstoqueIntegrationTest {

    @Autowired
    private JogoService jogoService;

    @Autowired
    private GerenteRepository gerenteRepository;

    @Test
    void shouldManageStockAndAvailability() {
        Gerente gerente = new Gerente();
        gerente.setNome("Gerente Estoque");
        gerente.setEmail("estoque." + UUID.randomUUID() + "@teste.com");
        gerente.setSenha("senha123");
        gerenteRepository.save(gerente);

        Jogo jogo = new Jogo();
        jogo.setNome("Ticket to Ride");
        jogo.setCategoria("Estratégia");
        jogo.setGenero("Família");
        jogo.setFaixaEtariaRecomendada(8);
        jogo.setPrecoLocacao(new BigDecimal("35.00"));
        jogo.setQuantidadeEstoque(5);
        jogo.setGerente(gerente);

        Jogo salvo = jogoService.salvar(jogo);
        jogoService.atualizarEstoque(salvo.getCodigoJogo(), 3);

        Jogo atualizado = jogoService.buscarPorId(salvo.getCodigoJogo());
        assertThat(atualizado.getQuantidadeEstoque()).isEqualTo(3);
        assertThat(atualizado.isStatusDisponibilidade()).isTrue();
    }
}
