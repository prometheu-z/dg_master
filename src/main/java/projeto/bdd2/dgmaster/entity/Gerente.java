package projeto.bdd2.dgmaster.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "gerente") @Getter @Setter @NoArgsConstructor
public class Gerente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer idGerente;
    @Column(nullable = false, length = 150) private String nome;
    @Column(nullable = false, unique = true, length = 150) private String email;
    @Column(nullable = false, length = 255) private String senha;
    @OneToMany(mappedBy = "gerente") private List<Jogo> jogos = new ArrayList<>();
}
