# RFC-003 — Unificação Arquitetural (Database-First) e Correções do Sistema DG Master

| Campo | Valor |
|---|---|
| Status | Aprovado para Execução (OpenCode) |
| Data | 20/11/2026 |
| Substitui / Consolida | `RFC-001` e `RFC-002-alinhamento-documento-codigo.md` |
| Artefatos Base | `DG MASTER - DB II - ATUALIZADO.pdf` + `db/01_schema_objetos.sql` + Repo `prometheu-z/dg_master` |
| Diretriz Arquitetural | **Database-Centric (SPOBDD2):** Regras de negócio, cálculos, máquina de estados, controle de estoque e auditoria residem no MySQL (`triggers`, `functions`, `procedures`, `views`, `constraints`). A aplicação Spring Boot atua como camada fina de orquestração, autenticação/autorização e apresentação. |

---

## 1. Contexto e Objetivo

Este documento unifica:
1. Os requisitos e diagramas do relatório acadêmico (`DG MASTER - DB II - ATUALIZADO.pdf` — disciplina SPOBDD2 / IFSP);
2. O projeto físico oficial em MySQL 8.0+ (`db/01_schema_objetos.sql`);
3. As incongruências e hipóteses de bugs identificadas na base de código Spring Boot (`prometheu-z/dg_master`).

**Objetivo operacional:** Servir como **prompt/especificação única para o agente OpenCode** refatorar o repositório local, eliminar duplicidade de regras entre Java e SQL, corrigir falhas de segurança e concorrência, e deixar código, banco e documentação 100% alinhados.

---

## 2. Decisões Arquiteturais Fechadas (Resolução de D1–D5)

Para eliminar ambiguidades entre o PDF, o README e o SQL, ficam estabelecidas as seguintes decisões:

| ID | Tema | Decisão Oficial | Impacto Técnico |
|---|---|---|---|
| **D1** | Expiração de reserva (RN3 — 24h) | Reserva online **não processa pagamento online** (PDF seção 2.5: sem gateway de pagamento). A reserva bloqueia o estoque por 24h (`RESERVADO`); se não retirada no prazo, passa a `EXPIRADO`, devolve o estoque via trigger e registra log em `log_auditoria` (incluindo valor para eventual estorno/crédito se pago no balcão). | Alinhar README (remover menção a "carteira digital" se não houver tabela de saldo) e manter `trg_auditoria_aluguel_au` + `sp_expirar_reservas()`. |
| **D2** | Cálculo da multa por atraso (RN1) | $\text{Multa} = \sum \min(\text{R\$ } 5{,}00 \times \text{dias\_atraso},\; \text{valor\_reposicao}) \times \text{quantidade\_alugada}$. | Coluna `jogo.valor_reposicao` obrigatória; cálculo centralizado em `fn_calcular_multa` e acionado por `sp_finalizar_aluguel` / `sp_registrar_atrasos`. |
| **D3** | Papel do Dependente (RF04) | Dependente **não possui login** (`dependente` não tem e-mail/senha). O titular pode vincular dependentes ao aluguel (`aluguel_dependente`) e o banco valida via trigger se a idade de cada dependente atende à `faixa_etaria_minima` dos jogos. | Remover `solicitarReserva()` da classe `Dependente` no PDF; adicionar tabela associativa `aluguel_dependente` com trigger de validação etária. |
| **D4** | Autenticação 2FA (RNF03) | Manter segurança robusta com BCrypt, sessão/CSRF, RBAC (`ROLE_CLIENTE` vs `ROLE_GERENTE`) e privilégios mínimos no MySQL (DCL). Se não houver provedor SMTP/OTP configurado no prazo, documentar RNF03 explicitamente como *escopo futuro (Fase 2)* no PDF. | Não quebrar o fluxo de login atual com 2FA incompleto; garantir matriz de autorização estrita nos endpoints. |
| **D5** | Fonte única da verdade (Triggers vs Java) | **100% das regras de negócio de dados ficam no MySQL** (`01_schema_objetos.sql`). O Java **não** decrementa/incrementa estoque, **não** calcula desconto/multa na mão e **não** valida limite de 4 jogos paralelamente — ele chama as *Stored Procedures* / repositórios e traduz `SQLSTATE '45000'` em mensagens HTTP/UI amigáveis. | Elimina dupla contagem de estoque (`BUG-01`) e divergência de cálculo (`BUG-10`). |

