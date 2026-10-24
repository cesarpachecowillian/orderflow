# OrderFlow

Sistema de gestão de pedidos para e-commerce construído em **arquitetura de microsserviços Java**, com mensageria assíncrona via **Kafka**, autenticação **JWT centralizada no API Gateway**, isolamento de rede entre serviços internos e um frontend em **React + TypeScript**.

O objetivo do projeto é demonstrar, de ponta a ponta, decisões reais de arquitetura distribuída: separação de bancos por domínio, comunicação síncrona e assíncrona, consumidores idempotentes, controle de acesso por papéis e testes automatizados.

---

## Funcionalidades

- **Cadastro e login** de usuários com senha criptografada (BCrypt) e emissão de JWT.
- **Papéis `USER` e `ADMIN`**, além de um **super-admin** único que pode promover/rebaixar usuários (e cujo papel não pode ser alterado).
- **Pedidos** com ciclo de vida `PENDING → CONFIRMED` (apenas admin) ou `PENDING → CANCELLED` (dono do pedido ou admin), com auditoria de quem executou cada transição, método e motivo do cancelamento.
- **Notificações por e-mail** disparadas por eventos Kafka (`ORDER_CREATED`, `ORDER_CONFIRMED`, `ORDER_CANCELLED`), com histórico persistido no MongoDB.
- **Painel administrativo**: listagem de clientes, resumo de pedidos por usuário e relatório de atividade dos administradores.
- **Documentação Swagger agregada** de todos os serviços no Gateway.

## Arquitetura

```
                 ┌──────────────┐
  Navegador ───► │  web (3000)  │  React + Vite + Tailwind, servido por nginx
                 └──────┬───────┘
                        │ /api/*
                        ▼
                 ┌──────────────────┐
                 │ API Gateway 8080 │  valida JWT, injeta headers de identidade
                 └──┬────────────┬──┘
                    │            │
          ┌─────────▼───┐   ┌────▼──────────┐
          │ Auth  8081  │   │ Order  8082   │
          └─────┬───────┘   └──┬─────────┬──┘
                │              │         │ publica
             MySQL          MySQL        ▼
            auth_db        order_db   Kafka (order-events)
                                         │ consome
                                 ┌───────▼──────────────┐
                                 │ Notification  8083   │
                                 └───────┬──────────────┘
                                         ▼
                                      MongoDB
                                  notifications_db
```

| Serviço | Responsabilidade | Persistência |
|---|---|---|
| **api-gateway** | Ponto único de entrada, roteamento declarativo, validação de JWT, agregação do Swagger | — |
| **auth-service** | Registro, login, emissão de tokens, gestão de usuários e papéis | MySQL |
| **order-service** | CRUD e transições de pedidos, relatórios, publicação de eventos | MySQL |
| **notification-service** | Consumo de eventos e envio de e-mails (fake em dev, SMTP em `prod`) | MongoDB |
| **web** | Interface do cliente e do administrador | — |

### Fluxo de autenticação

1. O cliente faz login em `/auth/login` e recebe um JWT contendo `name`, `role` e `superAdmin`.
2. Em toda requisição protegida, o **Gateway** valida o token e, em caso de sucesso, remove quaisquer headers de identidade enviados pelo cliente e reinjeta `X-User-Name`, `X-User-Email`, `X-User-Role` e `X-User-Super-Admin`.
3. Os serviços internos confiam nesses headers para autorização. Por isso eles **não publicam portas no host** (`expose` em vez de `ports`) — só são alcançáveis dentro da rede Docker, impedindo que alguém forje os headers.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17, TypeScript |
| Backend | Spring Boot 3, Spring Cloud Gateway (WebFlux), Spring Security 6 |
| Autenticação | JWT (jjwt 0.12) |
| Persistência | Spring Data JPA + MySQL 8 · Spring Data MongoDB + MongoDB 7 |
| Mensageria | Apache Kafka (Confluent 7.6) + Zookeeper |
| Frontend | React, Vite, Tailwind CSS, Axios |
| Infra | Docker, Docker Compose, nginx |
| Testes | JUnit 5, Mockito, H2, WireMock, EmbeddedKafka |
| Documentação | Springdoc OpenAPI (Swagger UI) |

