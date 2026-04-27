package io.github.gustavoalmeidas.finos.ledger.mapper;

import io.github.gustavoalmeidas.finos.ledger.domain.Account;
import io.github.gustavoalmeidas.finos.ledger.domain.Category;
import io.github.gustavoalmeidas.finos.ledger.domain.Counterparty;
import io.github.gustavoalmeidas.finos.ledger.domain.Tag;
import io.github.gustavoalmeidas.finos.ledger.domain.Transaction;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionSplit;
import io.github.gustavoalmeidas.finos.ledger.dto.AccountResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.CategoryResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.CounterpartyResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.TagResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.TransactionResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.TransactionSplitResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.TransactionSplitResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.CardResponse;
import io.github.gustavoalmeidas.finos.ledger.domain.Card;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LedgerMapper {

    public AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrency(),
                account.getInitialBalance(),
                account.getCurrentBalance(),
                account.isActive(),
                account.getInstitutionName(),
                account.getColor(),
                account.getNotes(),
                toCardResponses(account.getCards())
        );
    }

    public CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getParentCategory() == null ? null : category.getParentCategory().getId(),
                category.getColor(),
                category.isActive()
        );
    }

    public TransactionResponse toTransactionResponse(Transaction transaction) {
        Account destination = transaction.getDestinationAccount();
        Category category = transaction.getCategory();
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getAccount().getName(),
                destination == null ? null : destination.getId(),
                destination == null ? null : destination.getName(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getSource(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getDescription(),
                transaction.getNotes(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                toCounterpartyResponse(transaction.getCounterparty()),
                toTagResponses(transaction.getTags()),
                toSplitResponses(transaction.getSplits()),
                transaction.getImportBatch() != null ? transaction.getImportBatch().getId() : null,
                transaction.getMetadata(),
                transaction.getRecurrenceFrequency(),
                transaction.getRecurrenceEndDate(),
                transaction.getCard() == null ? null : transaction.getCard().getId()
        );
    }

    public CounterpartyResponse toCounterpartyResponse(Counterparty counterparty) {
        if (counterparty == null) {
            return null;
        }
        return new CounterpartyResponse(
                counterparty.getId(),
                counterparty.getName(),
                counterparty.getDocument(),
                counterparty.getType(),
                counterparty.getNotes()
        );
    }

    public TagResponse toTagResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getColor());
    }

    private Set<TagResponse> toTagResponses(Set<Tag> tags) {
        return tags.stream()
                .sorted(Comparator.comparing(Tag::getName))
                .map(this::toTagResponse)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<TransactionSplitResponse> toSplitResponses(List<TransactionSplit> splits) {
        return splits.stream()
                .map(split -> new TransactionSplitResponse(
                        split.getId(),
                        split.getCategory() == null ? null : split.getCategory().getId(),
                        split.getCategory() == null ? null : split.getCategory().getName(),
                        split.getAmount(),
                        split.getDescription()
                ))
                .toList();
    }

    public CardResponse toCardResponse(Card card) {
        return new CardResponse(
                card.getId(),
                card.getAccount().getId(),
                card.getName(),
                card.getType(),
                card.getCreditLimit(),
                card.getClosingDay(),
                card.getDueDay(),
                card.isActive()
        );
    }

    private List<CardResponse> toCardResponses(List<Card> cards) {
        if (cards == null) return List.of();
        return cards.stream().map(this::toCardResponse).toList();
    }
}
