package io.github.gustavoalmeidas.finos.ledger.application;

import io.github.gustavoalmeidas.finos.identity.application.UserService;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Counterparty;
import io.github.gustavoalmeidas.finos.ledger.domain.CounterpartyType;
import io.github.gustavoalmeidas.finos.ledger.dto.CounterpartyResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.CreateCounterpartyRequest;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.CounterpartyRepository;
import io.github.gustavoalmeidas.finos.ledger.mapper.LedgerMapper;
import io.github.gustavoalmeidas.finos.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CounterpartyService {

    private final UserService userService;
    private final CounterpartyRepository counterpartyRepository;
    private final LedgerMapper mapper;

    @Transactional(readOnly = true)
    public List<CounterpartyResponse> list() {
        User user = userService.currentUser();
        return counterpartyRepository.findByUserOrderByNameAsc(user).stream()
                .map(mapper::toCounterpartyResponse)
                .toList();
    }

    @Transactional
    public CounterpartyResponse create(CreateCounterpartyRequest request) {
        Counterparty counterparty = new Counterparty();
        counterparty.setUser(userService.currentUser());
        counterparty.setName(request.name());
        counterparty.setDocument(request.document());
        counterparty.setType(request.type() == null ? CounterpartyType.OTHER : request.type());
        counterparty.setNotes(request.notes());
        return mapper.toCounterpartyResponse(counterpartyRepository.save(counterparty));
    }

    @Transactional(readOnly = true)
    public Counterparty getOwnedEntity(Long id) {
        User user = userService.currentUser();
        return counterpartyRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("ledger.counterparty.not-found", "Contraparte não encontrada."));
    }
}
