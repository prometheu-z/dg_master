package projeto.bdd2.dgmaster.auth;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Gerente;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.GerenteRepository;
import projeto.bdd2.dgmaster.security.UserPrincipal;
import projeto.bdd2.dgmaster.service.ClienteService;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ClienteService clienteService;
    private final ClienteRepository clienteRepository;
    private final GerenteRepository gerenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final String chaveCadastroGerente;

    public AuthController(
            ClienteService clienteService,
            ClienteRepository clienteRepository,
            GerenteRepository gerenteRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            @Value("${dgmaster.admin.chave-cadastro-gerente:}") String chaveCadastroGerente) {
        this.clienteService = clienteService;
        this.clienteRepository = clienteRepository;
        this.gerenteRepository = gerenteRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.chaveCadastroGerente = chaveCadastroGerente;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<AuthResponse> cadastrarCliente(@Valid @RequestBody CadastroRequest request) {
        if (clienteRepository.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        ValidacaoSenha.validar(request.senha());

        Cliente cliente = new Cliente();
        cliente.setCpf(request.cpf());
        cliente.setNome(request.nome());
        cliente.setEmail(request.email());
        cliente.setSenha(request.senha());
        cliente.setDataNascimento(LocalDate.parse(request.dataNascimento()));

        // Delega ao serviço para aplicar RN7 (idade mínima), unicidade de CPF/e-mail
        // e o hash BCrypt da senha em um único ponto.
        Cliente salvo = clienteService.cadastrar(cliente);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse("CLIENTE", salvo.getCpf(), salvo.getNome(), salvo.getEmail(), true));
    }

    @PostMapping("/cadastro-gerente")
    public ResponseEntity<AuthResponse> cadastrarGerente(@Valid @RequestBody GerenteRequest request) {
        validarChaveAdministrador(request.chaveAdmin());

        if (gerenteRepository.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        ValidacaoSenha.validar(request.senha());

        Gerente gerente = new Gerente();
        gerente.setNome(request.nome());
        gerente.setEmail(request.email());
        gerente.setSenha(passwordEncoder.encode(request.senha()));
        gerenteRepository.save(gerente);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse("GERENTE", null, gerente.getNome(), gerente.getEmail(), true));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));

        sessionAuthenticationStrategy.onAuthentication(authentication, servletRequest, servletResponse);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, servletRequest, servletResponse);

        var principal = (UserPrincipal) authentication.getPrincipal();

        return ResponseEntity.ok(new AuthResponse(
                principal.getTipo(),
                principal.getCpf(),
                principal.getNome(),
                principal.getEmail(),
                true));
    }

    @PostMapping("/2fa/verificar")
    public ResponseEntity<Map<String, String>> verificarDoisFatores(@RequestBody Map<String, String> payload) {
        String codigo = payload.getOrDefault("codigo", "");
        if (codigo == null || codigo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", "Código de verificação obrigatório."));
        }
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(Map.of("mensagem", "A validação 2FA ainda não está configurada."));
    }

    /**
     * O cadastro de gerentes é uma operação administrativa: só é aceito quando
     * uma chave de administrador está configurada (DGMASTER_ADMIN_KEY) e o valor
     * informado confere. Sem chave configurada, a operação fica desabilitada.
     */
    private void validarChaveAdministrador(String chaveInformada) {
        if (chaveCadastroGerente == null || chaveCadastroGerente.isBlank()) {
            throw new AccessDeniedException(
                    "O cadastro de gerentes está desabilitado. Defina a variável de ambiente DGMASTER_ADMIN_KEY.");
        }
        if (!chaveCadastroGerente.equals(chaveInformada)) {
            throw new AccessDeniedException("Chave de administrador inválida.");
        }
    }
}