---

## 3. Correções no Script SQL (`db/01_schema_objetos.sql`)

Antes de aplicar o schema no projeto, o arquivo `db/01_schema_objetos.sql` foi revisado e atualizado para corrigir um bug de sintaxe e cobrir todos os objetos exigidos no PDF (Seções 5.3, 5.4 e RNF01/RNF08):

1. **Bug corrigido em `sp_registrar_atrasos()`**:
   - *Problema:* Havia um placeholder inválido `fn_calcular_multa(aluguel_id_placeholder_fix(), v_taxa)` na cláusula `ON DUPLICATE KEY UPDATE`.
   - *Correção:* Substituído por `fn_calcular_multa(penalidade.id_aluguel, v_taxa)`.
2. **Views Relacionais exigidas pelo PDF (Seções 5.3 e 5.4) adicionadas**:
   - `vw_relatorio_alugueis` (Seção 5.3): consolida aluguel, cliente, gerente, datas, dias de atraso, status e total de multas.
   - `vw_jogos_mais_alugados` (Seção 5.3): consolida quantidade de locações, unidades alugadas e receita por título.
   - `vw_faturamento_categoria` (Seção 5.4): agrupa itens alugados e calcula faturamento por categoria.
   - `vw_penalidades_pendentes`: apoia o painel do gerente na cobrança de multas em aberto.
3. **Tabela e Triggers de Validação Etária de Dependentes (`aluguel_dependente`)**:
   - Triggers `trg_aluguel_dependente_bi` e verificação complementar em `trg_item_valida_bi` garantem que um dependente só seja vinculado a jogos compatíveis com sua idade (`TIMESTAMPDIFF(YEAR, data_nascimento, CURDATE()) >= faixa_etaria_minima`).
4. **Trilha de Auditoria Completa (RNF08)**:
   - Além de `aluguel` e `penalidade (INSERT)`, adicionados triggers de auditoria para pagamento/alteração de `penalidade (UPDATE)`, alterações críticas em `jogo (UPDATE)` (estoque/diária) e cadastro/remoção de `cliente (INSERT/DELETE)`.
5. **Segurança DCL (RNF01 / Apêndice A.1)**:
   - A conta `dg_app` é provisionada pelo ambiente com senha obrigatória e exclusiva, nunca gravada no script SQL ou em valores padrão do Compose.
   - `db/02_app_permissions.sql` revoga os privilégios amplos concedidos pelo bootstrap da imagem MySQL e concede `SELECT`/`EXECUTE` no banco, além de DML apenas nas tabelas necessárias. A aplicação não recebe DML direto em `aluguel` ou `item_aluguel`, para que as operações de aluguel passem pelas procedures.
6. **Concorrência do limite RN2**:
   - `sp_adicionar_item` bloqueia a reserva e a linha do cliente, faz uma leitura bloqueante dos itens ativos e valida o limite antes de inserir. O app não recebe permissão de escrita direta em `item_aluguel`.
7. **Agregações das views gerenciais**:
   - `vw_jogos_mais_alugados` e `vw_faturamento_categoria` agregam somente itens de aluguéis `RESERVADO`, `RETIRADO` ou `FINALIZADO`; aluguéis cancelados/expirados não contam em unidades ou receita.

---

## 4. Tabela Consolidada de Incongruências e Bugs a Resolver no Código

### 4.1. Infraestrutura, Banco e Configuração (Prioridade P0)

