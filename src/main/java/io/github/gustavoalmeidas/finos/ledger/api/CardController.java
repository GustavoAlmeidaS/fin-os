package io.github.gustavoalmeidas.finos.ledger.api;

import io.github.gustavoalmeidas.finos.ledger.application.CardService;
import io.github.gustavoalmeidas.finos.ledger.dto.CardResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.CreateCardRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse createCard(@RequestBody @Valid CreateCardRequest request) {
        return cardService.createCard(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
    }
}
