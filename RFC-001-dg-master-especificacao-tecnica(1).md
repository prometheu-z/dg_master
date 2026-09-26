# RFC-001 — Sistema DG Master (Aluguel de Jogos de Tabuleiro)

| Campo | Valor |
|---|---|
| Status | Draft |
| Autor(es) | Equipe DG Master — Grupo 04 (IFSP, SPOBDD2 — Banco de Dados) |
| Stack alvo | Java 21 + Spring Boot 3.x + MySQL 8 + Thymeleaf/Bootstrap |
| Objetivo do documento | Especificação técnica completa para guiar a implementação do sistema (por humanos ou por um agente de IA via CLI) |

---

## 1. Visão Geral

O DG Master é uma plataforma web que permite o aluguel online de jogos de tabuleiro, com retirada física na loja. O sistema tem quatro atores: **Visitante**, **Cliente (Locatário)**, **Dependente** e **Gerente**, e cobre o ciclo completo: cadastro, catálogo, reserva, aluguel, devolução, penalidade por atraso e gestão administrativa de estoque.

Este documento consolida: modelo de domínio, regras de negócio, requisitos funcionais/não funcionais, histórias de usuário com critérios de aceitação, arquitetura técnica, esquema de banco de dados sugerido e um mapa de endpoints REST — tudo o que é necessário para começar a implementação sem precisar voltar aos documentos originais do backlog.

---

## 2. Arquitetura Técnica

### 2.1. Visão Geral da Arquitetura

O DG Master segue uma arquitetura em camadas, no padrão cliente-servidor, rodando como monolito modular em um único processo Spring Boot: apresentação, aplicação/negócio e dados.

```
Controller (REST/MVC) → Service (regras de negócio) → Repository (Spring Data JPA) → MySQL
```

A camada de apresentação trata a interação com os quatro atores (Visitante, Cliente, Dependente, Gerente); a camada de aplicação concentra as regras de negócio (RN1–RN7), como validação de elegibilidade (RN7), controle do limite de empréstimos (RN2) e cálculo de descontos (RN6); a camada de dados persiste as informações no MySQL, conforme o modelo lógico definido na seção 7.

Internamente, o back-end segue o padrão MVC adaptado ao modelo de camadas do Spring Boot: **Controller** (recebe requisições HTTP), **Service** (executa regras de negócio) e **Repository/DAO** (acessa o banco via Spring Data JPA). Essa separação favorece manutenibilidade e escalabilidade (RNF04), além de facilitar os testes unitários e de integração previstos na seção 8.

### 2.2. Front-End

Thymeleaf (motor de templates server-side nativo do Spring Boot) + Bootstrap para estilização e responsividade, sem SPA.

Essa escolha concentra toda a aplicação em uma única stack de linguagem, eliminando a necessidade de um projeto front-end separado (build tools, gerenciador de pacotes, CORS, deploy independente) — vantajoso para um grupo pequeno com prazo definido. O Bootstrap atende ao requisito de responsividade (RNF02), permitindo que catálogo e telas administrativas se adaptem a diferentes dispositivos sem CSS customizado adicional.

### 2.3. Back-End e Linguagem de Programação

Java 21 + Spring Boot 3.x, com os seguintes módulos:

- **Spring Web (MVC):** roteamento das requisições e construção dos controllers.
- **Spring Data JPA (com Hibernate):** mapeamento objeto-relacional das entidades da seção 7 (Cliente, Dependente, Jogo, AluguelReserva, Penalidade, Gerente) e implementação do padrão DAO/Repository.
- **Spring Security:** autenticação **por sessão** (padrão Thymeleaf) e controle de acesso por perfil (Cliente x Gerente), com suporte a 2FA (RNF03) como segunda etapa do login. Optou-se por sessão em vez de JWT por simplicidade — não há previsão de front-end desacoplado (SPA) neste momento; a migração para JWT fica registrada como possível evolução futura caso isso mude.
- **Bean Validation (Jakarta Validation):** validação de dados de entrada, como formato de CPF e força de senha (CA01/CA02 da HU01).
- **Spring Boot Starter Mail:** envio de notificações (ver 2.5).
- **Spring Scheduler (`@Scheduled`):** jobs de expiração de reserva (RN3) e verificação diária de atraso (RN1).
- **Thymeleaf:** renderização das views, conforme 2.2.

