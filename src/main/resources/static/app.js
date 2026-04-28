const state = {
  token: localStorage.getItem("financeAnalyzerToken"),
  user: JSON.parse(localStorage.getItem("financeAnalyzerUser") || "null"),
  accounts: [], categories: [], transactions: [], loans: [], goals: [], budgets: [], imports: [], counterparties: [], tags: [],
  summary: null, cashflow: [], categorySummary: [], biData: [],
  filters: { search: "", type: "", accountId: "", status: "", startDate: "", endDate: "", page: 0 },
  txPageInfo: { number: 0, totalPages: 1 },
  editingTxId: null, editingAccId: null, editingCatId: null
};

const brl = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const dateFormatter = new Intl.DateTimeFormat("pt-BR", { timeZone: "UTC" });

const els = {
  authPanel: document.querySelector("#authPanel"),
  dashboard: document.querySelector("#dashboard"),
  sessionStatus: document.querySelector("#sessionStatus"),
  userAvatar: document.querySelector("#userAvatar"),
  greetingText: document.querySelector("#greetingText"),
  pageTitle: document.querySelector("#pageTitle"),
  sidebar: document.querySelector("#sidebar"),
  
  // Forms
  loginForm: document.querySelector("#loginForm"),
  signupForm: document.querySelector("#signupForm"),
  accountForm: document.querySelector("#accountForm"),
  categoryForm: document.querySelector("#categoryForm"),
  counterpartyForm: document.querySelector("#counterpartyForm"),
  tagForm: document.querySelector("#tagForm"),
  transactionForm: document.querySelector("#transactionForm"),
  importForm: document.querySelector("#importForm"),
  loanForm: document.querySelector("#loanForm"),
  goalForm: document.querySelector("#goalForm"),
  budgetForm: document.querySelector("#budgetForm"),
  profileForm: document.querySelector("#profileForm"),
  preferencesForm: document.querySelector("#preferencesForm"),
  
  // Inputs & Buttons
  searchInput: document.querySelector("#searchInput"),
  filterType: document.querySelector("#filterType"),
  filterAccount: document.querySelector("#filterAccount"),
  cancelEditButton: document.querySelector("#cancelEditButton"),
  cancelAccEdit: document.querySelector("#cancelAccEdit"),
  cancelCatEdit: document.querySelector("#cancelCatEdit"),
  
  // Metrics & Charts
  totalBalance: document.querySelector("#totalBalance"),
  netWorthValue: document.querySelector("#netWorthValue"),
  totalAssetsValue: document.querySelector("#totalAssetsValue"),
  totalDebtValue: document.querySelector("#totalDebtValue"),
  monthIncomeValue: document.querySelector("#monthIncomeValue"),
  monthExpenseValue: document.querySelector("#monthExpenseValue"),
  cashflowChart: document.querySelector("#cashflowChart"),
  categoryDonut: document.querySelector("#categoryDonut"),
  
  // Lists & Tables
  accountList: document.querySelector("#accountList"),
  categoryList: document.querySelector("#categoryList"),
  counterpartyList: document.querySelector("#counterpartyList"),
  tagList: document.querySelector("#tagList"),
  categorySummary: document.querySelector("#categorySummary"),
  importList: document.querySelector("#importList"),
  transactionsTable: document.querySelector("#transactionsTable"),
  recentTransactionsTable: document.querySelector("#recentTransactionsTable"),
  biPreviewTable: document.querySelector("#biPreviewTable"),
  loansTable: document.querySelector("#loansTable"),
  goalsList: document.querySelector("#goalsList"),
  budgetsTable: document.querySelector("#budgetsTable"),
  
  // Modal
  modal: document.querySelector("#customModal"),
  toast: document.querySelector("#toast"),
  logoutButton: document.querySelector("#logoutButton"),
  
  // AI
  aiChatContainer: document.querySelector("#aiChatContainer"),
  aiChatInput: document.querySelector("#aiChatInput"),
  btnAiSend: document.querySelector("#btnAiSend")
};

// ==========================================
// CORE / API
// ==========================================
async function api(path, options = {}) {
  const headers = options.body instanceof FormData ? {} : { "Content-Type": "application/json" };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  
  const response = await fetch(path, { ...options, headers: { ...headers, ...(options.headers || {}) } });
  const payload = await response.json().catch(() => null);
  
  if (!response.ok || payload?.success === false) {
    if (response.status === 401) { clearSession(); }
    const err = new Error(payload?.message || "Erro na operação.");
    err.details = payload?.details;
    throw err;
  }
  
  if (options.method && ["POST", "PUT", "DELETE"].includes(options.method.toUpperCase())) {
    window.dispatchEvent(new CustomEvent('finos_api_action', { detail: { path, method: options.method.toUpperCase() } }));
  }
  
  return payload;
}

function serialize(form) { return Object.fromEntries(new FormData(form).entries()); }

// ==========================================
// UI HELPERS
// ==========================================
function showToast(message, isError = false) {
  els.toast.textContent = message;
  els.toast.className = `toast active ${isError ? 'error' : ''}`;
  clearTimeout(showToast.timeout);
  showToast.timeout = setTimeout(() => els.toast.classList.remove("active"), 3000);
}

function toggleButtonLoading(btn, isLoading) {
  if (!btn) return;
  if (isLoading) {
    btn.dataset.originalText = btn.innerHTML;
    btn.innerHTML = `<svg class="spinner" viewBox="0 0 50 50" style="width:20px;height:20px;animation:spin 1s linear infinite;"><circle cx="25" cy="25" r="20" fill="none" stroke="currentColor" stroke-width="5" stroke-dasharray="90 150" stroke-linecap="round"></circle></svg> Processando...`;
    btn.disabled = true;
  } else {
    btn.innerHTML = btn.dataset.originalText;
    btn.disabled = false;
  }
}

// Modal System
let modalResolve = null;
function openModal(title, message, inputConfig = null) {
  return new Promise(resolve => {
    modalResolve = resolve;
    document.getElementById("modalTitle").textContent = title;
    document.getElementById("modalMessage").textContent = message;
    
    const inputContainer = document.getElementById("modalInputContainer");
    const inputField = document.getElementById("modalInput");
    
    if (inputConfig) {
      inputContainer.classList.remove("hidden");
      document.getElementById("modalInputLabel").textContent = inputConfig.label;
      inputField.type = inputConfig.type || "text";
      inputField.value = "";
      setTimeout(() => inputField.focus(), 100);
    } else {
      inputContainer.classList.add("hidden");
    }
    
    els.modal.classList.add("active");
  });
}
function closeModal() { els.modal.classList.remove("active"); if (modalResolve) modalResolve(null); }
document.getElementById("modalConfirmBtn").addEventListener("click", () => {
  els.modal.classList.remove("active");
  const val = document.getElementById("modalInputContainer").classList.contains("hidden") 
    ? true 
    : document.getElementById("modalInput").value;
  if (modalResolve) modalResolve(val);
});

// Animations
function animateValue(obj, start, end, duration) {
  let startTimestamp = null;
  const step = (timestamp) => {
    if (!startTimestamp) startTimestamp = timestamp;
    const progress = Math.min((timestamp - startTimestamp) / duration, 1);
    obj.innerHTML = brl.format(progress * (end - start) + start);
    if (progress < 1) window.requestAnimationFrame(step);
  };
  window.requestAnimationFrame(step);
}

// ==========================================
// SESSION & NAVIGATION
// ==========================================
function setSession(auth) {
  state.token = auth.token; state.user = auth;
  localStorage.setItem("financeAnalyzerToken", auth.token);
  localStorage.setItem("financeAnalyzerUser", JSON.stringify(auth));
  renderShell();
  
  if (!localStorage.getItem("finos_tutorial_seen")) {
    setTimeout(() => {
      showToast("💡 Primeira vez aqui? Clique em 'Tutorial do Sistema' na barra lateral para um guia interativo!", false);
      localStorage.setItem("finos_tutorial_seen", "true");
    }, 1500);
  }
}
function clearSession() {
  state.token = null; state.user = null;
  localStorage.removeItem("financeAnalyzerToken");
  localStorage.removeItem("financeAnalyzerUser");
  renderShell();
}

function renderShell() {
  const signedIn = Boolean(state.token);
  els.authPanel.classList.toggle("hidden", signedIn);
  els.dashboard.classList.toggle("hidden", !signedIn);
  if (signedIn && state.user) {
    els.sessionStatus.textContent = state.user.username;
    els.userAvatar.textContent = (state.user.firstName ? state.user.firstName[0] : state.user.username[0]).toUpperCase();
    
    const hour = new Date().getHours();
    const greeting = hour < 12 ? "Bom dia" : hour < 18 ? "Boa tarde" : "Boa noite";
    els.greetingText.textContent = `${greeting}, ${state.user.firstName || state.user.username}!`;
  }
}

