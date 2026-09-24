package projeto.bdd2.dgmaster.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity @Table(name = "penalidade") @Getter @Setter @NoArgsConstructor
public class Penalidade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer idPenalidade;
    @Column(nullable = false, length = 50) private String tipoPenalidade;
    @Column(nullable = false) private Double valorMulta;
    private LocalDate dataFimSuspensao;
    @ManyToOne(optional = false) @JoinColumn(name = "aluguel_id", nullable = false) private AluguelReserva aluguelReserva;
}
