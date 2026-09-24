package projeto.bdd2.dgmaster.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "aluguel_reserva") @Getter @Setter @NoArgsConstructor
public class AluguelReserva {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer idAluguel;
    @Column(nullable = false) private LocalDateTime dataHoraReserva;
    @Column(nullable = false) private LocalDateTime dataLimiteRetirada;
    private LocalDateTime dataDevolucaoReal;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatusAluguel status;
    @Column(nullable = false) private Double valorTotal;
    @Column(nullable = false) private Integer quantidadeRenovacoes = 0;
    @ManyToOne(optional = false) @JoinColumn(name = "cliente_cpf", nullable = false) private Cliente cliente;
    @ManyToOne @JoinColumn(name = "dependente_id") private Dependente dependente;
    @Column(columnDefinition = "TEXT") private String termosDanos;
    private Double precificacaoMulta;
    private LocalDateTime dataAssinatura;
    @ManyToMany(mappedBy = "alugueis") private List<Jogo> jogos = new ArrayList<>();
    @OneToMany(mappedBy = "aluguelReserva") private List<Penalidade> penalidades = new ArrayList<>();
}
