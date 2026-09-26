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

    @Column(name = "jogo_codigo")
    private Integer jogoCodigo;

    @Column(name = "aluguel_id")
    private Integer aluguelId;
}