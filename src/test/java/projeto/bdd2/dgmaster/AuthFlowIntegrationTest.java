package projeto.bdd2.dgmaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import projeto.bdd2.dgmaster.auth.AuthController;
import projeto.bdd2.dgmaster.auth.AuthResponse;
import projeto.bdd2.dgmaster.auth.CadastroRequest;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.repository.ClienteRepository;

import java.util.UUID;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthFlowIntegrationTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldRegisterClientAndHashPassword() {
        String cpf = "123456789" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 100);
        String email = "cliente.auth." + UUID.randomUUID() + "@test.com";

        ResponseEntity<AuthResponse> response = authController.cadastrar(
                new CadastroRequest("CLIENTE", cpf, "Cliente Auth", email, "Senha@123", "1990-01-15"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().tipo()).isEqualTo("CLIENTE");
        assertThat(response.getBody().email()).isEqualTo(email);

        Cliente cliente = clienteRepository.findByEmail(email).orElseThrow();
        assertThat(cliente.getCpf()).isEqualTo(cpf);
        assertThat(passwordEncoder.matches("Senha@123", cliente.getSenha())).isTrue();
        assertThat(cliente.getSenha()).isNotEqualTo("Senha@123");
    }

    @Test
    void shouldNotClaimTwoFactorWasValidatedWithoutAConfiguredVerifier() {
        ResponseEntity<Map<String, String>> response = authController.verificarDoisFatores(Map.of("codigo", "123456"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(response.getBody()).containsEntry("mensagem", "A validação 2FA ainda não está configurada.");
    }
}
