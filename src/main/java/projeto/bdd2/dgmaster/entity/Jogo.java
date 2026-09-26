package projeto.bdd2.dgmaster.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Jogo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigoJogo;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 100)
    private String categoria;

    @Column(length = 100)
    private String genero;

    private Integer faixaEtariaRecomendada;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoLocacao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorReposicao;

    private Integer quantidadeEstoque = 0;
    private boolean statusDisponibilidade = true;
    private boolean ativoCatalogo = true;

    @ManyToOne
    @JoinColumn(name = "gerente_id")
    private Gerente gerente;

    @OneToMany(mappedBy = "jogo")
    private List<ItemAluguel> itensAluguel = new ArrayList<>();
}
