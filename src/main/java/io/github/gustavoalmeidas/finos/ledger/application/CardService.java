package io.github.gustavoalmeidas.finos.ledger.application;

import io.github.gustavoalmeidas.finos.identity.application.UserService;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Account;
import io.github.gustavoalmeidas.finos.ledger.domain.Card;
import io.github.gustavoalmeidas.finos.ledger.dto.CardResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.CreateCardRequest;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.AccountRepository;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.CardRepository;
import io.github.gustavoalmeidas.finos.ledger.mapper.LedgerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final LedgerMapper ledgerMapper;
    private final UserService userService;

    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        User user = userService.currentUser();
        
        Account account = accountRepository.findByIdAndUser(request.accountId(), user)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada ou não pertence ao usuário."));

        Card card = new Card();
        card.setAccount(account);
        card.setName(request.name());
        card.setType(request.type());
        card.setCreditLimit(request.creditLimit());
        card.setClosingDay(request.closingDay());
        card.setDueDay(request.dueDay());

        return ledgerMapper.toCardResponse(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(Long id) {
        User user = userService.currentUser();
        Card card = cardRepository.findById(id)
                .filter(c -> c.getAccount().getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado."));
                
        cardRepository.delete(card);
    }
}
