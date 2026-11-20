package projeto.bdd2.dgmaster.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
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
    @Column(name = "id_penalidade")
    private Integer idPenalidade;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_aluguel", nullable = false)
    private Aluguel aluguel;

    @Column(name = "motivo_penalidade", nullable = false, length = 100)
    private String motivoPenalidade;

    @Column(name = "valor_multa", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMulta;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false, length = 10)
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;

    @Column(name = "data_geracao", insertable = false, updatable = false)
    private LocalDateTime dataGeracao;
}
