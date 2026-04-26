package io.github.gustavoalmeidas.finos.ledger.api;

import io.github.gustavoalmeidas.finos.ledger.application.TagService;
import io.github.gustavoalmeidas.finos.ledger.dto.CreateTagRequest;
import io.github.gustavoalmeidas.finos.ledger.dto.TagResponse;
import io.github.gustavoalmeidas.finos.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ApiResponse<List<TagResponse>> list() {
        return ApiResponse.ok(tagService.list());
    }

    @PostMapping
    public ApiResponse<TagResponse> create(@Valid @RequestBody CreateTagRequest request) {
        return ApiResponse.ok("Marcador cadastrado com sucesso.", tagService.create(request));
    }
}