Estrutura de pacotes sugerida:

```
com.dgmaster
├── cliente/         (Cliente, Dependente: controller, service, repository, dto)
├── jogo/            (Jogo: controller, service, repository, dto)
├── aluguel/         (AluguelReserva, Penalidade: controller, service, repository, dto)
├── gerente/         (Gerente, relatórios/estatísticas)
├── seguranca/       (Spring Security, autenticação por sessão, 2FA)
├── notificacao/     (envio de e-mail via Mailtrap)
├── scheduler/       (jobs: expiração de reserva, cálculo de atraso)
├── comum/           (exceptions, validações, utilitários)
└── DgMasterApplication.java
```

### 2.4. Sistema Gerenciador de Banco de Dados (SGBD)

MySQL 8, na versão estável mais recente disponível no momento da implantação.

Justificativa: o modelo de dados é fortemente relacional, com diversas chaves estrangeiras e um relacionamento N:N (Jogo ↔ AluguelReserva, resolvido pela tabela associativa `contem`, seção 7.6); além disso, há ampla compatibilidade e integração nativa com Spring Data JPA/Hibernate via driver JDBC oficial (`mysql-connector-j`), exigindo configuração mínima em `application.properties`.

A comunicação entre aplicação e banco ocorre via JDBC, abstraída pelo Hibernate/Spring Data JPA, que traduz operações das entidades Java em comandos SQL (SELECT, INSERT, UPDATE, DELETE), automatizando grande parte das operações CRUD previstas na seção 5.

### 2.5. Integração (Mecanismos de Comunicação e APIs)

O sistema expõe endpoints internos no padrão REST (seção 9), consumidos pelas próprias views Thymeleaf.

Para notificações por e-mail — confirmação de cadastro, expiração de reserva (RN3) e alertas de atraso/penalidade (RN1) — será usado o Spring Boot Starter Mail integrado ao **Mailtrap** como provedor SMTP, cobrindo o ambiente de desenvolvimento e testes/homologação. A definição do provedor real de produção (ex.: Gmail SMTP, SendGrid, Amazon SES) fica como item em aberto (seção 15) e não deve afetar a implementação, já que a troca se resume a alterar as credenciais SMTP em `application.properties`.

Não estão previstas, neste primeiro momento, integrações com APIs de terceiros para pagamento ou geolocalização, podendo ser incorporadas em versões futuras do sistema.

### 2.6. Ferramentas Complementares, Controle de Versão e Infraestrutura

- **Controle de versão:** Git, com repositório no GitHub, permitindo trabalho colaborativo entre os integrantes do grupo.
- **Gerenciador de dependências e build:** Maven, responsável por gerenciar as bibliotecas e gerar o artefato final (`.jar`).
- **IDE:** IntelliJ IDEA (Community Edition) ou Eclipse/STS, ambas com suporte nativo a projetos Spring Boot.
- **Documentação de API:** springdoc-openapi (Swagger UI), gerando documentação interativa dos endpoints REST.
- **Ferramenta de teste de API:** Postman, para testes manuais exploratórios durante o desenvolvimento.
- **Gestão do projeto:** Jira (backlog e sprints do grupo).
- **Ambiente de banco de dados:** MySQL Workbench, para administração visual durante desenvolvimento e homologação.
- **Infraestrutura:** ambiente local via **Docker** tanto para desenvolvimento quanto para a etapa de homologação (HML) mencionada na Definição de Pronto do projeto — evita depender de hospedagem externa (ex.: GitHub Pages, que só serve conteúdo estático e não roda uma aplicação Java/Spring Boot). Migração para um serviço gratuito como Railway ou Render fica como evolução possível caso seja necessário expor o ambiente de homologação publicamente.

---

## 3. Atores

