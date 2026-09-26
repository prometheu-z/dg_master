package projeto.bdd2.dgmaster.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "penalidade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Penalidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPenalidade;

    @Column(nullable = false, length = 50)
    private String tipoPenalidade;

    @Column(nullable = false)
    private Double valorMulta;

    @Column(name = "multa_paga", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean multaPaga;

    private LocalDateTime dataFimSuspensao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aluguel_id", nullable = false)
    private AluguelReserva aluguelReserva;
}
