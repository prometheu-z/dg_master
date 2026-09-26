package projeto.bdd2.dgmaster.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Dependente;
import projeto.bdd2.dgmaster.entity.StatusAluguel;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.DependenteRepository;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final DependenteRepository dependenteRepository;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository clienteRepository, DependenteRepository dependenteRepository, PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.dependenteRepository = dependenteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Cliente cadastrar(Cliente cliente) {
        if (cliente.getEmail() == null || cliente.getEmail().isBlank()) {
            throw new IllegalArgumentException("E-mail obrigatório");
        }

        if (clienteRepository.findByEmail(cliente.getEmail()).isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }

        if (clienteRepository.findById(cliente.getCpf()).isPresent()) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }

        if (Period.between(cliente.getDataNascimento(), LocalDate.now()).getYears() < 18) {
            throw new IllegalArgumentException("Cliente deve ter 18 anos ou mais");
        }

        cliente.setSenha(passwordEncoder.encode(cliente.getSenha()));
        cliente.setStatusConta(true);
        cliente.setCreditoCarteira(cliente.getCreditoCarteira() != null ? cliente.getCreditoCarteira() : java.math.BigDecimal.ZERO);
        return clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorCpf(String cpf) {
        return clienteRepository.findById(cpf)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + cpf));
    }

    @Transactional(readOnly = true)
    public List<Dependente> listarDependentes(String cpf) {
        return dependenteRepository.findByClienteCpf(cpf);
    }

    @Transactional
    public Dependente adicionarDependente(String cpfCliente, Dependente dependente) {
        Cliente cliente = buscarPorCpf(cpfCliente);

        if (Period.between(cliente.getDataNascimento(), LocalDate.now()).getYears() < 18) {
            throw new IllegalArgumentException("Cliente precisa ter pelo menos 18 anos para cadastrar dependentes");
        }

        if (dependente == null) {
            throw new IllegalArgumentException("Dependente obrigatório");
        }

        if (dependente.getNome() == null || dependente.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do dependente obrigatório");
        }

        dependente.setCliente(cliente);
        return dependenteRepository.save(dependente);
    }

    @Transactional
    public Cliente atualizarPerfil(String cpf, String nome, String email) {
        Cliente cliente = buscarPorCpf(cpf);
        
        if (nome != null && !nome.isBlank()) {
            cliente.setNome(nome);
        }
        
        if (email != null && !email.isBlank() && !email.equals(cliente.getEmail())) {
            if (clienteRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("E-mail já cadastrado para outro usuário");
            }
            cliente.setEmail(email);
        }
        
        return clienteRepository.save(cliente);
    }

    @Transactional
    public void removerDependente(String cpfCliente, Integer idDependente) {
        Cliente cliente = buscarPorCpf(cpfCliente);
        Dependente dependente = dependenteRepository.findById(idDependente)
                .orElseThrow(() -> new IllegalArgumentException("Dependente não encontrado"));
        
        if (!dependente.getCliente().getCpf().equals(cpfCliente)) {
            throw new IllegalArgumentException("Dependente não pertence a este cliente");
        }
        
        // Verificar se dependente tem aluguéis ativos
        boolean temAlugueisAtivos = cliente.getAlugueis().stream()
                .anyMatch(a -> a.getDependente() != null && 
                              a.getDependente().getIdDependente().equals(idDependente) &&
                              (a.getStatus() == StatusAluguel.RESERVADO || 
                               a.getStatus() == StatusAluguel.RETIRADO || 
                               a.getStatus() == StatusAluguel.ATRASADO));
        
        if (temAlugueisAtivos) {
            throw new IllegalArgumentException("Não é possível remover dependente com aluguéis ativos");
        }
        
        dependenteRepository.delete(dependente);
    }
}