| Ator | Descrição |
|---|---|
| Visitante | Usuário não autenticado; acessa catálogo e formulário de cadastro |
| Cliente (Locatário) | Maior de 18 anos, autenticado, cadastro completo; aluga/reserva jogos |
| Dependente | Vinculado a um Cliente titular (pode ser menor de idade); solicita reservas sujeitas à aprovação do titular |
| Gerente | Administra estoque, prazos, devoluções e relatórios |
| Sistema | Ator automático: expira reservas, aplica penalidades, calcula descontos |

---

## 4. Regras de Negócio (RN)

| Cód. | Regra | Descrição | Implementação sugerida |
|---|---|---|---|
| RN1 | Política de atrasos | Cliente que atrasa devolução sofre suspensão por tempo proporcional ao atraso + multa | `PenalidadeService`, disparada no job de checagem diária |
| RN2 | Limite de empréstimos | Máximo de 4 jogos alugados simultaneamente, somando os dos dependentes | Validação em `AluguelService.solicitarReserva()` antes de persistir |
| RN3 | Tempo de retirada | Reserva não retirada em 24h expira automaticamente e o valor é reembolsado | `ReservaExpirationJob` (Spring `@Scheduled`, roda a cada hora) |
| RN4 | Contrato de aluguel | Todo aluguel exige contrato com termos de dano e valor de multa por atraso | Campos de contrato embutidos na entidade `AluguelReserva` |
| RN5 | Tempo de atividade | Sistema deve estar no ar 24x7 | Requisito de infraestrutura/deploy, fora do código de negócio |
| RN6 | Política de fidelidade | Desconto progressivo por múltiplos jogos alugados | `DescontoService.calcularDesconto(quantidadeJogos)` |
| RN7 | Elegibilidade | Cliente titular deve ter 18 anos completos ou mais | Validação em `ClienteService.cadastrar()`, calculada a partir de `dataNascimento` |

---

## 5. Requisitos Funcionais (RF) e Operações CRUD

| Cód. | Nome | Descrição | CRUD | Ator |
|---|---|---|---|---|
| RF01 | Cadastrar usuário | Cadastro com CPF, e-mail e senha segura | Create | Visitante |
| RF02 | Visualizar situação de jogos | Ver jogos a retirar, atrasados ou alugados | Read | Cliente |
| RF03 | Gerenciar reservas | Solicitar ou cancelar reservas | Create/Delete | Cliente/Dependente |
| RF04 | Cadastrar dependentes | Cliente cadastra dependentes com mesmas capacidades de aluguel | Create | Cliente |
| RF05 | Gerar descontos | Desconto automático por aluguel de múltiplos jogos | — (regra automática) | Sistema |
| RF06 | Filtrar catálogo | Filtro por idade, gênero e categoria | Read | Cliente/Visitante |
| RF07 | Renovar aluguel online | Renovação única do período de aluguel | Update | Cliente |
| RF08 | Gerenciar prazos de devolução | Visão de jogos alugados/estoque/atrasados/a retirar | Read | Gerente |
| RF09 | Gerenciar estoque | Inserir/remover jogos do catálogo | Create/Delete | Gerente |
| RF10 | Visualizar estatísticas | Relatórios de jogos mais/menos alugados e mais/menos rentáveis | Read | Gerente |

---

## 6. Requisitos Não Funcionais (RNF)

| Cód. | Tipo | Descrição | Implementação sugerida |
|---|---|---|---|
| RNF01 | Segurança | Dados armazenados e transmitidos de forma criptografada | HTTPS/TLS em produção; senha com `BCryptPasswordEncoder` |
| RNF02 | Usabilidade | Interface responsiva | Bootstrap (grid + componentes responsivos) |
| RNF03 | Segurança | Autenticação de dois fatores (2FA) opcional | Spring Security + TOTP (ex. biblioteca `java-otp`) ou código por e-mail |
| RNF04 | Compatibilidade/Escalabilidade | Suportar usuários simultâneos mantendo desempenho em operações críticas | Índices em CPF/e-mail, connection pool (HikariCP), paginação nas listagens |
| RNF05 | Produto | Catálogo organizado com filtros | Endpoints com query params + índices em `genero`, `faixaEtariaRecomendada` |
| RNF06 | Compatibilidade | Funcionar em navegadores Chromium/WebKit | Evitar recursos exclusivos de outros engines no front-end |
| RNF07 | Disponibilidade | Sistema ativo 24x7 | Infraestrutura de deploy (fora do escopo de código) |
| RNF08 | Auditoria | Operações críticas devem ser registradas para rastreabilidade | Tabela/log de auditoria (ex. Spring Data Envers ou tabela `log_auditoria`) |