| ID | Problema / Incongruência | Ação Obrigatória no Código |
|---|---|---|
| **DB-01** | Nome do banco divergente (`db_master` no `.env`/README vs `dg_master_db` no SQL e PDF 5.2). | Padronizar **`dg_master_db`** em `application.yml`/`application.properties`, `.env`, `.env.example`, `docker-compose.yml` e `README.md`. |
| **DB-02** | Fuso horário `UTC` no JDBC gera erro de data/hora nas regras `CURDATE()` e `NOW()` das triggers/procedures (expiração 24h, atraso, maioridade 18+). | Configurar `serverTimezone=America/Sao_Paulo` na `DB_URL`, `TZ=America/Sao_Paulo` no `docker-compose.yml` e `TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))` no `main()` do Spring Boot. |
| **DB-03** | `hibernate.ddl-auto=create` ou `update` altera ou ignora triggers, procedures, checks e views. | Usar `spring.jpa.hibernate.ddl-auto=validate`. Criar o banco executando manualmente `db/01_schema_objetos.sql` no MySQL Workbench ou cliente MySQL; não usar Flyway/Liquibase. No Docker opcional, montar `db/01_schema_objetos.sql` e `db/02_app_permissions.sql` em `/docker-entrypoint-initdb.d/`. `01_schema_objetos.sql` apaga e recria o banco e só deve ser usado em ambiente descartável. |
| **DB-04** | `DB_URL` no Docker usando `localhost` em vez do hostname do container `mysql`. | Garantir `jdbc:mysql://mysql:3306/dg_master_db?...` no `docker-compose.yml` e documentar `localhost` apenas para execução local fora do Docker. |

### 4.2. Mapeamento JPA e Integração com Triggers/Procedures (Prioridade P0)

