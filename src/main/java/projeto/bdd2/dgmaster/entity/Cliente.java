package projeto.bdd2.dgmaster.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {
    @Id
    @Column(length = 11, nullable = false, updatable = false)
    private String cpf;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String senha;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(name = "status_conta", nullable = false)
    private boolean statusConta = true;

        @Column(name = "credito_carteira", nullable = false, precision = 10, scale = 2,
            columnDefinition = "DECIMAL(10,2) NOT NULL DEFAULT 0.00")
        private BigDecimal creditoCarteira = BigDecimal.ZERO;

        private LocalDateTime dataFimSuspensao;

    @OneToMany(mappedBy = "cliente")
    private List<Dependente> dependentes = new ArrayList<>();

    @OneToMany(mappedBy = "cliente")
    private List<AluguelReserva> alugueis = new ArrayList<>();
}
