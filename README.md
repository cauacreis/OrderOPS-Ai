# 🍔 OrderOps AI (MVP)

> **Orquestração Inteligente e em Tempo Real para Delivery B2B**

O **OrderOps AI** é um sistema de demonstração de arquitetura (PoC) projetado para resolver o gargalo operacional de restaurantes que lidam com múltiplas plataformas de delivery simultaneamente. 

Utilizando agentes de IA autônomos e um backend reativo, o sistema extrai, centraliza e gerencia pedidos em tempo real, sem a necessidade de intervenção humana constante.

---

## 🚀 O Problema Resolvido
Restaurantes perdem eficiência e dinheiro operando múltiplas telas (diferentes apps de delivery como iFood, Rappi, Uber Eats) e alocando entregadores manualmente. O OrderOps centraliza a operação e automatiza a triagem operacional através de agentes inteligentes de IA, coordenando a cozinha e a logística de entrega em tempo real.

---

## 🛠️ Natureza do Projeto (Stand-alone PoC)
Este projeto está em fase de ideação e foi construído com a filosofia **"Zero-Friction"** para avaliação técnica. 
Para facilitar testes e demonstrações em entrevistas, o sistema roda de forma 100% isolada (Stand-alone):
* **Sem dependência externa:** Não exige conexão com APIs reais do iFood/Rappi. O frontend simula a injeção de dados via `POST`.
* **Motor de IA Mockado:** As heurísticas de decisão (Triage e Dispatch) rodam via simulação interna no backend, sem exigir chaves de API pagas (LLM) neste estágio inicial.
* **Banco de Dados Nativo (In-Memory):** Utiliza persistência em memória para que o avaliador possa baixar, rodar e ver o sistema funcionando em tempo real instantaneamente, sem a necessidade de configurar Docker ou bancos de dados relacionais.

---

## 🧠 Arquitetura e Stack Tecnológico
Este projeto foca em alta concorrência, baixa latência e design patterns corporativos:

* **Backend:** Java 17+ e Spring Boot (Arquitetura RESTful, Services isolados).
* **Inteligência Artificial:** Orquestração de Agentes assíncronos (TriageAgent, KitchenAgent, DispatcherAgent, RouteAgent e SentimentAgent).
* **Comunicação Real-Time:** WebSockets para atualização instantânea do painel operacional.
* **Frontend:** Vite + Vanilla JS & CSS com uma estética **Neo-Brutalista** de alto contraste (Dark Mode) focada em usabilidade operacional e dashboard reativo.

---

## 🏗️ Estrutura do Projeto

```
orderops-ai/
├── backend/            # Servidor Spring Boot REST & WebSockets
│   ├── src/main/java/com/orderops/
│   │   ├── api/        # Inicialização do Spring Boot
│   │   ├── config/     # Registros de CORS e WebSockets
│   │   ├── controller/ # Endpoints REST (Ingestão de pedidos, velocidade, reset)
│   │   ├── model/      # Entidades (Order, Driver, AgentLog)
│   │   ├── service/    # Regras de Negócio e Simulação do Motor de Agentes
│   │   └── websocket/  # Gerenciador do Gateway WebSocket
│   └── pom.xml
├── frontend/           # Interface de Operação (Dashboard Neo-Brutalist)
│   ├── index.html      # Estrutura HTML do Painel
│   ├── src/
│   │   ├── main.js     # Conexão WebSocket e Loop do Radar GPS no Canvas
│   │   └── style.css   # Estilo Visual Neo-Brutalista Dark Mode
│   └── package.json
└── .gitignore          # Filtro de arquivos para versionamento
```

---

## ⚙️ Configuração e Execução

### Pré-requisitos
* **Java JDK 17** ou superior (o sistema foi testado e validado no Java 24).
* **Node.js** v18+ e **npm** v9+.

