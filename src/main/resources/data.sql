-- Dados iniciais grandes: acervo + clientes + compras/alugueis
-- Fix: booleanos como 1/0 para compatibilidade total com inicializador do Spring Boot

-- Cliente 1
INSERT INTO cliente (cpf, nome, email, senha, data_nascimento, status_conta, credito_carteira) VALUES
('12345678901', 'Maria Silva', 'maria.silva@email.com', '$2a$10$hashsimulado', '1990-05-15', 1, 150.00);

-- Dependente de Maria
INSERT INTO dependente (nome, data_nascimento, cliente_cpf) VALUES
('Lucas Silva', '2015-03-10', '12345678901');

-- Cliente 2
INSERT INTO cliente (cpf, nome, email, senha, data_nascimento, status_conta, credito_carteira) VALUES
('98765432109', 'João Santos', 'joao.santos@email.com', '$2a$10$hashsimulado', '1985-11-22', 1, 50.00);

-- Cliente 3
INSERT INTO cliente (cpf, nome, email, senha, data_nascimento, status_conta, credito_carteira) VALUES
('45678912300', 'Ana Costa', 'ana.costa@email.com', '$2a$10$hashsimulado', '1995-08-30', 1, 0.00);

-- Cliente 4
INSERT INTO cliente (cpf, nome, email, senha, data_nascimento, status_conta, credito_carteira) VALUES
('32165498700', 'Pedro Lima', 'pedro.lima@email.com', '$2a$10$hashsimulado', '1988-01-05', 0, 0.00);

-- Gerente
INSERT INTO gerente (nome, email, senha) VALUES
('Carlos Gerente', 'carlos.gerente@email.com', '$2a$10$hashsimulado');

-- ACERVO GRANDE (20 jogos)
INSERT INTO jogo (nome, categoria, genero, faixa_etaria_recomendada, preco_locacao, valor_reposicao, quantidade_estoque, status_disponibilidade, ativo_catalogo, gerente_id) VALUES
('Azul', 'Estratégia', 'Cartas', 8, 25.00, 120.00, 3, 1, 1, 1),
('Ticket to Ride', 'Estratégia', 'Família', 8, 35.00, 150.00, 2, 1, 1, 1),
('Catan', 'Estratégia', 'Negociação', 10, 40.00, 180.00, 4, 1, 1, 1),
('Dixit', 'Criatividade', 'Cartas', 8, 30.00, 130.00, 1, 1, 1, 1),
('Carcassonne', 'Estratégia', 'Família', 7, 28.00, 110.00, 3, 1, 1, 1),
('Pandemic', 'Cooperativo', 'Estratégia', 13, 45.00, 200.00, 2, 1, 1, 1),
('Codinomes', 'Criatividade', 'Cartas', 14, 32.00, 140.00, 2, 1, 1, 1),
('7 Wonders', 'Estratégia', 'Cartas', 10, 38.00, 160.00, 1, 0, 1, 1),
('Splendor', 'Estratégia', 'Cartas', 10, 30.00, 120.00, 3, 1, 1, 1),
('King of Tokyo', 'Estratégia', 'Família', 8, 35.00, 150.00, 2, 1, 1, 1),
('Love Letter', 'Estratégia', 'Cartas', 10, 22.00, 90.00, 5, 1, 1, 1),
('Mysterium', 'Cooperativo', 'Criatividade', 10, 42.00, 170.00, 1, 1, 1, 1),
('Dobble', 'Família', 'Cartas', 6, 20.00, 85.00, 4, 1, 1, 1),
('Uno', 'Família', 'Cartas', 7, 18.00, 70.00, 6, 1, 1, 1),
('Magic: The Gathering Starter', 'Estratégia', 'Cartas', 13, 50.00, 220.00, 0, 0, 1, 1),
('Root', 'Estratégia', 'Família', 10, 55.00, 240.00, 2, 1, 1, 1),
('Gloomhaven', 'Estratégia', 'Cooperativo', 14, 120.00, 500.00, 1, 1, 1, 1),
('Terraforming Mars', 'Estratégia', 'Cartas', 12, 80.00, 350.00, 1, 1, 1, 1),
('Wingspan', 'Estratégia', 'Família', 10, 48.00, 190.00, 2, 1, 1, 1),
('Brass: Birmingham', 'Estratégia', 'Família', 14, 95.00, 380.00, 1, 1, 1, 1);

-- ALUGUÉIS (compras) para clientes
INSERT INTO aluguel_reserva (data_hora_reserva, data_limite_retirada, data_limite_devolucao, data_devolucao_real, status, valor_total, credito_aplicado, quantidade_renovacoes, cliente_cpf, dependente_id, termos_danos, termos_contrato, precificacao_multa, data_assinatura) VALUES
(NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 14 DAY), NULL, 'RESERVADO', 55.00, 0.00, 0, '12345678901', 1, 'Termos padrão', 'Contrato padrão', 0.00, NOW()),
(DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'DEVOLVIDO', 35.00, 10.00, 0, '98765432109', NULL, 'Termos padrão', 'Contrato padrão', 0.00, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, 'ATRASADO', 80.00, 0.00, 1, '45678912300', NULL, 'Termos padrão', 'Contrato padrão', 15.00, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), NULL, 'RETIRADO', 45.00, 0.00, 0, '98765432109', NULL, 'Termos padrão', 'Contrato padrão', 0.00, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), NULL, 'EXPIRADO', 25.00, 0.00, 0, '12345678901', NULL, 'Termos padrão', 'Contrato padrão', 0.00, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), NULL, 'RESERVADO', 120.00, 30.00, 0, '45678912300', NULL, 'Termos padrão', 'Contrato padrão', 0.00, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY), 'DEVOLVIDO', 28.00, 0.00, 0, '32165498700', NULL, 'Termos padrão', 'Contrato padrão', 0.00, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 23 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), NULL, 'CANCELADO', 0.00, 0.00, 0, '98765432109', NULL, 'Termos padrão', 'Contrato padrão', 0.00, DATE_SUB(NOW(), INTERVAL 25 DAY));

-- Itens dos aluguéis (contém)
INSERT INTO contem (jogo_codigo, aluguel_id, valor_unitario_cobrado) VALUES
(1, 1, 55.00),
(2, 2, 35.00),
(3, 3, 40.00),
(4, 3, 40.00),
(5, 4, 45.00),
(6, 5, 25.00),
(7, 6, 60.00),
(8, 6, 60.00),
(9, 7, 28.00),
(10, 8, 0.00);

-- Penalidades (multas para atrasados)
INSERT INTO penalidade (tipo_penalidade, valor_multa, multa_paga, data_fim_suspensao, aluguel_id) VALUES
('Atraso na devolução', 15.00, 0, DATE_ADD(NOW(), INTERVAL 30 DAY), 3),
('Dano ao jogo', 30.00, 0, NULL, 3);
