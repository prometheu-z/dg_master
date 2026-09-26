package projeto.bdd2.dgmaster.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Dependente;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.DependenteRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private DependenteRepository dependenteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setCpf("12345678901");
        cliente.setNome("Cliente Teste");
        cliente.setEmail("cliente@teste.com");
        cliente.setSenha("senha123");
        cliente.setDataNascimento(LocalDate.of(1990, 1, 1));
        cliente.setStatusConta(true);
    }

    @Test
    void deveCadastrarClienteComSucesso() {
        when(clienteRepository.findByEmail("cliente@teste.com")).thenReturn(Optional.empty());
        when(clienteRepository.findById("12345678901")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha123")).thenReturn("hashed-password");
        when(clienteRepository.save(any())).thenReturn(cliente);

        Cliente resultado = clienteService.cadastrar(cliente);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getSenha()).isEqualTo("hashed-password");
        verify(clienteRepository).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoEmailJaCadastrado() {
        when(clienteRepository.findByEmail("cliente@teste.com")).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> clienteService.cadastrar(cliente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E-mail já cadastrado");
    }

    @Test
    void deveLancarExcecaoQuandoCpfJaCadastrado() {
        when(clienteRepository.findByEmail("cliente@teste.com")).thenReturn(Optional.empty());
        when(clienteRepository.findById("12345678901")).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> clienteService.cadastrar(cliente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CPF já cadastrado");
    }

    @Test
    void deveLancarExcecaoQuandoClienteMenorDeIdade() {
        cliente.setDataNascimento(LocalDate.now().minusYears(17));
        when(clienteRepository.findByEmail("cliente@teste.com")).thenReturn(Optional.empty());
        when(clienteRepository.findById("12345678901")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.cadastrar(cliente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cliente deve ter 18 anos ou mais");
    }

    @Test
    void deveAdicionarDependenteComSucesso() {
        Dependente dependente = new Dependente();
        dependente.setNome("Dependente Teste");
        dependente.setDataNascimento(LocalDate.of(2010, 1, 1));

        when(clienteRepository.findById("12345678901")).thenReturn(Optional.of(cliente));
        when(dependenteRepository.save(any())).thenReturn(dependente);

        Dependente resultado = clienteService.adicionarDependente("12345678901", dependente);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCliente()).isEqualTo(cliente);
        verify(dependenteRepository).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoClienteNaoEncontradoAoAdicionarDependente() {
        Dependente dependente = new Dependente();
        when(clienteRepository.findById("12345678901")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.adicionarDependente("12345678901", dependente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cliente não encontrado");
    }
}
