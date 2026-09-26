package projeto.bdd2.dgmaster.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projeto.bdd2.dgmaster.entity.Gerente;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.entity.StatusAluguel;
import projeto.bdd2.dgmaster.repository.GerenteRepository;
import projeto.bdd2.dgmaster.repository.JogoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class JogoService {

    private final JogoRepository jogoRepository;
    private final GerenteRepository gerenteRepository;

    public JogoService(JogoRepository jogoRepository, GerenteRepository gerenteRepository) {
        this.jogoRepository = jogoRepository;
        this.gerenteRepository = gerenteRepository;
    }

    @Transactional
    public Jogo salvar(Jogo jogo) {
        validarJogo(jogo);

        if (jogo.getGerente() != null && jogo.getGerente().getIdGerente() == null) {
            Gerente gerentePersistido = gerenteRepository.save(jogo.getGerente());
            jogo.setGerente(gerentePersistido);
        }

        return jogoRepository.save(jogo);
    }

    @Transactional
    public Jogo salvarParaGerente(Jogo jogo, Integer idGerente) {
        Gerente gerente = gerenteRepository.findById(idGerente)
                .orElseThrow(() -> new IllegalArgumentException("Gerente não encontrado: " + idGerente));
        jogo.setGerente(gerente);
        return salvar(jogo);
    }

    @Transactional
    public Jogo atualizar(Integer codigoJogo, Jogo alteracoes, Integer idGerente) {
        validarJogo(alteracoes);
        Jogo jogo = buscarAtivo(codigoJogo);
        jogo.setNome(alteracoes.getNome());
        jogo.setCategoria(alteracoes.getCategoria());
        jogo.setGenero(alteracoes.getGenero());
        jogo.setFaixaEtariaRecomendada(alteracoes.getFaixaEtariaRecomendada());
        jogo.setPrecoLocacao(alteracoes.getPrecoLocacao());
        jogo.setValorReposicao(alteracoes.getValorReposicao());
        jogo.setQuantidadeEstoque(alteracoes.getQuantidadeEstoque());
        jogo.setStatusDisponibilidade(alteracoes.getQuantidadeEstoque() > 0);
        if (jogo.getGerente() == null) {
            jogo.setGerente(gerenteRepository.findById(idGerente)
                    .orElseThrow(() -> new IllegalArgumentException("Gerente não encontrado: " + idGerente)));
        }
        return jogoRepository.save(jogo);
    }

    @Transactional
    public void excluir(Integer codigoJogo) {
        Jogo jogo = buscarAtivo(codigoJogo);
        Set<StatusAluguel> estadosComPosse = Set.of(
                StatusAluguel.AGUARDANDO_APROVACAO,
                StatusAluguel.RESERVADO,
                StatusAluguel.RETIRADO,
                StatusAluguel.ATRASADO);
        for (var aluguel : jogo.getAlugueis()) {
            if (estadosComPosse.contains(aluguel.getStatus())) {
                throw new IllegalStateException("Não é possível remover um jogo reservado ou ainda não devolvido");
            }
        }
        jogo.setAtivoCatalogo(false);
        jogoRepository.save(jogo);
    }

    private void validarJogo(Jogo jogo) {
        if (jogo == null) {
            throw new IllegalArgumentException("Jogo obrigatório");
        }

        if (jogo.getNome() == null || jogo.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do jogo obrigatório");
        }
        if (jogo.getPrecoLocacao() == null || jogo.getPrecoLocacao().signum() <= 0) {
            throw new IllegalArgumentException("Preço de locação precisa ser maior que zero");
        }
        if (jogo.getValorReposicao() == null || jogo.getValorReposicao().signum() <= 0) {
            throw new IllegalArgumentException("Valor de reposição precisa ser maior que zero");
        }
        if (jogo.getQuantidadeEstoque() == null || jogo.getQuantidadeEstoque() < 0) {
            throw new IllegalArgumentException("Quantidade de estoque inválida");
        }
    }

    @Transactional
    public Jogo atualizarEstoque(Integer codigoJogo, Integer quantidadeDisponivel) {
        Jogo jogo = buscarPorId(codigoJogo);
        if (quantidadeDisponivel == null || quantidadeDisponivel < 0) {
            throw new IllegalArgumentException("Quantidade de estoque inválida");
        }

        jogo.setQuantidadeEstoque(quantidadeDisponivel);
        jogo.setStatusDisponibilidade(quantidadeDisponivel > 0);
        return jogoRepository.save(jogo);
    }

    @Transactional(readOnly = true)
    public List<Jogo> listarTodos() {
        List<Jogo> jogos = new ArrayList<>(jogoRepository.findAll());
        jogos.removeIf(jogo -> !jogo.isAtivoCatalogo());
        return jogos;
    }

    @Transactional(readOnly = true)
    public List<Jogo> filtrar(String categoria, String genero, Integer faixaEtariaMaxima) {
        List<Jogo> jogos = new ArrayList<>(jogoRepository.findAll());
        jogos.removeIf(jogo -> !jogo.isAtivoCatalogo());

        if (categoria != null && !categoria.isBlank()) {
            jogos.removeIf(jogo -> !Objects.equals(jogo.getCategoria(), categoria));
        }

        if (genero != null && !genero.isBlank()) {
            jogos.removeIf(jogo -> !Objects.equals(jogo.getGenero(), genero));
        }

        if (faixaEtariaMaxima != null) {
            jogos.removeIf(jogo -> jogo.getFaixaEtariaRecomendada() == null ||
                    jogo.getFaixaEtariaRecomendada() > faixaEtariaMaxima);
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
        Jogo jogo = buscarPorId(id);
        if (!jogo.isAtivoCatalogo()) {
            throw new IllegalArgumentException("Jogo não encontrado no catálogo: " + id);
        }
        return jogo;
    }
}
