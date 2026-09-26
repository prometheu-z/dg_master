package projeto.bdd2.dgmaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import projeto.bdd2.dgmaster.entity.Gerente;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.service.JogoService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JogoCatalogIntegrationTest {

    @Autowired
    private JogoService jogoService;

    @Test
    void shouldSaveAndFilterGamesByCategory() {
        Gerente gerente = new Gerente();
        gerente.setNome("Gerente Jogos");
        gerente.setEmail("gerente.jogos." + System.nanoTime() + "@test.com");
        gerente.setSenha("senha123");

        Jogo jogo = new Jogo();
        jogo.setNome("Catan");
        jogo.setCategoria("Estrategia");
        jogo.setGenero("Tabuleiro");
        jogo.setFaixaEtariaRecomendada(10);
        jogo.setPrecoLocacao(new BigDecimal("35.00"));
        jogo.setQuantidadeEstoque(5);
        jogo.setGerente(gerente);

        Jogo salvo = jogoService.salvar(jogo);

        assertThat(salvo.getCodigoJogo()).isNotNull();
        assertThat(jogoService.filtrar("Estrategia", null, 12)).isNotEmpty();
    }
}
