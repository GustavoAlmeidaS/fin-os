package io.github.gustavoalmeidas.finos.ledger.application;

import io.github.gustavoalmeidas.finos.identity.application.UserService;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Account;
import io.github.gustavoalmeidas.finos.ledger.domain.AccountType;
import io.github.gustavoalmeidas.finos.ledger.dto.AccountResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.CreateAccountRequest;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.AccountRepository;
import io.github.gustavoalmeidas.finos.ledger.mapper.LedgerMapper;
import io.github.gustavoalmeidas.finos.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private AccountRepository accountRepository;

    private final LedgerMapper mapper = new LedgerMapper();

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(userService, accountRepository, mapper);
    }

    @Test
    void createUsesBrlAndZeroBalanceWhenOptionalFieldsAreMissing() {
        User user = user();
        when(userService.currentUser()).thenReturn(user);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(10L);
            return account;
        });

        AccountResponse response = service.create(new CreateAccountRequest(
                "Conta Principal",
                AccountType.CHECKING,
                null,
                null,
                null,
                "#2f6f5e",
                null
        ));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.currency()).isEqualTo("BRL");
        assertThat(response.initialBalance()).isEqualByComparingTo("0");
        assertThat(response.currentBalance()).isEqualByComparingTo("0");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getUser()).isSameAs(user);
    }

    @Test
    void updateAdjustsCurrentBalanceByInitialBalanceDifference() {
        User user = user();
        Account account = account(7L, user, "100.00", "250.00");
        when(userService.currentUser()).thenReturn(user);
        when(accountRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        AccountResponse response = service.update(7L, new CreateAccountRequest(
                "Conta Principal",
                AccountType.CHECKING,
                "BRL",
                new BigDecimal("150.00"),
                "Banco",
                "#2f6f5e",
                "Notas"
        ));

        assertThat(response.initialBalance()).isEqualByComparingTo("150.00");
        assertThat(response.currentBalance()).isEqualByComparingTo("300.00");
    }

    @Test
    void getOwnedEntityThrowsWhenAccountDoesNotBelongToCurrentUser() {
        User user = user();
        when(userService.currentUser()).thenReturn(user);
        when(accountRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwnedEntity(99L))
                .isInstanceOf(NotFoundException.class);
    }

    private static User user() {
        User user = new User();
        user.setId(1L);
        user.setUsername("gustavo");
        user.setEmail("gustavo@example.com");
        return user;
    }

    private static Account account(Long id, User user, String initialBalance, String currentBalance) {
        Account account = new Account();
        account.setId(id);
        account.setUser(user);
        account.setName("Conta Principal");
        account.setType(AccountType.CHECKING);
        account.setCurrency("BRL");
        account.setInitialBalance(new BigDecimal(initialBalance));
        account.setCurrentBalance(new BigDecimal(currentBalance));
        return account;
    }
}
