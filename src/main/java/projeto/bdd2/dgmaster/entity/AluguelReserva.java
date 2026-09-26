package projeto.bdd2.dgmaster.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "aluguel_reserva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AluguelReserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAluguel;

    @Column(nullable = false)
    private LocalDateTime dataHoraReserva;

    @Column
    private LocalDateTime dataLimiteRetirada;

    private LocalDateTime dataLimiteDevolucao;

    private LocalDateTime dataDevolucaoReal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAluguel status;

    @Column(nullable = false)
    private Double valorTotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal creditoAplicado = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer quantidadeRenovacoes = 0;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_cpf", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "dependente_id")
    private Dependente dependente;

    @Column(columnDefinition = "TEXT")
    private String termosDanos;

    @Column(columnDefinition = "TEXT")
    private String termosContrato;

    private Double precificacaoMulta;
    private LocalDateTime dataAssinatura;

    @OneToMany(mappedBy = "aluguelReserva", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemAluguel> itensAluguel = new ArrayList<>();

    @OneToMany(mappedBy = "aluguelReserva")
    private List<Penalidade> penalidades = new ArrayList<>();
}