## Como rodar localmente

Há duas formas: **tudo em Docker** (mais simples, recomendada para avaliar o projeto) ou **infraestrutura em Docker + serviços rodando na sua máquina** (para desenvolver e depurar).

### Pré-requisitos

| Modo | Necessário |
|---|---|
| Tudo em Docker | Docker 24+ e Docker Compose v2 (Docker Desktop no Windows/macOS) |
| Desenvolvimento | Além do Docker: Java 17+, Maven 3.9+ e Node.js 18+ |

Deixe livres as portas `3000`, `8080`, `8090`, `3307`, `3308`, `27017`, `9092` e `2181`.

### Opção 1 — Tudo em Docker

**1. Clone o repositório**

```bash
git clone https://github.com/cesarpachecowillian/orderflow.git
cd orderflow
```

**2. Crie o arquivo de variáveis de ambiente**

```bash
cp .env.example .env          # no PowerShell: Copy-Item .env.example .env
```

O `.env.example` já vem com valores de demonstração prontos para uso local:

| Variável | Uso |
|---|---|
| `MYSQL_ROOT_PASSWORD`, `MYSQL_USER`, `MYSQL_PASSWORD` | Credenciais dos dois MySQL (auth e order) |
| `MONGO_INITDB_ROOT_USERNAME`, `MONGO_INITDB_ROOT_PASSWORD` | Credenciais do MongoDB |
| `JWT_SECRET` | Chave HMAC do JWT (mín. 32 caracteres), compartilhada entre auth-service e gateway |
| `JWT_EXPIRATION` | Validade do token em milissegundos (padrão: 24h) |

**3. Suba os containers**

```bash
docker compose up -d --build
```

A primeira execução baixa as imagens e compila os serviços, o que leva alguns minutos.

**4. Aguarde tudo ficar saudável**

```bash
docker compose ps
```

Os bancos e o Kafka devem aparecer como `healthy` (~30–60s). Para acompanhar um serviço: `docker compose logs -f order-service`.

**5. Acesse** http://localhost:3000 e entre com o administrador padrão:

- **E-mail:** `admin@orderflow.com`
- **Senha:** `admin123`

Ou crie uma conta comum pela tela de cadastro.

**6. (Opcional) Popule com dados de exemplo**

```bash
docker run --rm --network orderflow-network --entrypoint sh \
  -v "$PWD/scripts:/scripts" curlimages/curl:latest /scripts/seed-data.sh
```

