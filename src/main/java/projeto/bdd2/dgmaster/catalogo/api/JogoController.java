package projeto.bdd2.dgmaster.catalogo.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import projeto.bdd2.dgmaster.security.UserPrincipal;
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
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) Integer faixaEtaria,
            @RequestParam(required = false) String categoria) {
        List<JogoResponse> respostas = new ArrayList<>();
        for (Jogo jogo : jogoService.filtrar(categoria, genero, faixaEtaria)) {
            respostas.add(JogoResponse.from(jogo));
        }
        return respostas;
    }

    @GetMapping("/{codigo}")
    public JogoResponse buscar(@PathVariable Integer codigo) {
        return JogoResponse.from(jogoService.buscarAtivo(codigo));
    }

    @PreAuthorize("hasRole('GERENTE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JogoResponse criar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JogoRequest request) {
        return JogoResponse.from(jogoService.salvarParaGerente(toEntity(request), Integer.valueOf(principal.getCpf())));
    }

    @PreAuthorize("hasRole('GERENTE')")
    @PutMapping("/{codigo}")
    public JogoResponse atualizar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer codigo,
            @Valid @RequestBody JogoRequest request) {
        return JogoResponse.from(jogoService.atualizar(codigo, toEntity(request), Integer.valueOf(principal.getCpf())));
    }

    @PreAuthorize("hasRole('GERENTE')")
    @DeleteMapping("/{codigo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer codigo) {
        jogoService.excluir(codigo);
    }

    private Jogo toEntity(JogoRequest request) {
        Jogo jogo = new Jogo();
        jogo.setNome(request.nome());
        jogo.setCategoria(request.categoria());
        jogo.setGenero(request.genero());
        jogo.setFaixaEtariaRecomendada(request.faixaEtariaRecomendada());
        jogo.setPrecoLocacao(request.precoLocacao());
        jogo.setValorReposicao(request.valorReposicao());
        jogo.setQuantidadeEstoque(request.quantidadeEstoque());
        return jogo;
    }
}