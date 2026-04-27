package io.github.gustavoalmeidas.finos.ledger.application;

import io.github.gustavoalmeidas.finos.identity.application.UserService;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Account;
import io.github.gustavoalmeidas.finos.ledger.domain.Category;
import io.github.gustavoalmeidas.finos.ledger.domain.CategoryType;
import io.github.gustavoalmeidas.finos.ledger.domain.Counterparty;
import io.github.gustavoalmeidas.finos.ledger.domain.Tag;
import io.github.gustavoalmeidas.finos.ledger.domain.Transaction;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionSource;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionSplit;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionStatus;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;
import io.github.gustavoalmeidas.finos.ledger.domain.RecurrenceFrequency;
import io.github.gustavoalmeidas.finos.ledger.dto.CreateTransactionRequest;
import io.github.gustavoalmeidas.finos.ledger.dto.TransactionResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.TransactionSplitRequest;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.AccountRepository;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.CounterpartyRepository;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.TagRepository;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.TransactionRepository;
import io.github.gustavoalmeidas.finos.ledger.mapper.LedgerMapper;
import io.github.gustavoalmeidas.finos.shared.exception.BusinessException;
import io.github.gustavoalmeidas.finos.shared.exception.NotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final UserService userService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final TagRepository tagRepository;
    private final LedgerMapper mapper;

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(
            LocalDate startDate,
            LocalDate endDate,
            Long accountId,
            TransactionType type,
            Long counterpartyId,
            String tag,
            Pageable pageable
    ) {
        User user = userService.currentUser();
        return transactionRepository.findAll(byFilters(user, startDate, endDate, accountId, type, counterpartyId, tag), pageable)
                .map(mapper::toTransactionResponse);
    }

    @Transactional(readOnly = true)
    public Transaction getOwnedEntity(Long id) {
        User user = userService.currentUser();
        return transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("ledger.transaction.not-found", "Lançamento não encontrado."));
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(Long id) {
        return mapper.toTransactionResponse(getOwnedEntity(id));
    }

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        return mapper.toTransactionResponse(createEntity(request));
    }

    @Transactional
    public Transaction createEntity(CreateTransactionRequest request) {
        Transaction transaction = new Transaction();
        fillTransaction(transaction, request);
        Transaction saved = transactionRepository.save(transaction);
        applyToBalances(saved);
        return saved;
    }

    @Transactional
    public TransactionResponse update(Long id, CreateTransactionRequest request) {
        Transaction transaction = getOwnedEntity(id);
        TransactionStatus oldStatus = transaction.getStatus();
        
        reverseFromBalances(transaction);
        fillTransaction(transaction, request);
        Transaction saved = transactionRepository.save(transaction);
        applyToBalances(saved);
        
        if (oldStatus == TransactionStatus.PENDING && saved.getStatus() == TransactionStatus.POSTED) {
            handleRecurrence(saved);
        }
        
        return mapper.toTransactionResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Transaction transaction = getOwnedEntity(id);
        reverseFromBalances(transaction);
        transactionRepository.delete(transaction);
    }

    private void fillTransaction(Transaction transaction, CreateTransactionRequest request) {
        User user = userService.currentUser();
        Account account = accountService.getOwnedEntity(request.accountId());
        Account destinationAccount = request.destinationAccountId() == null ? null : accountService.getOwnedEntity(request.destinationAccountId());
        Category category = request.categoryId() == null ? null : categoryService.getOwnedEntity(request.categoryId());
        Counterparty counterparty = request.counterpartyId() == null ? null : getOwnedCounterparty(user, request.counterpartyId());
        Set<Tag> tags = getOwnedTags(user, request.tagIds());
        List<TransactionSplit> splits = buildSplits(request.splits());

        validate(request, account, destinationAccount, category, splits);

        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setDestinationAccount(destinationAccount);
        transaction.setType(request.type());
        transaction.setStatus(request.status() == null ? TransactionStatus.POSTED : request.status());
        transaction.setSource(request.source() == null ? TransactionSource.MANUAL : request.source());
        transaction.setAmount(request.amount());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setDescription(request.description());
        transaction.setNotes(request.notes());
        transaction.setCategory(category);
        transaction.setCounterparty(counterparty);
        
        if (request.cardId() != null) {
            transaction.setCard(account.getCards().stream()
                    .filter(c -> c.getId().equals(request.cardId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado nesta conta.")));
        } else {
            transaction.setCard(null);
        }
        transaction.getTags().clear();
        transaction.getTags().addAll(tags);
        transaction.replaceSplits(splits);
        // Note: setting import batch directly by ID needs the entity now, skipping for manual creation
        transaction.setRecurrenceFrequency(request.recurrenceFrequency() == null ? RecurrenceFrequency.NONE : request.recurrenceFrequency());
        transaction.setRecurrenceEndDate(request.recurrenceEndDate());
        transaction.setMetadata(request.metadata());
    }

    private void validate(
            CreateTransactionRequest request,
            Account account,
            Account destinationAccount,
            Category category,
            List<TransactionSplit> splits
    ) {
        if (request.type() == TransactionType.TRANSFER) {
            if (destinationAccount == null) {
                throw new BusinessException("ledger.transaction.destination-required", "Transferências exigem uma conta de destino.");
            }
            if (account.getId().equals(destinationAccount.getId())) {
                throw new BusinessException("ledger.transaction.same-account-transfer", "A conta de origem e destino devem ser diferentes.");
            }
        } else if (destinationAccount != null) {
            throw new BusinessException("ledger.transaction.destination-not-allowed", "Conta de destino só é permitida para transferências.");
        }

        if ((request.type() == TransactionType.INCOME || request.type() == TransactionType.EXPENSE) && category == null && splits.isEmpty()) {
            throw new BusinessException("ledger.transaction.category-required", "Receitas e despesas exigem categoria ou splits categorizados.");
        }

        if (category != null && category.getType() != CategoryType.valueOf(request.type().name())) {
            throw new BusinessException("ledger.category.incompatible-type", "Categoria incompatível com o tipo do lançamento.");
        }

        for (TransactionSplit split : splits) {
            if (split.getCategory() != null && split.getCategory().getType() != CategoryType.valueOf(request.type().name())) {
                throw new BusinessException("ledger.category.incompatible-type", "Categoria de split incompatível com o tipo do lançamento.");
            }
        }

        if (!splits.isEmpty()) {
            BigDecimal splitTotal = splits.stream()
                    .map(TransactionSplit::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (splitTotal.compareTo(request.amount()) != 0) {
                throw new BusinessException("ledger.transaction.split-total-mismatch", "A soma dos splits deve ser igual ao valor do lançamento.");
            }
        }
    }

    private void applyToBalances(Transaction transaction) {
        switch (transaction.getType()) {
            case INCOME, ADJUSTMENT -> transaction.getAccount().setCurrentBalance(
                    transaction.getAccount().getCurrentBalance().add(transaction.getAmount())
            );
            case EXPENSE -> transaction.getAccount().setCurrentBalance(
                    transaction.getAccount().getCurrentBalance().subtract(transaction.getAmount())
            );
            case TRANSFER -> {
                transaction.getAccount().setCurrentBalance(transaction.getAccount().getCurrentBalance().subtract(transaction.getAmount()));
                transaction.getDestinationAccount().setCurrentBalance(transaction.getDestinationAccount().getCurrentBalance().add(transaction.getAmount()));
                accountRepository.save(transaction.getDestinationAccount());
            }
        }
        accountRepository.save(transaction.getAccount());
    }

    private void reverseFromBalances(Transaction transaction) {
        switch (transaction.getType()) {
            case INCOME, ADJUSTMENT -> transaction.getAccount().setCurrentBalance(
                    transaction.getAccount().getCurrentBalance().subtract(transaction.getAmount())
            );
            case EXPENSE -> transaction.getAccount().setCurrentBalance(
                    transaction.getAccount().getCurrentBalance().add(transaction.getAmount())
            );
            case TRANSFER -> {
                transaction.getAccount().setCurrentBalance(transaction.getAccount().getCurrentBalance().add(transaction.getAmount()));
                transaction.getDestinationAccount().setCurrentBalance(transaction.getDestinationAccount().getCurrentBalance().subtract(transaction.getAmount()));
                accountRepository.save(transaction.getDestinationAccount());
            }
        }
        accountRepository.save(transaction.getAccount());
    }

    private Specification<Transaction> byFilters(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            Long accountId,
            TransactionType type,
            Long counterpartyId,
            String tag
    ) {
        return (root, query, criteriaBuilder) -> {
            if (tag != null && !tag.isBlank()) {
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("user"), user));
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), endDate));
            }
            if (accountId != null) {
                predicates.add(criteriaBuilder.equal(root.get("account").get("id"), accountId));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            if (counterpartyId != null) {
                predicates.add(criteriaBuilder.equal(root.get("counterparty").get("id"), counterpartyId));
            }
            if (tag != null && !tag.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.join("tags").get("name")), tag.toLowerCase()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Counterparty getOwnedCounterparty(User user, Long counterpartyId) {
        return counterpartyRepository.findByIdAndUser(counterpartyId, user)
                .orElseThrow(() -> new NotFoundException("ledger.counterparty.not-found", "Contraparte não encontrada."));
    }

    private Set<Tag> getOwnedTags(User user, Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Set.of();
        }
        Set<Tag> tags = new LinkedHashSet<>();
        for (Long tagId : tagIds) {
            tags.add(tagRepository.findByIdAndUser(tagId, user)
                    .orElseThrow(() -> new NotFoundException("ledger.tag.not-found", "Marcador não encontrado.")));
        }
        return tags;
    }

    private List<TransactionSplit> buildSplits(List<TransactionSplitRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<TransactionSplit> splits = new ArrayList<>();
        for (TransactionSplitRequest request : requests) {
            TransactionSplit split = new TransactionSplit();
            split.setAmount(request.amount());
            split.setDescription(request.description());
            if (request.categoryId() != null) {
                split.setCategory(categoryService.getOwnedEntity(request.categoryId()));
            }
            splits.add(split);
        }
        return splits;
    }

    private void handleRecurrence(Transaction original) {
        if (original.getRecurrenceFrequency() == null || original.getRecurrenceFrequency() == RecurrenceFrequency.NONE) {
            return;
        }

        LocalDate nextDate = original.getTransactionDate();
        switch (original.getRecurrenceFrequency()) {
            case WEEKLY -> nextDate = nextDate.plusWeeks(1);
            case MONTHLY -> nextDate = nextDate.plusMonths(1);
            case YEARLY -> nextDate = nextDate.plusYears(1);
        }

        if (original.getRecurrenceEndDate() != null && nextDate.isAfter(original.getRecurrenceEndDate())) {
            return;
        }

        Transaction nextTx = new Transaction();
        nextTx.setUser(original.getUser());
        nextTx.setAccount(original.getAccount());
        nextTx.setDestinationAccount(original.getDestinationAccount());
        nextTx.setType(original.getType());
        nextTx.setStatus(TransactionStatus.PENDING);
        nextTx.setSource(TransactionSource.SYSTEM);
        nextTx.setAmount(original.getAmount());
        nextTx.setTransactionDate(nextDate);
        nextTx.setDescription(original.getDescription());
        nextTx.setNotes(original.getNotes());
        nextTx.setCategory(original.getCategory());
        nextTx.setCounterparty(original.getCounterparty());
        nextTx.getTags().addAll(original.getTags());
        nextTx.setRecurrenceFrequency(original.getRecurrenceFrequency());
        nextTx.setRecurrenceEndDate(original.getRecurrenceEndDate());
        nextTx.setOriginalTransactionId(original.getId());
        
        for (TransactionSplit split : original.getSplits()) {
            TransactionSplit nextSplit = new TransactionSplit();
            nextSplit.setAmount(split.getAmount());
            nextSplit.setDescription(split.getDescription());
            nextSplit.setCategory(split.getCategory());
            nextSplit.setTransaction(nextTx);
            nextTx.getSplits().add(nextSplit);
        }
        
        transactionRepository.save(nextTx);
    }
}
