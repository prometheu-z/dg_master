1. Persistência e Entidades Domain (JPA)

 • [x] Mapear as entidades JPA no pacote `projeto.bdd2.dgmaster.entity`:
	 • Cliente
	 • Dependente
	 • Gerente
	 • Jogo
	 • AluguelReserva
	 • Penalidade
	 • StatusAluguel
	 • Tabela associativa `contem` (Jogo ↔ AluguelReserva)
 • [x] Criar repositórios Spring Data JPA para todas as entidades.
 • [x] Criar script de criação de tabelas em `src/main/resources/schema.sql`.
 • [ ] Configurar migrações versionadas com Flyway ou Liquibase, se necessário.
 • [ ] Persistir preço unitário no aluguel para relatórios históricos de rentabilidade.

2. Autenticação e Segurança (Épico 1 / RNF01, RNF03)

 • [x] Configurar Spring Security com autenticação baseada em sessão e Thymeleaf.
 • [x] Configurar criptografia de senha com `BCryptPasswordEncoder`.
 • [x] Proteger operações de escrita com CSRF em autenticação por sessão.
 • [x] Persistir sessão no login REST e rotacionar o identificador da sessão.
 • [x] Implementar endpoints de login e cadastro (`/api/auth/cadastro`).
 • [ ] Implementar validação real de 2FA (`/api/auth/2fa/verificar`); a rota responde `501` enquanto não há verificador configurado.
 • [x] Aplicar autorização por perfil para Cliente e Gerente.
 • [ ] Definir autenticação própria e autorização do Dependente.

3. Módulo do Cliente e Dependentes (Épico 1 / RF01, RF02, RF04, RN7)

 • [x] Validar idade mínima do titular (18 anos ou mais) no cadastro (RN7).
 • [x] Implementar serviços para cadastro e gestão de dependentes.
 • [x] Implementar endpoints autenticados para cadastro e gestão de dependentes.
 • [x] Implementar solicitação de reserva de dependente com aprovação do titular e retirada exclusiva pelo titular.
 • [x] Implementar consulta da situação dos jogos e histórico do cliente (`/api/clientes/{cpf}/alugueis`).
 • [x] Permitir que o titular consulte os aluguéis dos seus dependentes.

4. Catálogo e Gestão de Estoque (Épico 2 e 3 / RF06, RF09)

 • [x] Implementar consulta e filtragem por gênero, categoria e faixa etária na camada de serviço.
 • [x] Implementar endpoints REST públicos de consulta e filtragem por gênero, categoria e faixa etária.
 • [x] Implementar CRUD administrativo de jogos para o Gerente.
 • [x] Bloquear exclusão de jogo quando houver cópia alugada ou reservada.
 • [x] Implementar controle de quantidade e disponibilidade do estoque.

5. Reserva, Aluguel e Contrato (Épico 2 / RF03, RF05, RF07, RN2, RN4, RN6)

 • [x] Implementar serviço de solicitação de reserva.
 • [x] Validar o limite máximo de 4 jogos por cliente e dependentes (RN2).
 • [x] Gerar e incluir os termos de contrato no aluguel (RN4).
 • [x] Implementar cálculo automático de desconto progressivo por quantidade de jogos (RN6, RF05).
 • [x] Implementar cancelamento de reservas.
 • [x] Implementar retirada de jogos reservados.
 • [x] Definir prazo de aluguel de 7 dias no serviço e permitir uma renovação de mais 7 dias.
 • [x] Implementar endpoint de renovação única do aluguel (RF07).

6. Devolução, Multa e Tarefas Agendadas / Jobs (Épico 3 e Sistema / RN1, RN3, RF08)

 • [x] Implementar fluxo de devolução com registro da data real.
 • [x] Calcular multa diária por jogo, limitada ao valor de reposição, e gerar `Penalidade`.
 • [x] Suspender a conta por 2 dias por dia de atraso, com reativação após o prazo e pagamento.
 • [x] Criar job `@Scheduled` para expirar reservas não retiradas após 24 horas (RN3).
 • [x] Criar job `@Scheduled` para verificar diariamente os atrasos e reativar contas elegíveis.
 • [x] Converter expiração em crédito na carteira, aplicado na próxima locação.
 • [x] Implementar endpoint gerencial de prazos para reservas, retiradas e atrasos.
 • [ ] Implementar relatórios de rentabilidade com preço histórico por jogo.

7. Camada de Apresentação e Notificação

 • [ ] Criar templates Thymeleaf com Bootstrap para usuários e gerentes.
 • [ ] Implementar catálogo responsivo com filtros.
 • [ ] Implementar telas de cadastro, login, reservas, aluguéis e devoluções.
 • [ ] Integrar Spring Boot Starter Mail com Mailtrap para desenvolvimento e testes.
 • [ ] Enviar notificações de confirmação, expiração de reserva e atraso/penalidade.
 • [ ] Criar dashboard e relatórios estatísticos do Gerente (RF10).

8. Testes e Qualidade

 • [ ] Criar testes unitários para as regras RN1 a RN7.
 • [x] Criar testes de integração para repositories e serviços.
 • [x] Criar testes de integração dos endpoints de catálogo, dependentes, aluguéis e fluxos de autenticação existentes.
 • [ ] Validar critérios de aceitação das histórias HU01 a HU12.
 • [x] Executar `mvnw.cmd clean test -q` com Java 25 antes de cada entrega.

9. Infraestrutura e Documentação

 • [ ] Configurar conexão MySQL por variáveis de ambiente, sem credenciais no repositório.
 • [ ] Definir estratégia de inicialização do banco: `schema.sql` ou Flyway/Liquibase.
 • [ ] Configurar Docker para desenvolvimento e homologação.
 • [ ] Adicionar documentação interativa com springdoc/OpenAPI.
 • [x] Atualizar a RFC conforme as decisões de implementação.

Ordem sugerida de execução:

1. Persistência e entidades JPA (concluído parcialmente).
2. Segurança básica, cadastro e login.
3. CRUD de jogos e filtros do catálogo.
4. Cadastro e gestão de dependentes.
5. Reserva e aluguel com limite, contrato e desconto.
6. Jobs de expiração e verificação de atrasos.
7. Devolução, penalidades e renovação.
8. Views Thymeleaf, notificações e dashboard.
9. 2FA, auditoria, testes completos e infraestrutura.