window.switchAuthTab = function(tab) {
  document.querySelectorAll('.auth-tabs .tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.auth-form').forEach(f => f.classList.remove('active'));
  document.getElementById(tab === 'login' ? 'tabLogin' : 'tabSignup').classList.add('active');
  document.getElementById(tab === 'login' ? 'loginForm' : 'signupForm').classList.add('active');
}

window.toggleSidebar = function() { els.sidebar.classList.toggle("open"); }

document.querySelectorAll('.nav-item').forEach(item => {
  item.addEventListener('click', (e) => {
    e.preventDefault();
    window.location.hash = item.getAttribute('href');
    window.dispatchEvent(new Event('hashchange'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    item.classList.add('active');
    els.sidebar.classList.remove("open");
    
    const targetId = 'section-' + item.getAttribute('href').substring(1);
    document.querySelectorAll('.view-section').forEach(s => s.classList.add('hidden'));
    document.getElementById(targetId).classList.remove('hidden');
    els.pageTitle.textContent = item.textContent.trim();
  });
});

// ==========================================
// DATA LOADING
// ==========================================
async function loadDashboard() {
  if (!state.token) return switchView('auth');
  els.authPanel.classList.add('hidden');
  els.dashboard.classList.remove('hidden');
  
  try {
    const [accs, cats, txs, smry, cf, cs, bi, lns, gls, bdg, imp, cps, tgs, prof, pref] = await Promise.all([
      api("/api/accounts"),
      api("/api/categories"),
      api(`/api/transactions?size=20&page=${state.filters.page}&search=${state.filters.search}&type=${state.filters.type}&accountId=${state.filters.accountId}&status=${state.filters.status}&startDate=${state.filters.startDate}&endDate=${state.filters.endDate}`),
      api("/api/reports/dashboard-summary").catch(() => ({data: null})), // handle 404
      api("/api/reports/cash-flow/monthly"),
      api("/api/reports/categories/summary"),
      api("/api/reports/fact-transactions"),
      api("/api/loans"),
      api("/api/planning/goals"),
      api("/api/planning/budgets"),
      api("/api/imports?size=5&sort=id,desc"),
      api("/api/counterparties"),
      api("/api/tags"),
      api("/api/users/profile"),
      api("/api/users/preferences")
    ]);
    
    state.accounts = accs.data || [];
    state.categories = cats.data || [];
    state.transactions = txs.data?.content || txs.data || [];
    state.txPageInfo = {
      number: txs.data?.number || 0,
      totalPages: txs.data?.totalPages || 1
    };
    state.summary = smry.data;
    state.cashflow = cf.data?.items || [];
    state.categorySummary = cs.data?.categories || [];
    state.biData = bi.data || [];
    state.loans = lns.data || [];
    state.goals = gls.data || [];
    state.budgets = bdg.data || [];
    state.imports = imp.data?.content || imp.data || [];
    state.counterparties = cps.data || [];
    state.tags = tgs.data || [];
    state.profile = prof.data || {};
    state.preferences = pref.data || {};
    
    renderDropdowns();
    renderAll();
  } catch(e) { showToast(e.message, true); }
}

function renderAll() {
  renderMetrics(); renderCashflowChart(); renderDonutChart();
  renderDropdowns();
  renderAccounts(); renderCategories(); renderCounterparties(); renderTags();
  renderTransactions(); renderBiPreview(); renderLoans(); renderGoals(); renderBudgets();
  renderImports();
  renderSettings();
}

function renderSettings() {
  if (state.profile && els.profileForm) {
    els.profileForm.firstName.value = state.profile.firstName || '';
    els.profileForm.lastName.value = state.profile.lastName || '';
    els.profileForm.email.value = state.profile.email || '';
  }
  if (state.preferences && els.preferencesForm) {
    els.preferencesForm.defaultLocale.value = state.preferences.defaultLocale || '';
    els.preferencesForm.defaultCurrency.value = state.preferences.defaultCurrency || '';
    els.preferencesForm.defaultTimezone.value = state.preferences.defaultTimezone || '';
    els.preferencesForm.dateFormat.value = state.preferences.dateFormat || '';
    els.preferencesForm.numberFormatLocale.value = state.preferences.numberFormatLocale || '';
  }
}

// ==========================================
// RENDERING
// ==========================================
function renderMetrics() {
  if (!state.summary) return;
  animateValue(els.totalBalance, 0, state.summary.totalAssets || 0, 1000);
  animateValue(els.netWorthValue, 0, state.summary.netWorth || 0, 1000);
  animateValue(els.totalAssetsValue, 0, state.summary.totalAssets || 0, 1000);
  animateValue(els.totalDebtValue, 0, state.summary.totalDebt || 0, 1000);
  animateValue(els.monthIncomeValue, 0, state.summary.currentMonthIncome || 0, 1000);
  animateValue(els.monthExpenseValue, 0, state.summary.currentMonthExpenses || 0, 1000);
}

function renderCashflowChart() {
  if (!state.cashflow.length) {
    els.cashflowChart.innerHTML = `<div class="empty-state">
      <p>📊 Cadastre lançamentos para visualizar o fluxo de caixa mensal.</p>
    </div>`;
    return;
  }
  const maxVal = Math.max(...state.cashflow.map(i => Math.max(i.income, i.expenses, 1)));
  els.cashflowChart.innerHTML = state.cashflow.slice(-6).map(item => {
    const incHeight = (item.income / maxVal) * 100;
    const expHeight = (item.expenses / maxVal) * 100;
    return `
      <div class="chart-col">
        <div class="chart-bar income" style="height: ${incHeight}%"></div>
        <div class="chart-bar expense" style="height: ${expHeight}%"></div>
        <span class="chart-label">${item.yearMonth.substring(5)}</span>
        <div class="chart-tooltip">
          <span class="text-success">+${brl.format(item.income)}</span><br>
          <span class="text-danger">-${brl.format(item.expenses)}</span><br>
          Saldo: <strong>${brl.format(item.net)}</strong>
        </div>
      </div>
    `;
  }).join("");
}

function renderDonutChart() {
  if (!state.categorySummary.length) return;
  const top = state.categorySummary.slice(0, 5);
  const total = top.reduce((acc, c) => acc + c.totalAmount, 0);
  let conicString = "";
  let currentDeg = 0;
  
  els.categorySummary.innerHTML = top.map((item, i) => {
    const cat = state.categories.find(c => c.id === item.categoryId);
    const color = cat?.color || `hsl(${i*60}, 70%, 60%)`;
    const pct = total > 0 ? (item.totalAmount / total) * 100 : 0;
    conicString += `${color} ${currentDeg}% ${currentDeg + pct}%, `;
    currentDeg += pct;
    
    return `
      <div class="list-item" style="border-left: 4px solid ${color}">
        <span>${escapeHtml(item.categoryName)}</span>
        <strong class="text-danger">${brl.format(item.totalAmount)}</strong>
      </div>`;
  }).join("");
  
  if (total > 0) {
    els.categoryDonut.style.background = `conic-gradient(${conicString.slice(0,-2)})`;
  }
}

function renderDropdowns() {
  const accOpts = state.accounts.map(a => `<option value="${a.id}">${escapeHtml(a.name)}</option>`).join("");
  els.transactionForm.accountId.innerHTML = accOpts;
  els.transactionForm.destinationAccountId.innerHTML = `<option value="">Não se aplica</option>${accOpts}`;
  els.filterAccount.innerHTML = `<option value="">Todas as Contas</option>${accOpts}`;
  if(els.importForm) els.importForm.accountId.innerHTML = accOpts;
  
  els.transactionForm.categoryId.innerHTML = `<option value="">Sem categoria</option>` + 
    state.categories.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join("");
    
  let cpOpts = `<option value="">Opcional / Nenhuma</option>` + state.counterparties.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join("");
  els.transactionForm.counterpartyId.innerHTML = cpOpts;
  if(els.loanForm && els.loanForm.counterpartyId) els.loanForm.counterpartyId.innerHTML = cpOpts;
    
  if(els.budgetForm) {
    els.budgetForm.categoryId.innerHTML = state.categories.filter(c => c.type === 'EXPENSE')
      .map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join("");
  }
}

function buildEmpty(text, html = false) { return `<div class="empty-state">${html ? text : escapeHtml(text)}</div>`; }

function renderAccounts() {
  els.accountList.innerHTML = state.accounts.length === 0 ? buildEmpty(`🏦 Nenhuma conta cadastrada.<br><small style="color:var(--text-muted)">Use o formulário acima para adicionar sua primeira conta bancária.</small>`, true) : state.accounts.map(acc => `
    <div class="list-item">
      <div>
        <span style="font-weight: 500">${escapeHtml(acc.name)}</span>
        <span class="badge" style="margin-left: 0.5rem">${acc.type}</span>
      </div>
      <div style="display: flex; gap: 1rem; align-items: center;">
        <strong class="${acc.currentBalance < 0 ? 'text-danger' : 'text-success'}">${brl.format(acc.currentBalance || 0)}</strong>
        <div class="action-btns">
          <button class="btn-secondary btn-sm" onclick="editAccount(${acc.id})">Editar</button>
          <button class="btn-danger btn-sm" onclick="deleteAccount(${acc.id})">X</button>
        </div>
      </div>
    </div>`).join("");
}

function renderCategories() {
  els.categoryList.innerHTML = state.categories.length === 0 ? buildEmpty("Nenhuma categoria.") : state.categories.map(cat => `
    <div class="list-item" style="border-left: 4px solid ${cat.color || '#fff'}">
      <span>${escapeHtml(cat.name)}</span>
      <div style="display: flex; gap: 1rem; align-items: center;">
        <span class="badge">${labelTxType(cat.type)}</span>
        <div class="action-btns">
          <button class="btn-secondary btn-sm" onclick="editCategory(${cat.id})">Editar</button>
          <button class="btn-danger btn-sm" onclick="deleteCategory(${cat.id})">X</button>
        </div>
      </div>
    </div>`).join("");
}

function renderCounterparties() {
  els.counterpartyList.innerHTML = state.counterparties.length === 0 ? buildEmpty(`👥 Nenhuma pessoa ou empresa cadastrada.<br><small style="color:var(--text-muted)">Adicione as pessoas e empresas com quem você negocia.</small>`, true) : state.counterparties.map(cp => `
    <div class="list-item">
      <span>${escapeHtml(cp.name)} <small class="text-muted">(${cp.document||'S/N'})</small></span>
      <div class="action-btns">
        <button class="btn-danger btn-sm" onclick="deleteCp(${cp.id})">Remover</button>
      </div>
    </div>`).join("");
}

function renderTags() {
  els.tagList.innerHTML = state.tags.length === 0 ? buildEmpty("Nenhuma tag.") : state.tags.map(tag => `
    <div class="list-item">
      <span>#${escapeHtml(tag.name)}</span>
    </div>`).join("");
}

function renderTransactions() {
  let rows = state.transactions;
  const s = state.filters.search.toLowerCase();
  if (s) rows = rows.filter(tx => `${tx.description} ${tx.accountName||""} ${tx.categoryName||""}`.toLowerCase().includes(s));
  if (state.filters.type) rows = rows.filter(tx => tx.type === state.filters.type);
  if (state.filters.accountId) rows = rows.filter(tx => String(tx.accountId) === state.filters.accountId);

  if (rows.length === 0) {
    els.transactionsTable.innerHTML = `<tr><td colspan="7" class="text-center" style="padding: 2rem;">📝 Nenhum lançamento encontrado.<br><small style="color:var(--text-muted)">Registre receitas e despesas no formulário acima.</small></td></tr>`;
    els.recentTransactionsTable.innerHTML = `<tr><td colspan="4" class="text-center" style="padding: 2rem; color:var(--text-muted);">Registre movimentações no menu <strong>Receitas e Despesas</strong> para vê-las aqui.</td></tr>`;
    return;
  }

  const buildRow = (tx, full = true) => `
    <tr>
      <td>${dateFormatter.format(new Date(`${tx.transactionDate}T00:00:00Z`))}<br>
          <small class="badge" style="font-size:0.65rem">${tx.status === 'PENDING' ? '⏳ Pendente' : '✅ Pago'}</small></td>
      <td><strong>${escapeHtml(tx.description)}</strong>
          ${tx.counterpartyName ? `<br><small class="text-muted">${escapeHtml(tx.counterpartyName)}</small>` : ''}</td>
      ${full ? `<td>${escapeHtml(tx.accountName || "-")}</td>` : ''}
      <td><span class="badge" style="background: ${tx.categoryColor || 'transparent'}">${escapeHtml(tx.categoryName || "-")}</span></td>
      ${full ? `<td><span class="badge">${labelTxType(tx.type)}</span></td>` : ''}
      <td class="text-right ${tx.type === "INCOME" ? "text-success" : tx.type === "EXPENSE" ? "text-danger" : ""}">${brl.format(tx.amount || 0)}</td>
      ${full ? `<td class="text-right">
        <div class="action-btns">
          ${tx.status === 'PENDING' ? `<button class="btn-primary btn-sm" onclick="approveTx(${tx.id})" title="Aprovar/Efetivar">✔</button>` : ''}
          <button class="btn-secondary btn-sm" onclick="editTx(${tx.id})">✎</button>
          <button class="btn-danger btn-sm" onclick="deleteTx(${tx.id})">X</button>
        </div>
      </td>` : ''}
    </tr>`;

  els.transactionsTable.innerHTML = rows.map(tx => buildRow(tx, true)).join("");
  els.recentTransactionsTable.innerHTML = rows.slice(0, 5).map(tx => buildRow(tx, false)).join("");
  
  // Update Pagination Controls
  const p = state.txPageInfo || { number: 0, totalPages: 1 };
  const prevBtn = document.getElementById('btnPrevPage');
  const nextBtn = document.getElementById('btnNextPage');
  const pageInd = document.getElementById('pageIndicator');
  if (prevBtn && nextBtn && pageInd) {
    prevBtn.disabled = p.number <= 0;
    nextBtn.disabled = p.number >= p.totalPages - 1;
    pageInd.textContent = `Página ${p.number + 1} de ${Math.max(1, p.totalPages)}`;
  }
}

function renderGoals() {
  els.goalsList.innerHTML = state.goals.length === 0 ? buildEmpty(`🎯 Nenhuma meta definida.<br><small style="color:var(--text-muted)">Crie uma meta acima para acompanhar seus objetivos financeiros.</small>`, true) : state.goals.map(goal => {
    const progress = Math.min(100, Math.round((goal.currentAmount / goal.targetAmount) * 100)) || 0;
    return `
      <div class="metric-card" style="border-left: 4px solid ${goal.color}">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <span class="metric-label">${escapeHtml(goal.name)}</span>
          <span class="badge" style="background: ${goal.color}20; color: ${goal.color}">${progress}%</span>
        </div>
        <strong class="metric-value">${brl.format(goal.currentAmount)}</strong>
        <div style="font-size:0.8rem; color:var(--text-muted); margin-top:0.25rem;">Meta: ${brl.format(goal.targetAmount)}</div>
        <div class="progress-track">
          <div class="progress-fill" style="background: ${goal.color}; width: ${progress}%"></div>
        </div>
        <button class="btn-secondary btn-sm mt-4" style="width:100%" onclick="depositGoal(${goal.id})">+ Depositar Valor</button>
      </div>`;
  }).join("");
}

function renderBudgets() {
  els.budgetsTable.innerHTML = state.budgets.length === 0 ? `<tr><td colspan="2" class="text-center" style="padding: 2rem;">💰 Defina limites de gastos por categoria para controlar seu orçamento.</td></tr>` : state.budgets.map(b => {
    const sum = state.categorySummary.find(s => s.categoryId === b.categoryId);
    const spent = sum ? sum.totalAmount : 0;
    const progress = Math.min(100, (spent / b.amountLimit) * 100) || 0;
    const isOver = spent > b.amountLimit;
    const color = isOver ? 'var(--danger)' : 'var(--success)';
    return `
      <tr>
        <td><strong>${escapeHtml(b.categoryName)}</strong><br><small class="text-muted">${brl.format(spent)} de ${brl.format(b.amountLimit)}</small></td>
        <td style="vertical-align: middle;">
          <div class="progress-track" style="margin:0;">
            <div class="progress-fill" style="background: ${color}; width: ${progress}%"></div>
          </div>
        </td>
      </tr>`;
  }).join("");
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
    </tr>`).join("");
}

function renderImports() {
  if (!els.importList) return;
  els.importList.innerHTML = state.imports.length === 0 ? buildEmpty("Nenhuma importação recente.") : state.imports.map(imp => {
    let actions = "";
    if (imp.status === "PENDING_REVIEW") {
      actions = `
        <div style="margin-top: 1rem; display: flex; gap: 0.5rem;">
          <button class="btn-primary btn-sm" onclick="confirmImport(${imp.id})">Confirmar</button>
          <button class="btn-danger btn-sm" onclick="cancelImport(${imp.id})">Cancelar</button>
        </div>
      `;
    } else if (imp.status === 'COMPLETED' || imp.status === 'IMPORTED') {
      actions = `
        <div style="margin-top: 1rem; display: flex; gap: 0.5rem;">
          <button class="btn-secondary btn-sm" onclick="reviewBatchWithAi(${imp.id})" title="Enviar lote para revisão da Inteligência Artificial">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16" style="vertical-align: middle;"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
            Revisar Lote com IA
          </button>
        </div>
      `;
    }
    return `
      <div class="metric-card" style="margin-bottom: 1rem;">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <strong>Arquivo: ${escapeHtml(imp.filename)}</strong>
          <span class="badge" style="background: ${imp.status === 'COMPLETED' ? 'var(--success)' : imp.status === 'FAILED' ? 'var(--danger)' : 'var(--primary)'}">${imp.status}</span>
        </div>
        <div style="font-size:0.85rem; color:var(--text-muted); margin-top:0.5rem;">
          Registros: ${imp.totalRecords} totais | ${imp.validRecords} válidos | ${imp.invalidRecords} inválidos | ${imp.duplicateRecords} duplicados
        </div>
        ${actions}
      </div>`;
  }).join("");
}

function renderLoans() {
  els.loansTable.innerHTML = state.loans.length === 0 ? `<tr><td colspan="5" class="text-center" style="padding: 2rem;">✅ Nenhuma dívida registrada. Parabéns!<br><small style="color:var(--text-muted)">Use o formulário acima caso precise controlar um empréstimo.</small></td></tr>` : state.loans.map(loan => `
    <tr>
      <td><strong>${escapeHtml(loan.name)}</strong></td>
      <td><span class="badge">${loan.loanType === 'INSTALLMENT' ? 'Parcelado' : 'Aberto'}</span></td>
      <td>${dateFormatter.format(new Date(`${loan.startDate}T00:00:00Z`))}</td>
      <td class="text-right text-danger">${brl.format(loan.currentBalance || 0)}</td>
      <td class="text-right" style="white-space: nowrap;">
        <button class="btn-secondary btn-sm" onclick="payLoan(${loan.id})" title="Registrar Pagamento">Pgto</button>
        <button class="btn-danger btn-sm" onclick="deleteLoan(${loan.id})" title="Excluir">X</button>
      </td>
    </tr>`).join("");
}

// ==========================================
// ACTIONS & CRUD
// ==========================================
window.deleteTx = async id => {
  if (await openModal("Excluir Lançamento", "O saldo da conta será recalculado. Deseja continuar?")) {
    try { await api(`/api/transactions/${id}`, { method: "DELETE" }); showToast("Lançamento excluído", false); loadDashboard(); } 
    catch(e) { showToast(e.message, true); }
  }
}
window.approveTx = async id => {
  const tx = state.transactions.find(t => t.id === id);
  if (!tx) return;
  try {
    const updated = { ...tx, status: "POSTED" };
    await api(`/api/transactions/${id}`, { method: "PUT", body: JSON.stringify(updated) });
    showToast("Lançamento efetivado!", false);
    loadDashboard();
  } catch(e) { showToast(e.message, true); }
}
window.editTx = id => {
  const tx = state.transactions.find(t => t.id === id);
  if(!tx) return;
  state.editingTxId = id;
  els.transactionForm.accountId.value = tx.accountId;
  els.transactionForm.type.value = tx.type;
  els.transactionForm.categoryId.value = tx.categoryId || "";
  els.transactionForm.transactionDate.value = tx.transactionDate;
  els.transactionForm.amount.value = tx.amount;
  els.transactionForm.status.value = tx.status;
  els.transactionForm.description.value = tx.description;
  document.getElementById("txFormTitle").textContent = "Editar Lançamento";
  document.getElementById("btnSaveTx").textContent = "Atualizar";
  els.cancelEditButton.classList.remove("hidden");
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

window.deleteAccount = async id => {
  if (await openModal("Excluir Conta", "ATENÇÃO: Isso excluirá a conta e ocultará todos os lançamentos atrelados. Continuar?")) {
    try { await api(`/api/accounts/${id}`, { method: "DELETE" }); showToast("Conta excluída", false); loadDashboard(); } 
    catch(e) { showToast(e.message, true); }
  }
}
window.editAccount = id => {
  const acc = state.accounts.find(a => a.id === id);
  state.editingAccId = id;
  els.accountForm.name.value = acc.name;
  els.accountForm.type.value = acc.type;
  els.accountForm.initialBalance.value = acc.initialBalance;
  els.accountForm.institutionName.value = acc.institutionName || '';
  els.accountForm.color.value = acc.color || '#3b82f6';
  els.accountForm.notes.value = acc.notes || '';
  els.accountForm.querySelector("button[type=submit]").textContent = "Atualizar";
  els.cancelAccEdit.classList.remove("hidden");
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

window.deleteCategory = async id => {
  if (await openModal("Excluir Categoria", "Deseja remover esta categoria?")) {
    try { await api(`/api/categories/${id}`, { method: "DELETE" }); showToast("Categoria removida", false); loadDashboard(); } 
    catch(e) { showToast(e.message, true); }
  }
}
window.editCategory = id => {
  const cat = state.categories.find(c => c.id === id);
  state.editingCatId = id;
  els.categoryForm.name.value = cat.name;
  els.categoryForm.type.value = cat.type;
  els.categoryForm.color.value = cat.color || "#6366f1";
  els.categoryForm.querySelector("button[type=submit]").textContent = "Atualizar";
  els.cancelCatEdit.classList.remove("hidden");
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

window.deleteCp = async id => {
  if (await openModal("Remover Pessoa/Empresa", "Deseja remover este registro?")) {
    try { await api(`/api/counterparties/${id}`, { method: "DELETE" }); showToast("Removido", false); loadDashboard(); } 
    catch(e) { showToast(e.message, true); }
  }
}

window.deleteLoan = async id => {
  if (await openModal("Excluir Dívida", "Deseja remover este registro permanentemente?")) {
    try { await api(`/api/loans/${id}`, { method: "DELETE" }); loadDashboard(); } catch(e) { showToast(e.message, true); }
  }
}

window.payLoan = async id => {
  const amountStr = await openModal("Pagamento de Empréstimo", "Informe o valor do pagamento:", { type: "number", label: "Valor (R$)" });
  if (!amountStr) return;
  const amount = Number(amountStr);
  if (amount > 0) {
    try {
      const today = new Date().toISOString().split('T')[0];
      await api(`/api/loans/${id}/movements`, { 
        method: "POST", 
        body: JSON.stringify({ type: "PAYMENT", amount, movementDate: today, description: "Pagamento" }) 
      });
      showToast("Pagamento registrado!", false);
      loadDashboard();
    } catch(e) { showToast(e.message, true); }
  }
}

window.depositGoal = async id => {
  const amountStr = await openModal("Depositar na Meta", "Informe o valor a ser guardado para esta meta:", { type: "number", label: "Valor (R$)" });
  if (!amountStr) return;
  const amount = Number(amountStr);
  if (amount > 0) {
    try {
      await api(`/api/planning/goals/${id}/deposit`, { method: "PATCH", body: JSON.stringify({ amount }) });
      showToast("Depósito registrado!", false);
      loadDashboard();
    } catch(e) { showToast(e.message, true); }
  }
}

window.confirmImport = async id => {
  if (await openModal("Confirmar Importação", "Deseja efetivar todos os registros válidos no Livro Razão?")) {
    try {
      await api(`/api/imports/${id}/confirm`, { method: "POST" });
      showToast("Importação concluída!", false);
      loadDashboard();
    } catch(e) { showToast(e.message, true); }
  }
}

window.cancelImport = async id => {
  if (await openModal("Cancelar Importação", "Deseja descartar este lote de importação?")) {
    try {
      await api(`/api/imports/${id}/cancel`, { method: "POST" });
      showToast("Importação cancelada.", false);
      loadDashboard();
    } catch(e) { showToast(e.message, true); }
  }
}

// ==========================================
// FORM SUBMITS
// ==========================================
function bindForm(form, endpoint, methodObj) {
  if(!form) return;
  form.addEventListener("submit", async e => {
    e.preventDefault();
    const btn = form.querySelector('button[type="submit"]');
    toggleButtonLoading(btn, true);
    try {
      let url = endpoint;
      let method = methodObj.method || "POST";
      
      // Handle dynamic updates
      if (form.id === 'transactionForm' && state.editingTxId) { url = `/api/transactions/${state.editingTxId}`; method = "PUT"; }
      if (form.id === 'accountForm' && state.editingAccId) { url = `/api/accounts/${state.editingAccId}`; method = "PUT"; }
      if (form.id === 'categoryForm' && state.editingCatId) { url = `/api/categories/${state.editingCatId}`; method = "PUT"; }

      const body = methodObj.bodyBuilder ? methodObj.bodyBuilder(serialize(form)) : serialize(form);
      const res = await api(url, { method, body: JSON.stringify(body) });
      
      form.reset();
      
      // Reset edit states
      if (form.id === 'transactionForm') { resetTxForm(); form.transactionDate.valueAsDate = new Date(); }
      if (form.id === 'accountForm') { state.editingAccId = null; els.cancelAccEdit.classList.add('hidden'); btn.textContent = "Salvar Conta"; }
      if (form.id === 'categoryForm') { state.editingCatId = null; els.cancelCatEdit.classList.add('hidden'); btn.textContent = "Salvar Categoria"; }
      
      if(res.message) showToast(res.message, false);
      await loadDashboard();
    } catch (err) {
      form.querySelectorAll('.field-error').forEach(e => e.classList.remove('field-error'));
      if (err.details && Array.isArray(err.details)) {
        err.details.forEach(d => {
          const input = form.querySelector(`[name="${d.field}"]`);
          if (input) input.classList.add('field-error');
        });
        showToast(err.message + " (Verifique os campos destacados)", true);
      } else {
        showToast(err.message, true);
      }
    } finally {
      toggleButtonLoading(btn, false);
    }
  });
}

function resetTxForm() {
  state.editingTxId = null;
  document.getElementById("txFormTitle").textContent = "Novo Lançamento";
  document.getElementById("btnSaveTx").textContent = "Salvar Lançamento";
  els.cancelEditButton.classList.add("hidden");
  els.transactionForm.reset();
}

els.cancelEditButton?.addEventListener("click", resetTxForm);
els.cancelAccEdit?.addEventListener("click", () => { state.editingAccId=null; els.accountForm.reset(); els.cancelAccEdit.classList.add("hidden"); els.accountForm.querySelector("button[type=submit]").textContent = "Salvar Conta"; });
els.cancelCatEdit?.addEventListener("click", () => { state.editingCatId=null; els.categoryForm.reset(); els.cancelCatEdit.classList.add("hidden"); els.categoryForm.querySelector("button[type=submit]").textContent = "Salvar Categoria"; });

bindForm(els.loginForm, "/api/auth/login", { method: "POST" }, true);
bindForm(els.signupForm, "/api/auth/signup", { method: "POST" }, true);
els.loginForm.addEventListener('submit', async e => {
  e.preventDefault(); toggleButtonLoading(els.loginForm.querySelector('button'), true);
  try { const res = await api("/api/auth/login", { method: "POST", body: JSON.stringify(serialize(els.loginForm)) }); setSession(res.data); await loadDashboard(); } 
  catch (err) { showToast(err.message, true); } finally { toggleButtonLoading(els.loginForm.querySelector('button'), false); }
});
els.signupForm.addEventListener('submit', async e => {
  e.preventDefault(); toggleButtonLoading(els.signupForm.querySelector('button'), true);
  try { const res = await api("/api/auth/signup", { method: "POST", body: JSON.stringify(serialize(els.signupForm)) }); setSession(res.data); await loadDashboard(); } 
  catch (err) { showToast(err.message, true); } finally { toggleButtonLoading(els.signupForm.querySelector('button'), false); }
});

bindForm(els.accountForm, "/api/accounts", { bodyBuilder: d => ({ ...d, currency: "BRL", initialBalance: Number(d.initialBalance||0) }) });
bindForm(els.importForm, "/api/imports/csv", { method: "POST", bodyBuilder: data => {
  const fd = new FormData();
  fd.append("accountId", data.accountId);
  fd.append("file", els.importForm.file.files[0]);
  return fd;
}});
bindForm(els.goalForm, "/api/planning/goals", { method: "POST" });
bindForm(els.budgetForm, "/api/planning/budgets", { method: "POST" });
bindForm(els.profileForm, "/api/users/profile", { method: "PUT" });
bindForm(els.preferencesForm, "/api/users/preferences", { method: "PUT" });
bindForm(els.categoryForm, "/api/categories", { bodyBuilder: d => d });
bindForm(els.counterpartyForm, "/api/counterparties", { bodyBuilder: d => d });
bindForm(els.tagForm, "/api/tags", { bodyBuilder: d => d });
bindForm(els.budgetForm, "/api/planning/budgets", { bodyBuilder: d => ({...d, categoryId: Number(d.categoryId), amountLimit: Number(d.amountLimit)}) });
bindForm(els.loanForm, "/api/loans", { method: "POST", bodyBuilder: d => ({
  ...d, 
  principalAmount: Number(d.principalAmount), 
  interestRate: d.interestRate ? Number(d.interestRate) : null,
  counterpartyId: d.counterpartyId ? Number(d.counterpartyId) : null
}) });

els.transactionForm.addEventListener("submit", async e => {
  e.preventDefault();
  const btn = document.getElementById("btnSaveTx");
  toggleButtonLoading(btn, true);
  try {
    const data = serialize(els.transactionForm);
    const payload = {
      accountId: Number(data.accountId),
      destinationAccountId: data.destinationAccountId ? Number(data.destinationAccountId) : null,
      type: data.type, amount: Number(data.amount), transactionDate: data.transactionDate,
      description: data.description, categoryId: data.categoryId ? Number(data.categoryId) : null,
      counterpartyId: data.counterpartyId ? Number(data.counterpartyId) : null,
      status: data.status
    };
    
    let url = state.editingTxId ? `/api/transactions/${state.editingTxId}` : "/api/transactions";
    let method = state.editingTxId ? "PUT" : "POST";
    
    await api(url, { method, body: JSON.stringify(payload) });
    resetTxForm();
    els.transactionForm.transactionDate.valueAsDate = new Date();
    await loadDashboard();
    showToast("Lançamento salvo!", false);
  } catch(e) { showToast(e.message, true); } finally { toggleButtonLoading(btn, false); }
});

els.importForm.addEventListener("submit", async e => {
  e.preventDefault();
  const btn = els.importForm.querySelector('button[type="submit"]');
  toggleButtonLoading(btn, true);
  try { await api("/api/imports/csv", { method: "POST", body: new FormData(els.importForm) }); els.importForm.reset(); await loadDashboard(); showToast("Enviado!", false); } 
  catch (err) { showToast(err.message, true); } finally { toggleButtonLoading(btn, false); }
});

// Filters
els.searchInput.addEventListener("input", e => { state.filters.search = e.target.value; renderTransactions(); });
els.filterType?.addEventListener('change', e => { state.filters.type = e.target.value; loadDashboard(); });
els.filterAccount?.addEventListener('change', e => { state.filters.accountId = e.target.value; loadDashboard(); });
document.getElementById('filterStatus')?.addEventListener('change', e => { state.filters.status = e.target.value; loadDashboard(); });
document.getElementById('filterStartDate')?.addEventListener('change', e => { state.filters.startDate = e.target.value; loadDashboard(); });
document.getElementById('filterEndDate')?.addEventListener('change', e => { state.filters.endDate = e.target.value; loadDashboard(); });
document.getElementById('btnPrevPage')?.addEventListener('click', () => { if(state.filters.page > 0) { state.filters.page--; loadDashboard(); } });
document.getElementById('btnNextPage')?.addEventListener('click', () => { if(state.txPageInfo && state.filters.page < state.txPageInfo.totalPages - 1) { state.filters.page++; loadDashboard(); } });
document.getElementById('btnExportCsv')?.addEventListener('click', () => {
  if (!state.transactions || state.transactions.length === 0) return showToast("Nenhum dado para exportar.", true);
  
  const headers = "ID,Data,Descrição,Conta,Categoria,Tipo,Status,Valor\n";
  const rows = state.transactions.map(tx => {
    return [
      tx.id,
      tx.transactionDate,
      `"${(tx.description || '').replace(/"/g, '""')}"`,
      `"${(tx.accountName || '').replace(/"/g, '""')}"`,
      `"${(tx.categoryName || '').replace(/"/g, '""')}"`,
      tx.type,
      tx.status,
      tx.amount
    ].join(",");
  }).join("\n");
  
  const blob = new Blob([headers + rows], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.setAttribute("href", url);
  link.setAttribute("download", `extrato_${new Date().toISOString().split('T')[0]}.csv`);
  link.style.visibility = 'hidden';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
});
els.searchInput?.addEventListener('keyup', e => { 
  clearTimeout(state.searchTimer);
  state.searchTimer = setTimeout(() => { state.filters.search = e.target.value; loadDashboard(); }, 500);
});

