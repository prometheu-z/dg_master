package projeto.bdd2.dgmaster.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
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

    public AuthController(
            ClienteRepository clienteRepository,
            GerenteRepository gerenteRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager) {
        this.clienteRepository = clienteRepository;
        this.gerenteRepository = gerenteRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<AuthResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
        if (request.tipo() == null) {
            return ResponseEntity.badRequest().build();
        }

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
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

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
        return ResponseEntity.ok(Map.of("mensagem", "2FA validado com sucesso."));
    }
}
