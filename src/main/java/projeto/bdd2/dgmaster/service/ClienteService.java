package projeto.bdd2.dgmaster.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Dependente;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.DependenteRepository;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final DependenteRepository dependenteRepository;

    public ClienteService(ClienteRepository clienteRepository, DependenteRepository dependenteRepository) {
        this.clienteRepository = clienteRepository;
        this.dependenteRepository = dependenteRepository;
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
}
