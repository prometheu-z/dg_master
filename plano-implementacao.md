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

2. Autenticação e Segurança (Épico 1 / RNF01, RNF03)

 • [ ] Configurar Spring Security com autenticação baseada em sessão e Thymeleaf.
 • [ ] Configurar criptografia de senha com `BCryptPasswordEncoder`.
 • [ ] Implementar endpoints de login, cadastro (`/api/auth/cadastro`) e verificação 2FA (`/api/auth/2fa/verificar`).
 • [ ] Aplicar autorização por perfil: Cliente, Dependente e Gerente.

3. Módulo do Cliente e Dependentes (Épico 1 / RF01, RF02, RF04, RN7)

 • [ ] Validar idade mínima do titular (18 anos ou mais) no cadastro (RN7).
 • [ ] Implementar endpoints e serviços para cadastro e gestão de dependentes.
 • [ ] Implementar consulta da situação dos jogos e histórico do cliente (`/api/clientes/{cpf}/alugueis`).
 • [ ] Permitir que o titular consulte os aluguéis dos seus dependentes.

4. Catálogo e Gestão de Estoque (Épico 2 e 3 / RF06, RF09)

 • [ ] Implementar endpoints de consulta e filtragem por gênero, categoria e faixa etária.
 • [ ] Implementar CRUD administrativo de jogos para o Gerente.
 • [ ] Bloquear exclusão de jogo quando houver cópia alugada ou reservada.
 • [ ] Implementar controle de quantidade e disponibilidade do estoque.

5. Reserva, Aluguel e Contrato (Épico 2 / RF03, RF05, RF07, RN2, RN4, RN6)

 • [ ] Implementar serviço de solicitação de reserva.
 • [ ] Validar o limite máximo de 4 jogos por cliente e dependentes (RN2).
 • [ ] Gerar e incluir os termos de contrato no aluguel (RN4).
 • [ ] Implementar cálculo automático de desconto progressivo por quantidade de jogos (RN6, RF05).
 • [ ] Implementar cancelamento de reservas.
 • [ ] Implementar retirada de jogos reservados.
 • [ ] Implementar endpoint de renovação única do aluguel (RF07).

6. Devolução, Multa e Tarefas Agendadas / Jobs (Épico 3 e Sistema / RN1, RN3, RF08)

 • [ ] Implementar fluxo de devolução com registro da data real.
 • [ ] Calcular atraso e gerar `Penalidade`.
 • [ ] Suspender a conta conforme a política de atrasos (RN1).
 • [ ] Criar job `@Scheduled` para expirar reservas não retiradas após 24 horas (RN3).
 • [ ] Criar job `@Scheduled` para verificar diariamente os atrasos.
 • [ ] Implementar visão gerencial de jogos alugados, atrasados e aguardando retirada.

7. Camada de Apresentação e Notificação

 • [ ] Criar templates Thymeleaf com Bootstrap para usuários e gerentes.
 • [ ] Implementar catálogo responsivo com filtros.
 • [ ] Implementar telas de cadastro, login, reservas, aluguéis e devoluções.
 • [ ] Integrar Spring Boot Starter Mail com Mailtrap para desenvolvimento e testes.
 • [ ] Enviar notificações de confirmação, expiração de reserva e atraso/penalidade.
 • [ ] Criar dashboard e relatórios estatísticos do Gerente (RF10).

8. Testes e Qualidade

 • [ ] Criar testes unitários para as regras RN1 a RN7.
 • [ ] Criar testes de integração para repositories e serviços.
 • [ ] Criar testes dos endpoints REST e dos fluxos de autenticação.
 • [ ] Validar critérios de aceitação das histórias HU01 a HU12.
 • [ ] Executar `mvnw.cmd clean test -q` com Java 25 antes de cada entrega.

9. Infraestrutura e Documentação

 • [ ] Configurar conexão MySQL por variáveis de ambiente, sem credenciais no repositório.
 • [ ] Definir estratégia de inicialização do banco: `schema.sql` ou Flyway/Liquibase.
 • [ ] Configurar Docker para desenvolvimento e homologação.
 • [ ] Adicionar documentação interativa com springdoc/OpenAPI.
 • [ ] Atualizar a RFC conforme as decisões de implementação.

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
