package projeto.bdd2.dgmaster.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "contem")
@Getter
@Setter
@NoArgsConstructor
public class ItemAluguel {

    @EmbeddedId
    private ItemAluguelId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("jogoCodigo")
    @JoinColumn(name = "jogo_codigo", nullable = false)
    private Jogo jogo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("aluguelId")
    @JoinColumn(name = "aluguel_id", nullable = false)
    private AluguelReserva aluguelReserva;

    @Column(name = "valor_unitario_cobrado", precision = 10, scale = 2)
    private BigDecimal valorUnitarioCobrado;

    public ItemAluguel(Jogo jogo, AluguelReserva aluguelReserva, BigDecimal valorUnitarioCobrado) {
        this.jogo = jogo;
        this.aluguelReserva = aluguelReserva;
        this.valorUnitarioCobrado = valorUnitarioCobrado;
        this.id = new ItemAluguelId(jogo.getCodigoJogo(), aluguelReserva.getIdAluguel());
    }
}