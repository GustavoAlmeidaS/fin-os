package io.github.gustavoalmeidas.finos.identity.api;

import io.github.gustavoalmeidas.finos.identity.application.AuthService;
import io.github.gustavoalmeidas.finos.identity.dto.AuthResponse;
import io.github.gustavoalmeidas.finos.identity.dto.LoginRequest;
import io.github.gustavoalmeidas.finos.identity.dto.SignupRequest;
import io.github.gustavoalmeidas.finos.shared.api.ApiResponse;
import io.github.gustavoalmeidas.finos.shared.i18n.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final MessageService messageService;

    @PostMapping("/signup")
    public ApiResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.ok(messageService.getMessage("auth.signup.success"), authService.signup(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(messageService.getMessage("auth.login.success"), authService.login(request));
    }
}
