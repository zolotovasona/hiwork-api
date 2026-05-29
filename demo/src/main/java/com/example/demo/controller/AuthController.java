package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 1. Проверка пароля
            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                return ResponseEntity.status(401).body(Map.of("error", "Неверный пароль"));
            }

            // 2. Проверка кода для EMPLOYEE + срок действия
            if ("EMPLOYEE".equals(user.getRole())) {
                String providedCode = request.code();
                String storedCode = user.getQrCode();
                LocalDateTime expiresAt = user.getQrCodeExpiresAt();

                if (providedCode != null && !providedCode.isEmpty()) {
                    if (!providedCode.equals(storedCode)) {
                        return ResponseEntity.status(403).body(Map.of("error", "Код не совпадает"));
                    }
                    // ✅ КРИТИЧНО: проверка истечения кода
                    if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
                        return ResponseEntity.status(403).body(Map.of(
                                "error", "Код истёк", "expired", true,
                                "expiresAt", expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        ));
                    }
                }
            }

            // 3. Проверка кода для HR
            if ("HR".equals(user.getRole()) && request.code() != null && !request.code().isEmpty()
                    && !"04062026".equals(request.code())) {
                return ResponseEntity.status(403).body(Map.of("error", "Неверный код HR"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true, "userId", user.getId(), "email", user.getEmail(),
                    "role", user.getRole(), "fullName", user.getFullName(), "isNewUser", false
            ));

        } else {
            // РЕГИСТРАЦИЯ нового пользователя
            User newUser = new User();
            newUser.setEmail(request.email());
            newUser.setPasswordHash(passwordEncoder.encode(request.password()));
            newUser.setFullName(request.fullName() != null ? request.fullName() : request.email());

            if (request.code() != null && !request.code().isEmpty()) {
                newUser.setQrCode(request.code());
                newUser.setQrCodeExpiresAt(LocalDateTime.now().plusMinutes(2));
            }

            newUser.setRole("EMPLOYEE");
            newUser.setCreatedAt(LocalDateTime.now());
            userRepository.save(newUser);

            return ResponseEntity.ok(Map.of(
                    "success", true, "userId", newUser.getId(), "email", newUser.getEmail(),
                    "role", newUser.getRole(), "fullName", newUser.getFullName(), "isNewUser", true
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Auth API is running"));
    }
}