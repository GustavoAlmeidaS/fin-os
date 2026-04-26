const state = {
  token: localStorage.getItem("financeAnalyzerToken"),
  user: JSON.parse(localStorage.getItem("financeAnalyzerUser") || "null"),
  accounts: [],
  categories: [],
  transactions: [],
  loans: [],
  goals: [],
  budgets: [],
  imports: [],
  summary: null,
  categorySummary: [],
  biData: [],
  search: ""
};

const brl = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const dateFormatter = new Intl.DateTimeFormat("pt-BR", { timeZone: "UTC" });

const els = {
  authPanel: document.querySelector("#authPanel"),
  dashboard: document.querySelector("#dashboard"),
  sessionStatus: document.querySelector("#sessionStatus"),
  logoutButton: document.querySelector("#logoutButton"),
  totalBalance: document.querySelector("#totalBalance"),
  
  // Forms
  loginForm: document.querySelector("#loginForm"),
  signupForm: document.querySelector("#signupForm"),
  accountForm: document.querySelector("#accountForm"),
  categoryForm: document.querySelector("#categoryForm"),
  transactionForm: document.querySelector("#transactionForm"),
  importForm: document.querySelector("#importForm"),
  loanForm: document.querySelector("#loanForm"),
  goalForm: document.querySelector("#goalForm"),
  budgetForm: document.querySelector("#budgetForm"),
  
  // Buttons & Inputs
  refreshButton: document.querySelector("#refreshButton"),
  cancelEditButton: document.querySelector("#cancelEditButton"),
  searchInput: document.querySelector("#searchInput"),
  
  // Metrics
  netWorthValue: document.querySelector("#netWorthValue"),
  totalAssetsValue: document.querySelector("#totalAssetsValue"),
  totalDebtValue: document.querySelector("#totalDebtValue"),
  
  // Lists
  accountList: document.querySelector("#accountList"),
  categoryList: document.querySelector("#categoryList"),
  categorySummary: document.querySelector("#categorySummary"),
  importList: document.querySelector("#importList"),
  
  // Tables
  transactionsTable: document.querySelector("#transactionsTable"),
  recentTransactionsTable: document.querySelector("#recentTransactionsTable"),
  biPreviewTable: document.querySelector("#biPreviewTable"),
  loansTable: document.querySelector("#loansTable"),
  goalsList: document.querySelector("#goalsList"),
  budgetsTable: document.querySelector("#budgetsTable"),
  
  toast: document.querySelector("#toast")
};

async function api(path, options = {}) {
  const headers = options.body instanceof FormData ? {} : { "Content-Type": "application/json" };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(path, { ...options, headers: { ...headers, ...(options.headers || {}) } });
  const payload = await response.json().catch(() => null);
  if (!response.ok || payload?.success === false) {
    throw new Error(payload?.message || "Erro na operação.");
  }
  return payload;
}