| ID | Problema / Bug | Ação Obrigatória no Código |
|---|---|---|
| **JPA-01** | Entidades desalinhadas com o DDL de `01_schema_objetos.sql`. | Ajustar entidades para refletir exatamente as tabelas e colunas: <br>• `Cliente`: `cpf` (`CHAR(11)`), `telefone` (`VARCHAR(20)`), `senha` (`VARCHAR(255)`), `email` (`VARCHAR(254)`), `dataCadastro` (`LocalDateTime`, `insertable=false, updatable=false`).<br>• `Gerente`: `email` (`VARCHAR(254)`), `senha` (`VARCHAR(255)`), `matricula` (`VARCHAR(20)`).<br>• `Jogo`: adicionar `valorReposicao` (`BigDecimal`, coluna `valor_reposicao`, `nullable=false`).<br>• `Aluguel`: `idGerente` (`nullable=true`), `dataReserva` (`LocalDateTime`), `dataExpiracaoReserva` (`LocalDateTime`), `dataRetirada` (`LocalDateTime`), `devolucaoPrevista` (`LocalDate`), `devolucaoEfetiva` (`LocalDate`), `valorDesconto` (`BigDecimal`), `valorTotal` (`BigDecimal`), `qtdRenovacoes` (`Integer`), `statusAluguel` (`EnumType.STRING`: `RESERVADO`, `RETIRADO`, `FINALIZADO`, `EXPIRADO`, `CANCELADO`).<br>• `ItemAluguel`: chave composta `(id_aluguel, id_jogo)`, `quantidadeAlugada`, `valorCobrado`.<br>• `Penalidade`: `motivoPenalidade`, `valorMulta`, `statusPagamento` (`PENDENTE`, `PAGO`), `dataGeracao`. |
| **JPA-02** | **Cache de 1º nível do Hibernate desatualizado após Triggers**: quando `aluguel` ou `item_aluguel` são inseridos/atualizados, triggers preenchem `data_expiracao_reserva`, `valor_cobrado`, `valor_desconto`, `valor_total`, `data_retirada`, `devolucao_efetiva` e alteram `jogo.quantidade_estoque`. | Após operações de escrita ou chamada de procedures, executar `entityManager.flush()` e `entityManager.refresh(...)` (ou `entityManager.clear()` antes de reconsultar) para que o contexto JPA leia os valores calculados pelas triggers do MySQL. |
| **BUG-01** | **Estoque decrementado duas vezes** (Java subtraindo `quantidadeEstoque` + trigger `trg_atualiza_estoque_aluguel`). | **Remover completamente** qualquer manipulação manual de `quantidadeEstoque` nos `Service`s de reserva, cancelamento, expiração e devolução. Deixar 100% a cargo das triggers `trg_atualiza_estoque_aluguel` e `trg_restaura_estoque_devolucao`. |
| **BUG-02** | **Status antigo (`DEVOLVIDO`)** impedindo disparo de `trg_restaura_estoque_devolucao` e `trg_aluguel_estados_bu`. | Padronizar o enum/status de encerramento para **`FINALIZADO`** em todo o back-end, controllers, queries e templates Thymeleaf. |
| **PROC-01** | Regras duplicadas em Java em vez de invocar as Stored Procedures do banco. | Refatorar `AluguelService` e `Scheduler` para invocar diretamente:<br>• `CALL sp_criar_reserva(?, ?, @id)` + `CALL sp_adicionar_item(?, ?, ?)` (dentro da mesma `@Transactional`)<br>• `CALL sp_retirar_aluguel(?, ?)`<br>• `CALL sp_renovar_aluguel(?, ?)`<br>• `CALL sp_finalizar_aluguel(?, ?)`<br>• `CALL sp_cancelar_reserva(?)`<br>• Job de expiração: `CALL sp_expirar_reservas()`<br>• Job de atrasos: `CALL sp_registrar_atrasos()` |
| **VIEW-01** | Relatórios gerenciais (RF10) fazendo agregações pesadas/duplicadas em Java. | Consultar diretamente as views `vw_relatorio_alugueis`, `vw_jogos_mais_alugados`, `vw_faturamento_categoria` e `vw_penalidades_pendentes` (e exibir `log_auditoria` no painel administrativo). |
| **ERR-01** | Erros lançados pelas triggers (`SIGNAL SQLSTATE '45000'`) estourando como `500 Internal Server Error` genérico. | Criar/atualizar `@ControllerAdvice` / `@RestControllerAdvice` para capturar `DataIntegrityViolationException` / `JpaSystemException` / `SQLException`, extrair a mensagem raiz (`SQLException.getMessage()`, ex.: `"RN2: limite de jogos ativos por cliente excedido"`) e retornar `400 Bad Request` (API) ou `RedirectAttributes` com alerta amigável (Thymeleaf). |
| **CPF-01** | Falha na constraint `ck_cliente_cpf` (`CHAR(11)` apenas dígitos) ao cadastrar cliente com pontuação (`000.000.000-00`). | Sanitizar CPF (`cpf.replaceAll("\\D", "")`) no DTO/Controller/Service antes de salvar e validar dígitos verificadores (módulo 11) na entrada. |

### 4.3. Segurança e Controle de Acesso (Prioridade P0/P1)

| ID | Vulnerabilidade / Problema | Correção Obrigatória |
|---|---|---|
| **BUG-04** | **IDOR** em `/api/clientes/{cpf}`, `/api/clientes/{cpf}/alugueis`, `/api/clientes/{cpf}/dependentes` e `/api/alugueis/{id}/cancelar` ou `/renovar`: cliente autenticado A consegue consultar ou alterar dados do cliente B. | Validar no Controller/Service (`@PreAuthorize` ou verificação explícita com `Authentication`): se o usuário logado tiver `ROLE_CLIENTE`, o `cpf` da URL (ou o `id_cliente` dono do `id_aluguel`) **deve** coincidir com o usuário autenticado; caso contrário, lançar `AccessDeniedException` (`403 Forbidden`). Apenas `ROLE_GERENTE` pode acessar dados de terceiros. |
| **BUG-05** | Endpoint/ação de retirada (`retirar`) acessível por `ROLE_CLIENTE`. | Restringir `PUT /api/alugueis/{id}/retirar` (e equivalente MVC) exclusivamente a `hasRole("GERENTE")`, passando obrigatoriamente o `id_gerente` autenticado para `sp_retirar_aluguel`. |
| **BUG-15** | Validação da chave de cadastro de gerente (`/admin`) vulnerável a timing attack ou sem proteção. | Comparar segredo usando `MessageDigest.isEqual(...)` em bytes UTF-8 e nunca logar o valor da chave. |

