package projeto.bdd2.dgmaster.web;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import projeto.bdd2.dgmaster.auth.AuthController;
import projeto.bdd2.dgmaster.auth.CadastroRequest;
import projeto.bdd2.dgmaster.entity.Dependente;
import projeto.bdd2.dgmaster.repository.AluguelReservaRepository;
import projeto.bdd2.dgmaster.repository.PenalidadeRepository;
import projeto.bdd2.dgmaster.security.UserPrincipal;
import projeto.bdd2.dgmaster.service.AluguelService;
import projeto.bdd2.dgmaster.service.ClienteService;
import projeto.bdd2.dgmaster.service.JogoService;
import projeto.bdd2.dgmaster.service.PenalidadeService;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Controller
public class WebMvpController {

    private final AuthController authController;
    private final ClienteService clienteService;
    private final JogoService jogoService;
    private final AluguelService aluguelService;
    private final PenalidadeService penalidadeService;
    private final AluguelReservaRepository aluguelReservaRepository;
    private final PenalidadeRepository penalidadeRepository;

    public WebMvpController(
            AuthController authController,
            ClienteService clienteService,
            JogoService jogoService,
            AluguelService aluguelService,
            PenalidadeService penalidadeService,
            AluguelReservaRepository aluguelReservaRepository,
            PenalidadeRepository penalidadeRepository) {
        this.authController = authController;
        this.clienteService = clienteService;
        this.jogoService = jogoService;
        this.aluguelService = aluguelService;
        this.penalidadeService = penalidadeService;
        this.aluguelReservaRepository = aluguelReservaRepository;
        this.penalidadeRepository = penalidadeRepository;
    }

