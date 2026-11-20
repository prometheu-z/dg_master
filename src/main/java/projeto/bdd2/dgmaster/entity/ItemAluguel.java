package projeto.bdd2.dgmaster.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "item_aluguel")
@Getter
@Setter
@NoArgsConstructor
public class ItemAluguel {

    @EmbeddedId
    private ItemAluguelId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idJogo")
    @JoinColumn(name = "id_jogo", nullable = false)
    private Jogo jogo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idAluguel")
    @JoinColumn(name = "id_aluguel", nullable = false)
    private Aluguel aluguel;

    @Column(name = "quantidade_alugada", nullable = false)
    private Integer quantidadeAlugada = 1;

    @Column(name = "valor_cobrado", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorCobrado = BigDecimal.ZERO;
}
