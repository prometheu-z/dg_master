package projeto.bdd2.dgmaster.web;

import projeto.bdd2.dgmaster.entity.Jogo;

import java.math.BigDecimal;

public record JogoForm(
        String nome,
        String categoria,
        String genero,
        Integer faixaEtariaRecomendada,
        BigDecimal precoLocacao,
        BigDecimal valorReposicao,
        Integer quantidadeEstoque) {

    public Jogo toEntity() {
        Jogo jogo = new Jogo();
        jogo.setNome(nome);
        jogo.setCategoria(categoria);
        jogo.setGenero(genero);
        jogo.setFaixaEtariaRecomendada(faixaEtariaRecomendada);
        jogo.setPrecoLocacao(precoLocacao);
        jogo.setValorReposicao(valorReposicao);
        jogo.setQuantidadeEstoque(quantidadeEstoque);
        return jogo;
    }
}