    @GetMapping("/")
    public String inicio(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return "redirect:/catalogo";
        }
        return "redirect:" + ("GERENTE".equals(principal.getTipo()) ? "/gerente" : "/cliente");
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal UserPrincipal principal) {
        return principal == null ? "login" : "redirect:/";
    }

    @GetMapping("/cadastro")
    public String cadastro() {
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@ModelAttribute CadastroForm form, RedirectAttributes redirectAttributes) {
        var resultado = authController.cadastrar(new CadastroRequest(
                "CLIENTE", form.cpf(), form.nome(), form.email(), form.senha(), form.dataNascimento()));
        if (resultado.getStatusCode().is2xxSuccessful()) {
            redirectAttributes.addFlashAttribute("sucesso", "Conta criada. Entre para continuar.");
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("erro", "Não foi possível criar a conta. Verifique os dados ou use outro e-mail.");
        return "redirect:/cadastro";
    }

    @GetMapping("/catalogo")
    public String catalogo(
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute CatalogoFiltro filtro,
            Model model) {
        model.addAttribute("jogos", jogoService.filtrar(filtro.categoria(), filtro.genero(), filtro.faixaEtaria()));
        model.addAttribute("categoria", filtro.categoria() == null ? "" : filtro.categoria());
        model.addAttribute("genero", filtro.genero() == null ? "" : filtro.genero());
        model.addAttribute("faixaEtaria", filtro.faixaEtaria());
        model.addAttribute("principal", principal);
        model.addAttribute("cliente", principal != null && "CLIENTE".equals(principal.getTipo()));
        model.addAttribute("dependentes", principal != null && "CLIENTE".equals(principal.getTipo())
                ? clienteService.listarDependentes(principal.getCpf())
                : List.of());
        return "catalogo";
    }

    @PostMapping("/catalogo/reservar")
    @PreAuthorize("hasRole('CLIENTE')")
    public String reservar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) List<Integer> idsJogos,
            @RequestParam(required = false) Integer idDependente,
            RedirectAttributes redirectAttributes) {
        exigirTipo(principal, "CLIENTE");
        try {
            aluguelService.solicitarReserva(principal.getCpf(), idDependente, idsJogos);
            redirectAttributes.addFlashAttribute("sucesso", "Reserva criada. Retire os jogos em até 24 horas.");
            return "redirect:/cliente";
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("erro", exception.getMessage());
            return "redirect:/catalogo";
        }
    }

    @GetMapping("/cliente")
    @PreAuthorize("hasRole('CLIENTE')")
    public String painelCliente(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        exigirTipo(principal, "CLIENTE");
        var cliente = clienteService.buscarPorCpf(principal.getCpf());
        model.addAttribute("principal", principal);
        model.addAttribute("cliente", cliente);
        model.addAttribute("dependentes", clienteService.listarDependentes(principal.getCpf()));
        model.addAttribute("alugueis", aluguelReservaRepository.findByClienteCpf(principal.getCpf()));
        return "cliente";
    }

    @PostMapping("/cliente/dependentes")
    @PreAuthorize("hasRole('CLIENTE')")
    public String cadastrarDependente(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String nome,
            @RequestParam LocalDate dataNascimento,
            RedirectAttributes redirectAttributes) {
        exigirTipo(principal, "CLIENTE");
        Dependente dependente = new Dependente();
        dependente.setNome(nome);
        dependente.setDataNascimento(dataNascimento);
        try {
            clienteService.adicionarDependente(principal.getCpf(), dependente);
            redirectAttributes.addFlashAttribute("sucesso", "Perfil de dependente cadastrado.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("erro", exception.getMessage());
        }
        return "redirect:/cliente";
    }

    @PostMapping("/cliente/alugueis/{id}/cancelar")
    @PreAuthorize("hasRole('CLIENTE')")
    public String cancelar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        exigirTipo(principal, "CLIENTE");
        return executarAcao(redirectAttributes, () -> aluguelService.cancelarReserva(id, principal.getCpf()), "/cliente",
                "Reserva cancelada e estoque liberado.");
    }

    @PostMapping("/cliente/alugueis/{id}/retirar")
    @PreAuthorize("hasRole('CLIENTE')")
    public String retirar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        exigirTipo(principal, "CLIENTE");
        return executarAcao(redirectAttributes, () -> aluguelService.retirarReserva(id, principal.getCpf()), "/cliente",
                "Retirada registrada. O prazo de devolução é de sete dias.");
    }

    @PostMapping("/cliente/alugueis/{id}/renovar")
    @PreAuthorize("hasRole('CLIENTE')")
    public String renovar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        exigirTipo(principal, "CLIENTE");
        return executarAcao(redirectAttributes,
                () -> aluguelService.renovarAluguel(id, principal.getCpf(), LocalDateTime.now()),
            "/cliente",
                "Aluguel renovado por mais sete dias.");
    }

    @GetMapping("/gerente")
    @PreAuthorize("hasRole('GERENTE')")
    public String painelGerente(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        exigirTipo(principal, "GERENTE");
        model.addAttribute("principal", principal);
        model.addAttribute("jogos", jogoService.listarTodos());
        model.addAttribute("prazos", aluguelService.listarPrazosGerenciais());
        model.addAttribute("multas", penalidadeRepository.findByMultaPagaFalse());
        model.addAttribute("jogoForm", new JogoForm("", "", "", null, null, null, 0));
        return "gerente";
    }

    @PostMapping("/gerente/jogos")
    @PreAuthorize("hasRole('GERENTE')")
    public String criarJogo(
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute JogoForm form,
            RedirectAttributes redirectAttributes) {
        exigirTipo(principal, "GERENTE");
        return executarAcao(redirectAttributes,
                () -> jogoService.salvarParaGerente(form.toEntity(), Integer.valueOf(principal.getCpf())),
                "/gerente", "Jogo cadastrado no catálogo.");
    }

    @PostMapping("/gerente/jogos/{codigo}/atualizar")
    @PreAuthorize("hasRole('GERENTE')")
    public String atualizarJogo(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer codigo,
            @ModelAttribute JogoForm form,
            RedirectAttributes redirectAttributes) {
        exigirTipo(principal, "GERENTE");
        return executarAcao(redirectAttributes,
                () -> jogoService.atualizar(codigo, form.toEntity(), Integer.valueOf(principal.getCpf())),
                "/gerente", "Cadastro do jogo atualizado.");
    }

    @PostMapping("/gerente/jogos/{codigo}/remover")
    @PreAuthorize("hasRole('GERENTE')")
    public String removerJogo(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer codigo,
            RedirectAttributes redirectAttributes) {
        exigirTipo(principal, "GERENTE");
        return executarAcao(redirectAttributes, () -> jogoService.excluir(codigo),
                "/gerente", "Jogo removido do catálogo; histórico preservado.");
    }

    @PostMapping("/gerente/alugueis/{id}/devolver")
    @PreAuthorize("hasRole('GERENTE')")
    public String devolverJogo(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        exigirTipo(principal, "GERENTE");
        return executarAcao(redirectAttributes, () -> aluguelService.devolver(id, LocalDateTime.now()),
                "/gerente", "Devolução registrada.");
    }

    @PostMapping("/gerente/penalidades/{id}/pagamento")
    @PreAuthorize("hasRole('GERENTE')")
    public String registrarPagamento(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        exigirTipo(principal, "GERENTE");
        return executarAcao(redirectAttributes, () -> penalidadeService.registrarPagamentoMulta(id),
                "/gerente", "Pagamento baixado.");
    }

    private String executarAcao(
            RedirectAttributes redirectAttributes,
            Runnable acao,
            String destino,
            String sucesso) {
        try {
            acao.run();
            redirectAttributes.addFlashAttribute("sucesso", sucesso);
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("erro", exception.getMessage());
        }
        return "redirect:" + destino;
    }

    private void exigirTipo(UserPrincipal principal, String tipo) {
        if (principal == null || !tipo.equals(principal.getTipo())) {
            throw new AccessDeniedException("Esta tela não está disponível para este perfil");
        }
    }
}