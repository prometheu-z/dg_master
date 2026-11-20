package projeto.bdd2.dgmaster.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapeamento fiel da tabela {@code cliente} do script físico (RFC-003 / JPA-01).
 * A PK é {@code id_cliente}; o CPF é uma coluna única de 11 dígitos.
 */
@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 11, columnDefinition = "CHAR(11)")
    private String cpf;

    @Column(length = 20)
    private String telefone;

    @Column(nullable = false, length = 255)
    private String senha;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "data_cadastro", insertable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @OneToMany(mappedBy = "cliente")
    private List<Dependente> dependentes = new ArrayList<>();

    @OneToMany(mappedBy = "cliente")
    private List<Aluguel> alugueis = new ArrayList<>();
}
