package projeto.bdd2.dgmaster.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Contrato de aluguel/reserva. Todos os campos derivados
 * ({@code dataExpiracaoReserva}, {@code dataRetirada}, {@code devolucaoEfetiva},
 * {@code valorDesconto}, {@code valorTotal}, {@code statusAluguel}) são
 * controlados pelas triggers/procedures do MySQL (RFC-003, D5/JPA-02).
 */
@Entity
@Table(name = "aluguel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Aluguel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aluguel")
    private Integer idAluguel;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    /** Preenchido exclusivamente na retirada por um gerente. */
    @ManyToOne
    @JoinColumn(name = "id_gerente")
    private Gerente gerente;

    @Column(name = "data_reserva", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dataReserva;

    @Column(name = "data_expiracao_reserva", insertable = false, updatable = false)
    private LocalDateTime dataExpiracaoReserva;

    @Column(name = "data_retirada", insertable = false, updatable = false)
    private LocalDateTime dataRetirada;

    @Column(name = "devolucao_prevista", nullable = false)
    private LocalDate devolucaoPrevista;

    @Column(name = "devolucao_efetiva", insertable = false, updatable = false)
    private LocalDate devolucaoEfetiva;

    @Column(name = "valor_desconto", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDesconto = BigDecimal.ZERO;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "qtd_renovacoes", nullable = false)
    private Integer qtdRenovacoes = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_aluguel", nullable = false, length = 20)
    private StatusAluguel statusAluguel = StatusAluguel.RESERVADO;

    @OneToMany(mappedBy = "aluguel")
    private List<ItemAluguel> itensAluguel = new ArrayList<>();

    @OneToMany(mappedBy = "aluguel")
    private List<Penalidade> penalidades = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "aluguel_dependente",
            joinColumns = @JoinColumn(name = "id_aluguel"),
            inverseJoinColumns = @JoinColumn(name = "id_dependente"))
    private Set<Dependente> dependentes = new LinkedHashSet<>();
}
