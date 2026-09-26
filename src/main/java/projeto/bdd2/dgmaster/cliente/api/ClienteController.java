package projeto.bdd2.dgmaster.cliente.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import projeto.bdd2.dgmaster.entity.Dependente;
import projeto.bdd2.dgmaster.security.UserPrincipal;
import projeto.bdd2.dgmaster.service.ClienteService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping("/{cpf}")
    public ClienteResponse perfil(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String cpf) {
        validarTitular(principal, cpf);
        return ClienteResponse.from(clienteService.buscarPorCpf(cpf));
    }

    @GetMapping("/{cpf}/dependentes")
    public List<DependenteResponse> listarDependentes(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String cpf) {
        validarTitular(principal, cpf);
        List<DependenteResponse> respostas = new ArrayList<>();
        for (Dependente dependente : clienteService.listarDependentes(cpf)) {
            respostas.add(DependenteResponse.from(dependente));
        }
        return respostas;
    }

    @PostMapping("/{cpf}/dependentes")
    @ResponseStatus(HttpStatus.CREATED)
    public DependenteResponse cadastrarDependente(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String cpf,
            @Valid @RequestBody DependenteRequest request) {
        validarTitular(principal, cpf);
        Dependente dependente = new Dependente();
        dependente.setNome(request.nome());
        dependente.setDataNascimento(request.dataNascimento());
        return DependenteResponse.from(clienteService.adicionarDependente(cpf, dependente));
    }

    private void validarTitular(UserPrincipal principal, String cpf) {
        if (principal == null || !"CLIENTE".equals(principal.getTipo()) || !cpf.equals(principal.getCpf())) {
            throw new AccessDeniedException("Clientes só podem acessar o próprio perfil e dependentes");
        }
    }
}