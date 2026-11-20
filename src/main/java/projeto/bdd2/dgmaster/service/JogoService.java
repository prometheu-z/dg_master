package projeto.bdd2.dgmaster.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projeto.bdd2.dgmaster.entity.Categoria;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.repository.CategoriaRepository;
import projeto.bdd2.dgmaster.repository.JogoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class JogoService {

    private final JogoRepository jogoRepository;
    private final CategoriaRepository categoriaRepository;

    public JogoService(JogoRepository jogoRepository, CategoriaRepository categoriaRepository) {
        this.jogoRepository = jogoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public Jogo salvar(Jogo jogo, String nomeCategoria) {
        validarJogo(jogo);
        jogo.setCategoria(resolverCategoria(nomeCategoria));
        return jogoRepository.save(jogo);
    }

    @Transactional
    public Jogo atualizar(Integer idJogo, Jogo alteracoes, String nomeCategoria) {
        validarJogo(alteracoes);
        Jogo jogo = buscarPorId(idJogo);
        jogo.setTitulo(alteracoes.getTitulo());
        jogo.setDescricao(alteracoes.getDescricao());
        jogo.setFaixaEtariaMinima(alteracoes.getFaixaEtariaMinima());
        jogo.setValorDiaria(alteracoes.getValorDiaria());
        jogo.setValorReposicao(alteracoes.getValorReposicao());
        jogo.setQuantidadeEstoque(alteracoes.getQuantidadeEstoque());
        jogo.setCategoria(resolverCategoria(nomeCategoria));
        return jogoRepository.save(jogo);
    }

    @Transactional
    public void excluir(Integer idJogo) {
        Jogo jogo = buscarPorId(idJogo);
        if (jogo.getItensAluguel() != null && !jogo.getItensAluguel().isEmpty()) {
            throw new IllegalStateException(
                    "Não é possível remover um jogo com histórico de aluguéis; preservando a integridade referencial");
        }
        jogoRepository.delete(jogo);
    }

    @Transactional
    public Jogo atualizarEstoque(Integer idJogo, Integer quantidadeDisponivel) {
        Jogo jogo = buscarPorId(idJogo);
        if (quantidadeDisponivel == null || quantidadeDisponivel < 0) {
            throw new IllegalArgumentException("Quantidade de estoque inválida");
        }
        jogo.setQuantidadeEstoque(quantidadeDisponivel);
        return jogoRepository.save(jogo);
    }

    @Transactional(readOnly = true)
    public List<Jogo> listarTodos() {
        return new ArrayList<>(jogoRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<Jogo> filtrar(String categoria, Integer faixaEtariaMaxima) {
        List<Jogo> jogos = new ArrayList<>(jogoRepository.findAll());

        if (categoria != null && !categoria.isBlank()) {
            jogos.removeIf(jogo -> jogo.getCategoria() == null
                    || !Objects.equals(jogo.getCategoria().getNomeCategoria(), categoria));
        }

        if (faixaEtariaMaxima != null) {
            jogos.removeIf(jogo -> jogo.getFaixaEtariaMinima() == null
                    || jogo.getFaixaEtariaMinima() > faixaEtariaMaxima);
        }

        return jogos;
    }

    @Transactional(readOnly = true)
    public Jogo buscarPorId(Integer id) {
        return jogoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Jogo não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Jogo buscarAtivo(Integer id) {
        return buscarPorId(id);
    }

    private Categoria resolverCategoria(String nomeCategoria) {
        String nome = (nomeCategoria == null || nomeCategoria.isBlank()) ? "Geral" : nomeCategoria.trim();
        return categoriaRepository.findByNomeCategoriaIgnoreCase(nome)
                .orElseGet(() -> categoriaRepository.save(new Categoria(null, nome, new ArrayList<>())));
    }

    private void validarJogo(Jogo jogo) {
        if (jogo == null) {
            throw new IllegalArgumentException("Jogo obrigatório");
        }
        if (jogo.getTitulo() == null || jogo.getTitulo().isBlank()) {
            throw new IllegalArgumentException("Título do jogo obrigatório");
        }
        if (jogo.getValorDiaria() == null || jogo.getValorDiaria().signum() <= 0) {
            throw new IllegalArgumentException("Valor da diária precisa ser maior que zero");
        }
        if (jogo.getValorReposicao() == null || jogo.getValorReposicao().signum() <= 0) {
            throw new IllegalArgumentException("Valor de reposição precisa ser maior que zero");
        }
        if (jogo.getQuantidadeEstoque() == null || jogo.getQuantidadeEstoque() < 0) {
            throw new IllegalArgumentException("Quantidade de estoque inválida");
        }
    }
}
