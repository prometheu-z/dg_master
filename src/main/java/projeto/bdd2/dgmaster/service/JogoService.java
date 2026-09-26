package projeto.bdd2.dgmaster.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projeto.bdd2.dgmaster.entity.Gerente;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.repository.GerenteRepository;
import projeto.bdd2.dgmaster.repository.JogoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        if (jogo == null) {
            throw new IllegalArgumentException("Jogo obrigatório");
        }

        if (jogo.getNome() == null || jogo.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do jogo obrigatório");
        }

        if (jogo.getGerente() != null && jogo.getGerente().getIdGerente() == null) {
            Gerente gerentePersistido = gerenteRepository.save(jogo.getGerente());
            jogo.setGerente(gerentePersistido);
        }

        return jogoRepository.save(jogo);
    }

    @Transactional(readOnly = true)
    public List<Jogo> listarTodos() {
        return jogoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Jogo> filtrar(String categoria, String genero, Integer faixaEtariaMaxima) {
        List<Jogo> jogos = new ArrayList<>(jogoRepository.findAll());

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
}
