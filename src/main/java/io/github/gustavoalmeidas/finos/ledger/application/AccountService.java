package io.github.gustavoalmeidas.finos.ledger.application;

import io.github.gustavoalmeidas.finos.identity.application.UserService;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Account;
import io.github.gustavoalmeidas.finos.ledger.dto.AccountResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.CreateAccountRequest;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.AccountRepository;
import io.github.gustavoalmeidas.finos.ledger.mapper.LedgerMapper;
import io.github.gustavoalmeidas.finos.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserService userService;
    private final AccountRepository accountRepository;
    private final LedgerMapper mapper;

    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        User user = userService.currentUser();
        return accountRepository.findByUserOrderByNameAsc(user)
                .stream()
                .map(mapper::toAccountResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Account getOwnedEntity(Long id) {
        User user = userService.currentUser();
        return accountRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("ledger.account.not-found", "Conta não encontrada."));
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Long id) {
        return mapper.toAccountResponse(getOwnedEntity(id));
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        User user = userService.currentUser();
        Account account = new Account();
        account.setUser(user);
        applyRequest(account, request);
        BigDecimal openingBalance = request.initialBalance() == null ? BigDecimal.ZERO : request.initialBalance();
        account.setInitialBalance(openingBalance);
        account.setCurrentBalance(openingBalance);
        return mapper.toAccountResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse update(Long id, CreateAccountRequest request) {
        Account account = getOwnedEntity(id);
        BigDecimal previousInitialBalance = account.getInitialBalance();
        applyRequest(account, request);
        if (request.initialBalance() != null && previousInitialBalance.compareTo(request.initialBalance()) != 0) {
            BigDecimal difference = request.initialBalance().subtract(previousInitialBalance);
            account.setInitialBalance(request.initialBalance());
            account.setCurrentBalance(account.getCurrentBalance().add(difference));
        }
        return mapper.toAccountResponse(accountRepository.save(account));
    }

    @Transactional
    public void delete(Long id) {
        Account account = getOwnedEntity(id);
        account.setActive(false);
        account.setDeletedAt(java.time.LocalDateTime.now());
        accountRepository.save(account);
    }

    private void applyRequest(Account account, CreateAccountRequest request) {
        account.setName(request.name());
        account.setType(request.type());
        account.setCurrency(request.currency() == null ? "BRL" : request.currency());
        account.setInstitutionName(request.institutionName());
        account.setColor(request.color());
        account.setNotes(request.notes());
    }
}
