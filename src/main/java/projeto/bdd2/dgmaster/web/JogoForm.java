package projeto.bdd2.dgmaster.web;

import projeto.bdd2.dgmaster.entity.Jogo;

import java.math.BigDecimal;

public record JogoForm(
        String titulo,
        String categoria,
        String descricao,
        Integer faixaEtariaMinima,
        BigDecimal valorDiaria,
        BigDecimal valorReposicao,
        Integer quantidadeEstoque) {

    public Jogo toEntity() {
        Jogo jogo = new Jogo();
        jogo.setTitulo(titulo);
        jogo.setDescricao(descricao);
        jogo.setFaixaEtariaMinima(faixaEtariaMinima == null ? 0 : faixaEtariaMinima);
        jogo.setValorDiaria(valorDiaria);
        jogo.setValorReposicao(valorReposicao);
        jogo.setQuantidadeEstoque(quantidadeEstoque);
        return jogo;
    }
}