### 1. Inicializando o Backend
Navegue até a pasta `backend/` e execute o servidor empacotado:
```bash
cd backend
java -jar target/api-0.0.1-SNAPSHOT.jar
```
O backend iniciará na porta **8080** ([http://localhost:8080](http://localhost:8080)).
* *Nota:* Por padrão, o sistema rodará em modo simulação de alta fidelidade com heurísticas inteligentes. Se desejar usar a API real da Gemini para triagem de texto, crie um arquivo `.env` na raiz do projeto contendo sua chave de API: `GEMINI_API_KEY=sua_chave_aqui`.

### 2. Inicializando o Frontend
Navegue até a pasta `frontend/`, instale as dependências e rode o servidor de desenvolvimento:
```bash
cd frontend
npm install
npm run dev
```
O frontend iniciará na porta **5173** ([http://localhost:5173](http://localhost:5173)).

---

## 🧪 Como Testar a Experiência Operacional

1. Acesse o painel pelo navegador em [http://localhost:5173](http://localhost:5173).
2. Verifique o indicador `ONLINE` no cabeçalho sinalizando que o WebSocket se conectou com sucesso ao backend.
3. Clique em **`⚡ Ingest Random Order`** no menu superior:
   * Um pedido cru entrará na coluna **Ingesting & Triage**.
   * Veja os logs de pensamento da IA rolando em tempo real no console lateral (**Live Agent Thought Stream**).
   * O pedido avançará para a cozinha (**Kitchen Prep**), preparando a comida em tempo real.
   * O **DispatcherAgent** localizará o entregador disponível mais próximo (Carlos, Ana ou Marcos).
   * O status mudará para **In Transit**, e no painel de **Radar GPS (Canvas)** você verá a moto/bike saindo do HQ (centro) e navegando em tempo real até as coordenadas do cliente!
   * Por fim, o pedido é marcado como **Delivered** e os logs finais de pós-entrega são consolidados.
4. Experimente colar qualquer texto de chat de cliente no campo de texto inferior e clique em **`🚀 Dispatch to AI Triage`** para ver as heurísticas processarem seus inputs manuais.
5. Altere a velocidade da simulação (1.0x, 2.0x, 4.0x) ou limpe o histórico com o botão **`🗑️ Reset Demo`**.

---

## ⚠️ Limitações do Simulador (Known Issues)

O simulador de inteligência artificial (**Mock AI**) presente no backend foi desenvolvido utilizando heurísticas de expressões regulares (RegEx) rápidas e sem estado para parsing de texto não-estruturado. Esta abordagem gera as seguintes limitações técnicas:

1. **Falso Positivo na Extração de Itens (`order_items`):** O parser de itens tenta mapear padrões de quantidade e nome do produto utilizando uma RegEx genérica `(\\d+)\\s*(?:x|\\*|-)?\\s*([a-zA-Záéíóúâêôãõç\\s]{3,25})`. Isso faz com que partes do endereço ou instruções (ex: *"2300 bloco B ap"*, *"1000 - Recepção"*, *"450 apto"*) sejam erroneamente classificados como itens de pedido com quantidades gigantescas baseadas no número do endereço (ex: `2300` de quantidade de "bloco B ap" e `1000` de "Recepção").
2. **Inflação do Preço Total do Pedido:** Como consequência do falso positivo acima, o valor total do pedido (`totalPrice`) é calculado multiplicando essas quantidades absurdas por preços simulados aleatórios, resultando em totais irrealistas nas ordens afetadas (como `R$ 96.664,00`).

**Prontidão de Arquitetura:** A arquitetura do `AgentOrchestrator` está 100% preparada para solucionar essas anomalias. Há uma ramificação nativa no orquestrador pronta para utilizar a API real do **Google Gemini (LLM)** assim que a chave `GEMINI_API_KEY` for configurada no arquivo `.env` da raiz. Com a API da Gemini ativada, a estruturação do payload deixa de depender de RegEx estático e passa a rodar via modelos de linguagem de última geração, mapeando os campos com precisão cirúrgica.

---

## 🔍 Relatório de QA Autônomo

Realizamos uma varredura operacional e técnica no projeto OrderOps AI e identificamos as seguintes anomalias operacionais, de design e concorrência:

### 1. Concorrência no Canal do WebSocket (Thread Safety)
* **Impacto:** Alto (Riscos de concorrência e perdas de mensagens operacionais).
* **Descrição:** O método `broadcast` no `OrderWebSocketHandler.java` envia atualizações simultâneas para o mesmo canal de WebSocket usando threads assíncronas do `ExecutorService`. Como a chamada `session.sendMessage()` não é inerentemente thread-safe no Spring WebSocket (Tomcat), ocorria a exceção `java.lang.IllegalStateException: The remote endpoint was in state [TEXT_PARTIAL_WRITING]`.
* **Resolução Aplicada:** Implementado bloco `synchronized(session)` no envio da mensagem para garantir atomicidade de cada broadcast.

### 2. Tratamento Incorreto de Endereços sem Dois-Pontos (`:`)
* **Impacto:** Médio (Erros de dados operacionais).
* **Descrição:** A RegEx de mapeamento de endereço `(?i)(?:endereço|entrega|rua|av|avenida)\\s*:\\s*([^\\n]+)` assume obrigatoriamente a presença de dois-pontos. Em mensagens comuns como *"Entrega na Rua da Consolação, 2300..."* (que não contêm `:`), a regex falha e o sistema define o endereço como `"Retirada no Balcão"`, ocultando o endereço real do cliente e impactando o Radar GPS.

### 3. TypeError no Loop de Radar GPS
* **Impacto:** Alto (Quebra visual total do Canvas de Radar).
* **Descrição:** No arquivo `main.js`, o radar animado lia `order.id.replace(/\\D/g, '')`. Caso a ordem estivesse em processo de ingestão (`PENDING`) e o ID ainda fosse nulo/indefinido, ocorria um erro crítico de `TypeError: Cannot read properties of null`, o que congelava o loop `requestAnimationFrame` da renderização do mapa.
* **Resolução Aplicada:** Correção no frontend com fallback seguro `(order.id || '').replace(...)`.

### 4. Ordem Cronológica Invertida no Console Stream
* **Impacto:** Baixo/Médio (Usabilidade operacional).
* **Descrição:** Após alterarmos o backend para retornar logs ordenados por data decrescente (mais recentes primeiro), a inicialização do frontend renderizava os logs históricos de cabeça para baixo no console central. Além disso, as novas atualizações do WebSocket eram injetadas no final, gerando descontinuidade no console.
* **Resolução Aplicada:** Ajuste no `fetchInitialState` aplicando um `.reverse()` no array de logs do REST para manter toda a linha do tempo estritamente cronológica, do mais antigo para o mais recente com rolagem automática para o final.

---

## 🔒 Segurança e Boas Práticas
* Separação de responsabilidades e camadas isoladas de execução (Controller -> Service -> Model -> WebSockets).
* Prevenção de versionamento de dependências e arquivos de build no repositório Git através do `.gitignore` configurado.

---
*Desenvolvido com foco em Engenharia de Software Moderna e IA Orquestrada.*
