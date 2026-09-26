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
import projeto.bdd2.dgmaster.auth.GerenteRequest;
import projeto.bdd2.dgmaster.entity.AluguelReserva;
import projeto.bdd2.dgmaster.entity.Dependente;
import projeto.bdd2.dgmaster.entity.StatusAluguel;
import projeto.bdd2.dgmaster.repository.AluguelReservaRepository;
import projeto.bdd2.dgmaster.repository.PenalidadeRepository;
import projeto.bdd2.dgmaster.security.UserPrincipal;
import projeto.bdd2.dgmaster.service.AluguelService;
import projeto.bdd2.dgmaster.service.ClienteService;
import projeto.bdd2.dgmaster.service.EstatisticasService;
import projeto.bdd2.dgmaster.service.JogoService;
import projeto.bdd2.dgmaster.service.PenalidadeService;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class WebMvpController {

    private final AuthController authController;
    private final ClienteService clienteService;
    private final JogoService jogoService;
    private final AluguelService aluguelService;
    private final PenalidadeService penalidadeService;
    private final EstatisticasService estatisticasService;
    private final AluguelReservaRepository aluguelReservaRepository;
    private final PenalidadeRepository penalidadeRepository;

    public WebMvpController(
            AuthController authController,
            ClienteService clienteService,
            JogoService jogoService,
            AluguelService aluguelService,
            PenalidadeService penalidadeService,
            EstatisticasService estatisticasService,
            AluguelReservaRepository aluguelReservaRepository,
            PenalidadeRepository penalidadeRepository) {
        this.authController = authController;
        this.clienteService = clienteService;
        this.jogoService = jogoService;
        this.aluguelService = aluguelService;
        this.penalidadeService = penalidadeService;
        this.estatisticasService = estatisticasService;
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
        var resultado = authController.cadastrarCliente(new CadastroRequest(
                form.cpf(), form.nome(), form.email(), form.senha(), form.dataNascimento()));
        if (resultado.getStatusCode().is2xxSuccessful()) {
            redirectAttributes.addFlashAttribute("sucesso", "Conta criada. Entre para continuar.");
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("erro", "Não foi possível criar a conta. Verifique os dados ou use outro e-mail.");
        return "redirect:/cadastro";
    }
    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @PostMapping("/admin")
    public String criarGerente(
            @RequestParam String nome,
            @RequestParam String email,
            @RequestParam String senha,
            RedirectAttributes redirectAttributes) {
        try {
            var resultado = authController.cadastrarGerente(
                    new GerenteRequest(nome, email, senha)
            );

            if (resultado.getStatusCode().is2xxSuccessful()) {
                redirectAttributes.addFlashAttribute("sucesso", "Gerente criado com sucesso.");
                return "redirect:/login";
            }

            redirectAttributes.addFlashAttribute("erro", "Não foi possível criar o gerente. Verifique os dados ou use outro e-mail.");
            return "redirect:/admin";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao criar gerente: " + e.getMessage());
            return "redirect:/admin";
        }
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

    @GetMapping("/catalogo/{codigo}")
    public String detalhesJogo(
            @PathVariable Integer codigo,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {
        var jogo = jogoService.buscarAtivo(codigo);
        model.addAttribute("jogo", jogo);
        model.addAttribute("principal", principal);
        model.addAttribute("cliente", principal != null && "CLIENTE".equals(principal.getTipo()));
        model.addAttribute("dependentes", principal != null && "CLIENTE".equals(principal.getTipo())
                ? clienteService.listarDependentes(principal.getCpf())
                : List.of());
        return "detalhes-jogo";
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
        model.addAttribute("alugueisAtivos", aluguelReservaRepository.findByClienteCpfAndStatusIn(
                principal.getCpf(), 
                List.of(StatusAluguel.RESERVADO, StatusAluguel.RETIRADO, StatusAluguel.ATRASADO)
        ));
        model.addAttribute("alugueisHistorico", aluguelReservaRepository.findByClienteCpfAndStatusIn(
                principal.getCpf(),
                List.of(StatusAluguel.DEVOLVIDO, StatusAluguel.EXPIRADO, StatusAluguel.CANCELADO)
        ));
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

    @PostMapping("/cliente/perfil")
    @PreAuthorize("hasRole('CLIENTE')")
    public String atualizarPerfil(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String nome,
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {
        exigirTipo(principal, "CLIENTE");
        try {
            clienteService.atualizarPerfil(principal.getCpf(), nome, email);
            redirectAttributes.addFlashAttribute("sucesso", "Perfil atualizado com sucesso.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("erro", exception.getMessage());
        }
        return "redirect:/cliente";
    }

    @PostMapping("/cliente/dependentes/{id}/remover")
    @PreAuthorize("hasRole('CLIENTE')")
    public String removerDependente(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        exigirTipo(principal, "CLIENTE");
        try {
            clienteService.removerDependente(principal.getCpf(), id);
            redirectAttributes.addFlashAttribute("sucesso", "Dependente removido com sucesso.");
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
    public String painelGerente(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String filtroCliente,
            @RequestParam(required = false) String filtroStatus,
            Model model) {
        exigirTipo(principal, "GERENTE");
        
        List<AluguelReserva> prazos = aluguelService.listarPrazosGerenciais();
        
        // Aplicar filtros se fornecidos
        if (filtroCliente != null && !filtroCliente.isBlank()) {
            prazos = prazos.stream()
                    .filter(a -> a.getCliente().getCpf().equals(filtroCliente) || 
                               a.getCliente().getNome().toLowerCase().contains(filtroCliente.toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (filtroStatus != null && !filtroStatus.isBlank()) {
            try {
                StatusAluguel status = StatusAluguel.valueOf(filtroStatus.toUpperCase());
                prazos = prazos.stream()
                        .filter(a -> a.getStatus() == status)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Status inválido, ignora filtro
            }
        }
        
        model.addAttribute("principal", principal);
        model.addAttribute("jogos", jogoService.listarTodos());
        model.addAttribute("prazos", prazos);
        model.addAttribute("multas", penalidadeRepository.findByMultaPagaFalse());
        model.addAttribute("jogoForm", new JogoForm("", "", "", null, null, null, 0));
        model.addAttribute("alugueisHistorico", aluguelReservaRepository.findByStatusIn(
                List.of(StatusAluguel.DEVOLVIDO, StatusAluguel.EXPIRADO, StatusAluguel.CANCELADO)
        ));
        model.addAttribute("filtroCliente", filtroCliente != null ? filtroCliente : "");
        model.addAttribute("filtroStatus", filtroStatus != null ? filtroStatus : "");
        return "gerente";
    }

    @GetMapping("/gerente/estatisticas")
    @PreAuthorize("hasRole('GERENTE')")
    public String estatisticasGerente(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        exigirTipo(principal, "GERENTE");
        model.addAttribute("principal", principal);
        model.addAttribute("estatisticas", estatisticasService.gerarEstatisticasCompletas());
        return "estatisticas";
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