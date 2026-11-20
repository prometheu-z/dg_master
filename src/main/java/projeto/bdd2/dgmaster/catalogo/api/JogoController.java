package projeto.bdd2.dgmaster.catalogo.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import projeto.bdd2.dgmaster.entity.Jogo;
import projeto.bdd2.dgmaster.service.JogoService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/jogos")
public class JogoController {

    private final JogoService jogoService;

    public JogoController(JogoService jogoService) {
        this.jogoService = jogoService;
    }

    @GetMapping
    public List<JogoResponse> listar(
            @RequestParam(required = false) Integer faixaEtaria,
            @RequestParam(required = false) String categoria) {
        List<JogoResponse> respostas = new ArrayList<>();
        for (Jogo jogo : jogoService.filtrar(categoria, faixaEtaria)) {
            respostas.add(JogoResponse.from(jogo));
        }
        return respostas;
    }

    @GetMapping("/{id}")
    public JogoResponse buscar(@PathVariable Integer id) {
        return JogoResponse.from(jogoService.buscarPorId(id));
    }

    @PreAuthorize("hasRole('GERENTE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JogoResponse criar(@Valid @RequestBody JogoRequest request) {
        return JogoResponse.from(jogoService.salvar(toEntity(request), request.categoria()));
    }

    @PreAuthorize("hasRole('GERENTE')")
    @PutMapping("/{id}")
    public JogoResponse atualizar(@PathVariable Integer id, @Valid @RequestBody JogoRequest request) {
        return JogoResponse.from(jogoService.atualizar(id, toEntity(request), request.categoria()));
    }

    @PreAuthorize("hasRole('GERENTE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer id) {
        jogoService.excluir(id);
    }

    private Jogo toEntity(JogoRequest request) {
        Jogo jogo = new Jogo();
        jogo.setTitulo(request.titulo());
        jogo.setDescricao(request.descricao());
        jogo.setFaixaEtariaMinima(request.faixaEtariaMinima() == null ? 0 : request.faixaEtariaMinima());
        jogo.setValorDiaria(request.valorDiaria());
        jogo.setValorReposicao(request.valorReposicao());
        jogo.setQuantidadeEstoque(request.quantidadeEstoque());
        return jogo;
    }
}
