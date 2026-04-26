package io.github.gustavoalmeidas.finos.identity.api;

import io.github.gustavoalmeidas.finos.identity.application.UserService;
import io.github.gustavoalmeidas.finos.identity.dto.UpdateUserPreferencesRequest;
import io.github.gustavoalmeidas.finos.identity.dto.UpdateUserProfileRequest;
import io.github.gustavoalmeidas.finos.identity.dto.UserPreferencesResponse;
import io.github.gustavoalmeidas.finos.identity.dto.UserProfileResponse;
import io.github.gustavoalmeidas.finos.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> profile() {
        return ApiResponse.ok(userService.profile());
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.ok("Perfil atualizado com sucesso.", userService.updateProfile(request));
    }

    @GetMapping("/preferences")
    public ApiResponse<UserPreferencesResponse> preferences() {
        return ApiResponse.ok(userService.preferences());
    }

    @PutMapping("/preferences")
    public ApiResponse<UserPreferencesResponse> updatePreferences(@Valid @RequestBody UpdateUserPreferencesRequest request) {
        return ApiResponse.ok("Preferências atualizadas com sucesso.", userService.updatePreferences(request));
    }
}