---

## 7. Modelo de Domínio (Entidades)

> Baseado no Projeto Lógico (DER) já definido pelo grupo. `Aluguel_Reserva_Contrato` foi mantida como uma única entidade, já que o diagrama lógico incorpora os campos de contrato diretamente nela.

### 7.1. Cliente
| Campo | Tipo | Observação |
|---|---|---|
| cpf | VARCHAR(11) | **PK** |
| nome | VARCHAR | |
| email | VARCHAR | único |
| senha | VARCHAR | hash (BCrypt), nunca texto puro |
| dataNascimento | DATE | usada para validar RN7 |
| statusConta | BOOLEAN | ativo/suspenso (RN1) |

### 7.2. Dependente
| Campo | Tipo | Observação |
|---|---|---|
| idDependente | INTEGER | **PK**, auto-incremento |
| nome | VARCHAR | |
| dataNascimento | DATE | |
| clienteCpf | VARCHAR(11) | **FK** → Cliente |

### 7.3. Gerente
| Campo | Tipo | Observação |
|---|---|---|
| idGerente | INTEGER | **PK**, auto-incremento |
| nome | VARCHAR | |
| email | VARCHAR | único |
| senha | VARCHAR | hash |

### 7.4. Jogo
| Campo | Tipo | Observação |
|---|---|---|
| codigoJogo | INTEGER | **PK**, auto-incremento |
| nome | VARCHAR | |
| categoria | VARCHAR | |
| genero | VARCHAR | |
| faixaEtariaRecomendada | INTEGER | |
| precoLocacao | DECIMAL(10,2) | preço definido pelo gerente para locação do jogo |
| quantidadeEstoque | INTEGER | |
| statusDisponibilidade | BOOLEAN | |
| gerenteId | INTEGER | **FK** → Gerente (quem cadastrou/gerencia) |

### 7.5. AluguelReserva (inclui dados de contrato)
| Campo | Tipo | Observação |
|---|---|---|
| idAluguel | INTEGER | **PK**, auto-incremento |
| dataHoraReserva | DATETIME | |
| dataLimiteRetirada | DATETIME | dataHoraReserva + 24h (RN3) |
| dataDevolucaoReal | DATETIME | nulo até devolução |
| status | VARCHAR/ENUM | `RESERVADO`, `RETIRADO`, `DEVOLVIDO`, `ATRASADO`, `EXPIRADO`, `CANCELADO` |
| valorTotal | DOUBLE | após aplicar desconto (RN6) |
| quantidadeRenovacoes | INTEGER | máx. 1 (RF07) |
| clienteCpf | VARCHAR(11) | **FK** → Cliente |
| dependenteId | INTEGER | **FK** → Dependente, nulo se for o próprio titular |
| termosDanos | VARCHAR/TEXT | cláusula do contrato (RN4) |
| precificacaoMulta | VARCHAR/DOUBLE | valor de multa por atraso |
| dataAssinatura | DATETIME | data de aceite do contrato |

### 7.6. Contém (tabela associativa Jogo ↔ AluguelReserva, N:N)
| Campo | Tipo |
|---|---|
| jogoCodigo | **FK** → Jogo |
| aluguelId | **FK** → AluguelReserva |

