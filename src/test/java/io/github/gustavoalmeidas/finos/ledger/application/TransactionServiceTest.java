package io.github.gustavoalmeidas.finos.ledger.application;

import io.github.gustavoalmeidas.finos.identity.application.UserService;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Account;
import io.github.gustavoalmeidas.finos.ledger.domain.AccountType;
import io.github.gustavoalmeidas.finos.ledger.domain.Category;
import io.github.gustavoalmeidas.finos.ledger.domain.CategoryType;
import io.github.gustavoalmeidas.finos.ledger.domain.RecurrenceFrequency;
import io.github.gustavoalmeidas.finos.ledger.domain.Transaction;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionSource;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionStatus;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;
import io.github.gustavoalmeidas.finos.ledger.dto.CreateTransactionRequest;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.AccountRepository;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.CounterpartyRepository;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.TagRepository;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.TransactionRepository;
import io.github.gustavoalmeidas.finos.ledger.mapper.LedgerMapper;
import io.github.gustavoalmeidas.finos.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private AccountService accountService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CounterpartyRepository counterpartyRepository;

    @Mock
    private TagRepository tagRepository;

    private final LedgerMapper mapper = new LedgerMapper();

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(
                userService,
                accountService,
                categoryService,
                transactionRepository,
                accountRepository,
                counterpartyRepository,
                tagRepository,
                mapper
        );
    }

    @Test
    void createIncomeAddsAmountToAccountBalance() {
        User user = user();
        Account account = account(10L, user, "100.00");
        Category category = category(20L, user, CategoryType.INCOME);
        when(userService.currentUser()).thenReturn(user);
        when(accountService.getOwnedEntity(10L)).thenReturn(account);
        when(categoryService.getOwnedEntity(20L)).thenReturn(category);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(30L);
            return transaction;
        });

        service.create(request(10L, null, TransactionType.INCOME, "50.00", 20L, null, null));

        assertThat(account.getCurrentBalance()).isEqualByComparingTo("150.00");
        verify(accountRepository).save(account);
    }

    @Test
    void createExpenseRequiresCategoryOrSplits() {
        User user = user();
        Account account = account(10L, user, "100.00");
        when(userService.currentUser()).thenReturn(user);
        when(accountService.getOwnedEntity(10L)).thenReturn(account);

        assertThatThrownBy(() -> service.create(request(10L, null, TransactionType.EXPENSE, "25.00", null, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createTransferMovesBalanceBetweenDifferentAccounts() {
        User user = user();
        Account origin = account(10L, user, "100.00");
        Account destination = account(11L, user, "20.00");
        when(userService.currentUser()).thenReturn(user);
        when(accountService.getOwnedEntity(10L)).thenReturn(origin);
        when(accountService.getOwnedEntity(11L)).thenReturn(destination);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(30L);
            return transaction;
        });

        service.create(request(10L, 11L, TransactionType.TRANSFER, "40.00", null, null, null));

        assertThat(origin.getCurrentBalance()).isEqualByComparingTo("60.00");
        assertThat(destination.getCurrentBalance()).isEqualByComparingTo("60.00");
        verify(accountRepository).save(origin);
        verify(accountRepository).save(destination);
    }

    @Test
    void createTransferRejectsSameOriginAndDestinationAccount() {
        User user = user();
        Account origin = account(10L, user, "100.00");
        when(userService.currentUser()).thenReturn(user);
        when(accountService.getOwnedEntity(10L)).thenReturn(origin);

        assertThatThrownBy(() -> service.create(request(10L, 10L, TransactionType.TRANSFER, "40.00", null, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateFromPendingToPostedCreatesNextRecurringTransaction() {
        User user = user();
        Account account = account(10L, user, "100.00");
        Category category = category(20L, user, CategoryType.EXPENSE);
        Transaction existing = transaction(30L, user, account, category, TransactionStatus.PENDING);
        when(userService.currentUser()).thenReturn(user);
        when(transactionRepository.findByIdAndUser(30L, user)).thenReturn(Optional.of(existing));
        when(accountService.getOwnedEntity(10L)).thenReturn(account);
        when(categoryService.getOwnedEntity(20L)).thenReturn(category);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(30L, request(
                10L,
                null,
                TransactionType.EXPENSE,
                "25.00",
                20L,
                TransactionStatus.POSTED,
                RecurrenceFrequency.MONTHLY
        ));

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, org.mockito.Mockito.times(2)).save(transactionCaptor.capture());
        List<Transaction> savedTransactions = transactionCaptor.getAllValues();
        Transaction recurring = savedTransactions.get(1);

        assertThat(recurring.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(recurring.getSource()).isEqualTo(TransactionSource.SYSTEM);
        assertThat(recurring.getTransactionDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(recurring.getOriginalTransactionId()).isEqualTo(30L);
    }

    private static CreateTransactionRequest request(
            Long accountId,
            Long destinationAccountId,
            TransactionType type,
            String amount,
            Long categoryId,
            TransactionStatus status,
            RecurrenceFrequency recurrenceFrequency
    ) {
        return new CreateTransactionRequest(
                accountId,
                destinationAccountId,
                type,
                status,
                null,
                new BigDecimal(amount),
                LocalDate.of(2026, 4, 10),
                "Lancamento",
                null,
                categoryId,
                null,
                null,
                null,
                null,
                recurrenceFrequency,
                null,
                null,
                null
        );
    }

    private static User user() {
        User user = new User();
        user.setId(1L);
        user.setUsername("gustavo");
        user.setEmail("gustavo@example.com");
        return user;
    }

    private static Account account(Long id, User user, String currentBalance) {
        Account account = new Account();
        account.setId(id);
        account.setUser(user);
        account.setName("Conta " + id);
        account.setType(AccountType.CHECKING);
        account.setCurrency("BRL");
        account.setInitialBalance(BigDecimal.ZERO);
        account.setCurrentBalance(new BigDecimal(currentBalance));
        return account;
    }

    private static Category category(Long id, User user, CategoryType type) {
        Category category = new Category();
        category.setId(id);
        category.setUser(user);
        category.setName("Categoria " + id);
        category.setType(type);
        category.setColor("#386b8f");
        return category;
    }

    private static Transaction transaction(Long id, User user, Account account, Category category, TransactionStatus status) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setStatus(status);
        transaction.setSource(TransactionSource.MANUAL);
        transaction.setAmount(new BigDecimal("25.00"));
        transaction.setTransactionDate(LocalDate.of(2026, 4, 10));
        transaction.setDescription("Lancamento");
        transaction.setCategory(category);
        transaction.setRecurrenceFrequency(RecurrenceFrequency.MONTHLY);
        return transaction;
    }
}
