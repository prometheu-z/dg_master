package projeto.bdd2.dgmaster.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ClienteRepository clienteRepository;
    private final GerenteRepository gerenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public AuthController(
            ClienteRepository clienteRepository,
            GerenteRepository gerenteRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.clienteRepository = clienteRepository;
        this.gerenteRepository = gerenteRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<AuthResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
        if (request.tipo().equalsIgnoreCase("CLIENTE")) {
            if (clienteRepository.existsByEmail(request.email())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            Cliente cliente = new Cliente();
            cliente.setCpf(request.cpf());
            cliente.setNome(request.nome());
            cliente.setEmail(request.email());
            cliente.setSenha(passwordEncoder.encode(request.senha()));
            cliente.setDataNascimento(LocalDate.parse(request.dataNascimento()));
            cliente.setStatusConta(true);
            clienteRepository.save(cliente);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthResponse("CLIENTE", cliente.getCpf(), cliente.getNome(), cliente.getEmail(), true));
        }

        if (request.tipo().equalsIgnoreCase("GERENTE")) {
            if (gerenteRepository.existsByEmail(request.email())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            Gerente gerente = new Gerente();
            gerente.setNome(request.nome());
            gerente.setEmail(request.email());
            gerente.setSenha(passwordEncoder.encode(request.senha()));
            gerenteRepository.save(gerente);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthResponse("GERENTE", null, gerente.getNome(), gerente.getEmail(), true));
        }

        return ResponseEntity.badRequest().build();
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
}