### 7.7. Penalidade
| Campo | Tipo | Observação |
|---|---|---|
| idPenalidade | INTEGER | **PK**, auto-incremento |
| tipoPenalidade | VARCHAR | ex. "multa", "suspensão" |
| valorMulta | DOUBLE | |
| dataFimSuspensao | DATE | |
| aluguelId | INTEGER | **FK** → AluguelReserva |

---

## 8. Rascunho de DDL (MySQL) 

```sql
CREATE TABLE cliente (
  cpf VARCHAR(11) PRIMARY KEY,
  nome VARCHAR(150) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  senha VARCHAR(255) NOT NULL,
  data_nascimento DATE NOT NULL,
  status_conta BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE gerente (
  id_gerente INT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(150) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  senha VARCHAR(255) NOT NULL
);

CREATE TABLE dependente (
  id_dependente INT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(150) NOT NULL,
  data_nascimento DATE NOT NULL,
  cliente_cpf VARCHAR(11) NOT NULL,
  FOREIGN KEY (cliente_cpf) REFERENCES cliente(cpf)
);

CREATE TABLE jogo (
  codigo_jogo INT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(150) NOT NULL,
  categoria VARCHAR(100),
  genero VARCHAR(100),
  faixa_etaria_recomendada INT,
  preco_locacao DECIMAL(10,2) NOT NULL,
  quantidade_estoque INT NOT NULL DEFAULT 0,
  status_disponibilidade BOOLEAN NOT NULL DEFAULT TRUE,
  gerente_id INT,
  FOREIGN KEY (gerente_id) REFERENCES gerente(id_gerente)
);

CREATE TABLE aluguel_reserva (
  id_aluguel INT AUTO_INCREMENT PRIMARY KEY,
  data_hora_reserva DATETIME NOT NULL,
  data_limite_retirada DATETIME NOT NULL,
  data_devolucao_real DATETIME NULL,
  status VARCHAR(20) NOT NULL,
  valor_total DOUBLE NOT NULL,
  quantidade_renovacoes INT NOT NULL DEFAULT 0,
  cliente_cpf VARCHAR(11) NOT NULL,
  dependente_id INT NULL,
  termos_danos TEXT,
  precificacao_multa DOUBLE,
  data_assinatura DATETIME,
  FOREIGN KEY (cliente_cpf) REFERENCES cliente(cpf),
  FOREIGN KEY (dependente_id) REFERENCES dependente(id_dependente)
);

CREATE TABLE contem (
  jogo_codigo INT NOT NULL,
  aluguel_id INT NOT NULL,
  PRIMARY KEY (jogo_codigo, aluguel_id),
  FOREIGN KEY (jogo_codigo) REFERENCES jogo(codigo_jogo),
  FOREIGN KEY (aluguel_id) REFERENCES aluguel_reserva(id_aluguel)
);

CREATE TABLE penalidade (
  id_penalidade INT AUTO_INCREMENT PRIMARY KEY,
  tipo_penalidade VARCHAR(50) NOT NULL,
  valor_multa DOUBLE NOT NULL,
  data_fim_suspensao DATE,
  aluguel_id INT NOT NULL,
  FOREIGN KEY (aluguel_id) REFERENCES aluguel_reserva(id_aluguel)
);
```

> Índices recomendados (RNF04): `email` (cliente e gerente, já UNIQUE cobre), `genero` e `faixa_etaria_recomendada` em `jogo`, `status` e `cliente_cpf` em `aluguel_reserva`.

---

## 9. Mapa de Endpoints REST

### Autenticação
- `POST /api/auth/cadastro` — RF01, CA01/CA02/CA03 da HU01
- `POST /api/auth/login`
- `POST /api/auth/2fa/verificar` — RNF03

### Cliente / Dependente
- `GET /api/clientes/{cpf}`
- `POST /api/clientes/{cpf}/dependentes` — RF04, HU03
- `GET /api/clientes/{cpf}/dependentes`
- `GET /api/clientes/{cpf}/alugueis` — RF02, HU02 (retorna vazio → mensagem "Você não possui jogos alugados ou reservados no momento")

