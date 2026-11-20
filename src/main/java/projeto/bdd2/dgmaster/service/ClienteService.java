package projeto.bdd2.dgmaster.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Dependente;
import projeto.bdd2.dgmaster.entity.StatusAluguel;
import projeto.bdd2.dgmaster.repository.AluguelRepository;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.DependenteRepository;
import projeto.bdd2.dgmaster.util.CpfValidator;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final DependenteRepository dependenteRepository;
    private final AluguelRepository aluguelRepository;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository clienteRepository,
                          DependenteRepository dependenteRepository,
                          AluguelRepository aluguelRepository,
                          PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.dependenteRepository = dependenteRepository;
        this.aluguelRepository = aluguelRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Cliente cadastrar(Cliente cliente) {
        cliente.setCpf(CpfValidator.sanitizar(cliente.getCpf()));
        CpfValidator.validar(cliente.getCpf());

        if (cliente.getEmail() == null || cliente.getEmail().isBlank()) {
            throw new IllegalArgumentException("E-mail obrigatório");
        }
        if (clienteRepository.findByEmail(cliente.getEmail()).isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
        if (clienteRepository.existsByCpf(cliente.getCpf())) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }

        // RN7 também é reforçada por trg_cliente_valida_bi no MySQL.
        if (Period.between(cliente.getDataNascimento(), LocalDate.now()).getYears() < 18) {
            throw new IllegalArgumentException("RN7: cliente titular deve ter idade mínima de 18 anos");
        }

        cliente.setSenha(passwordEncoder.encode(cliente.getSenha()));
        return clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorCpf(String cpf) {
        return clienteRepository.findByCpf(CpfValidator.sanitizar(cpf))
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + cpf));
    }

    @Transactional(readOnly = true)
    public List<Dependente> listarDependentes(String cpf) {
        return dependenteRepository.findByClienteCpf(CpfValidator.sanitizar(cpf));
    }

    @Transactional
    public Dependente adicionarDependente(String cpfCliente, Dependente dependente) {
        Cliente cliente = buscarPorCpf(cpfCliente);

        if (dependente == null) {
            throw new IllegalArgumentException("Dependente obrigatório");
        }
        if (dependente.getNome() == null || dependente.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do dependente obrigatório");
        }
        if (dependente.getDataNascimento() == null) {
            throw new IllegalArgumentException("Data de nascimento do dependente obrigatória");
        }
        if (dependente.getGrauParentesco() == null || dependente.getGrauParentesco().isBlank()) {
            dependente.setGrauParentesco("NÃO INFORMADO");
        }

        dependente.setCliente(cliente);
        return dependenteRepository.save(dependente);
    }

    @Transactional
    public Cliente atualizarPerfil(String cpf, String nome, String telefone, String email) {
        Cliente cliente = buscarPorCpf(cpf);

        if (nome != null && !nome.isBlank()) {
            cliente.setNome(nome);
        }
        if (telefone != null) {
            cliente.setTelefone(telefone);
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
        buscarPorCpf(cpfCliente);
        Dependente dependente = dependenteRepository.findById(idDependente)
                .orElseThrow(() -> new IllegalArgumentException("Dependente não encontrado"));

        if (!dependente.getCliente().getCpf().equals(CpfValidator.sanitizar(cpfCliente))) {
            throw new IllegalArgumentException("Dependente não pertence a este cliente");
        }

        boolean possuiAluguelAtivo = aluguelRepository.findAll().stream()
                .filter(aluguel -> aluguel.getStatusAluguel() == StatusAluguel.RESERVADO
                        || aluguel.getStatusAluguel() == StatusAluguel.RETIRADO)
                .anyMatch(aluguel -> aluguel.getDependentes().stream()
                        .anyMatch(d -> d.getIdDependente().equals(idDependente)));

        if (possuiAluguelAtivo) {
            throw new IllegalStateException("Não é possível remover dependente com aluguéis ativos");
        }

        dependenteRepository.delete(dependente);
    }
}
