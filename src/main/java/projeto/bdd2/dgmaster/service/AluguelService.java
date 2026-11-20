package projeto.bdd2.dgmaster.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projeto.bdd2.dgmaster.entity.Aluguel;
import projeto.bdd2.dgmaster.entity.Cliente;
import projeto.bdd2.dgmaster.entity.Dependente;
import projeto.bdd2.dgmaster.entity.StatusAluguel;
import projeto.bdd2.dgmaster.repository.AluguelRepository;
import projeto.bdd2.dgmaster.repository.ClienteRepository;
import projeto.bdd2.dgmaster.repository.DependenteRepository;
import projeto.bdd2.dgmaster.util.CpfValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Camada fina de orquestração (RFC-003, D5): toda baixa/devolução de estoque,
 * cálculo de desconto, multa e transição de status é executada pelas
 * triggers/procedures do MySQL. O Java apenas chama {@code sp_*} e recarrega
 * o estado calculado pelo banco (JPA-02).
 */
@Service
public class AluguelService {

    private static final int PRAZO_ALUGUEL_DIAS = 7;
    private static final int DIAS_RENOVACAO = 7;

    private final AluguelRepository aluguelRepository;
    private final ClienteRepository clienteRepository;
    private final DependenteRepository dependenteRepository;
    private final JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public AluguelService(AluguelRepository aluguelRepository,
                          ClienteRepository clienteRepository,
                          DependenteRepository dependenteRepository,
                          JdbcTemplate jdbcTemplate) {
        this.aluguelRepository = aluguelRepository;
        this.clienteRepository = clienteRepository;
        this.dependenteRepository = dependenteRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Aluguel reservar(String cpfCliente, List<Integer> idsJogos) {
        return solicitarReserva(cpfCliente, null, idsJogos);
    }

    @Transactional
    public Aluguel solicitarReserva(String cpfCliente, Integer idDependente, List<Integer> idsJogos) {
        if (idsJogos == null || idsJogos.isEmpty()) {
            throw new IllegalArgumentException("É necessário selecionar pelo menos um jogo");
        }
        if (Set.copyOf(idsJogos).size() != idsJogos.size()) {
            throw new IllegalArgumentException("A solicitação não pode conter jogos duplicados");
        }

        Cliente cliente = clienteRepository.findByCpf(CpfValidator.sanitizar(cpfCliente))
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + cpfCliente));

        Dependente dependente = null;
        if (idDependente != null) {
            dependente = dependenteRepository.findById(idDependente)
                    .orElseThrow(() -> new IllegalArgumentException("Dependente não encontrado: " + idDependente));
            if (!dependente.getCliente().getIdCliente().equals(cliente.getIdCliente())) {
                throw new IllegalArgumentException("Dependente não pertence ao cliente informado");
            }
        }

        Integer idAluguel = chamarCriarReserva(cliente.getIdCliente(), PRAZO_ALUGUEL_DIAS);
        for (Integer idJogo : idsJogos) {
            chamarAdicionarItem(idAluguel, idJogo, 1);
        }

        if (dependente != null) {
            entityManager.createNativeQuery(
                            "INSERT INTO aluguel_dependente (id_aluguel, id_dependente) VALUES (?, ?)")
                    .setParameter(1, idAluguel)
                    .setParameter(2, dependente.getIdDependente())
                    .executeUpdate();
        }

        return recarregar(idAluguel);
    }

    @Transactional
    public Aluguel cancelarReserva(Integer idAluguel, String cpfTitular) {
        Aluguel aluguel = buscarAluguel(idAluguel);
        validarTitular(aluguel, cpfTitular);
        chamarProcedimento("sp_cancelar_reserva", idAluguel);
        return recarregar(idAluguel);
    }

    /**
     * BUG-05: operação exclusiva de gerente. O id do gerente autenticado é
     * obrigatório e repassado a {@code sp_retirar_aluguel}.
     */
    @Transactional
    public Aluguel retirarAluguel(Integer idAluguel, Integer idGerente) {
        if (idGerente == null) {
            throw new IllegalStateException("A retirada deve ser registrada por um gerente");
        }
        chamarProcedimento("sp_retirar_aluguel", idAluguel, idGerente);
        return recarregar(idAluguel);
    }

    @Transactional
    public Aluguel renovarAluguel(Integer idAluguel, String cpfTitular) {
        Aluguel aluguel = buscarAluguel(idAluguel);
        validarTitular(aluguel, cpfTitular);
        chamarProcedimento("sp_renovar_aluguel", idAluguel, DIAS_RENOVACAO);
        return recarregar(idAluguel);
    }

    @Transactional
    public Aluguel finalizarAluguel(Integer idAluguel, LocalDate dataDevolucao) {
        chamarProcedimento("sp_finalizar_aluguel", idAluguel, dataDevolucao);
        return recarregar(idAluguel);
    }

    @Transactional
    public int expirarReservasVencidas() {
        // sp_expirar_reservas restaura estoque e audita via triggers.
        jdbcTemplate.execute("CALL sp_expirar_reservas()");
        return 0;
    }

    @Transactional
    public int registrarAtrasos() {
        // sp_registrar_atrasos é idempotente (UNIQUE id_aluguel + motivo).
        jdbcTemplate.execute("CALL sp_registrar_atrasos()");
        return 0;
    }

    @Transactional(readOnly = true)
    public List<Aluguel> listarPrazosGerenciais() {
        return aluguelRepository.findByStatusAluguelIn(
                List.of(StatusAluguel.RESERVADO, StatusAluguel.RETIRADO));
    }

    @Transactional(readOnly = true)
    public Aluguel buscarAluguel(Integer idAluguel) {
        return aluguelRepository.findById(idAluguel)
                .orElseThrow(() -> new IllegalArgumentException("Aluguel não encontrado: " + idAluguel));
    }

    private Integer chamarCriarReserva(Integer idCliente, int dias) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_criar_reserva");
        query.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Integer.class, ParameterMode.OUT);
        query.setParameter(1, idCliente);
        query.setParameter(2, dias);
        query.execute();
        return (Integer) query.getOutputParameterValue(3);
    }

    private void chamarAdicionarItem(Integer idAluguel, Integer idJogo, int quantidade) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_adicionar_item");
        query.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Integer.class, ParameterMode.IN);
        query.setParameter(1, idAluguel);
        query.setParameter(2, idJogo);
        query.setParameter(3, quantidade);
        query.execute();
    }

    private void chamarProcedimento(String nome, Integer idAluguel) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery(nome);
        query.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
        query.setParameter(1, idAluguel);
        query.execute();
    }

    private void chamarProcedimento(String nome, Integer idAluguel, Integer segundoParametro) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery(nome);
        query.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Integer.class, ParameterMode.IN);
        query.setParameter(1, idAluguel);
        query.setParameter(2, segundoParametro);
        query.execute();
    }

    private void chamarProcedimento(String nome, Integer idAluguel, LocalDate data) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery(nome);
        query.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, LocalDate.class, ParameterMode.IN);
        query.setParameter(1, idAluguel);
        query.setParameter(2, data);
        query.execute();
    }

    private Aluguel recarregar(Integer idAluguel) {
        entityManager.flush();
        entityManager.clear();
        return buscarAluguel(idAluguel);
    }

    private void validarTitular(Aluguel aluguel, String cpfTitular) {
        if (!aluguel.getCliente().getCpf().equals(CpfValidator.sanitizar(cpfTitular))) {
            throw new IllegalArgumentException("Somente o titular pode realizar esta operação");
        }
    }
}