### Catálogo
- `GET /api/jogos?genero=&faixaEtaria=&categoria=` — RF06, HU04 (resposta em até 2s, CA01)
- `GET /api/jogos/{codigo}`
- `POST /api/jogos` — RF09, HU05 (Gerente)
- `PUT /api/jogos/{codigo}` — RF09
- `DELETE /api/jogos/{codigo}` — RF09, HU05 CA01/CA02 (bloquear exclusão se houver cópia alugada/reservada)

### Aluguel / Reserva
- `POST /api/alugueis` — RF03, HU06 (valida RN2, RN7, gera contrato RN4)
- `DELETE /api/alugueis/{id}` — cancelar reserva (RF03)
- `PUT /api/alugueis/{id}/retirar` — muda status para RETIRADO
- `PUT /api/alugueis/{id}/renovar` — RF07 (valida `quantidadeRenovacoes < 1`)
- `PUT /api/alugueis/{id}/devolver` — registra `dataDevolucaoReal`, calcula atraso (RN1)
- `GET /api/alugueis?status=&clienteCpf=` — uso do gerente (RF08)

### Gerente / Relatórios
- `GET /api/gerente/estatisticas` — RF10, HU (mais/menos alugados, mais/menos rentáveis)
- `GET /api/gerente/prazos` — RF08

### Jobs internos (sem endpoint, executados via `@Scheduled`)
- Expiração de reserva não retirada em 24h (RN3) → reembolso
- Verificação diária de atraso → gera `Penalidade` e suspende `Cliente` (RN1)

---

## 10. Épicos

| Épico | Escopo |
|---|---|
| EP1 — Cliente | Cadastro, autenticação, gestão de perfil e dependentes |
| EP2 — Reserva e Aluguel | Busca, reserva, aluguel, renovação, descontos |
| EP3 — Gerenciamento | Módulo administrativo: estoque, prazos, relatórios |
| EP4 — Infraestrutura e Banco de Dados | Modelagem e persistência |
| EP5 — Ambiente de Desenvolvimento | Setup, CI/CD, testes, documentação técnica |

---

## 11. Histórias de Usuário e Critérios de Aceitação

| HU | Como... | Quero... | Critérios de aceitação |
|---|---|---|---|
| HU01 | visitante | criar conta com CPF e e-mail | CPF válido; senha forte (letras, números, 1 caractere especial, ≥8 dígitos); dados criptografados |
| HU02 | cliente | ver situação dos meus aluguéis | Se vazio, exibir "Você não possui jogos alugados ou reservados no momento" |
| HU03 | cliente | cadastrar dependentes que não atendem RN7 | Cliente vê aluguéis dos dependentes; recebe solicitação de aprovação de aluguel do dependente |
| HU04 | cliente | filtrar catálogo por idade/gênero | Resposta em até 2s; mensagem se nada encontrado |
| HU05 | gerente | inserir/remover jogos | Bloquear exclusão de jogo com cópia alugada ou reservada |
| HU06 | cliente | alugar jogos | Conta ativa; contrato em conformidade; bloqueio se já houver 4 jogos alugados (RN2) |
| HU07 | dev | estruturar perfis de usuário | Tabelas físicas no MySQL; PKs obrigatórias; FKs sem órfãos |
| HU08 | dev | estruturar tabelas de negócio | Tabelas principais criadas; tabela associativa para N:N |
| HU09 | dev | modelar entidades no nível lógico | Todas as entidades e atributos essenciais mapeados; cardinalidades corretas |
| HU10 | dev | script físico (DDL) | Script MySQL completo; relações N:M convertidas em tabelas associativas; FKs corretamente amarradas |
| HU11 | dev | configurar ambiente de BD | Servidor acessível externamente; banco nomeado; credenciais com permissões adequadas |
| HU12 | dev | configurar conexão back-end/banco (DAO) | Conexão direta estabelecida; camada DAO pronta para ser herdada/consumida |

---

## 12. Fluxo de Eventos (DDD — Comando → Evento)