### 4.4. Higiene do Repositório e Documentação (Prioridade P2)

| ID | Problema | Correção Obrigatória |
|---|---|---|
| **REPO-01** | Scripts temporários na raiz (`fix_*.py`, `tmp_update_css.py`, `check_links.py`) e `RFC-001-dg-master-especificacao-tecnica(1).md` com sufixo `(1)`. | Remover scripts temporários (`tmp_*`, `fix_*`) ou movê-los para `scripts/` se úteis; organizar RFCs em `docs/` sem `(1)`. |
| **REPO-02** | `LICENSE` ausente apesar de citada no `README.md`. | Criar arquivo `LICENSE` (Apache 2.0) na raiz e atualizar o `README.md` com a arquitetura *Database-Centric* (diagrama de triggers/procedures/views) e matriz de permissões por papel. |

---

## 5. Guia de Execução Passo a Passo para o OpenCode

> **Instruções para o agente OpenCode:** Execute as etapas abaixo em ordem, validando a compilação e os testes a cada etapa.

### Etapa 1 — Inspeção Inicial do Repositório
1. Mapear a árvore completa de arquivos (`src/`, `pom.xml`, `docker-compose.yml`, `.env*`, scripts `.sql` existentes).
2. Identificar onde estão:
   - Configurações de banco (`application.yml` ou `application.properties`);
   - Entidades JPA (`Cliente`, `Gerente`, `Dependente`, `Jogo`, `Categoria`, `Aluguel`, `ItemAluguel`, `Penalidade`);
   - Serviços (`AluguelService`, `ClienteService`, `JogoService`, `PenalidadeService`, etc.) e `@Scheduled` jobs;
   - `SecurityConfig` e controllers `web` / `api`.

### Etapa 2 — Banco MySQL (manual) e Docker opcional
1. Manter `db/01_schema_objetos.sql` como o script único do schema físico, funções, triggers, procedures, views e dados iniciais.
2. Para rodar pela IDE, iniciar o MySQL local e executar `db/01_schema_objetos.sql` uma vez pelo MySQL Workbench. A aplicação não cria o banco nem modifica o schema automaticamente.
3. Manter `spring.jpa.hibernate.ddl-auto=validate`; não adicionar Flyway/Liquibase.
4. Atualizar `docker-compose.yml`, `.env.example` e `application.*` para:
   - Banco: `dg_master_db`
   - Timezone: `America/Sao_Paulo`
   - `healthcheck` no serviço MySQL e `depends_on: condition: service_healthy` no serviço da aplicação.
   - Para IntelliJ, usar `localhost`, usuário MySQL local e senha configurada na Run Configuration quando necessária. O arquivo `.env` é usado pelo Docker Compose e não é carregado automaticamente pelo IntelliJ.

### Etapa 3 — Refatoração das Entidades e Camada de Serviço (Database-First)
1. Atualizar as entidades JPA conforme **JPA-01** (incluindo `jogo.valorReposicao`, `aluguel.idGerente` nullable, `aluguel.dataExpiracaoReserva`, `aluguel.dataRetirada`, `aluguel.qtdRenovacoes`, enum `FINALIZADO`).
2. Refatorar os `Service`s conforme **BUG-01**, **PROC-01** e **JPA-02**:
   - Remover lógica duplicada de baixa/devolução de estoque, cálculo manual de desconto/multa e transições manuais de status.
   - Delegar criação de reserva, adição de item, retirada, renovação, devolução/finalização, cancelamento, expiração e registro de atrasos às *Stored Procedures* (`sp_*`).
   - Garantir `flush()`/`refresh()` após chamadas que disparam triggers.
