package io.github.gustavoalmeidas.finos.ledger.application;

import io.github.gustavoalmeidas.finos.identity.application.UserService;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Tag;
import io.github.gustavoalmeidas.finos.ledger.dto.CreateTagRequest;
import io.github.gustavoalmeidas.finos.ledger.dto.TagResponse;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.TagRepository;
import io.github.gustavoalmeidas.finos.ledger.mapper.LedgerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final UserService userService;
    private final TagRepository tagRepository;
    private final LedgerMapper mapper;

    @Transactional(readOnly = true)
    public List<TagResponse> list() {
        User user = userService.currentUser();
        return tagRepository.findByUserOrderByNameAsc(user).stream()
                .map(mapper::toTagResponse)
                .toList();
    }

    @Transactional
    public TagResponse create(CreateTagRequest request) {
        User user = userService.currentUser();
        Tag tag = tagRepository.findByUserAndNameIgnoreCase(user, request.name()).orElseGet(Tag::new);
        tag.setUser(user);
        tag.setName(request.name());
        tag.setColor(request.color());
        return mapper.toTagResponse(tagRepository.save(tag));
    }
}
