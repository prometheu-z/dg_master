package projeto.bdd2.dgmaster.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ItemAluguelId implements Serializable {

    @Column(name = "id_aluguel")
    private Integer idAluguel;

    @Column(name = "id_jogo")
    private Integer idJogo;
}
