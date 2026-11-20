package projeto.bdd2.dgmaster.auth;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
import projeto.bdd2.dgmaster.util.CpfValidator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
        String cpf = CpfValidator.sanitizar(request.cpf());
        CpfValidator.validar(cpf);

        if (clienteRepository.existsByEmail(request.email()) || clienteRepository.existsByCpf(cpf)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        ValidacaoSenha.validar(request.senha());

        Cliente cliente = new Cliente();
        cliente.setCpf(cpf);
        cliente.setNome(request.nome());
        cliente.setEmail(request.email());
        cliente.setSenha(request.senha());
        cliente.setDataNascimento(LocalDate.parse(request.dataNascimento()));

        // Delega ao serviço para aplicar RN7, unicidade de CPF/e-mail e hash BCrypt.
        Cliente salvo = clienteService.cadastrar(cliente);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse("CLIENTE", salvo.getCpf(), salvo.getNome(), salvo.getEmail(), true));
    }

    @PostMapping("/cadastro-gerente")
    public ResponseEntity<AuthResponse> cadastrarGerente(@Valid @RequestBody GerenteRequest request) {
        validarChaveAdministrador(request.chaveAdmin());

        if (gerenteRepository.existsByEmail(request.email()) || gerenteRepository.existsByMatricula(request.matricula())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        ValidacaoSenha.validar(request.senha());

        Gerente gerente = new Gerente();
        gerente.setNome(request.nome());
        gerente.setEmail(request.email());
        gerente.setSenha(passwordEncoder.encode(request.senha()));
        gerente.setMatricula(request.matricula());
        gerenteRepository.save(gerente);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse("GERENTE", gerente.getMatricula(), gerente.getNome(), gerente.getEmail(), true));
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
     * BUG-15: comparação em tempo constante para evitar timing attack.
     * A chave nunca é registrada em log.
     */
    private void validarChaveAdministrador(String chaveInformada) {
        if (chaveCadastroGerente == null || chaveCadastroGerente.isBlank()) {
            throw new AccessDeniedException(
                    "O cadastro de gerentes está desabilitado. Defina a variável de ambiente DGMASTER_ADMIN_KEY.");
        }
        byte[] esperado = chaveCadastroGerente.getBytes(StandardCharsets.UTF_8);
        byte[] informado = (chaveInformada == null ? "" : chaveInformada).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(esperado, informado)) {
            throw new AccessDeniedException("Chave de administrador inválida.");
        }
    }
}
