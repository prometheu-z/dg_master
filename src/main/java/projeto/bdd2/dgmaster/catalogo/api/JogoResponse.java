package projeto.bdd2.dgmaster.catalogo.api;

import projeto.bdd2.dgmaster.entity.Jogo;

import java.math.BigDecimal;

public record JogoResponse(
        Integer codigoJogo,
        String nome,
        String categoria,
        String genero,
        Integer faixaEtariaRecomendada,
        BigDecimal precoLocacao,
        BigDecimal valorReposicao,
        Integer quantidadeEstoque,
        boolean statusDisponibilidade) {

    public static JogoResponse from(Jogo jogo) {
        return new JogoResponse(
                jogo.getCodigoJogo(),
                jogo.getNome(),
                jogo.getCategoria(),
                jogo.getGenero(),
                jogo.getFaixaEtariaRecomendada(),
                jogo.getPrecoLocacao(),
                jogo.getValorReposicao(),
                jogo.getQuantidadeEstoque(),
                jogo.isStatusDisponibilidade());
    }
}