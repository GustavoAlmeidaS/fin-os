# Tutorial de Uso: FinOS (ERP Financeiro Pessoal)

Bem-vindo ao **FinOS**! Este sistema não é apenas um "app de gastos", mas sim uma plataforma de controladoria financeira pessoal completa. Ele foi desenhado para tratar suas finanças como as finanças de uma empresa, mantendo um livro-razão (ledger) estrito, controle de patrimônio líquido (Net Worth), gestão de dívidas e uma base sólida para Data Science e BI.

Abaixo, você encontra o passo a passo de como operar o sistema no seu dia a dia.

---

## 1. Primeiros Passos: Autenticação
Quando você acessar o sistema pela primeira vez, será recebido pela tela de Autenticação.
1. **Cadastrar:** Vá na aba "Cadastrar", preencha seu nome, e-mail, um nome de usuário (ex: `gusta`) e uma senha.
2. **Login:** Após o cadastro, você será levado para o painel principal. Nos próximos acessos, basta usar a aba "Entrar".

> **Dica - Automação Inteligente:** Logo que você cria a sua conta, o sistema **automaticamente cria categorias padrão** para você (Salário, Alimentação, Moradia, Transporte, Transferências e Ajustes). Você não precisa criar tudo do zero!

---

## 2. Preparando a Casa: Contas e Categorias
Antes de registrar qualquer gasto, você precisa informar onde o dinheiro está e para onde ele vai.

Vá no menu lateral: **Contas e Categorias**.

### 2.1 Criando suas Contas (Ativos)
As Contas representam a origem ou o destino do dinheiro real.
- **Como fazer:** No painel "Contas", insira o Nome (ex: *Nubank*, *Itaú*, *Carteira*), selecione o Tipo (Conta corrente, Poupança, etc.) e o **Saldo Inicial** (quanto você tem hoje lá).
- Ao salvar, o dinheiro será contabilizado imediatamente no seu **Total em Ativos**.

### 2.2 Criando Categorias (Classificação)
Categorias servem para agrupar o fluxo de caixa para análise.
- **Como fazer:** No painel "Categorias", crie categorias de `Receita` (ex: *Bônus*, *Dividendos*) ou de `Despesa` (ex: *Lazer*, *Assinaturas*).

---

## 3. Registrando o Passivo: Empréstimos / Dívidas
Se você tem alguma dívida ou empréstimo ativo, este é o momento de registrá-lo.

Vá no menu lateral: **Empréstimos / Dívidas**.
- **Como fazer:** Preencha o formulário de "Novo Empréstimo / Dívida".
- **Tipos de Dívida:**
  - `Parcelado`: Financiamentos ou empréstimos com prazo (Data Final).
  - `Aberto`: Um dinheiro que você pegou emprestado com alguém, sem prazo para devolução.
- **O Impacto:** O valor inserido aqui vai direto para o seu saldo de **Total em Dívidas**. O sistema vai subtrair as dívidas dos seus Ativos para te dar o seu **Patrimônio Líquido Real**.

---

## 4. O Dia a Dia: Ledger & Lançamentos
Aqui é onde acontece a operação diária do sistema. Como um livro-razão contábil, todas as movimentações financeiras são lançadas aqui.

Vá no menu lateral: **Ledger & Lançamentos**.

### Fazendo um Novo Lançamento
Preencha o painel superior com os dados do fato financeiro:
- **Conta:** De onde o dinheiro está saindo ou entrando.
- **Tipo de Lançamento:**
  - **Receita / Despesa:** Dinheiro entrando ou saindo do seu patrimônio.
  - **Transferência:** Movimentação entre *duas contas suas*. (Ex: Sacar dinheiro do Itaú para a Carteira). *Nota: Transferências não alteram seu patrimônio líquido, apenas trocam o dinheiro de lugar!* Neste caso, você deve selecionar também a "Conta Destino".
  - **Ajuste:** Usado apenas se você esquecer de anotar algo e o saldo do banco não bater com o do sistema. Ele "corrige" o saldo para igualar com a vida real.
- **Categoria:** Escolha onde classificar esse gasto/receita.
- **Metadata (JSON):** *Exclusivo para Data Science*. Você pode colocar marcações extras em formato JSON. Por exemplo: `{"tags": ["viagem_disney", "emergencia"]}`.

### O Livro-Razão (Tabela inferior)
Aqui você pode buscar e visualizar todo o seu histórico financeiro. Se você excluir um lançamento (Excluir), o saldo da conta associada é imediatamente estornado/corrigido.

---

## 5. Automação: Importação CSV
Se você não quiser digitar gasto por gasto, pode importar a fatura do cartão ou extrato da conta.

Vá no menu lateral: **Importação CSV**.
1. Selecione a **Conta de Destino** (para qual conta esses registros irão).
2. Faça o upload do arquivo `.csv` baixado do seu banco.
3. O arquivo entrará em estado de "Análise" (Previewed).
4. Clique em **Confirmar** para que os dados sejam ejetados oficialmente no seu Ledger!

> **Nota:** Os lançamentos via CSV recebem categorias padrão automaticamente como "Importações - Receitas" ou "Importações - Despesas". Depois você pode reclassificá-los, se desejar.

---

## 6. O Painel Gerencial: Visão Geral (Dashboard)
Sempre que quiser saber "Como estou?", este é o lugar.
- **Métricas Superiores:** Mostram a conta exata do seu *Patrimônio Líquido*, *Ativos* e *Dívidas*.
- **Despesas por Categoria:** Um resumo que lista exatamente onde você mais gastou dinheiro, servindo de termômetro para economia.
- **Lançamentos Recentes:** Uma visão rápida das últimas operações.

---

## 7. Dados BI (Analytics) - O Diferencial do Sistema
O seu FinOS é uma Fundação de Dados. Isso significa que ele foi desenhado para ser acoplado a ferramentas de Business Intelligence (como Power BI e Metabase) ou a scripts de inteligência artificial em Python.

Na aba **Dados BI (Analytics)**, você encontra as views que foram preparadas no banco de dados para os Cientistas de Dados (ou para você mesmo) explorarem:
- **v_fact_transactions_ml:** Tabela mastigada com lógicas de Machine Learning prontas (ex: `signed_amount`, `is_weekend`). Você espeta o Power BI nela e consegue prever como será seu fluxo de caixa na próxima semana!
- **v_monthly_cashflow:** Um balanço do que você gastou e recebeu por mês, expurgando transferências e ruídos operacionais.

---
### Resumo da Filosofia de Uso
1. Tenha suas **Contas** e **Dívidas** atualizadas (Balanço Patrimonial).
2. Lance seu dia a dia no **Ledger** (DRE e Fluxo de Caixa).
3. Acompanhe a saúde do seu bolso no **Dashboard**.
4. Quando a massa de dados for grande, use ferramentas externas conectadas na **Camada de BI** para prever seu futuro!