3. Conectar os relatórios administrativos (RF10) e auditoria (RNF08) às views (`vw_relatorio_alugueis`, `vw_jogos_mais_alugados`, `vw_faturamento_categoria`, `vw_penalidades_pendentes`) e à tabela `log_auditoria`.
4. Implementar tratamento global de exceções (`@ControllerAdvice` / `@RestControllerAdvice`) para extrair mensagens `SQLSTATE '45000'` das triggers/procedures e exibi-las corretamente na API e nas telas Thymeleaf.

### Etapa 4 — Segurança e Validações de Entrada
1. Corrigir **BUG-04** (IDOR) e **BUG-05** (endpoint `retirar` restrito a `GERENTE`).
2. Garantir sanitização de CPF (`11` dígitos) e validação de maioridade/campos nos DTOs antes de persistir.

### Etapa 5 — Testes Automatizados e Limpeza
1. Criar/atualizar testes de integração contra MySQL (ou Testcontainers) validando:
   - **RN1:** Multa de R$ 5,00/jogo/dia com teto no `valor_reposicao` e idempotência de `sp_registrar_atrasos()`.
   - **RN2:** Bloqueio ao tentar ultrapassar 4 unidades ativas (reservas + retirados).
   - **RN3:** Expiração após 24h via `sp_expirar_reservas()` restaurando estoque automaticamente.
   - **RN6:** Descontos progressivos (0%, 10%, 20%, 25%) calculados pelo trigger `trg_recalcula_totais_aluguel`.
   - **RN7:** Bloqueio de cadastro de titular menor de 18 anos (`trg_cliente_valida_bi`).
   - **Segurança:** Retorno `403 Forbidden` em tentativa de acesso cruzado (IDOR) e chamada de `retirar` por `CLIENTE`.
2. Limpar arquivos temporários na raiz (`fix_*.py`, `tmp_*`), adicionar `LICENSE` e atualizar `README.md`.

---

## 6. Ajustes Pendentes no Documento Acadêmico (`DG MASTER - DB II - ATUALIZADO.pdf`)

Para a entrega final da disciplina SPOBDD2, atualizar o texto do PDF nos seguintes pontos:
1. **Seções 1.1 e 1.2:** Remover texto duplicado (1.1 = contexto/problema; 1.2 = solução).
2. **Seção 2.1, 2.3 e 2.4:** Alinhar com a arquitetura *Database-Centric* (explicando que triggers, functions e procedures garantem a integridade das regras RN1–RN7 diretamente no SGBD), trocar `Aluguel_Reserva_Contrato`/`Contém` por `ALUGUEL`/`ITEM_ALUGUEL` e corrigir documentação do front-end (CSS utilizado vs Bootstrap).
3. **Seção 3.2, 3.4 e 3.5:** Corrigir quebra de linha entre RF02 e RF03 na tabela; unificar "gênero" como "categoria" (RF06); criar subseções formais para **Regras de Negócio (RN1–RN7)** e **Histórias de Usuário (HU01, CA01, CA02)**.
4. **Seção 4 (Diagramas e Telas):** Corrigir "finalizar compra" para "finalizar reserva" e "email ou CPF" para "email e CPF" no Caso de Uso; adicionar o ator **Gerente**; remover `solicitarReserva()` da classe `Dependente`; tornar `id_gerente` opcional (`0..1`) em `ALUGUEL`; incluir prints reais do sistema na Seção 4.4.
5. **Seções 5.3, 6, 7, 8 e Apêndice A:** Documentar todos os objetos criados (`fn_parametro`, `fn_percentual_desconto`, `fn_calcular_multa`, todas as triggers e procedures, e as 4 views); corrigir os títulos repetidos das Seções 6 e 7; preencher as seções marcadas como "EM DESENVOLVIMENTO"; colar `db/01_schema_objetos.sql` integralmente no **Apêndice A.1** e `db/02_app_permissions.sql` no **Apêndice A.2**.