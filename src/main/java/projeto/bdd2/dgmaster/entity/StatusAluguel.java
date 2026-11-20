package projeto.bdd2.dgmaster.entity;

/**
 * Máquina de estados oficial do banco (trg_aluguel_estados_bu):
 * RESERVADO -> RETIRADO | CANCELADO | EXPIRADO ; RETIRADO -> FINALIZADO.
 */
public enum StatusAluguel {
    RESERVADO,
    RETIRADO,
    FINALIZADO,
    EXPIRADO,
    CANCELADO
}
