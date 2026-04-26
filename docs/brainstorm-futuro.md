# Brainstorm: Evoluções Futuras do FinOS

Este documento compila ideias de funcionalidades e módulos para a evolução contínua do sistema, transformando-o de um registro financeiro básico para um ERP Pessoal e plataforma analítica de alto nível.

## 1. Módulo de Orçamentos e Metas (Planejamento)
- **Orçamentos (Budgets):** Definição de limites (tetos) de gastos por categoria mensalmente. O sistema compara o valor planejado vs. o valor executado, disparando alertas se o limite for atingido.
- **Metas (Goals):** Criação de "caixinhas" ou objetivos financeiros (ex: "Viagem fim do ano"). O usuário pode alocar parte do seu patrimônio ou fluxo de caixa diretamente nessa meta, acompanhando uma barra de progresso.

## 2. Contas a Pagar / Receber (Previsibilidade)
- **Lançamentos Futuros e Recorrentes:** Registro de despesas que vão vencer (ex: boleto pro dia 10) ou assinaturas mensais fixas (Netflix).
- **Projeção de Fluxo de Caixa:** Com os lançamentos futuros no sistema, a view analítica (`v_monthly_cashflow`) poderia ser expandida para projetar o saldo bancário para daqui a 30, 60 ou 90 dias, permitindo prever furos no caixa antes que aconteçam.

## 3. Conciliação Bancária Avançada
- Substituir a importação cega de CSV por uma **Tela de Conciliação**.
- A interface dividiria os dados do banco e os dados do Ledger, tentando fazer um "Match" automático por valor e data.
- O que não encontrar match pode ser inserido no Ledger com um clique. Isso extingue o risco de duplicidade de transações.

## 4. Módulo de Investimentos (Marcação a Mercado)
- Sair do modelo atual de "saldo fixo" para contas de investimento.
- Criação de cadastro de **Ativos/Tickets** (Ações, Cripto, Tesouro Direto).
- O usuário registraria a quantidade de ativos comprados, e o sistema buscaria cotações atuais para calcular o valor real flutuante do patrimônio.

## 5. Categorização por Inteligência Artificial (Machine Learning Aplicado)
- Aproveitar a fundação de dados do sistema para aplicar Inteligência Artificial no back-end.
- Durante a importação de CSVs ou cadastro manual rápido, em vez de exigir que o usuário escolha a categoria, o sistema utilizaria algoritmos de classificação (ex: Naive Bayes) baseados no histórico do próprio usuário, ou utilizaria integrações via API (LLMs) para ler a descrição da compra e classificar automaticamente a Categoria e as Tags adequadas na coluna `metadata`.

## 6. Suporte a Múltiplas Moedas (Multi-currency)
- Para quem recebe do exterior ou possui contas fora do país.
- Atualização em tempo real da taxa de câmbio para que o Net Worth (Patrimônio Líquido) seja consolidado e exibido em uma única moeda base (BRL), mas sem perder o lastro operacional da moeda estrangeira na tabela de `transactions`.
