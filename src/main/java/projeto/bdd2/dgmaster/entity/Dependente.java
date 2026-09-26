package projeto.bdd2.dgmaster.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "dependente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Dependente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idDependente;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_cpf", nullable = false)
    private Cliente cliente;
}