function serialize(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function showToast(message) {
  els.toast.textContent = message;
  els.toast.classList.remove("hidden");
  clearTimeout(showToast.timeout);
  showToast.timeout = setTimeout(() => els.toast.classList.add("hidden"), 3000);
}

function setSession(auth) {
  state.token = auth.token;
  state.user = auth;
  localStorage.setItem("financeAnalyzerToken", auth.token);
  localStorage.setItem("financeAnalyzerUser", JSON.stringify(auth));
  renderShell();
}

function clearSession() {
  state.token = null;
  state.user = null;
  localStorage.removeItem("financeAnalyzerToken");
  localStorage.removeItem("financeAnalyzerUser");
  renderShell();
}

function renderShell() {
  const signedIn = Boolean(state.token);
  els.authPanel.classList.toggle("hidden", signedIn);
  els.dashboard.classList.toggle("hidden", !signedIn);
  els.sessionStatus.textContent = signedIn ? state.user?.username : "";
}

async function loadDashboard() {
  if (!state.token) return;
  const [accounts, categories, transactions, summary, categorySummary, imports, biData, loans, goals, budgets] = await Promise.all([
    api("/api/accounts"),
    api("/api/categories"),
    api("/api/transactions?size=50&sort=transactionDate,desc"),
    api("/api/reports/dashboard-summary"),
    api("/api/reports/categories/summary"),
    api("/api/imports?size=5&sort=id,desc"),
    api("/api/reports/fact-transactions"),
    api("/api/loans"),
    api("/api/planning/goals"),
    api("/api/planning/budgets")
  ]);
  state.accounts = accounts.data || [];
  state.categories = categories.data || [];
  state.transactions = transactions.data || [];
  state.summary = summary.data || null;
  state.categorySummary = categorySummary.data?.categories || [];
  state.imports = imports.data || [];
  state.biData = biData.data || [];
  state.loans = loans.data || [];
  state.goals = goals.data || [];
  state.budgets = budgets.data || [];
  
  renderAll();
}

function renderAll() {
  renderMetrics();
  renderAccountOptions();
  renderCategoryOptions();
  renderAccounts();
  renderCategories();
  renderCategorySummary();
  renderImports();
  renderTransactions();
  renderBiPreview();
  renderLoans();
  renderGoals();
  renderBudgets();
}

function renderMetrics() {
  const assets = Number(state.summary?.totalAssets || 0);
  const debt = Number(state.summary?.totalDebt || 0);
  const netWorth = Number(state.summary?.netWorth || 0);
  
  els.totalBalance.textContent = brl.format(assets); // header
  els.netWorthValue.textContent = brl.format(netWorth);
  els.totalAssetsValue.textContent = brl.format(assets);
  els.totalDebtValue.textContent = brl.format(debt);
}

function renderAccountOptions() {
  const options = state.accounts.map(acc => `<option value="${acc.id}">${escapeHtml(acc.name)}</option>`);
  els.transactionForm.accountId.innerHTML = options.join("");
  els.transactionForm.destinationAccountId.innerHTML = `<option value="">Não se aplica</option>${options.join("")}`;
  if (els.importForm) els.importForm.accountId.innerHTML = options.join("");
}

function renderCategoryOptions() {
  const options = state.categories.map(cat => `<option value="${cat.id}">${escapeHtml(cat.name)}</option>`);
  els.transactionForm.categoryId.innerHTML = `<option value="">Sem categoria</option>${options.join("")}`;
}

function renderAccounts() {
  els.accountList.innerHTML = state.accounts.map(acc => `
    <div class="list-item">
      <span>${escapeHtml(acc.name)}</span>
      <strong>${brl.format(acc.currentBalance || 0)}</strong>
    </div>`).join("");
}

function renderCategories() {
  els.categoryList.innerHTML = state.categories.map(cat => `
    <div class="list-item">
      <span>${escapeHtml(cat.name)}</span>
      <span class="badge">${labelTxType(cat.type)}</span>
    </div>`).join("");
}

function renderCategorySummary() {
  els.categorySummary.innerHTML = state.categorySummary.map(item => `
    <div class="list-item">
      <span>${escapeHtml(item.categoryName)}</span>
      <strong class="text-danger">${brl.format(item.totalAmount || 0)}</strong>
    </div>`).join("");
}

function renderImports() {
  els.importList.innerHTML = state.imports.map(batch => `
    <div class="list-item">
      <div>
        <strong>${escapeHtml(batch.filename)}</strong><br>
        <small class="badge">${batch.status}</small>
      </div>
      ${batch.status === "PREVIEWED" ? `<button class="btn-secondary btn-sm" onclick="confirmImport(${batch.id})">Confirmar</button>` : ""}
    </div>`).join("");
}

window.confirmImport = async function(id) {
  try {
    await api(`/api/imports/${id}/confirm`, { method: "POST" });
    await loadDashboard();
    showToast("Importação confirmada.");
  } catch(e) { showToast(e.message); }
}

function renderTransactions() {
  const rows = state.transactions.filter(tx => 
    `${tx.description} ${tx.accountName || ""} ${tx.categoryName || ""} ${labelTxType(tx.type)}`.toLowerCase().includes(state.search.toLowerCase())
  );
  
  const buildRow = (tx, withActions = false) => `
    <tr>
      <td>${dateFormatter.format(new Date(`${tx.transactionDate}T00:00:00Z`))}</td>
      <td>${escapeHtml(tx.description)}</td>
      ${withActions ? `<td>${escapeHtml(tx.accountName || "-")}</td>` : ''}
      <td>${escapeHtml(tx.categoryName || "-")}</td>
      ${withActions ? `<td><span class="badge">${labelTxType(tx.type)}</span></td>` : ''}
      <td class="text-right ${tx.type === "INCOME" ? "text-success" : tx.type === "EXPENSE" ? "text-danger" : ""}">${brl.format(tx.amount || 0)}</td>
      ${withActions ? `<td><button class="btn-secondary btn-sm" onclick="deleteTx(${tx.id})">Excluir</button></td>` : ''}
    </tr>`;

  els.transactionsTable.innerHTML = rows.map(tx => buildRow(tx, true)).join("");
  els.recentTransactionsTable.innerHTML = rows.slice(0, 5).map(tx => buildRow(tx, false)).join("");
}

function renderBiPreview() {
  els.biPreviewTable.innerHTML = state.biData.slice(0, 10).map(row => `
    <tr>
      <td>${row.transaction_id}</td>
      <td>${row.year_month}</td>
      <td><span class="badge">${row.transaction_type}</span></td>
      <td>${escapeHtml(row.account_name)}</td>
      <td>${escapeHtml(row.category_name || '-')}</td>
      <td class="text-right">${row.amount}</td>
      <td><code>${escapeHtml(row.metadata || 'null')}</code></td>
    </tr>
  `).join("");
}

function renderLoans() {
  els.loansTable.innerHTML = state.loans.map(loan => `
    <tr>
      <td>${escapeHtml(loan.name)}</td>
      <td><span class="badge">${loan.loanType === 'INSTALLMENT' ? 'Parcelado' : 'Aberto'}</span></td>
      <td>${dateFormatter.format(new Date(`${loan.startDate}T00:00:00Z`))}</td>
      <td class="text-right text-danger">${brl.format(loan.currentBalance || 0)}</td>
      <td><button class="btn-secondary btn-sm" onclick="deleteLoan(${loan.id})">Excluir</button></td>
    </tr>
  `).join("");
}

window.deleteLoan = async function(id) {
  if (!confirm("Excluir empréstimo/dívida?")) return;
  try {
    await api(`/api/loans/${id}`, { method: "DELETE" });
    await loadDashboard();
    showToast("Dívida excluída.");
  } catch(e) { showToast(e.message); }
}

function renderGoals() {
  if (!els.goalsList) return;
  els.goalsList.innerHTML = state.goals.map(goal => {
    const progress = Math.min(100, Math.round((goal.currentAmount / goal.targetAmount) * 100)) || 0;
    return `
      <div class="metric-card" style="border-left: 4px solid ${goal.color}">
        <div class="flex-between">
          <span class="metric-label">${escapeHtml(goal.name)}</span>
          <span class="badge" style="background: ${goal.color}; color: white">${progress}%</span>
        </div>
        <strong class="metric-value" style="font-size: 1.25rem">${brl.format(goal.currentAmount)} / ${brl.format(goal.targetAmount)}</strong>
        <div style="background: #e2e8f0; height: 6px; border-radius: 3px; margin-top: 10px;">
          <div style="background: ${goal.color}; height: 100%; border-radius: 3px; width: ${progress}%"></div>
        </div>
      </div>
    `;
  }).join("");
}

function renderBudgets() {
  if (!els.budgetsTable) return;
  els.budgetsTable.innerHTML = state.budgets.map(budget => {
    const summary = state.categorySummary.find(s => s.categoryId === budget.categoryId);
    const spent = summary ? summary.total : 0;
    const progress = Math.min(100, Math.round((spent / budget.amountLimit) * 100)) || 0;
    const isOver = spent > budget.amountLimit;
    return `
      <tr>
        <td>${escapeHtml(budget.categoryName)}</td>
        <td class="text-right">${brl.format(spent)} / ${brl.format(budget.amountLimit)}</td>
        <td class="text-center">
          <div style="background: #e2e8f0; height: 10px; border-radius: 5px; width: 100%; position: relative;">
            <div style="background: ${isOver ? 'var(--danger-color)' : 'var(--primary-color)'}; height: 100%; border-radius: 5px; width: ${progress}%"></div>
          </div>
        </td>
      </tr>
    `;
  }).join("");
}

window.deleteTx = async function(id) {
  if (!confirm("Excluir lançamento?")) return;
  try {
    await api(`/api/transactions/${id}`, { method: "DELETE" });
    await loadDashboard();
    showToast("Lançamento excluído.");
  } catch(e) { showToast(e.message); }
}

function labelTxType(type) {
  return { INCOME: "Receita", EXPENSE: "Despesa", TRANSFER: "Transferência", ADJUSTMENT: "Ajuste" }[type] || type;
}

// Events
els.loginForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const res = await api("/api/auth/login", { method: "POST", body: JSON.stringify(serialize(els.loginForm)) });
    setSession(res.data);
    await loadDashboard();
  } catch (err) { showToast(err.message); }
});