// Keyboard Shortcuts
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') closeModal();
  if (e.altKey && e.key === 'n') {
    e.preventDefault();
    document.querySelector('a[href="#ledger"]').click();
    setTimeout(() => els.transactionForm.amount.focus(), 100);
  }
  if (e.altKey && e.key === 's') {
    e.preventDefault();
    els.searchInput.focus();
  }
});

els.logoutButton.addEventListener("click", clearSession);

function escapeHtml(val) { return String(val ?? "").replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' })[m]); }
function labelTxType(t) { return { INCOME: "Receita", EXPENSE: "Despesa", TRANSFER: "Transfer.", ADJUSTMENT: "Ajuste" }[t] || t; }

renderShell();
if(els.transactionForm) els.transactionForm.transactionDate.valueAsDate = new Date();
if(state.token) loadDashboard().catch(e => { showToast(e.message, true); clearSession(); });

// ==========================================
// AI ASSISTANT (OLLAMA)
// ==========================================
function addAiMessage(text, isUser = false) {
  const div = document.createElement("div");
  div.className = `ai-message ${isUser ? 'user-message' : 'system-message'}`;
  
  let formattedText;
  if (isUser) {
    formattedText = escapeHtml(text);
  } else {
    // Parse markdown-like formatting for AI responses
    formattedText = escapeHtml(text)
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/^### (.+)$/gm, '<h4 style="margin:0.5rem 0 0.25rem;color:var(--primary)">$1</h4>')
      .replace(/^## (.+)$/gm, '<h3 style="margin:0.75rem 0 0.25rem;color:var(--primary)">$1</h3>')
      .replace(/^(\d+)\. (.+)$/gm, '<div style="margin-left:1rem;padding:0.15rem 0">$1. $2</div>')
      .replace(/^- (.+)$/gm, '<div style="margin-left:1rem;padding:0.15rem 0">• $1</div>')
      .replace(/\n/g, '<br>');

    // Handle <think> tags (reasoning models) — should already be cleaned by backend
    if (formattedText.includes('&lt;think&gt;')) {
      formattedText = formattedText.replace(/&lt;think&gt;([\s\S]*?)&lt;\/think&gt;/g, 
        '<details class="ai-thought"><summary>Raciocínio da IA</summary><div class="thought-content">$1</div></details>');
    }
  }

  div.innerHTML = `<strong>${isUser ? 'Você' : 'FinOS Advisor'}:</strong> ${formattedText}`;
  els.aiChatContainer.appendChild(div);
  els.aiChatContainer.scrollTop = els.aiChatContainer.scrollHeight;
}

async function generateAiInsights() {
  const btn = document.getElementById("btnAiInsights");
  toggleButtonLoading(btn, true);
  addAiMessage("Gerando relatório geral com base no seu mês atual...", true);
  
  const loadingDiv = document.createElement("div");
  loadingDiv.className = "ai-message system-message";
  loadingDiv.innerHTML = `<strong>FinOS Advisor:</strong> <span class="ai-typing">Analisando dados...</span>`;
  els.aiChatContainer.appendChild(loadingDiv);
  els.aiChatContainer.scrollTop = els.aiChatContainer.scrollHeight;

  try {
    const res = await api("/api/ai/insights");
    loadingDiv.remove();
    addAiMessage(res.data?.response || "Relatório concluído.", false);
  } catch (err) {
    loadingDiv.remove();
    addAiMessage(`❌ ${err.message}`, false);
  } finally {
    toggleButtonLoading(btn, false);
  }
}

window.askAiQuestion = async function(presetQuestion = null) {
  const input = els.aiChatInput;
  const question = presetQuestion || input.value.trim();
  if (!question) return;

  if (!presetQuestion) input.value = '';
  addAiMessage(question, true);
  
  const btn = els.btnAiSend;
  toggleButtonLoading(btn, true);

  const loadingDiv = document.createElement("div");
  loadingDiv.className = "ai-message system-message";
  loadingDiv.innerHTML = `<strong>FinOS Advisor:</strong> <span class="ai-typing">Pensando...</span>`;
  els.aiChatContainer.appendChild(loadingDiv);
  els.aiChatContainer.scrollTop = els.aiChatContainer.scrollHeight;

  try {
    const res = await api("/api/ai/chat", {
      method: "POST",
      body: JSON.stringify({ question })
    });
    loadingDiv.remove();
    addAiMessage(res.data?.response || "Concluído.", false);
  } catch (err) {
    loadingDiv.remove();
    addAiMessage(`❌ ${err.message}`, false);
  } finally {
    toggleButtonLoading(btn, false);
  }
}

document.getElementById('aiChatForm')?.addEventListener('submit', function(e) {
  e.preventDefault();
  askAiQuestion();
});

// ==========================================
// INTERACTIVE TUTORIAL SYSTEM
// ==========================================
(function() {
  const TUTORIAL_STEPS = [
    {
      target: null,
      section: null,
      title: "🎉 Bem-vindo ao FinOS!",
      desc: "Este tutorial interativo vai te ensinar a usar o sistema na prática. Clique em 'Começar' e faça o que for pedido na tela. Vamos começar?",
      tip: "Você pode fechar e continuar o tutorial a qualquer momento pela barra lateral."
    },
    {
      target: ".sidebar-nav a[href='#accounts']",
      section: null,
      title: "1. Acesse suas Contas",
      desc: "Primeiro, clique em 'Minhas Contas' no menu lateral para cadastrar suas contas bancárias.",
      waitForEvent: "hashchange_accounts"
    },
    {
      target: "#accountForm",
      section: "accounts",
      title: "2. Crie uma Conta",
      desc: "Vamos criar sua primeira conta. Preencha os campos 'Nome' (ex: Santander), selecione 'Conta Corrente' e clique em Salvar Conta.",
      waitForApi: "/api/accounts"
    },
    {
      target: ".sidebar-nav a[href='#ledger']",
      section: "accounts",
      title: "3. Registre uma Movimentação",
      desc: "Ótimo! Agora que você tem uma conta, clique em 'Receitas e Despesas' no menu lateral para registrar uma entrada de dinheiro.",
      waitForEvent: "hashchange_ledger"
    },
    {
      target: "#transactionForm",
      section: "ledger",
      title: "4. Registre seu primeiro dinheiro",
      desc: "Preencha o formulário para adicionar dinheiro à sua conta. Coloque o valor de R$ 1000,00, selecione sua conta recém-criada e clique em Salvar.",
      waitForApi: "/api/transactions"
    },
    {
      target: ".sidebar-nav a[href='#overview']",
      section: "ledger",
      title: "5. Veja o Painel",
      desc: "Perfeito! Agora clique em 'Visão Geral' para ver o seu painel de indicadores atualizado com o seu novo saldo.",
      waitForEvent: "hashchange_overview"
    },
    {
      target: "#section-overview .metrics-grid",
      section: "overview",
      title: "🚀 Tudo Certo!",
      desc: "Parabéns! Você já sabe o básico do FinOS. Seu saldo já está atualizado. Continue explorando: registre despesas, crie metas de economia, importe extratos bancários, ou pergunte ao Assistente IA.",
      tip: "Lembre-se: use Alt+N de qualquer lugar para criar um novo lançamento rápido!"
    }
  ];

  const overlay = document.getElementById("tutorialOverlay");
  const highlight = document.getElementById("tutorialHighlight");
  const tooltip = document.getElementById("tutorialTooltip");
  const titleEl = document.getElementById("tutorialTitle");
  const descEl = document.getElementById("tutorialDesc");
  const tipEl = document.getElementById("tutorialTip");
  const badgeEl = document.getElementById("tutorialStepBadge");
  const progressBar = document.getElementById("tutorialProgressBar");
  const prevBtn = document.getElementById("tutorialPrevBtn");
  const nextBtn = document.getElementById("tutorialNextBtn");
  const closeBtn = document.getElementById("tutorialCloseBtn");
  const startBtn = document.getElementById("btnStartTutorial");
  const backdrop = document.getElementById("tutorialBackdrop");

  let currentStep = 0;

  function startTutorial() {
    currentStep = 0;
    overlay.classList.remove("hidden");
    document.body.style.overflow = "hidden";
    renderStep();
  }

  function endTutorial() {
    overlay.classList.add("hidden");
    document.body.style.overflow = "";
    highlight.style.display = "none";
    localStorage.setItem("finos_tutorial_seen", "true");
  }

  function navigateToSection(sectionName) {
    if (!sectionName) return;
    const navLink = document.querySelector(`a.nav-item[href="#${sectionName}"]`);
    if (navLink) {
      document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
      navLink.classList.add('active');
      document.querySelectorAll('.view-section').forEach(s => s.classList.add('hidden'));
      const sectionEl = document.getElementById('section-' + sectionName);
      if (sectionEl) sectionEl.classList.remove('hidden');
      const pageTitleEl = document.getElementById('pageTitle');
      if (pageTitleEl) pageTitleEl.textContent = navLink.textContent.trim();
    }
  }

  function checkStepCondition() {
    const step = TUTORIAL_STEPS[currentStep];
    if (!step) return;
    // Always show the next button so users can skip interactive parts
    nextBtn.style.display = "block";
  }

  function renderStep() {
    const step = TUTORIAL_STEPS[currentStep];
    const total = TUTORIAL_STEPS.length;

    // Update text
    titleEl.textContent = step.title;
    descEl.textContent = step.desc;
    badgeEl.textContent = `${currentStep + 1} / ${total}`;
    progressBar.style.width = `${((currentStep + 1) / total) * 100}%`;

    // Tip
    if (step.tip) {
      tipEl.textContent = step.tip;
      tipEl.classList.add("visible");
    } else {
      tipEl.classList.remove("visible");
    }

    // Buttons
    prevBtn.style.visibility = currentStep === 0 ? "hidden" : "visible";
    nextBtn.textContent = currentStep === total - 1 ? "Concluir ✓" : (currentStep === 0 ? "Começar" : "Próximo →");
    checkStepCondition();

    // Navigate to section automatically to support "Next" skipping
    if (step.section) {
      navigateToSection(step.section);
    }

    // Highlight target element
    if (step.target) {
      const targetEl = document.querySelector(step.target);
      if (targetEl) {
        targetEl.scrollIntoView({ behavior: "smooth", block: "center" });
        setTimeout(() => positionHighlightAndTooltip(targetEl), 350);
        
        // Add a pulsing glow class to the target element to encourage clicking
        document.querySelectorAll('.tutorial-pulse').forEach(e => e.classList.remove('tutorial-pulse'));
        targetEl.classList.add('tutorial-pulse');
      } else {
        showCentered();
      }
    } else {
      showCentered();
    }
  }

  function positionHighlightAndTooltip(targetEl) {
    const rect = targetEl.getBoundingClientRect();
    const pad = 10;
    highlight.style.display = "block";
    highlight.style.top = (rect.top - pad) + "px";
    highlight.style.left = (rect.left - pad) + "px";
    highlight.style.width = (rect.width + pad * 2) + "px";
    highlight.style.height = (rect.height + pad * 2) + "px";

    const tooltipWidth = Math.min(420, window.innerWidth - 32);
    const tooltipHeight = tooltip.offsetHeight || 280;
    const vw = window.innerWidth;
    const vh = window.innerHeight;

    let top, left;
    if (rect.bottom + pad + tooltipHeight + 20 < vh) top = rect.bottom + pad + 16;
    else if (rect.top - pad - tooltipHeight - 16 > 0) top = rect.top - pad - tooltipHeight - 16;
    else top = Math.max(16, (vh - tooltipHeight) / 2);

    left = rect.left + (rect.width / 2) - (tooltipWidth / 2);
    left = Math.max(16, Math.min(left, vw - tooltipWidth - 16));

    tooltip.style.top = top + "px";
    tooltip.style.left = left + "px";
    tooltip.style.animation = "none";
    tooltip.offsetHeight;
    tooltip.style.animation = "tutorialSlideIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)";
  }

  function showCentered() {
    highlight.style.display = "none";
    document.querySelectorAll('.tutorial-pulse').forEach(e => e.classList.remove('tutorial-pulse'));
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    const tooltipWidth = Math.min(420, vw - 32);
    const tooltipHeight = tooltip.offsetHeight || 300;

    tooltip.style.top = Math.max(60, (vh - tooltipHeight) / 2) + "px";
    tooltip.style.left = Math.max(16, (vw - tooltipWidth) / 2) + "px";
    tooltip.style.animation = "none";
    tooltip.offsetHeight;
    tooltip.style.animation = "tutorialSlideIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)";
  }

  function goNext() {
    if (currentStep < TUTORIAL_STEPS.length - 1) {
      currentStep++;
      renderStep();
    } else {
      endTutorial();
      showToast("Tutorial concluído! 🎓 Agora explore o sistema à vontade.", false);
    }
  }

  function goPrev() {
    if (currentStep > 0) {
      currentStep--;
      renderStep();
    }
  }

  // Event listeners
  if (startBtn) startBtn.addEventListener("click", startTutorial);
  if (nextBtn) nextBtn.addEventListener("click", goNext);
  if (prevBtn) prevBtn.addEventListener("click", goPrev);
  if (closeBtn) closeBtn.addEventListener("click", endTutorial);

  // Allow clicking outside highlight to always close the tutorial
  if (backdrop) backdrop.addEventListener("click", () => {
    endTutorial();
  });

  // Handle Event-Driven progression
  window.addEventListener("hashchange", () => {
    if (overlay.classList.contains("hidden")) return;
    const step = TUTORIAL_STEPS[currentStep];
    const hash = window.location.hash.substring(1);
    if (step && step.waitForEvent === "hashchange_" + hash) {
       goNext();
    }
  });

  window.addEventListener("finos_api_action", (e) => {
    if (overlay.classList.contains("hidden")) return;
    const step = TUTORIAL_STEPS[currentStep];
    if (step && step.waitForApi && e.detail.method === "POST" && e.detail.path.includes(step.waitForApi)) {
       goNext();
    }
  });

  document.addEventListener("keydown", e => {
    if (overlay.classList.contains("hidden")) return;
    const step = TUTORIAL_STEPS[currentStep];
    if (e.key === "Escape") endTutorial();
    // Only allow keyboard next if no waiting condition
    if ((e.key === "ArrowRight" || e.key === "Enter") && (!step.waitForEvent && !step.waitForApi)) goNext();
    if (e.key === "ArrowLeft") goPrev();
  });

  window.startTutorial = startTutorial;
})();

// --- AI HITL Review Logic ---
let aiCategories = [];

window.loadAiSuggestions = async function() {
  const tableBody = document.getElementById("aiSuggestionsTableBody");
  if (!tableBody) return;
  tableBody.innerHTML = "<tr><td colspan='3' style='text-align: center;'>Carregando sugestões...</td></tr>";

  try {
    // Fetch categories first if not loaded
    if (aiCategories.length === 0) {
      const catRes = await api("/api/categories");
      if (catRes.data) aiCategories = catRes.data;
    }

    const res = await api("/api/ai/suggestions");
    if (res.data && res.data.length > 0) {
      tableBody.innerHTML = res.data.map(s => {
        // Format amount
        const amountFormatted = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(s.sampleAmount || 0);
        // Format date
        let dateFormatted = "";
        if (s.sampleDate) {
          const d = new Date(s.sampleDate + 'T00:00:00');
          dateFormatted = d.toLocaleDateString('pt-BR');
        }

        // Category options
        const optionsHtml = aiCategories.map(c => 
          `<option value="${c.id}" ${c.id === s.suggestedCategoryId ? 'selected' : ''}>${c.name}</option>`
        ).join("");

        return `
        <tr id="ai-row-${s.id}">
          <td>
            <div style="font-weight: 500;">${s.sampleRawDescription || s.keyword}</div>
            <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.2rem;">
              <span style="display: inline-block; padding: 2px 6px; background: var(--surface-light); border-radius: 4px; margin-right: 6px;">${dateFormatted}</span>
              <span style="${s.sampleAmount < 0 ? 'color: var(--danger);' : 'color: var(--success);'}">${amountFormatted}</span>
            </div>
          </td>
          <td>
            <div id="ai-display-${s.id}">
              <span class="badge" style="background: var(--surface-light); font-size: 0.85rem;">${s.suggestedCategoryName}</span>
            </div>
            <div id="ai-edit-${s.id}" class="hidden" style="display: flex; flex-direction: column; gap: 0.5rem;">
              <select id="ai-select-${s.id}" class="input-field" style="padding: 0.3rem; font-size: 0.85rem;">
                ${optionsHtml}
              </select>
            </div>
          </td>
          <td>
            <div id="ai-actions-${s.id}" style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
              <button class="btn-primary btn-sm" onclick="resolveAiSuggestion('${s.id}', true)">✅ Aprovar</button>
              <button class="btn-secondary btn-sm" onclick="toggleAiEdit('${s.id}')">✏️ Corrigir</button>
              <button class="btn-secondary btn-sm" onclick="resolveAiSuggestion('${s.id}', false)" style="color: var(--danger);">❌ Rejeitar</button>
            </div>
            <div id="ai-save-actions-${s.id}" class="hidden" style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
              <button class="btn-primary btn-sm" onclick="saveAiCorrection('${s.id}')">Salvar Correção</button>
              <button class="btn-secondary btn-sm" onclick="toggleAiEdit('${s.id}')">Cancelar</button>
            </div>
          </td>
        </tr>
      `}).join("");
      
      document.getElementById("btnApproveAllAi").classList.remove("hidden");
    } else {
      tableBody.innerHTML = `
        <tr>
          <td colspan="3" style="text-align: center; padding: 3rem 1rem;">
            <div style="font-size: 2.5rem; margin-bottom: 1rem;">✨</div>
            <h3 style="color: var(--text); margin-bottom: 0.5rem;">Tudo limpo!</h3>
            <p style="color: var(--text-muted);">Nenhuma sugestão da IA pendente no momento.</p>
          </td>
        </tr>`;
      document.getElementById("btnApproveAllAi").classList.add("hidden");
    }
  } catch (err) {
    showToast("Erro ao carregar sugestões da IA", true);
  }
};

window.toggleAiEdit = function(id) {
  document.getElementById(`ai-display-${id}`).classList.toggle("hidden");
  document.getElementById(`ai-edit-${id}`).classList.toggle("hidden");
  document.getElementById(`ai-actions-${id}`).classList.toggle("hidden");
  document.getElementById(`ai-save-actions-${id}`).classList.toggle("hidden");
};

window.saveAiCorrection = async function(id) {
  const select = document.getElementById(`ai-select-${id}`);
  const overrideCategoryId = select.value;
  if (!overrideCategoryId) return;
  
  await resolveAiSuggestion(id, true, parseInt(overrideCategoryId));
};

window.resolveAiSuggestion = async function(id, approved, overrideCategoryId = null) {
  try {
    const body = { approved };
    if (overrideCategoryId) {
      body.overrideCategoryId = overrideCategoryId;
    }
    
    await api(`/api/ai/suggestions/${id}/resolve`, {
      method: "POST",
      body: JSON.stringify(body)
    });
    showToast(`Sugestão ${approved ? (overrideCategoryId ? 'corrigida ✅' : 'aprovada ✅') : 'rejeitada ❌'} com sucesso!`);
    loadAiSuggestions();
  } catch (err) {
    showToast("Erro ao resolver sugestão", true);
  }
};

window.resolveAllAiSuggestions = async function() {
  const rows = document.querySelectorAll("#aiSuggestionsTableBody tr[id^='ai-row-']");
  if (rows.length === 0) return;
  
  if (!confirm(`Tem certeza que deseja aprovar as ${rows.length} sugestões pendentes?`)) return;
  
  showToast("Aprovando sugestões...", false);
  let successCount = 0;
  
  for (const row of rows) {
    const id = row.id.replace('ai-row-', '');
    try {
      await api(`/api/ai/suggestions/${id}/resolve`, {
        method: "POST",
        body: JSON.stringify({ approved: true })
      });
      successCount++;
    } catch (e) {
      console.error("Erro ao aprovar", id, e);
    }
  }
  
  showToast(`${successCount} sugestões aprovadas com sucesso!`);
  loadAiSuggestions();
};

// --- AI Review with Progress Overlay ---
let aiReviewTimer = null;
let aiReviewSeconds = 0;

function showAiReviewOverlay() {
  const overlay = document.getElementById("aiReviewOverlay");
  const loading = document.getElementById("aiReviewLoading");
  const result = document.getElementById("aiReviewResult");
  const statusText = document.getElementById("aiReviewStatusText");

  overlay.classList.remove("hidden");
  loading.classList.remove("hidden");
  result.classList.add("hidden");

  // Reset step icons
  document.getElementById("aiStep1Icon").textContent = "⏳";
  document.getElementById("aiStep2Icon").textContent = "⬜";
  document.getElementById("aiStep3Icon").textContent = "⬜";

  aiReviewSeconds = 0;
  statusText.textContent = "Conectando ao Ollama...";

  // Animate steps over time
  aiReviewTimer = setInterval(() => {
    aiReviewSeconds++;
    const minutes = Math.floor(aiReviewSeconds / 60);
    const secs = aiReviewSeconds % 60;
    const timeStr = minutes > 0 ? `${minutes}m ${secs}s` : `${secs}s`;

    if (aiReviewSeconds >= 3) {
      document.getElementById("aiStep1Icon").textContent = "✅";
      document.getElementById("aiStep2Icon").textContent = "⏳";
      statusText.textContent = `Analisando transações com IA local... (${timeStr})`;
    }
    if (aiReviewSeconds >= 10) {
      document.getElementById("aiStep2Icon").textContent = "⏳";
      statusText.textContent = `Processando padrões de categorização... (${timeStr})`;
    }
    if (aiReviewSeconds >= 30) {
      statusText.textContent = `Modelo processando (isso é normal para IA local)... (${timeStr})`;
    }
    if (aiReviewSeconds >= 120) {
      statusText.textContent = `Quase lá... o modelo está finalizando a análise (${timeStr})`;
    }
  }, 1000);
}

function showAiReviewResult(success, summary, errorMsg) {
  if (aiReviewTimer) {
    clearInterval(aiReviewTimer);
    aiReviewTimer = null;
  }

  const loading = document.getElementById("aiReviewLoading");
  const result = document.getElementById("aiReviewResult");
  const icon = document.getElementById("aiReviewResultIcon");
  const title = document.getElementById("aiReviewResultTitle");
  const msg = document.getElementById("aiReviewResultMsg");
  const details = document.getElementById("aiReviewResultDetails");

  // Complete all steps
  document.getElementById("aiStep1Icon").textContent = "✅";
  document.getElementById("aiStep2Icon").textContent = "✅";
  document.getElementById("aiStep3Icon").textContent = "✅";

  setTimeout(() => {
    loading.classList.add("hidden");
    result.classList.remove("hidden");

    if (!success) {
      icon.textContent = "❌";
      title.textContent = "Erro na Revisão";
      msg.textContent = errorMsg || "Ocorreu um erro ao processar a revisão inteligente.";
      details.innerHTML = `
        <p style="color: var(--text-muted);">Verifique se o Ollama está rodando e se o modelo <strong>llama3:8b</strong> está instalado.</p>
        <p style="margin-top: 0.5rem; font-size: 0.85rem; color: var(--text-muted);">Execute: <code>ollama pull llama3:8b</code></p>
      `;
      return;
    }

    if (summary) {
      const status = summary.status || "NO_SUGGESTIONS";
      icon.textContent = status === "SUCCESS" ? "🎉" : status === "PARTIAL" ? "⚡" : "🔍";
      title.textContent = status === "SUCCESS" ? "Revisão Concluída com Sucesso!" :
                           status === "PARTIAL" ? "Revisão Parcial" : "Nenhuma Correção Necessária";
      msg.textContent = summary.message || "";

      details.innerHTML = `
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem;">
          <div style="padding: 0.5rem; background: var(--surface); border-radius: 6px; text-align: center;">
            <div style="font-size: 1.5rem; font-weight: 700; color: var(--primary);">${summary.totalTransactions || 0}</div>
            <div style="font-size: 0.8rem; color: var(--text-muted);">Transações Analisadas</div>
          </div>
          <div style="padding: 0.5rem; background: var(--surface); border-radius: 6px; text-align: center;">
            <div style="font-size: 1.5rem; font-weight: 700; color: var(--success);">${summary.suggestionsCreated || 0}</div>
            <div style="font-size: 0.8rem; color: var(--text-muted);">Regras Sugeridas</div>
          </div>
          <div style="padding: 0.5rem; background: var(--surface); border-radius: 6px; text-align: center;">
            <div style="font-size: 1.5rem; font-weight: 700; color: var(--warning, #f59e0b);">${summary.transactionsUpdated || 0}</div>
            <div style="font-size: 0.8rem; color: var(--text-muted);">Transações Atualizadas</div>
          </div>
          <div style="padding: 0.5rem; background: var(--surface); border-radius: 6px; text-align: center;">
            <div style="font-size: 1.5rem; font-weight: 700; color: ${summary.categoriesNotFound > 0 ? 'var(--danger)' : 'var(--text-muted)'};">${summary.categoriesNotFound || 0}</div>
            <div style="font-size: 0.8rem; color: var(--text-muted);">Categorias Não Encontradas</div>
          </div>
        </div>
        ${summary.categoriesNotFound > 0 ? '<p style="margin-top: 0.75rem; font-size: 0.85rem; color: var(--danger);">⚠️ Algumas categorias sugeridas pela IA não existem no seu cadastro. Crie-as em Configurações → Categorias e tente novamente.</p>' : ''}
        ${summary.suggestionsCreated > 0 ? '<p style="margin-top: 0.75rem; font-size: 0.85rem; color: var(--success);">✅ Clique em "Ver Sugestões da IA" para aprovar as regras de aprendizado!</p>' : ''}
      `;
    }
  }, 600);
}

window.closeAiReview = function(goToSuggestions) {
  document.getElementById("aiReviewOverlay").classList.add("hidden");
  if (goToSuggestions) {
    window.location.hash = "#ai";
    setTimeout(() => loadAiSuggestions(), 300);
  }
};

window.reviewBatchWithAi = async function(batchId) {
  showAiReviewOverlay();
  try {
    const res = await api(`/api/ai/review-batch/${batchId}`, { method: "POST" });
    if (res.success && res.data) {
      showAiReviewResult(true, res.data, null);
    } else {
      showAiReviewResult(false, null, res.message || "Erro desconhecido");
    }
  } catch (err) {
    showAiReviewResult(false, null, err.message || "Erro ao conectar com o servidor");
  }
};

// Auto-load suggestions if AI tab is opened
document.querySelector('a[href="#ai"]').addEventListener('click', () => loadAiSuggestions());

