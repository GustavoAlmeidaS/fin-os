# Personal Finance ERP & Analytics Foundation

Bem-vindo ao **Personal Finance ERP**, um sistema de controladoria financeira pessoal de nível corporativo. Mais do que um simples rastreador de despesas, este projeto foi desenhado do zero com foco em **Governança de Dados** e **Business Intelligence**.

A arquitetura do sistema abstrai a complexidade transacional e entrega uma fundação analítica pronta para consumo por ferramentas de Machine Learning (Python/Pandas) e BI (Power BI, Metabase).

## 🌟 Diferenciais da Arquitetura

Ao invés de apenas uma API CRUD, o sistema atua como um verdadeiro ERP:

- **Data Foundation para BI**: Views SQL materializadas (`v_fact_transactions`, `v_monthly_cashflow`) desenhadas com base no modelo *Star Schema* (Tabelas Fato). O trabalho pesado de cruzamento de dados já é feito no banco.
- **Machine Learning Ready**: A view de ML entrega variáveis booleanas (`is_weekend`, `is_month_start`) e valores sinalizados (`signed_amount`) no próprio grain do banco, acelerando a etapa de feature engineering para regressões preditivas.
- **Extensibilidade via JSONB**: Colunas de `metadata` em formato JSONB garantem que scripts externos (como classificadores de IA) possam enriquecer as transações com predições de IA sem necessidade de quebrar o contrato da tabela (ex: inserir score de confiança e tags).

## 💼 Módulos do Sistema

### 1. Livro Razão (Ledger Core)
- Registro de dupla-entrada implícito: todo fluxo tem uma Conta Fonte e pode ser alocado em Categorias e Contrapartes.
- Suporte a `Soft Delete` gerenciado automaticamente via anotações `@SQLRestriction` do Hibernate 6.

### 2. Gestão de Dívidas e Empréstimos
- Tratamento de passivos de longo prazo. Empréstimos parcelados ou abertos são computados separadamente das despesas rotineiras para não poluir o fluxo de caixa mensal.
- Cálculo automático de **Patrimônio Líquido (Net Worth)** considerando saldo de ativos reais menos o saldo devedor.

### 3. Planejamento (Metas e Orçamentos)
- **Orçamentos**: Tetos mensais rígidos de gastos por categoria.
- **Metas**: Objetivos de acúmulo de patrimônio, visualmente representados por progresso.
- **Previsibilidade**: O sistema clona automaticamente faturas recorrentes (mensais/anuais) assim que as atuais são pagas, injetando Lançamentos Pendentes no fluxo de caixa projetado de forma autônoma.

### 4. Processamento em Lote
- Importação via Parsing de CSV bancário com algoritmos de hash criptográfico (`SHA-256`) na linha do CSV.
- Isso previne categoricamente a inserção duplicada do mesmo lançamento durante conciliações.

## 🛠️ Stack Tecnológica

- **Backend**: Java 21, Spring Boot 3.5.0
- **Persistência**: Spring Data JPA / Hibernate 6, Flyway Migrations
- **Banco de Dados Oficial**: PostgreSQL
- **Segurança**: Spring Security + Autenticação Stateless (JWT)
- **Frontend**: Vanilla HTML/JS minimalista (foco em performance) com design Dark Mode "Premium Sóbrio".

## 🚀 Como Executar

### Pré-requisitos
- Java 21+ instalado
- Instância do PostgreSQL rodando (ou manter o driver H2 configurado em memória para dev rápido).

### Setup

```bash
# 1. Clone o repositório
git clone <SEU_NOVO_REPOSITORIO>
cd finos

# 2. Inicie a aplicação
./mvnw.cmd spring-boot:run
```

Acesse o sistema localmente via `http://localhost:8080`.

## 🗄️ Estrutura de Migrations (Flyway)

A integridade do banco é mantida estritamente via Flyway. A modelagem garante o rastreio histórico de deleções lógicas em entidades dimensão (Account, Category, etc).
- `V1` a `V5`: Criação do Core, Analytics Views, Empréstimos e Planejamento Preditivo.

---
Desenvolvido como uma prova de conceito para construção de Sistemas Transacionais que priorizam qualidade de consumo de dados analíticos.