| Comando | Ator | Evento resultante | RF/RN |
|---|---|---|---|
| Cadastrar conta | Visitante | Usuário cadastrado | RF01 |
| Autenticar | Cliente/Gerente | Sessão iniciada | RF01, RNF03 |
| Cadastrar dependente | Cliente | Dependente cadastrado | RF04 |
| Filtrar catálogo | Cliente/Visitante | Catálogo filtrado | RF06 |
| Solicitar reserva | Cliente/Dependente | Reserva solicitada | RF03 |
| Cancelar reserva | Cliente/Dependente | Reserva cancelada | RF03 |
| Renovar aluguel | Cliente | Aluguel renovado | RF07 |
| Aprovar contrato | Cliente | Contrato assinado | RN4 |
| Retirar jogo reservado | Cliente | Jogo alugado (retirado) | RN3, RF02 |
| Expirar reserva | Sistema | Reserva expirada | RN3 |
| Reembolsar valor | Sistema | Valor reembolsado | RN3 |
| Gerar desconto | Sistema | Desconto aplicado | RF05, RN6 |
| Registrar devolução com atraso | Sistema/Gerente | Atraso registrado | RN1, RF08 |
| Aplicar penalidade | Sistema | Penalidade aplicada | RN1 |
| Inserir jogo | Gerente | Jogo inserido no estoque | RF09 |
| Remover jogo | Gerente | Jogo removido do estoque | RF09 |
| Visualizar estatísticas | Gerente | Relatório visualizado | RF10 |

---

## 13. Glossário (Linguagem Ubíqua)

- **Visitante** — usuário não autenticado que acessa o catálogo ou cria conta.
- **Cliente (Locatário)** — maior de 18 anos, autenticado, elegível para alugar/reservar de forma autônoma.
- **Dependente** — vinculado à conta de um Cliente titular; reservas sujeitas à aprovação do titular.
- **Gerente** — administra estoque, métricas financeiras e de aluguéis.
- **Sistema** — ator automático (expira reservas, aplica penalidades, calcula descontos).
- **Catálogo** — vitrine digital de jogos, com filtros de idade e gênero.
- **Estoque** — controle da quantidade física de cópias disponíveis.
- **Reserva** — separação temporária (máx. 24h) de um jogo.
- **Aluguel/Locação** — retirada física efetivada, inicia contagem de devolução.
- **Contrato de Aluguel** — termo gerado pelo sistema com regras, multas e responsabilidades.
- **Penalidade** — sanção automática (multa ou suspensão) por atraso.

---

## 14. Ordem sugerida de implementação (para o agente de IA)

1. Setup do projeto Spring Boot (Maven, dependências, `application.properties` com MySQL).
2. Modelagem das entidades JPA (seção 7) + `schema.sql`/migrations (Flyway recomendado).
3. Camada de segurança básica: cadastro + login + hash de senha (RF01, HU01).
4. CRUD de Jogo + filtros de catálogo (RF06, RF09).
5. Cadastro de Dependente (RF04, HU03).
6. Fluxo de reserva/aluguel com validações de RN2 e RN7 (RF03, HU06).
7. Geração de contrato (RN4) embutida no aluguel.
8. Job de expiração de reserva de 24h (RN3).
9. Cálculo de desconto progressivo (RN6, RF05).
10. Fluxo de devolução + cálculo de atraso + penalidade (RN1, RF08).
11. Renovação de aluguel (RF07).
12. Painel/endpoints administrativos e estatísticas (RF10, RF08).
13. 2FA (RNF03) e auditoria (RNF08) — podem ficar para uma segunda iteração.
14. Testes unitários e de integração cobrindo as regras de negócio críticas (RN1–RN7).

---

## 15. Itens em aberto

- Definir provedor de e-mail (SMTP) a ser usado em **produção** (Mailtrap cobre apenas dev/testes, ver 2.5).
- Definir se `status` das entidades será `VARCHAR` ou `ENUM` no MySQL (sugestão: `VARCHAR` + enum Java, mais flexível para migrações).
- Confirmar regra exata de cálculo de multa/suspensão em RN1 (proporcionalidade não detalhada nos documentos originais).
