package io.github.gustavoalmeidas.finos.ai.application;

import io.github.gustavoalmeidas.finos.ai.infrastructure.OllamaClient;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;
import io.github.gustavoalmeidas.finos.reporting.application.ReportingService;
import io.github.gustavoalmeidas.finos.reporting.dto.CashFlowMonthlyResponse;
import io.github.gustavoalmeidas.finos.reporting.dto.CategorySummaryResponse;
import io.github.gustavoalmeidas.finos.reporting.dto.DashboardSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AiAdvisorService {

    private final OllamaClient ollamaClient;
    private final ReportingService reportingService;

    private static final String SYSTEM_PROMPT = """
            Você é o FinOS Advisor, um consultor financeiro pessoal inteligente integrado ao sistema FinOS.

            REGRAS OBRIGATÓRIAS:
            1. Responda EXCLUSIVAMENTE em Português do Brasil. NUNCA use palavras em inglês.
            2. Seja objetivo e prático. Use no máximo 3 parágrafos curtos.
            3. Baseie-se APENAS nos dados financeiros fornecidos pelo usuário. Não invente valores.
            4. Use os valores exatos informados nos dados. Formate valores como R$ X.XXX,XX.
            5. Se os dados mostrarem valores zerados ou sem movimentações, diga claramente que o usuário ainda não possui dados cadastrados e sugira que comece cadastrando contas e lançamentos.
            6. Dê no máximo 2 dicas práticas e acionáveis por resposta.
            7. Mantenha um tom profissional, educado e encorajador.
            8. Não repita informações que o usuário já pode ver no painel.
            9. Foque em insights que agregam valor: tendências, riscos, oportunidades de economia.
            """;

    public String generateGeneralInsights() {
        String context = buildFinancialContext();

        if (isDataEmpty()) {
            return "📊 **Ainda não há dados financeiros cadastrados.**\n\n"
                    + "Para que eu possa gerar análises e recomendações personalizadas, comece por:\n\n"
                    + "1. **Cadastrar suas contas** (corrente, poupança, cartão) na seção \"Contas e Categorias\"\n"
                    + "2. **Registrar seus lançamentos** (receitas e despesas) na seção \"Ledger & Lançamentos\"\n\n"
                    + "Assim que tiver movimentações registradas, clique novamente em \"Gerar Relatório Geral\" para receber uma análise completa da sua saúde financeira.";
        }

        String userPrompt = "Analise meus dados financeiros abaixo e forneça um resumo da minha saúde financeira "
                + "com dicas práticas de melhoria.\n\n"
                + "MEUS DADOS FINANCEIROS:\n" + context;

        return ollamaClient.generateAdvice(SYSTEM_PROMPT, userPrompt);
    }

    public String askQuestion(String question) {
        String context = buildFinancialContext();

        if (isDataEmpty()) {
            String userPrompt = "O usuário ainda não cadastrou dados financeiros no sistema. "
                    + "Ele perguntou: \"" + question + "\"\n\n"
                    + "Responda de forma útil, mencionando que ele precisa primeiro cadastrar contas e lançamentos "
                    + "para que você possa dar conselhos personalizados. Se a pergunta for genérica sobre finanças, "
                    + "dê uma dica geral útil.";
            return ollamaClient.generateAdvice(SYSTEM_PROMPT, userPrompt);
        }

        String userPrompt = "Com base nos meus dados financeiros abaixo, responda minha pergunta.\n\n"
                + "MEUS DADOS FINANCEIROS:\n" + context + "\n\n"
                + "MINHA PERGUNTA: " + question;

        return ollamaClient.generateAdvice(SYSTEM_PROMPT, userPrompt);
    }

    private boolean isDataEmpty() {
        try {
            LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
            DashboardSummaryResponse dashboard = reportingService.getDashboardSummary(startOfMonth, endOfMonth);

            boolean allZero = isZero(dashboard.netWorth())
                    && isZero(dashboard.totalAssets())
                    && isZero(dashboard.totalDebt())
                    && isZero(dashboard.currentMonthIncome())
                    && isZero(dashboard.currentMonthExpenses());

            return allZero;
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private String buildFinancialContext() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        DashboardSummaryResponse dashboard = reportingService.getDashboardSummary(startOfMonth, endOfMonth);
        CashFlowMonthlyResponse cashflow = reportingService.getMonthlyCashFlow(YearMonth.now().minusMonths(3), YearMonth.now());
        CategorySummaryResponse expenses = reportingService.getCategorySummary(startOfMonth, endOfMonth, TransactionType.EXPENSE);

        StringBuilder context = new StringBuilder();

        context.append("RESUMO PATRIMONIAL:\n");
        context.append("- Patrimônio Líquido: R$ ").append(dashboard.netWorth()).append("\n");
        context.append("- Total em Ativos: R$ ").append(dashboard.totalAssets()).append("\n");
        context.append("- Total em Dívidas: R$ ").append(dashboard.totalDebt()).append("\n\n");

        context.append("MÊS ATUAL:\n");
        context.append("- Receita no Mês: R$ ").append(dashboard.currentMonthIncome()).append("\n");
        context.append("- Despesas no Mês: R$ ").append(dashboard.currentMonthExpenses()).append("\n");
        context.append("- Saldo Líquido no Mês: R$ ").append(dashboard.currentMonthNetCashFlow()).append("\n\n");

        if (!expenses.categories().isEmpty()) {
            context.append("DESPESAS POR CATEGORIA (MÊS ATUAL):\n");
            expenses.categories().forEach(cat -> {
                context.append("- ").append(cat.categoryName()).append(": R$ ").append(cat.totalAmount()).append("\n");
            });
            context.append("\n");
        }

        if (!cashflow.months().isEmpty()) {
            context.append("FLUXO DE CAIXA (ÚLTIMOS MESES):\n");
            cashflow.months().forEach(cf -> {
                context.append("- ").append(cf.yearMonth())
                        .append(": Receita R$ ").append(cf.income())
                        .append(", Despesa R$ ").append(cf.expenses())
                        .append(", Líquido R$ ").append(cf.net()).append("\n");
            });
        }

        return context.toString();
    }
}
