package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.domain.user.UserRepository;
import com.jungle_choi.namanmu.security.JwtTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/signup")
    public AuthResponse signUp(@Valid @RequestBody AuthRequest request) {
        String username = request.username().trim();
        String password = request.password();
        String accountEmail = User.accountEmail(username);

        if (userRepository.existsByEmail(accountEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        User user = User.createRegisteredUser(
                username,
                passwordEncoder.encode(password));
        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        String username = request.username().trim();
        String password = request.password();
        String accountEmail = User.accountEmail(username);
        User user = userRepository.findByEmail(accountEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(user.getName(), jwtTokenService.createToken(user));
    }

    public record AuthRequest(
            @NotBlank(message = "username is required.")
            String username,
            @NotBlank(message = "password is required.")
            @Size(min = 6, message = "password must be at least 6 characters.")
            String password) {
    }

    public record AuthResponse(String name, String token) {
    }
}
