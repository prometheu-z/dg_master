# DG Master - Sistema de Aluguel de Jogos de Tabuleiro

Sistema web completo para aluguel de jogos de tabuleiro com retirada física na loja.

## 🚀 Tecnologias

- **Backend**: Java 25 + Spring Boot 4.1.1
- **Banco de Dados**: MySQL 8
- **Frontend**: Thymeleaf (server-side rendering)
- **Build**: Maven
- **Segurança**: Spring Security (autenticação por sessão)
- **Documentação**: SpringDoc OpenAPI (Swagger UI)
- **Containerização**: Docker + Docker Compose

## 📋 Pré-requisitos

- Java 25
- Maven 3.9+
- MySQL 8 (ou usar Docker)
- Docker e Docker Compose (opcional, para containerização)

## 🔧 Configuração

### Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto (baseado no `.env.example`):

```bash
cp .env.example .env
```

Edite o arquivo `.env` com suas credenciais:

```env
DB_URL=jdbc:mysql://localhost:3306/db_master?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=sua_senha_aqui
```

## 🐳 Docker (Recomendado)

### Subir o ambiente completo (MySQL + Aplicação):

```bash
docker-compose up -d
```

### Apenas o banco de dados MySQL:

```bash
docker-compose up -d mysql
```

### Parar o ambiente:

```bash
docker-compose down
```

### Parar e remover volumes (limpar tudo):

```bash
docker-compose down -v
```

## 💻 Execução Local

### 1. Subir o MySQL (se não estiver usando Docker)

```bash
# Usando Docker
docker run --name dgmaster-mysql -e MYSQL_ROOT_PASSWORD=sua_senha -e MYSQL_DATABASE=db_master -p 3306:3306 -d mysql:8.0
```

### 2. Compilar e executar

```bash
# Windows
./mvnw.cmd clean spring-boot:run

# Linux/Mac
./mvnw clean spring-boot:run
```

### 3. Acessar a aplicação

- **Aplicação**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Cadastro Cliente**: http://localhost:8080/cadastro
- **Cadastro Gerente (Admin)**: http://localhost:8080/admin
- **Catálogo**: http://localhost:8080/catalogo
- **Estatísticas (Gerente)**: http://localhost:8080/gerente/estatisticas

## 📚 Documentação da API

A documentação interativa da API está disponível através do Swagger UI:

- **URL**: http://localhost:8080/swagger-ui.html
- **Autenticação**: Use Basic Auth ou sessão (cookie JSESSIONID)

### Endpoints Principais

#### Autenticação
- `POST /api/auth/cadastro` - Cadastro de novo cliente
- `POST /api/auth/cadastro-gerente` - Cadastro de novo gerente (admin)
- `POST /api/auth/login` - Login

#### Catálogo
- `GET /api/jogos` - Listar jogos (com filtros)
- `GET /api/jogos/{codigo}` - Detalhes do jogo
- `POST /api/jogos` - Adicionar jogo (Gerente)
- `PUT /api/jogos/{codigo}` - Atualizar jogo (Gerente)
- `DELETE /api/jogos/{codigo}` - Remover jogo (Gerente)

#### Aluguéis
- `POST /api/alugueis` - Criar reserva
- `DELETE /api/alugueis/{id}` - Cancelar reserva
- `PUT /api/alugueis/{id}/retirar` - Retirar jogos
- `PUT /api/alugueis/{id}/renovar` - Renovar aluguel
- `PUT /api/alugueis/{id}/devolver` - Devolver jogos (Gerente)

#### Estatísticas (Gerente)
- `GET /api/estatisticas` - Relatórios estatísticos completos

#### Clientes
- `GET /api/clientes/{cpf}` - Dados do cliente
- `GET /api/clientes/{cpf}/alugueis` - Histórico de aluguéis
- `POST /api/clientes/{cpf}/dependentes` - Adicionar dependente
- `GET /api/clientes/{cpf}/dependentes` - Listar dependentes

## 🧪 Testes

### Executar todos os testes

```bash
# Windows
./mvnw.cmd clean test

# Linux/Mac
./mvnw clean test
```

### Testes específicos

```bash
# Testes unitários de serviços
./mvnw test -Dtest=*ServiceTest

# Testes de integração
./mvnw test -Dtest=*IntegrationTest
```

## 📊 Funcionalidades Implementadas

### ✅ Core
- [x] Autenticação e autorização (Spring Security)
- [x] Cadastro de clientes com validação de idade (18+)
- [x] Gestão de dependentes
- [x] Catálogo de jogos com filtros
- [x] Sistema de reservas (limite de 4 jogos)
- [x] Cálculo de descontos progressivos
- [x] Contrato digital
- [x] Sistema de penalidades por atraso
- [x] Jobs agendados (expiração de reservas, verificação de atrasos)
- [x] Renovação de aluguéis

### ✅ Melhorias Recentes
- [x] Credenciais do banco em variáveis de ambiente
- [x] Relatórios estatísticos para gerente (RF10)
- [x] Testes unitários para services
- [x] Configuração Docker
- [x] Documentação OpenAPI/Swagger
- [x] Dashboard de estatísticas completas
- [x] Página de detalhes de jogos específicos
- [x] Histórico completo de aluguéis (ativos e finalizados)
- [x] Filtros avançados para gerente (busca por cliente, status)
- [x] Edição de perfil do cliente
- [x] Remoção de dependentes

## 🔒 Segurança

- Senhas criptografadas com BCrypt
- Proteção CSRF em operações de escrita
- Rotação de ID de sessão no login
- Autorização por perfil (CLIENTE/GERENTE)
- Variáveis de ambiente para credenciais

## 📝 Regras de Negócio

- **RN1**: Multa de R$ 5,00 por jogo/dia de atraso (limitada ao valor de reposição)
- **RN2**: Limite de 4 jogos ativos por cliente
- **RN3**: Expiração de reserva em 24h (crédito em carteira)
- **RN4**: Contrato digital com termos de responsabilidade
- **RN6**: Desconto progressivo (10% em 2 jogos, 20% em 3, 25% em 4+)
- **RN7**: Elegibilidade (cliente deve ter 18+ anos)

## 🏗 Estrutura do Projeto

```
dgmaster/
├── src/
│   ├── main/
│   │   ├── java/projeto/bdd2/dgmaster/
│   │   │   ├── aluguel/api/        # Controllers de aluguel
│   │   │   ├── auth/               # Autenticação
│   │   │   ├── catalogo/api/       # Controllers de catálogo
│   │   │   ├── cliente/api/        # Controllers de cliente
│   │   │   ├── config/             # Configurações (OpenAPI)
│   │   │   ├── entity/             # Entidades JPA
│   │   │   ├── repository/         # Repositórios Spring Data
│   │   │   ├── scheduler/          # Jobs agendados
│   │   │   ├── security/           # Configuração de segurança
│   │   │   ├── service/            # Regras de negócio
│   │   │   └── web/                # Controllers MVC
│   │   └── resources/
│   │       ├── templates/          # Templates Thymeleaf
│   │       ├── static/             # CSS/JS
│   │       └── application.yaml    # Configuração
│   └── test/                       # Testes
├── Dockerfile                      # Configuração Docker
├── docker-compose.yml              # Orquestração Docker
├── pom.xml                         # Dependências Maven
└── .env.example                    # Exemplo de variáveis de ambiente
```

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença Apache 2.0.
