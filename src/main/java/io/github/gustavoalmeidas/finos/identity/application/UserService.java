package io.github.gustavoalmeidas.finos.identity.application;

import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.identity.domain.UserPreferences;
import io.github.gustavoalmeidas.finos.identity.dto.UpdateUserPreferencesRequest;
import io.github.gustavoalmeidas.finos.identity.dto.UpdateUserProfileRequest;
import io.github.gustavoalmeidas.finos.identity.dto.UserPreferencesResponse;
import io.github.gustavoalmeidas.finos.identity.dto.UserProfileResponse;
import io.github.gustavoalmeidas.finos.identity.infrastructure.UserPreferencesRepository;
import io.github.gustavoalmeidas.finos.identity.infrastructure.UserRepository;
import io.github.gustavoalmeidas.finos.identity.security.UserPrincipal;
import io.github.gustavoalmeidas.finos.shared.exception.BusinessException;
import io.github.gustavoalmeidas.finos.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;

    @Transactional(readOnly = true)
    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException("identity.unauthenticated", "Usuário não autenticado.");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("identity.user.not-found", "Usuário não encontrado."));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile() {
        return toProfile(currentUser());
    }

    @Transactional
    public UserProfileResponse updateProfile(UpdateUserProfileRequest request) {
        User user = currentUser();
        if (request.email() != null && !request.email().equals(user.getEmail()) && userRepository.existsByEmail(request.email())) {
            throw new BusinessException("identity.email.already_exists", "E-mail já está em uso.");
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        return toProfile(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserPreferencesResponse preferences() {
        User user = currentUser();
        UserPreferences preferences = preferencesRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException("Preferências do usuário não encontradas."));
        return toPreferences(preferences);
    }

    @Transactional
    public UserPreferencesResponse updatePreferences(UpdateUserPreferencesRequest request) {
        User user = currentUser();
        UserPreferences preferences = preferencesRepository.findByUser(user)
                .orElseGet(() -> new UserPreferences(user));
        if (request.defaultLocale() != null) {
            preferences.setDefaultLocale(request.defaultLocale());
        }
        if (request.defaultTimezone() != null) {
            preferences.setDefaultTimezone(request.defaultTimezone());
        }
        if (request.defaultCurrency() != null) {
            preferences.setDefaultCurrency(request.defaultCurrency());
        }
        if (request.dateFormat() != null) {
            preferences.setDateFormat(request.dateFormat());
        }
        if (request.numberFormatLocale() != null) {
            preferences.setNumberFormatLocale(request.numberFormatLocale());
        }
        return toPreferences(preferencesRepository.save(preferences));
    }

    private UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isActive()
        );
    }

    private UserPreferencesResponse toPreferences(UserPreferences preferences) {
        return new UserPreferencesResponse(
                preferences.getDefaultLocale(),
                preferences.getDefaultTimezone(),
                preferences.getDefaultCurrency(),
                preferences.getDateFormat(),
                preferences.getNumberFormatLocale()
        );
    }
}
