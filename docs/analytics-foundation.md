# Analytics Foundation

Este documento descreve a camada analítica do **FinOS**, projetada para permitir que os dados transacionais e históricos sejam facilmente consumidos por ferramentas de BI (Power BI, Metabase) ou scripts de Data Science (Python, Pandas).

## Objetivo da Camada Analítica
O objetivo principal é abstrair a complexidade de joins e cálculos do banco de dados relacional OLTP, expondo **Views** e estruturas de dados otimizadas (Star Schema / Tabelas Fato).

## Entidades e Views

### `v_fact_transactions`
**Grain:** Uma linha por transação financeira (`Transaction`).
- **O que contém:** Todas as transações da base desnormalizadas, contendo o ID, o valor, os joins pré-computados (Nome da conta, Nome da categoria, Nome da contraparte).

### `v_monthly_cashflow`
**Grain:** Uma linha por Usuário por Mês-Ano e Status (`user_id`, `year_month`, `transaction_status`).
- **O que contém:** O total agregado de receitas (`total_income`), despesas (`total_expense`) e o fluxo de caixa líquido (`net_cashflow`).
- **Lendo Histórico vs Futuro:** A coluna `transaction_status` divide o fluxo de caixa em duas categorias fundamentais:
  - **Histórico (`POSTED`):** Valores reais que já aconteceram e foram executados (pagos/recebidos).
  - **Futuro/Projetado (`PENDING`):** Valores agendados/recorrentes que ainda vão acontecer (Previsibilidade de 30/60/90 dias).
  - **Uso no BI:** Ao plotar um gráfico de barras ao longo dos meses, use a coluna `transaction_status` como Legenda/Cor para distinguir visualmente o dinheiro consolidado do dinheiro projetado na mesma visualização.

### `v_fact_transactions_ml` (Machine Learning Ready)
**Grain:** Uma linha por transação financeira (mesmo grain da `v_fact_transactions`).
- **O que contém:** Tabela baseada em `v_fact_transactions`, mas enriquecida com features nativas calculadas no banco de dados para evitar reprocessamento no Python:
  - `signed_amount`: Valor com sinal (+ para Receitas, - para Despesas). Fundamental para regressões de fluxo de caixa.
  - `is_weekend`: Flag (TRUE/FALSE) indicando se a transação ocorreu sábado ou domingo.
  - `is_month_start`: Flag (TRUE/FALSE) indicando início de mês (dias 1 a 5).
  - `is_month_end`: Flag (TRUE/FALSE) indicando fim de mês (dias 25+).
- **Extensibilidade:** Esta view é considerada a fundação principal para Modelos Preditivos. Mais flags temporais ou de categoria podem ser embutidas aqui sem quebrar o front-end.

## Integração de Empréstimos (Loans)

O sistema possui um módulo dedicado a Empréstimos/Dívidas (`loans`) e suas movimentações (`loan_movements`).

### Como Dívidas se relacionam com o Ledger
- As entidades `loans` vivem de forma autônoma para garantir rastreabilidade.
- Os pagamentos de parcelas de empréstimos (ou tomada de empréstimo) são salvos em `loan_movements`.
- O contrato estipula a coluna `loan_movements.transaction_id`, que é uma chave estrangeira opcional apontando para a transação principal no `transactions`.
- Isso garante que, se houver um pagamento de empréstimo (Expense), ele aparece na `v_fact_transactions_ml`, e você pode fazer join com `loan_movements` usando o `transaction_id` para entender o impacto da dívida e do montante na saúde do Ledger.

### Cálculo no Dashboard
O patrimônio líquido (Net Worth) é calculado no backend através do método `ReportingService.getDashboardSummary()`:
1. `totalAssets` = Soma dos saldos de todas as contas ATIVAS.
2. `totalDebt` = Soma do `current_balance` de todos os empréstimos ATIVOS.
3. `netWorth` = `totalAssets` - `totalDebt`.

## Convenção para `transactions.metadata`
A coluna `metadata` na tabela de transações é do tipo `jsonb` (ou text/json).

**Padrão sugerido para Data Science:**
```json
{
  "tags": ["viagem_ferias", "imposto_renda"],
  "is_anomaly": true,
  "confidence_score": 0.98,
  "predicted_category": "Alimentação",
  "import_source_bank": "Nubank"
}
```
Scripts Python externos podem ler e gravar nessa coluna livremente para enriquecer as transações com predições de Machine Learning.