els.signupForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const res = await api("/api/auth/signup", { method: "POST", body: JSON.stringify(serialize(els.signupForm)) });
    setSession(res.data);
    await loadDashboard();
  } catch (err) { showToast(err.message); }
});

els.accountForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const data = serialize(els.accountForm);
  try {
    await api("/api/accounts", {
      method: "POST",
      body: JSON.stringify({ ...data, currency: "BRL", initialBalance: Number(data.initialBalance||0) })
    });
    els.accountForm.reset();
    await loadDashboard();
    showToast("Conta salva.");
  } catch (err) { showToast(err.message); }
});

els.categoryForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const data = serialize(els.categoryForm);
  try {
    await api("/api/categories", { method: "POST", body: JSON.stringify(data) });
    els.categoryForm.reset();
    await loadDashboard();
    showToast("Categoria salva.");
  } catch (err) { showToast(err.message); }
});

els.transactionForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const data = serialize(els.transactionForm);
  try {
    const payload = {
      accountId: Number(data.accountId),
      destinationAccountId: data.destinationAccountId ? Number(data.destinationAccountId) : null,
      type: data.type,
      amount: Number(data.amount),
      transactionDate: data.transactionDate,
      description: data.description,
      categoryId: data.categoryId ? Number(data.categoryId) : null,
      metadata: data.metadata || null
    };
    await api("/api/transactions", { method: "POST", body: JSON.stringify(payload) });
    els.transactionForm.reset();
    els.transactionForm.transactionDate.valueAsDate = new Date();
    await loadDashboard();
    showToast("Lançamento salvo.");
  } catch (err) { showToast(err.message); }
});

