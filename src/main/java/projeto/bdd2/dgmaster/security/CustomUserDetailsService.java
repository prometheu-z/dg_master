package projeto.bdd2.dgmaster.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Gerente;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.GerenteRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final ClienteRepository clienteRepository;
    private final GerenteRepository gerenteRepository;

    public CustomUserDetailsService(ClienteRepository clienteRepository, GerenteRepository gerenteRepository) {
        this.clienteRepository = clienteRepository;
        this.gerenteRepository = gerenteRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return clienteRepository.findByEmail(email)
                .map(cliente -> new UserPrincipal(
                        cliente.getEmail(),
                        cliente.getSenha(),
                        cliente.getNome(),
                        cliente.getCpf(),
                        "CLIENTE"))
                .or(() -> gerenteRepository.findByEmail(email)
                        .map(gerente -> new UserPrincipal(
                                gerente.getEmail(),
                                gerente.getSenha(),
                                gerente.getNome(),
                                String.valueOf(gerente.getIdGerente()),
                                "GERENTE")))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }
}
