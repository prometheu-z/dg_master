-- =====================================================================
-- DG MASTER LTDA. — banco dg_master_db
-- Apêndice A.1 — Projeto Físico (DDL) + Objetos do Banco
-- Compatível com MySQL 8.0+ (testado também em MariaDB 10.11)
-- Execução: mysql -u root -p < 01_schema_objetos.sql
-- ATENÇÃO: este script apaga e recria dg_master_db; use apenas em ambiente descartável.
-- A conta da aplicação deve ser provisionada fora deste script; ver db/02_app_permissions.sql.
-- =====================================================================

DROP DATABASE IF EXISTS dg_master_db;
CREATE DATABASE dg_master_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dg_master_db;

-- ---------------------------------------------------------------------
-- 1. TABELAS
-- ---------------------------------------------------------------------

CREATE TABLE categoria (
  id_categoria   INT AUTO_INCREMENT PRIMARY KEY,
  nome_categoria VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE cliente (
  id_cliente      INT AUTO_INCREMENT PRIMARY KEY,
  nome            VARCHAR(100) NOT NULL,
  cpf             CHAR(11)     NOT NULL UNIQUE,            -- somente dígitos
  telefone        VARCHAR(20),
  senha           VARCHAR(255) NOT NULL,                   -- hash BCrypt (60 chars)
  data_nascimento DATE         NOT NULL,
  email           VARCHAR(254) NOT NULL UNIQUE,
  data_cadastro   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_cliente_cpf   CHECK (CHAR_LENGTH(cpf) = 11 AND cpf NOT REGEXP '[^0-9]'),
  CONSTRAINT ck_cliente_email CHECK (email LIKE '%_@_%._%')
) ENGINE=InnoDB;

CREATE TABLE gerente (
  id_gerente INT AUTO_INCREMENT PRIMARY KEY,
  nome       VARCHAR(100) NOT NULL,
  email      VARCHAR(254) NOT NULL UNIQUE,
  senha      VARCHAR(255) NOT NULL,
  matricula  VARCHAR(20)  NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE dependente (
  id_dependente   INT AUTO_INCREMENT PRIMARY KEY,
  id_cliente      INT          NOT NULL,
  nome            VARCHAR(100) NOT NULL,
  data_nascimento DATE         NOT NULL,
  grau_parentesco VARCHAR(30)  NOT NULL,
  CONSTRAINT fk_dependente_cliente FOREIGN KEY (id_cliente)
    REFERENCES cliente (id_cliente) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE jogo (
  id_jogo             INT AUTO_INCREMENT PRIMARY KEY,
  id_categoria        INT           NOT NULL,
  titulo              VARCHAR(100)  NOT NULL,
  descricao           TEXT,
  faixa_etaria_minima INT NOT NULL DEFAULT 0,
  valor_diaria        DECIMAL(10,2) NOT NULL,
  valor_reposicao     DECIMAL(10,2) NOT NULL,              -- teto da multa (RN1)
  quantidade_estoque  INT           NOT NULL DEFAULT 0,
  CONSTRAINT fk_jogo_categoria FOREIGN KEY (id_categoria)
    REFERENCES categoria (id_categoria),
  CONSTRAINT ck_jogo_diaria    CHECK (valor_diaria > 0),
  CONSTRAINT ck_jogo_reposicao CHECK (valor_reposicao > 0),
  CONSTRAINT ck_jogo_estoque   CHECK (quantidade_estoque >= 0)
) ENGINE=InnoDB;

CREATE TABLE aluguel (
  id_aluguel             INT AUTO_INCREMENT PRIMARY KEY,
  id_cliente             INT      NOT NULL,
  id_gerente             INT      NULL,                    -- preenchido só na retirada
  data_reserva           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  data_expiracao_reserva DATETIME NULL,                    -- preenchida por trigger (RN3)
  data_retirada          DATETIME NULL,
  devolucao_prevista     DATE     NOT NULL,
  devolucao_efetiva      DATE     NULL,
  valor_desconto         DECIMAL(10,2) NOT NULL DEFAULT 0,
  valor_total            DECIMAL(10,2) NOT NULL DEFAULT 0,
  qtd_renovacoes         INT NOT NULL DEFAULT 0,
  status_aluguel ENUM('RESERVADO','RETIRADO','FINALIZADO','EXPIRADO','CANCELADO')
                 NOT NULL DEFAULT 'RESERVADO',
  CONSTRAINT fk_aluguel_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
  CONSTRAINT fk_aluguel_gerente FOREIGN KEY (id_gerente) REFERENCES gerente (id_gerente),
  CONSTRAINT ck_aluguel_prazo   CHECK (devolucao_prevista >= DATE(data_reserva)),
  CONSTRAINT ck_aluguel_valores CHECK (valor_desconto >= 0 AND valor_total >= 0)
) ENGINE=InnoDB;

CREATE TABLE item_aluguel (
  id_aluguel         INT NOT NULL,
  id_jogo            INT NOT NULL,
  quantidade_alugada INT NOT NULL DEFAULT 1,
  valor_cobrado      DECIMAL(10,2) NOT NULL DEFAULT 0,     -- calculado por trigger
  PRIMARY KEY (id_aluguel, id_jogo),
  CONSTRAINT fk_item_aluguel FOREIGN KEY (id_aluguel) REFERENCES aluguel (id_aluguel),
  CONSTRAINT fk_item_jogo    FOREIGN KEY (id_jogo)    REFERENCES jogo (id_jogo),
  CONSTRAINT ck_item_qtd     CHECK (quantidade_alugada > 0)
) ENGINE=InnoDB;

-- Relacionamento opcional para indicar dependentes que terão acesso aos jogos do aluguel (RF04)
CREATE TABLE aluguel_dependente (
  id_aluguel    INT NOT NULL,
  id_dependente INT NOT NULL,
  PRIMARY KEY (id_aluguel, id_dependente),
  CONSTRAINT fk_alg_dep_aluguel    FOREIGN KEY (id_aluguel)    REFERENCES aluguel (id_aluguel) ON DELETE CASCADE,
  CONSTRAINT fk_alg_dep_dependente FOREIGN KEY (id_dependente) REFERENCES dependente (id_dependente) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE penalidade (
  id_penalidade     INT AUTO_INCREMENT PRIMARY KEY,
  id_aluguel        INT NOT NULL,
  motivo_penalidade VARCHAR(100) NOT NULL,
  valor_multa       DECIMAL(10,2) NOT NULL,
  status_pagamento  ENUM('PENDENTE','PAGO') NOT NULL DEFAULT 'PENDENTE',
  data_geracao      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_penalidade_aluguel FOREIGN KEY (id_aluguel) REFERENCES aluguel (id_aluguel),
  CONSTRAINT uq_penalidade_motivo UNIQUE (id_aluguel, motivo_penalidade),  -- idempotência do job
  CONSTRAINT ck_penalidade_valor CHECK (valor_multa >= 0)
) ENGINE=InnoDB;

-- Tabelas de apoio (regras configuráveis sem "números mágicos" no código)
CREATE TABLE parametro_sistema (
  chave     VARCHAR(40) PRIMARY KEY,
  valor     DECIMAL(10,2) NOT NULL,
  descricao VARCHAR(120) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE faixa_desconto (
  qtd_minima TINYINT UNSIGNED PRIMARY KEY,
  percentual DECIMAL(5,2) NOT NULL,
  CONSTRAINT ck_faixa_pct CHECK (percentual BETWEEN 0 AND 100)
) ENGINE=InnoDB;

-- Trilha de auditoria (RNF08), alimentada por triggers
CREATE TABLE log_auditoria (
  id_log      BIGINT AUTO_INCREMENT PRIMARY KEY,
  data_hora   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  tabela      VARCHAR(40)  NOT NULL,
  operacao    ENUM('INSERT','UPDATE','DELETE') NOT NULL,
  id_registro INT          NOT NULL,
  detalhe     VARCHAR(255) NOT NULL,
  usuario_db  VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

-- Índices de apoio às consultas e aos jobs
CREATE INDEX ix_aluguel_cliente_status   ON aluguel (id_cliente, status_aluguel);
CREATE INDEX ix_aluguel_expiracao        ON aluguel (status_aluguel, data_expiracao_reserva);
CREATE INDEX ix_aluguel_prazo            ON aluguel (status_aluguel, devolucao_prevista);
CREATE INDEX ix_jogo_categoria_faixa     ON jogo (id_categoria, faixa_etaria_minima);
CREATE INDEX ix_jogo_titulo              ON jogo (titulo);
CREATE INDEX ix_item_jogo                ON item_aluguel (id_jogo);
CREATE INDEX ix_log_tabela_registro      ON log_auditoria (tabela, id_registro);

INSERT INTO parametro_sistema (chave, valor, descricao) VALUES
  ('LIMITE_JOGOS',            4,    'RN2: máximo de unidades ativas (reservadas + retiradas) por cliente'),
  ('HORAS_EXPIRACAO_RESERVA', 24,   'RN3: prazo, em horas, para retirar a reserva'),
  ('MULTA_DIARIA_POR_JOGO',   5.00, 'RN1: multa por jogo/dia de atraso (R$)'),
  ('IDADE_MINIMA_TITULAR',    18,   'RN7: idade mínima do cliente titular'),
  ('MAX_RENOVACOES',          1,    'RF07: renovações permitidas por aluguel');

INSERT INTO faixa_desconto (qtd_minima, percentual) VALUES
  (1, 0), (2, 10), (3, 20), (4, 25);                       -- RN6

-- ---------------------------------------------------------------------
-- 2. FUNÇÕES
-- ---------------------------------------------------------------------
DELIMITER $$

CREATE FUNCTION fn_parametro(p_chave VARCHAR(40)) RETURNS DECIMAL(10,2)
READS SQL DATA
BEGIN
  DECLARE v DECIMAL(10,2);
  SELECT valor INTO v FROM parametro_sistema WHERE chave = p_chave;
  IF v IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Parametro do sistema nao cadastrado';
  END IF;
  RETURN v;
END$$

-- RN6: percentual de desconto conforme a quantidade de jogos
CREATE FUNCTION fn_percentual_desconto(p_qtd INT) RETURNS DECIMAL(5,2)
READS SQL DATA
BEGIN
  DECLARE v DECIMAL(5,2);
  SELECT percentual INTO v FROM faixa_desconto
   WHERE qtd_minima <= p_qtd ORDER BY qtd_minima DESC LIMIT 1;
  RETURN COALESCE(v, 0);
END$$

-- RN1: por jogo, min(taxa x dias de atraso ; valor de reposição), vezes a quantidade
CREATE FUNCTION fn_calcular_multa(p_id_aluguel INT, p_taxa_diaria DECIMAL(10,2))
RETURNS DECIMAL(10,2)
READS SQL DATA
BEGIN
  DECLARE v_prevista DATE;
  DECLARE v_efetiva  DATE;
  DECLARE v_dias     INT;
  DECLARE v_multa    DECIMAL(10,2);

  SELECT devolucao_prevista, COALESCE(devolucao_efetiva, CURDATE())
    INTO v_prevista, v_efetiva
    FROM aluguel WHERE id_aluguel = p_id_aluguel;

  SET v_dias = GREATEST(COALESCE(DATEDIFF(v_efetiva, v_prevista), 0), 0);
  IF v_dias = 0 THEN
    RETURN 0;
  END IF;

  SELECT COALESCE(SUM(LEAST(p_taxa_diaria * v_dias, j.valor_reposicao) * i.quantidade_alugada), 0)
    INTO v_multa
    FROM item_aluguel i JOIN jogo j ON j.id_jogo = i.id_jogo
   WHERE i.id_aluguel = p_id_aluguel;

  RETURN v_multa;
END$$

-- ---------------------------------------------------------------------
-- 3. TRIGGERS
-- ---------------------------------------------------------------------

-- RN7: elegibilidade (idade mínima do titular)
CREATE TRIGGER trg_cliente_valida_bi BEFORE INSERT ON cliente
FOR EACH ROW
BEGIN
  IF TIMESTAMPDIFF(YEAR, NEW.data_nascimento, CURDATE()) < fn_parametro('IDADE_MINIMA_TITULAR') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'RN7: cliente titular deve ter idade minima de 18 anos';
  END IF;
END$$

CREATE TRIGGER trg_cliente_valida_bu BEFORE UPDATE ON cliente
FOR EACH ROW
BEGIN
  IF NEW.data_nascimento <> OLD.data_nascimento
     AND TIMESTAMPDIFF(YEAR, NEW.data_nascimento, CURDATE()) < fn_parametro('IDADE_MINIMA_TITULAR') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'RN7: cliente titular deve ter idade minima de 18 anos';
  END IF;
END$$

-- RN3: toda reserva nasce RESERVADA, sem gerente, com prazo de expiração
CREATE TRIGGER trg_aluguel_bi BEFORE INSERT ON aluguel
FOR EACH ROW
BEGIN
  SET NEW.status_aluguel = 'RESERVADO';
  SET NEW.id_gerente = NULL;
  SET NEW.valor_desconto = 0;
  SET NEW.valor_total = 0;
  SET NEW.data_expiracao_reserva =
      DATE_ADD(NEW.data_reserva, INTERVAL CAST(fn_parametro('HORAS_EXPIRACAO_RESERVA') AS SIGNED) HOUR);
END$$

-- Máquina de estados do aluguel
--   RESERVADO -> RETIRADO | CANCELADO | EXPIRADO ;  RETIRADO -> FINALIZADO
CREATE TRIGGER trg_aluguel_estados_bu BEFORE UPDATE ON aluguel
FOR EACH ROW
BEGIN
  IF OLD.status_aluguel IN ('FINALIZADO','CANCELADO','EXPIRADO') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Aluguel encerrado nao pode ser alterado';
  END IF;

  IF NEW.status_aluguel <> OLD.status_aluguel THEN
    IF NOT ((OLD.status_aluguel = 'RESERVADO' AND NEW.status_aluguel IN ('RETIRADO','CANCELADO','EXPIRADO'))
         OR (OLD.status_aluguel = 'RETIRADO'  AND NEW.status_aluguel = 'FINALIZADO')) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Transicao de status invalida';
    END IF;

    IF NEW.status_aluguel = 'RETIRADO' THEN
      IF NEW.id_gerente IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A retirada deve ser registrada por um gerente';
      END IF;
      SET NEW.data_retirada = COALESCE(NEW.data_retirada, NOW());
    ELSEIF NEW.status_aluguel = 'FINALIZADO' THEN
      SET NEW.devolucao_efetiva = COALESCE(NEW.devolucao_efetiva, CURDATE());
    END IF;
  END IF;
END$$

-- Item: valida reserva aberta, faixa etária de dependentes vinculados e calcula valor
CREATE TRIGGER trg_item_valida_bi BEFORE INSERT ON item_aluguel
FOR EACH ROW
BEGIN
  DECLARE v_status      VARCHAR(20);
  DECLARE v_dias        INT;
  DECLARE v_diaria      DECIMAL(10,2);
  DECLARE v_faixa_jogo  INT;
  DECLARE v_dep_incomp  INT DEFAULT 0;

  SELECT status_aluguel, GREATEST(1, DATEDIFF(devolucao_prevista, DATE(data_reserva)))
    INTO v_status, v_dias
    FROM aluguel WHERE id_aluguel = NEW.id_aluguel;

  IF v_status IS NULL OR v_status <> 'RESERVADO' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Itens so podem ser adicionados a uma reserva em aberto';
  END IF;

  SELECT valor_diaria, faixa_etaria_minima INTO v_diaria, v_faixa_jogo
    FROM jogo WHERE id_jogo = NEW.id_jogo;

  -- Se já houver dependentes vinculados ao aluguel, valida faixa etária do novo jogo
  SELECT COUNT(*) INTO v_dep_incomp
    FROM aluguel_dependente ad
    JOIN dependente d ON d.id_dependente = ad.id_dependente
   WHERE ad.id_aluguel = NEW.id_aluguel
     AND TIMESTAMPDIFF(YEAR, d.data_nascimento, CURDATE()) < v_faixa_jogo;

  IF v_dep_incomp > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'RF04: dependente vinculado nao atende a faixa etaria minima do jogo';
  END IF;

  SET NEW.valor_cobrado = v_diaria * NEW.quantidade_alugada * v_dias;
END$$

-- Valida vínculo de dependente ao aluguel (titularidade e faixa etária dos jogos do aluguel)
CREATE TRIGGER trg_aluguel_dependente_bi BEFORE INSERT ON aluguel_dependente
FOR EACH ROW
BEGIN
  DECLARE v_titular_aluguel INT;
  DECLARE v_titular_dep     INT;
  DECLARE v_idade_dep       INT;
  DECLARE v_max_faixa       INT;

  SELECT id_cliente INTO v_titular_aluguel FROM aluguel WHERE id_aluguel = NEW.id_aluguel;
  SELECT id_cliente, TIMESTAMPDIFF(YEAR, data_nascimento, CURDATE())
    INTO v_titular_dep, v_idade_dep
    FROM dependente WHERE id_dependente = NEW.id_dependente;

  IF v_titular_aluguel IS NULL OR v_titular_dep IS NULL OR v_titular_aluguel <> v_titular_dep THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'RF04: dependente nao pertence ao cliente titular deste aluguel';
  END IF;

  SELECT COALESCE(MAX(j.faixa_etaria_minima), 0) INTO v_max_faixa
    FROM item_aluguel i JOIN jogo j ON j.id_jogo = i.id_jogo
   WHERE i.id_aluguel = NEW.id_aluguel;

  IF v_idade_dep < v_max_faixa THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'RF04: idade do dependente inferior a faixa etaria minima dos jogos';
  END IF;
END$$

-- Estoque: baixa atômica (a condição no WHERE evita corrida entre sessões)
CREATE TRIGGER trg_atualiza_estoque_aluguel AFTER INSERT ON item_aluguel
FOR EACH ROW
BEGIN
  UPDATE jogo
     SET quantidade_estoque = quantidade_estoque - NEW.quantidade_alugada
   WHERE id_jogo = NEW.id_jogo AND quantidade_estoque >= NEW.quantidade_alugada;
  IF ROW_COUNT() = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Estoque insuficiente para o jogo solicitado';
  END IF;
END$$

-- RN6: recalcula subtotal, desconto e total a cada item inserido
CREATE TRIGGER trg_recalcula_totais_aluguel AFTER INSERT ON item_aluguel
FOR EACH ROW
BEGIN
  DECLARE v_subtotal DECIMAL(10,2);
  DECLARE v_qtd      INT;
  DECLARE v_desc     DECIMAL(10,2);

  SELECT SUM(valor_cobrado), SUM(quantidade_alugada) INTO v_subtotal, v_qtd
    FROM item_aluguel WHERE id_aluguel = NEW.id_aluguel;

  SET v_desc = ROUND(v_subtotal * fn_percentual_desconto(v_qtd) / 100, 2);
  UPDATE aluguel
     SET valor_desconto = v_desc, valor_total = v_subtotal - v_desc
   WHERE id_aluguel = NEW.id_aluguel;
END$$

-- Itens são imutáveis depois de criados (preserva consistência do estoque)
CREATE TRIGGER trg_item_imutavel_bu BEFORE UPDATE ON item_aluguel
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Itens de aluguel nao podem ser alterados';
END$$

CREATE TRIGGER trg_item_imutavel_bd BEFORE DELETE ON item_aluguel
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Itens de aluguel nao podem ser removidos';
END$$

-- Estoque volta ao encerrar o aluguel: devolução, cancelamento OU expiração
CREATE TRIGGER trg_restaura_estoque_devolucao AFTER UPDATE ON aluguel
FOR EACH ROW
BEGIN
  IF OLD.status_aluguel IN ('RESERVADO','RETIRADO')
     AND NEW.status_aluguel IN ('FINALIZADO','CANCELADO','EXPIRADO') THEN
    UPDATE jogo j
      JOIN item_aluguel i ON i.id_jogo = j.id_jogo
       SET j.quantidade_estoque = j.quantidade_estoque + i.quantidade_alugada
     WHERE i.id_aluguel = NEW.id_aluguel;
  END IF;
END$$

-- Auditoria (RNF08)
CREATE TRIGGER trg_auditoria_cliente_ai AFTER INSERT ON cliente
FOR EACH ROW
BEGIN
  INSERT INTO log_auditoria (tabela, operacao, id_registro, detalhe, usuario_db)
  VALUES ('cliente', 'INSERT', NEW.id_cliente,
          CONCAT('Cliente cadastrado: ', NEW.nome), USER());
END$$

CREATE TRIGGER trg_auditoria_cliente_ad AFTER DELETE ON cliente
FOR EACH ROW
BEGIN
  INSERT INTO log_auditoria (tabela, operacao, id_registro, detalhe, usuario_db)
  VALUES ('cliente', 'DELETE', OLD.id_cliente,
          CONCAT('Cliente removido: ', OLD.nome), USER());
END$$

CREATE TRIGGER trg_auditoria_jogo_au AFTER UPDATE ON jogo
FOR EACH ROW
BEGIN
  IF NEW.quantidade_estoque <> OLD.quantidade_estoque OR NEW.valor_diaria <> OLD.valor_diaria THEN
    INSERT INTO log_auditoria (tabela, operacao, id_registro, detalhe, usuario_db)
    VALUES ('jogo', 'UPDATE', NEW.id_jogo,
            CONCAT('Jogo "', NEW.titulo, '" | estoque: ', OLD.quantidade_estoque, ' -> ', NEW.quantidade_estoque,
                   ' | diaria: R$ ', FORMAT(OLD.valor_diaria, 2), ' -> R$ ', FORMAT(NEW.valor_diaria, 2)),
            USER());
  END IF;
END$$

CREATE TRIGGER trg_auditoria_aluguel_ai AFTER INSERT ON aluguel
FOR EACH ROW
BEGIN
  INSERT INTO log_auditoria (tabela, operacao, id_registro, detalhe, usuario_db)
  VALUES ('aluguel', 'INSERT', NEW.id_aluguel,
          CONCAT('Reserva criada para o cliente ', NEW.id_cliente), USER());
END$$

CREATE TRIGGER trg_auditoria_aluguel_au AFTER UPDATE ON aluguel
FOR EACH ROW
BEGIN
  IF NEW.status_aluguel <> OLD.status_aluguel THEN
    INSERT INTO log_auditoria (tabela, operacao, id_registro, detalhe, usuario_db)
    VALUES ('aluguel', 'UPDATE', NEW.id_aluguel,
            CONCAT('Status ', OLD.status_aluguel, ' -> ', NEW.status_aluguel,
                   IF(NEW.status_aluguel = 'EXPIRADO',
                      CONCAT(' | estornar R$ ', FORMAT(NEW.valor_total, 2)), '')),
            USER());
  END IF;
END$$

CREATE TRIGGER trg_auditoria_penalidade_ai AFTER INSERT ON penalidade
FOR EACH ROW
BEGIN
  INSERT INTO log_auditoria (tabela, operacao, id_registro, detalhe, usuario_db)
  VALUES ('penalidade', 'INSERT', NEW.id_penalidade,
          CONCAT('Multa de R$ ', FORMAT(NEW.valor_multa, 2), ' no aluguel ', NEW.id_aluguel), USER());
END$$

CREATE TRIGGER trg_auditoria_penalidade_au AFTER UPDATE ON penalidade
FOR EACH ROW
BEGIN
  IF NEW.status_pagamento <> OLD.status_pagamento OR NEW.valor_multa <> OLD.valor_multa THEN
    INSERT INTO log_auditoria (tabela, operacao, id_registro, detalhe, usuario_db)
    VALUES ('penalidade', 'UPDATE', NEW.id_penalidade,
            CONCAT('Penalidade aluguel ', NEW.id_aluguel, ': ', OLD.status_pagamento, ' -> ', NEW.status_pagamento,
                   ' | valor R$ ', FORMAT(NEW.valor_multa, 2)), USER());
  END IF;
END$$

-- ---------------------------------------------------------------------
-- 4. PROCEDURES (chamar dentro de uma transação quando houver >1 passo)
-- ---------------------------------------------------------------------

CREATE PROCEDURE sp_criar_reserva(IN p_id_cliente INT, IN p_dias INT, OUT p_id_aluguel INT)
BEGIN
  IF p_dias < 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Periodo de aluguel deve ser de pelo menos 1 dia';
  END IF;
  INSERT INTO aluguel (id_cliente, devolucao_prevista)
  VALUES (p_id_cliente, DATE_ADD(CURDATE(), INTERVAL p_dias DAY));
  SET p_id_aluguel = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_adicionar_item(IN p_id_aluguel INT, IN p_id_jogo INT, IN p_qtd INT)
BEGIN
  DECLARE v_id_cliente INT DEFAULT NULL;
  DECLARE v_cliente_bloqueado INT DEFAULT NULL;
  DECLARE v_qtd_item INT DEFAULT 0;
  DECLARE v_qtd_ativos INT DEFAULT 0;
  DECLARE v_limite INT;
  DECLARE v_fim BOOLEAN DEFAULT FALSE;
  DECLARE cur_itens CURSOR FOR
    SELECT i.quantidade_alugada
      FROM item_aluguel i
      JOIN aluguel a ON a.id_aluguel = i.id_aluguel
     WHERE a.id_cliente = v_id_cliente
       AND a.status_aluguel IN ('RESERVADO','RETIRADO')
     FOR UPDATE;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_fim = TRUE;

  IF p_qtd IS NULL OR p_qtd < 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Quantidade de itens deve ser maior que zero';
  END IF;

  SELECT id_cliente INTO v_id_cliente
    FROM aluguel
   WHERE id_aluguel = p_id_aluguel
   FOR UPDATE;

  IF v_id_cliente IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Aluguel nao encontrado';
  END IF;

  SELECT id_cliente INTO v_cliente_bloqueado
    FROM cliente
   WHERE id_cliente = v_id_cliente
   FOR UPDATE;

  IF v_cliente_bloqueado IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cliente nao encontrado';
  END IF;

  SET v_fim = FALSE;
  OPEN cur_itens;
  leitura_itens: LOOP
    FETCH cur_itens INTO v_qtd_item;
    IF v_fim THEN
      LEAVE leitura_itens;
    END IF;
    SET v_qtd_ativos = v_qtd_ativos + v_qtd_item;
  END LOOP;
  CLOSE cur_itens;

  SET v_limite = CAST(fn_parametro('LIMITE_JOGOS') AS SIGNED);
  IF v_qtd_ativos + p_qtd > v_limite THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'RN2: limite de jogos ativos por cliente excedido';
  END IF;

  INSERT INTO item_aluguel (id_aluguel, id_jogo, quantidade_alugada)
  VALUES (p_id_aluguel, p_id_jogo, p_qtd);
END$$

CREATE PROCEDURE sp_cancelar_reserva(IN p_id_aluguel INT)
BEGIN
  UPDATE aluguel SET status_aluguel = 'CANCELADO' WHERE id_aluguel = p_id_aluguel;
  IF ROW_COUNT() = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Aluguel nao encontrado';
  END IF;
END$$

CREATE PROCEDURE sp_retirar_aluguel(IN p_id_aluguel INT, IN p_id_gerente INT)
BEGIN
  DECLARE v_status VARCHAR(20);
  DECLARE v_exp    DATETIME;

  SELECT status_aluguel, data_expiracao_reserva INTO v_status, v_exp
    FROM aluguel WHERE id_aluguel = p_id_aluguel FOR UPDATE;

  IF v_status IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Aluguel nao encontrado';
  END IF;
  IF v_status = 'RESERVADO' AND NOW() > v_exp THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Reserva expirada: nao pode mais ser retirada';
  END IF;

  UPDATE aluguel SET status_aluguel = 'RETIRADO', id_gerente = p_id_gerente
   WHERE id_aluguel = p_id_aluguel;
END$$

-- RF07: renovação (limitada por MAX_RENOVACOES, sem atraso); cobra o período extra com desconto
CREATE PROCEDURE sp_renovar_aluguel(IN p_id_aluguel INT, IN p_dias INT)
BEGIN
  DECLARE v_status VARCHAR(20);
  DECLARE v_prev   DATE;
  DECLARE v_ren    INT;
  DECLARE v_extra  DECIMAL(10,2);
  DECLARE v_qtd    INT;
  DECLARE v_desc   DECIMAL(10,2);

  IF p_dias < 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Periodo de renovacao invalido';
  END IF;

  SELECT status_aluguel, devolucao_prevista, qtd_renovacoes INTO v_status, v_prev, v_ren
    FROM aluguel WHERE id_aluguel = p_id_aluguel FOR UPDATE;

  IF v_status IS NULL OR v_status <> 'RETIRADO' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Somente alugueis retirados podem ser renovados';
  END IF;
  IF v_prev < CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Aluguel em atraso nao pode ser renovado';
  END IF;
  IF v_ren >= fn_parametro('MAX_RENOVACOES') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Limite de renovacoes atingido';
  END IF;

  SELECT SUM(j.valor_diaria * i.quantidade_alugada * p_dias), SUM(i.quantidade_alugada)
    INTO v_extra, v_qtd
    FROM item_aluguel i JOIN jogo j ON j.id_jogo = i.id_jogo
   WHERE i.id_aluguel = p_id_aluguel;

  SET v_desc = ROUND(v_extra * fn_percentual_desconto(v_qtd) / 100, 2);

  UPDATE aluguel
     SET devolucao_prevista = DATE_ADD(devolucao_prevista, INTERVAL p_dias DAY),
         qtd_renovacoes     = qtd_renovacoes + 1,
         valor_desconto     = valor_desconto + v_desc,
         valor_total        = valor_total + v_extra - v_desc
   WHERE id_aluguel = p_id_aluguel;
END$$

-- Encerra o contrato: grava devolução, volta estoque (trigger) e gera multa (se houver atraso)
CREATE PROCEDURE sp_finalizar_aluguel(IN p_id_aluguel INT, IN p_data_devolucao DATE)
BEGIN
  DECLARE v_multa DECIMAL(10,2);

  UPDATE aluguel
     SET status_aluguel = 'FINALIZADO',
         devolucao_efetiva = COALESCE(p_data_devolucao, CURDATE())
   WHERE id_aluguel = p_id_aluguel;
  IF ROW_COUNT() = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Aluguel nao encontrado';
  END IF;

  SET v_multa = fn_calcular_multa(p_id_aluguel, fn_parametro('MULTA_DIARIA_POR_JOGO'));
  IF v_multa > 0 THEN
    INSERT INTO penalidade (id_aluguel, motivo_penalidade, valor_multa)
    VALUES (p_id_aluguel, 'ATRASO NA DEVOLUCAO', v_multa)
    ON DUPLICATE KEY UPDATE valor_multa = v_multa;
  END IF;
END$$

-- Job: expira reservas não retiradas no prazo (o trigger devolve o estoque)
CREATE PROCEDURE sp_expirar_reservas()
BEGIN
  UPDATE aluguel SET status_aluguel = 'EXPIRADO'
   WHERE status_aluguel = 'RESERVADO' AND data_expiracao_reserva <= NOW();
  SELECT ROW_COUNT() AS reservas_expiradas;
END$$

-- Job: gera/atualiza multa de alugueis retirados e vencidos (idempotente)
CREATE PROCEDURE sp_registrar_atrasos()
BEGIN
  DECLARE v_taxa DECIMAL(10,2);
  SET v_taxa = fn_parametro('MULTA_DIARIA_POR_JOGO');

  INSERT INTO penalidade (id_aluguel, motivo_penalidade, valor_multa)
  SELECT a.id_aluguel, 'ATRASO NA DEVOLUCAO', fn_calcular_multa(a.id_aluguel, v_taxa)
    FROM aluguel a
   WHERE a.status_aluguel = 'RETIRADO' AND a.devolucao_prevista < CURDATE()
  ON DUPLICATE KEY UPDATE valor_multa = fn_calcular_multa(penalidade.id_aluguel, v_taxa);
END$$

DELIMITER ;

-- ---------------------------------------------------------------------
-- 5. VIEWS RELACIONAIS (Seções 5.3 e 5.4 — Relatórios Gerenciais RF10)
-- ---------------------------------------------------------------------

-- Seção 5.3: unifica contrato, cliente, gerente, prazos, valores e status
CREATE OR REPLACE VIEW vw_relatorio_alugueis AS
SELECT
  a.id_aluguel,
  c.id_cliente,
  c.nome  AS nome_cliente,
  c.cpf   AS cpf_cliente,
  c.email AS email_cliente,
  g.id_gerente,
  g.nome  AS nome_gerente,
  a.data_reserva,
  a.data_expiracao_reserva,
  a.data_retirada,
  a.devolucao_prevista,
  a.devolucao_efetiva,
  GREATEST(0, DATEDIFF(COALESCE(a.devolucao_efetiva, CURDATE()), a.devolucao_prevista)) AS dias_atraso,
  a.qtd_renovacoes,
  a.valor_desconto,
  a.valor_total,
  a.status_aluguel,
  COALESCE(SUM(p.valor_multa), 0) AS total_multas
FROM aluguel a
JOIN cliente c ON c.id_cliente = a.id_cliente
LEFT JOIN gerente g ON g.id_gerente = a.id_gerente
LEFT JOIN penalidade p ON p.id_aluguel = a.id_aluguel
GROUP BY
  a.id_aluguel, c.id_cliente, c.nome, c.cpf, c.email,
  g.id_gerente, g.nome, a.data_reserva, a.data_expiracao_reserva,
  a.data_retirada, a.devolucao_prevista, a.devolucao_efetiva,
  a.qtd_renovacoes, a.valor_desconto, a.valor_total, a.status_aluguel;

-- Seção 5.3: total de locações e receita gerada por título, ordenado por popularidade
CREATE OR REPLACE VIEW vw_jogos_mais_alugados AS
SELECT
  j.id_jogo,
  j.titulo,
  c.nome_categoria,
  j.faixa_etaria_minima,
  j.valor_diaria,
  j.quantidade_estoque,
  COALESCE(r.total_locacoes, 0)          AS total_locacoes,
  COALESCE(r.unidades_alugadas, 0)       AS unidades_alugadas,
  COALESCE(r.receita_bruta_gerada, 0)    AS receita_bruta_gerada
FROM jogo j
JOIN categoria c ON c.id_categoria = j.id_categoria
LEFT JOIN (
  SELECT
    i.id_jogo,
    COUNT(DISTINCT i.id_aluguel) AS total_locacoes,
    SUM(i.quantidade_alugada) AS unidades_alugadas,
    SUM(i.valor_cobrado) AS receita_bruta_gerada
  FROM item_aluguel i
  JOIN aluguel a ON a.id_aluguel = i.id_aluguel
  WHERE a.status_aluguel IN ('RESERVADO','RETIRADO','FINALIZADO')
  GROUP BY i.id_jogo
) r ON r.id_jogo = j.id_jogo
ORDER BY unidades_alugadas DESC, receita_bruta_gerada DESC;

-- Seção 5.4: faturamento agrupado por categoria para análise de rentabilidade
CREATE OR REPLACE VIEW vw_faturamento_categoria AS
SELECT
  c.id_categoria,
  c.nome_categoria,
  COUNT(DISTINCT j.id_jogo)              AS jogos_no_catalogo,
  COALESCE(r.total_alugueis, 0)           AS total_alugueis,
  COALESCE(r.unidades_alugadas, 0)        AS unidades_alugadas,
  COALESCE(r.faturamento_bruto, 0)        AS faturamento_bruto
FROM categoria c
LEFT JOIN jogo j ON j.id_categoria = c.id_categoria
LEFT JOIN (
  SELECT
    j.id_categoria,
    COUNT(DISTINCT a.id_aluguel) AS total_alugueis,
    SUM(i.quantidade_alugada) AS unidades_alugadas,
    SUM(i.valor_cobrado) AS faturamento_bruto
  FROM jogo j
  JOIN item_aluguel i ON i.id_jogo = j.id_jogo
  JOIN aluguel a ON a.id_aluguel = i.id_aluguel
  WHERE a.status_aluguel IN ('RESERVADO','RETIRADO','FINALIZADO')
  GROUP BY j.id_categoria
) r ON r.id_categoria = c.id_categoria
GROUP BY c.id_categoria, c.nome_categoria, r.total_alugueis,
         r.unidades_alugadas, r.faturamento_bruto
ORDER BY faturamento_bruto DESC;

-- Apoio gerencial: penalidades pendentes de quitação
CREATE OR REPLACE VIEW vw_penalidades_pendentes AS
SELECT
  p.id_penalidade,
  p.id_aluguel,
  c.id_cliente,
  c.nome  AS nome_cliente,
  c.cpf   AS cpf_cliente,
  c.telefone,
  p.motivo_penalidade,
  p.valor_multa,
  p.status_pagamento,
  p.data_geracao
FROM penalidade p
JOIN aluguel a ON a.id_aluguel = p.id_aluguel
JOIN cliente c ON c.id_cliente = a.id_cliente
WHERE p.status_pagamento = 'PENDENTE'
ORDER BY p.data_geracao ASC;

-- ---------------------------------------------------------------------
-- 6. SEGURANÇA E CONTROLE DE ACESSO (DCL — RNF01)
-- ---------------------------------------------------------------------
-- Privilégios da aplicação ficam em db/02_app_permissions.sql.
-- A conta deve ser criada pelo mecanismo seguro do ambiente (ex.: MYSQL_USER/MYSQL_PASSWORD).