if (els.loanForm) {
  els.loanForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = serialize(els.loanForm);
    try {
      const payload = {
        name: data.name,
        loanType: data.loanType,
        principalAmount: Number(data.principalAmount),
        startDate: data.startDate,
        endDate: data.endDate || null,
        interestRate: data.interestRate ? Number(data.interestRate) : null
      };
      await api("/api/loans", { method: "POST", body: JSON.stringify(payload) });
      els.loanForm.reset();
      await loadDashboard();
      showToast("Empréstimo salvo com sucesso.");
    } catch (err) { showToast(err.message); }
  });
}

els.importForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    await api("/api/imports/csv", { method: "POST", body: new FormData(els.importForm) });
    els.importForm.reset();
    await loadDashboard();
    showToast("Arquivo enviado.");
  } catch (err) { showToast(err.message); }
});

els.searchInput.addEventListener("input", () => {
  state.search = els.searchInput.value;
  renderTransactions();
});

els.refreshButton.addEventListener("click", () => loadDashboard());
els.logoutButton.addEventListener("click", clearSession);

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' })[m]);
}

renderShell();
if(els.transactionForm) els.transactionForm.transactionDate.valueAsDate = new Date();
if (state.token) {
  loadDashboard().catch(e => { showToast(e.message); clearSession(); });
}
