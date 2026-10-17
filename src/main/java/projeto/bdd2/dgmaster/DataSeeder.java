package projeto.bdd2.dgmaster;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public DataSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) throws Exception {
        // Só popula se o acervo estiver vazio (evita duplicar quando já existe)
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM jogo", Integer.class);
        if (count != null && count > 0) {
            return;
        }

        jdbc.execute("INSERT INTO cliente (cpf, nome, email, senha, data_nascimento, status_conta, credito_carteira) VALUES ('12345678901','Maria Silva','maria@email.com','$2a$10$hash',DATE('1990-05-15'),1,150.00)");
        jdbc.execute("INSERT INTO dependente (nome, data_nascimento, cliente_cpf) VALUES ('Lucas Silva','2015-03-10','12345678901')");
        jdbc.execute("INSERT INTO gerente (nome, email, senha) VALUES ('Carlos Gerente','carlos@email.com','$2a$10$hash')");

        jdbc.execute("INSERT INTO jogo (nome, categoria, genero, faixa_etaria_recomendada, preco_locacao, valor_reposicao, quantidade_estoque, status_disponibilidade, ativo_catalogo, gerente_id) VALUES ('Azul','Estratégia','Cartas',8,25.00,120.00,3,1,1,1)");
        jdbc.execute("INSERT INTO jogo (nome, categoria, genero, faixa_etaria_recomendada, preco_locacao, valor_reposicao, quantidade_estoque, status_disponibilidade, ativo_catalogo, gerente_id) VALUES ('Ticket to Ride','Estratégia','Família',8,35.00,150.00,2,1,1,1)");
        jdbc.execute("INSERT INTO jogo (nome, categoria, genero, faixa_etaria_recomendada, preco_locacao, valor_reposicao, quantidade_estoque, status_disponibilidade, ativo_catalogo, gerente_id) VALUES ('Catan','Estratégia','Negociação',10,40.00,180.00,4,1,1,1)");
        jdbc.execute("INSERT INTO jogo (nome, categoria, genero, faixa_etaria_recomendada, preco_locacao, valor_reposicao, quantidade_estoque, status_disponibilidade, ativo_catalogo, gerente_id) VALUES ('Dixit','Criatividade','Cartas',8,30.00,130.00,1,1,1,1)");
        jdbc.execute("INSERT INTO jogo (nome, categoria, genero, faixa_etaria_recomendada, preco_locacao, valor_reposicao, quantidade_estoque, status_disponibilidade, ativo_catalogo, gerente_id) VALUES ('Carcassonne','Estratégia','Família',7,28.00,110.00,3,1,1,1)");

        jdbc.execute("INSERT INTO aluguel_reserva (data_hora_reserva, status, valor_total, quantidade_renovacoes, cliente_cpf, dependente_id) VALUES (NOW(),'RESERVADO',55.00,0,'12345678901',1)");
    }
}
