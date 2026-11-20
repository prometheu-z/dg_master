package projeto.bdd2.dgmaster.catalogo.api;

import projeto.bdd2.dgmaster.entity.Jogo;

import java.math.BigDecimal;

public record JogoResponse(
        Integer idJogo,
        String titulo,
        String categoria,
        String descricao,
        Integer faixaEtariaMinima,
        BigDecimal valorDiaria,
        BigDecimal valorReposicao,
        Integer quantidadeEstoque,
        boolean disponivel) {

    public static JogoResponse from(Jogo jogo) {
        return new JogoResponse(
                jogo.getIdJogo(),
                jogo.getTitulo(),
                jogo.getCategoria() == null ? null : jogo.getCategoria().getNomeCategoria(),
                jogo.getDescricao(),
                jogo.getFaixaEtariaMinima(),
                jogo.getValorDiaria(),
                jogo.getValorReposicao(),
                jogo.getQuantidadeEstoque(),
                jogo.isDisponivel());
    }
}