No PowerShell, escreva o comando numa linha só (ou use crase `` ` `` no lugar da barra invertida). O script cria 30 usuários e dezenas de pedidos com distribuição realista de status (confirmados, cancelados pelo cliente, cancelados pelo admin e pendentes).

**7. Para parar**

```bash
docker compose down        # para os containers e mantém os dados
docker compose down -v     # para e apaga os dados (os bancos são recriados pelos scripts de init)
```

### Opção 2 — Infraestrutura em Docker, serviços na máquina

Útil para rodar um serviço pela IDE com debug. O perfil Spring `default` já aponta para `localhost` e usa os mesmos valores de demonstração do `.env.example`.

**1. Suba só a infraestrutura**

```bash
cp .env.example .env
docker compose up -d mysql-auth mysql-order mongodb zookeeper kafka kafka-ui
```

**2. Rode cada serviço Java em um terminal**

```bash
cd auth-service         && mvn spring-boot:run   # porta 8081
cd order-service        && mvn spring-boot:run   # porta 8082
cd notification-service && mvn spring-boot:run   # porta 8083
cd api-gateway          && mvn spring-boot:run   # porta 8080
```

Ou execute a classe `*Application` de cada módulo pela IDE.

**3. Rode o frontend**

```bash
cd web
npm install
npm run dev              # http://localhost:3000
```

O Vite faz proxy de `/api/*` para o gateway em `localhost:8080`, então o fluxo é o mesmo da versão em Docker.

> Nesse modo os serviços internos ficam acessíveis em `localhost:808x`, sem o isolamento de rede do Docker. Use apenas em desenvolvimento.

### Problemas comuns

| Sintoma | Causa provável |
|---|---|
| `port is already allocated` | Outra aplicação usando a porta (ex.: um MySQL local na 3307/3308). Pare-a ou altere o mapeamento no `docker-compose.yml`. |
| Serviço reiniciando em loop logo após o `up` | Dependência ainda subindo; aguarde os healthchecks. Confira com `docker compose logs <serviço>`. |
| Login retorna 401 em todas as rotas | `JWT_SECRET` diferente entre auth-service e gateway. |
| Mudou o `.sql` de init e nada aconteceu | Os scripts só rodam com volume vazio: `docker compose down -v` e suba de novo. |

### Endereços

| Recurso | URL |
|---|---|
| **Web UI** | http://localhost:3000 |
| API Gateway | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Kafka UI | http://localhost:8090 |
| MySQL auth_db / order_db | localhost:3307 / localhost:3308 |
| MongoDB | localhost:27017 |

## API (resumo)

| Método | Rota | Acesso |
|---|---|---|
| `POST` | `/auth/register` | Público |
| `POST` | `/auth/login` | Público |
| `GET` | `/auth/users` · `/auth/users/{id}` | Admin |
| `PATCH` | `/auth/users/{id}/role` | Super-admin |
| `POST` | `/orders` | Autenticado |
| `GET` | `/orders` · `/orders/{id}` | Dono ou admin |
| `PATCH` | `/orders/{id}/cancel` | Dono ou admin |
| `PATCH` | `/orders/{id}/confirm` | Admin |
| `GET` | `/orders/summary` · `/orders/summary/by-user` · `/orders/admin-activity` | Admin |

A documentação completa, com schemas de request/response, está no Swagger.

## Testes

Cada serviço é um módulo Maven independente; rode os testes dentro do diretório dele:

```bash
cd order-service && mvn clean verify                           # build + todos os testes
cd order-service && mvn test -Dtest=OrderServiceTest           # uma classe
```

Os testes usam o perfil `test` (H2 em memória + EmbeddedKafka + WireMock) e **não precisam de Docker** nem de nenhuma infraestrutura rodando.

## Estrutura do repositório

```
orderflow/
├── docker-compose.yml
├── .env.example
├── infrastructure/
│   ├── mysql/{auth,order}-init.sql
│   └── mongo/init-mongo.js
├── scripts/seed-data.sh
├── api-gateway/
├── auth-service/
├── order-service/
├── notification-service/
└── web/
```

## Decisões arquiteturais

| Decisão | Justificativa |
|---|---|
| Um banco por serviço | Isolamento de schema e autonomia entre domínios |
| JWT validado só no Gateway | Lógica de segurança centralizada; serviços stateless |
| `expose` em vez de `ports` | Serviços internos inacessíveis de fora da rede Docker |
| Kafka para notificações | O fluxo de pedido não espera o envio de e-mail |
| Consumer idempotente | Kafka entrega *at-least-once*; evita e-mails duplicados |
| MongoDB para logs de notificação | Carga de escrita alta, sem relações, schema flexível |
| `BigDecimal` para valores monetários | Evita imprecisão de `float`/`double` |
| `@Enumerated(STRING)` | Reordenar enums não corrompe dados persistidos |
| Schema via scripts SQL + `ddl-auto: validate` | Schema versionado e explícito, sem alterações automáticas |
| Perfis Spring (`default`, `docker`, `test`, `prod`) | Mesmo código rodando local, em container, em teste e em produção |

## Próximas implementações possíveis

Este é um projeto de demonstração e algumas simplificações foram feitas de propósito. Abaixo estão as limitações que existem hoje no código, por que elas importam e como poderiam ser resolvidas.

### Consistência e mensageria

**1. Publicação de eventos sem Transactional Outbox**
Hoje o `OrderService` grava o pedido e chama `kafkaTemplate.send(...)` dentro da mesma transação, mas banco e Kafka não compartilham transação. Se o Kafka estiver fora no momento, o pedido é salvo e o evento se perde, e o cliente nunca recebe o e-mail. No caminho inverso, o evento pode sair antes do commit e o commit falhar depois, gerando uma notificação de um pedido que não existe.
*Por que mudar:* gravar o evento numa tabela `outbox` na mesma transação do pedido, com um publicador separado (ou Debezium/CDC) lendo e enviando ao Kafka, garante que todo pedido salvo tenha seu evento entregue *at-least-once*.

**2. Falhas de notificação não são reprocessadas**
O `NotificationService` captura qualquer exceção do envio de e-mail e só grava um log com status `FAILED`. O Kafka considera a mensagem processada e ninguém tenta enviar de novo.
*Por que mudar:* configurar retry com backoff no consumer (`DefaultErrorHandler`) e uma *Dead Letter Topic* para as mensagens que falharem várias vezes. Assim falhas temporárias (SMTP fora do ar) se resolvem sozinhas, e as permanentes ficam visíveis para análise.

**3. Idempotência com condição de corrida**
A checagem "já existe notificação `SENT` para este pedido e tipo?" é feita antes do envio, sem trava. Duas entregas simultâneas da mesma mensagem (por exemplo, durante um rebalance do consumer group) passam pela checagem e enviam o e-mail duas vezes.
*Por que mudar:* um índice único em `(orderId, type)` no MongoDB, com o registro "reservado" antes do envio, torna a idempotência garantida pelo banco e não pela ordem de execução.

**4. Contratos de evento duplicados entre serviços**
`order-service` e `notification-service` têm cada um a sua cópia de `OrderEventDTO`. Se alguém renomear um campo em um lado só, a desserialização falha ou deixa campos `null` sem nenhum erro aparente.
*Por que mudar:* um Schema Registry com Avro (ou um módulo de contrato compartilhado) versiona o formato do evento e rejeita mudanças incompatíveis antes de chegarem em produção.

**5. Concorrência nas transições de status**
A entidade `Order` não tem `@Version`. Dois administradores confirmando e cancelando o mesmo pedido ao mesmo tempo podem ler `PENDING`, passar nas validações, e o último a gravar vence, com dois eventos contraditórios publicados.
*Por que mudar:* *optimistic locking* com `@Version` faz a segunda operação falhar com conflito (HTTP 409), mantendo a máquina de estados consistente.

### Segurança

**6. Serviços internos confiam cegamente nos headers do Gateway**
A autorização nos serviços se baseia em `X-User-Role`, `X-User-Email` etc. A proteção é apenas de rede (`expose` em vez de `ports`). Qualquer processo que consiga falar com `order-service` dentro da rede pode forjar `X-User-Role: ADMIN`.
*Por que mudar:* numa abordagem *zero trust*, cada serviço validaria o próprio JWT (ou um token interno assinado pelo Gateway) ou usaria mTLS entre serviços, para que comprometer um container não dê acesso a todos os outros.

**7. Verificação de dono no cancelamento usa o nome, não o e-mail**
Em `OrderService.cancel`, o dono é verificado comparando `customerName` com o nome do usuário logado. Nomes não são únicos: dois clientes chamados "Maria Silva" conseguem cancelar os pedidos um do outro. A consulta de detalhe (`findById`) já usa o e-mail corretamente.
*Por que mudar:* comparar pelo identificador único (e-mail ou, idealmente, o ID do usuário no JWT) fecha essa brecha de autorização e deixa a regra igual em todos os endpoints.

**8. Preço do item definido pelo cliente**
O `CreateOrderItemRequest` recebe `unitPrice` direto do frontend, e o total do pedido é calculado a partir dele. Um usuário pode enviar qualquer preço via API.
*Por que mudar:* um catálogo de produtos (novo serviço ou tabela) sendo a fonte do preço, com o pedido enviando só `productId` e `quantity`, é o comportamento esperado de um e-commerce real.

**9. Token JWT no `localStorage` e sem revogação**
O frontend guarda o token no `localStorage`, que fica acessível a qualquer script da página em caso de XSS. O token vale 24h, não existe refresh token, e o logout ou uma mudança de papel não invalida tokens já emitidos.
*Por que mudar:* access token curto, refresh token em cookie `HttpOnly` + `Secure` + `SameSite` e uma lista de revogação (ex.: Redis) limitam o estrago de um token vazado e fazem a mudança de papel valer na hora.

**10. CORS totalmente aberto**
O Gateway aceita qualquer origem (`allowedOriginPatterns("*")`) com `allowCredentials(true)`.
*Por que mudar:* restringir às origens conhecidas do frontend evita que outros sites façam requisições autenticadas em nome do usuário.

**11. Sem proteção contra força bruta no login**
`/auth/login` não tem limite de tentativas nem bloqueio temporário.
*Por que mudar:* rate limiting no Gateway (o Spring Cloud Gateway já tem `RequestRateLimiter` com Redis) e bloqueio progressivo por conta dificultam ataques de dicionário.

**12. Segredos com valores padrão no código**
Os `application.yml` têm *fallbacks* para `JWT_SECRET` e senhas de banco, e o seed cria `admin@orderflow.com` / `admin123`. Isso facilita rodar a demo, mas se uma variável de ambiente for esquecida em produção, o sistema sobe silenciosamente com segredos públicos.
*Por que mudar:* em produção a aplicação deveria falhar ao iniciar sem os segredos, que viriam de um cofre (AWS Secrets Manager, Vault), e o admin inicial deveria ser criado com senha gerada e troca obrigatória no primeiro acesso.

### Operação e infraestrutura

**13. Schema sem migrations versionadas**
O schema é criado por scripts em `docker-entrypoint-initdb.d`, que só rodam com o volume vazio. Qualquer mudança exige `docker compose down -v`, o que apaga todos os dados.
*Por que mudar:* Flyway ou Liquibase aplicam alterações incrementais e versionadas no startup, sem perder dados, e mantêm o histórico do schema junto com o código.

**14. Pouca observabilidade**
Não há tracing distribuído nem métricas centralizadas. Seguir um pedido do Gateway até o e-mail exige olhar os logs de quatro containers separadamente.
*Por que mudar:* Micrometer + OpenTelemetry (traces com o ID propagado inclusive pelo Kafka), Prometheus/Grafana para métricas e logs estruturados com *correlation id* tornam a depuração de um sistema distribuído viável.

**15. Sem resiliência entre Gateway e serviços**
Se `auth-service` ou `order-service` ficar lento, o Gateway espera indefinidamente e acumula conexões.
*Por que mudar:* timeouts, *circuit breaker* e *fallback* com Resilience4j impedem que a lentidão de um serviço derrube o sistema inteiro.

**16. Infraestrutura de desenvolvimento**
O Kafka depende do Zookeeper (já descontinuado nas versões novas, que usam KRaft), o `kafka-ui` usa a tag `latest` (builds não reproduzíveis) e tudo roda em um único host com Docker Compose.
*Por que mudar:* migrar para KRaft, fixar versões de imagem e ter manifests Kubernetes (ou Helm) aproximam o ambiente do que seria usado em produção, com escala horizontal e *self-healing*.

**17. Sem pipeline de integração contínua**
Os testes existem, mas rodam só manualmente.
*Por que mudar:* um pipeline (GitHub Actions, por exemplo) rodando build, testes e análise estática (SonarQube) a cada push evita regressões e documenta a saúde do projeto.

## Autor

**César Willian Pacheco** — [github.com/cesarpachecowillian](https://github.com/cesarpachecowillian)
