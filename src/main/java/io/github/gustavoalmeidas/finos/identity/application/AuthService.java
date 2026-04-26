package io.github.gustavoalmeidas.finos.identity.application;

import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.identity.domain.UserPreferences;
import io.github.gustavoalmeidas.finos.identity.dto.AuthResponse;
import io.github.gustavoalmeidas.finos.identity.dto.LoginRequest;
import io.github.gustavoalmeidas.finos.identity.dto.SignupRequest;
import io.github.gustavoalmeidas.finos.identity.infrastructure.UserPreferencesRepository;
import io.github.gustavoalmeidas.finos.identity.infrastructure.UserRepository;
import io.github.gustavoalmeidas.finos.identity.security.JwtService;
import io.github.gustavoalmeidas.finos.identity.security.UserPrincipal;
import io.github.gustavoalmeidas.finos.ledger.application.CategoryService;
import io.github.gustavoalmeidas.finos.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CategoryService categoryService;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("identity.username.already_exists", "Nome de usuário já está em uso.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("identity.email.already_exists", "E-mail já está em uso.");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        User savedUser = userRepository.save(user);

        preferencesRepository.save(new UserPreferences(savedUser));
        categoryService.createDefaultCategories(savedUser);

        String token = jwtService.generateToken(UserPrincipal.from(savedUser));
        return new AuthResponse(token, "Bearer", savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password())
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        return new AuthResponse(token, "Bearer", principal.getId(), principal.getUsername(), principal.getEmail());
    }
}
