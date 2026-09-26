package projeto.bdd2.dgmaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Dependente;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.service.ClienteService;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ClienteDependenteIntegrationTest {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    void shouldCreateDependentForAdultClient() {
        String cpf = "123456789" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10);
        Cliente cliente = new Cliente();
        cliente.setCpf(cpf);
        cliente.setNome("Titular Adulto");
        cliente.setEmail("titular." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha123");
        cliente.setDataNascimento(LocalDate.of(1990, 1, 15));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);

        Dependente dependente = new Dependente();
        dependente.setNome("Dependente Menor");
        dependente.setDataNascimento(LocalDate.now().minusYears(12));

        Dependente salvo = clienteService.adicionarDependente(cpf, dependente);

        assertThat(salvo.getIdDependente()).isNotNull();
        assertThat(salvo.getCliente().getCpf()).isEqualTo(cpf);
    }

    @Test
    void shouldRejectRegistrationWhenClientIsUnderAge() {
        String cpf = "987654321" + String.format("%02d", Math.abs(UUID.randomUUID().hashCode()) % 90 + 10);
        Cliente cliente = new Cliente();
        cliente.setCpf(cpf);
        cliente.setNome("Titular Menor");
        cliente.setEmail("menor." + UUID.randomUUID() + "@teste.com");
        cliente.setSenha("senha123");
        cliente.setDataNascimento(LocalDate.now().minusYears(17));
        cliente.setStatusConta(true);
        clienteRepository.save(cliente);

        Dependente dependente = new Dependente();
        dependente.setNome("Dependente");
        dependente.setDataNascimento(LocalDate.now().minusYears(8));

        assertThatThrownBy(() -> clienteService.adicionarDependente(cpf, dependente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("18 anos");
    }
}
