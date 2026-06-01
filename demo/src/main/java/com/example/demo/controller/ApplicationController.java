package com.example.demo.controller;

import com.example.demo.entity.Application;
import com.example.demo.entity.User;
import com.example.demo.repository.ApplicationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

@CrossOrigin(origins = {"https://hiwork-hiring-site.onrender.com", "http://localhost:5500", "*"})
@RestController
@RequestMapping("/api/applications")
@Log
public class ApplicationController {

    @Autowired private ApplicationRepository appRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    // ✅ Базовый URL для формирования полных ссылок на файлы (для Render)
    @Value("${app.base-url:https://hiwork-api.onrender.com}")
    private String baseUrl;

    private final String uploadDir = "uploads/";

    // ==================== GET: Получить все заявки ====================
    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(required = false) String status) {
        try {
            List<Application> applications;
            if (status != null && !status.isEmpty()) {
                applications = appRepo.findByStatusOrderByCreatedAtDesc(status);
            } else {
                applications = appRepo.findAllByOrderByCreatedAtDesc();
            }
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Ошибка при получении заявок", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Не удалось загрузить заявки: " + e.getMessage()));
        }
    }

    // ==================== POST: Создать новую заявку ====================
    @PostMapping
    public ResponseEntity<?> createApplication(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("careerTrack") String careerTrack,
            @RequestParam("aboutMe") String aboutMe,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "resumeText", required = false) String resumeText,
            @RequestParam(value = "resume", required = false) MultipartFile resume,
            @RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile,
            @RequestParam(value = "portfolio", required = false) MultipartFile portfolio,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {

        try {
            // Валидация обязательных полей
            if (fullName == null || fullName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "ФИО обязательно"));
            }
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email обязателен"));
            }
            if (careerTrack == null || careerTrack.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Карьерный трек обязателен"));
            }

            Application app = new Application();
            app.setFullName(fullName.trim());
            app.setEmail(email.trim().toLowerCase());
            app.setCareerTrack(careerTrack.trim());
            app.setAboutMe(aboutMe != null ? aboutMe.trim() : "");
            app.setDepartment(department != null ? department.trim() : "");
            app.setExperience(experience != null ? experience : "0");
            app.setStatus("new");
            app.setCreatedAt(LocalDateTime.now());

            // 📝 Резюме: текст
            if (resumeText != null && !resumeText.trim().isEmpty()) {
                app.setResumeTextContent(resumeText.trim());
            }

            // 📎 Резюме: файл (поддержка двух имён поля для совместимости)
            MultipartFile resumeToSave = null;
            if (resume != null && !resume.isEmpty()) {
                resumeToSave = resume;
            } else if (resumeFile != null && !resumeFile.isEmpty()) {
                resumeToSave = resumeFile;
            }
            if (resumeToSave != null) {
                app.setResumeUrl(saveFile(resumeToSave, "resume"));
            }

            // 📁 Портфолио
            if (portfolio != null && !portfolio.isEmpty()) {
                app.setPortfolioUrl(saveFile(portfolio, "portfolio"));
            }

            // 🖼 Фото профиля
            if (photo != null && !photo.isEmpty()) {
                app.setProfilePhotoUrl(saveFile(photo, "photo"));
            }

            appRepo.save(app);
            log.info("✅ Новая заявка создана: " + app.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "id", app.getId(),
                    "message", "Заявка успешно отправлена"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Ошибка при создании заявки", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Ошибка сервера: " + e.getMessage()));
        }
    }

    // ==================== POST: Пригласить на собеседование ====================
    @PostMapping("/{id}/interview")
    public ResponseEntity<?> interview(@PathVariable Long id) {
        try {
            Application app = appRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

            if (!"new".equalsIgnoreCase(app.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Можно пригласить только новую заявку"));
            }

            app.setStatus("interviewing");
            appRepo.save(app);

            // Отправляем письмо (не блокируем процесс, если почта не работает)
            try {
                emailService.sendInterviewInvite(app.getEmail(), app.getFullName());
            } catch (Exception mailErr) {
                log.warning("⚠️ Письмо-приглашение не отправлено: " + mailErr.getMessage());
            }

            log.info("✅ Заявка " + id + " переведена в собеседование");
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Приглашение отправлено",
                    "status", "interviewing"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Ошибка при приглашении", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Ошибка сервера: " + e.getMessage()));
        }
    }

    // ==================== POST: Принять на работу (с генерацией QR) ====================
    @PostMapping("/{id}/hire")
    public ResponseEntity<?> hire(@PathVariable Long id, @RequestBody(required = false) Map<String, String> updates) {
        try {
            Application app = appRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

            if (!"interviewing".equalsIgnoreCase(app.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Можно принять только кандидата с собеседования"));
            }

            // Обновляем данные, если переданы
            if (updates != null) {
                if (updates.containsKey("fullName") && updates.get("fullName") != null) {
                    app.setFullName(updates.get("fullName").trim());
                }
                if (updates.containsKey("careerTrack") && updates.get("careerTrack") != null) {
                    app.setCareerTrack(updates.get("careerTrack").trim());
                }
                if (updates.containsKey("department") && updates.get("department") != null) {
                    app.setDepartment(updates.get("department").trim());
                }
            }

            // 🎫 Генерация QR-кода
            String qrCode = String.format("%07d", new java.util.Random().nextInt(10_000_000));
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(2);

            app.setQrCode(qrCode);
            app.setQrCodeExpiresAt(expiresAt);
            app.setStatus("hired");
            appRepo.save(app);

            // 👤 Создаём или обновляем пользователя
            User employee = userRepo.findByEmail(app.getEmail()).orElse(new User());
            employee.setEmail(app.getEmail());
            employee.setPasswordHash(passwordEncoder.encode("temp123")); // Временный пароль
            employee.setFullName(app.getFullName());
            employee.setCareerTrack(app.getCareerTrack());
            employee.setDepartment(app.getDepartment());
            employee.setAboutMe(app.getAboutMe());
            employee.setResumeUrl(app.getResumeUrl());
            employee.setPortfolioUrl(app.getPortfolioUrl());
            employee.setProfilePhotoUrl(app.getProfilePhotoUrl());
            employee.setQrCode(qrCode);
            employee.setQrCodeExpiresAt(expiresAt);
            employee.setRole("EMPLOYEE");
            employee.setCreatedAt(LocalDateTime.now());
            userRepo.save(employee);

            // 📧 Отправляем письмо с подтверждением (не блокируем процесс)
            try {
                String qrImageUrl = baseUrl + "/api/qr/generate?code=" + qrCode + "&size=250";
                emailService.sendHireConfirmation(employee.getEmail(), employee.getFullName(), qrCode, qrImageUrl);
            } catch (Exception mailErr) {
                log.warning("⚠️ Письмо о приёме не отправлено: " + mailErr.getMessage());
            }

            log.info("✅ Сотрудник принят: " + employee.getEmail() + ", QR: " + qrCode);

            // ✅ Возвращаем expiresAt в формате, который поймёт Android
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "qrCode", qrCode,
                    "expiresAt", expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "secondsRemaining", 120,
                    "message", "Сотрудник создан. Код действителен 2 минуты."
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Ошибка при приёме на работу", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Ошибка сервера: " + e.getMessage()));
        }
    }

    // ==================== POST: Перегенерировать QR-код ====================
@PostMapping("/{id}/regenerate-qr")
public ResponseEntity<?> regenerateQr(@PathVariable Long id) {
    try {
        Application app = appRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        if (!"hired".equals(app.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Перегенерация доступна только для принятых"));
        }

        // Генерируем новый код
        String newQr = String.format("%07d", new java.util.Random().nextInt(10_000_000));
        LocalDateTime newExpires = LocalDateTime.now().plusMinutes(2);

        // ✅ ОБНОВЛЯЕМ ПОЛЯ
        app.setQrCode(newQr);
        app.setQrCodeExpiresAt(newExpires);
        
        // ✅ СОХРАНЯЕМ В БАЗУ (это критично!)
        appRepo.save(app);

        // Если есть User — обновляем и его
        User employee = userRepo.findByEmail(app.getEmail()).orElse(null);
        if (employee != null) {
            employee.setQrCode(newQr);
            employee.setQrCodeExpiresAt(newExpires);
            userRepo.save(employee);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "qrCode", newQr,
                "expiresAt", newExpires.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));

    } catch (Exception e) {
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "Ошибка: " + e.getMessage()));
    }
}
    // ==================== DELETE: Отклонить заявку ====================
@DeleteMapping("/{id}")
public ResponseEntity<?> reject(@PathVariable Long id) {
    if (!appRepo.existsById(id)) {
        return ResponseEntity.badRequest().body(Map.of("error", "Заявка не найдена"));
    }
    appRepo.deleteById(id); // ✅ Физическое удаление
    return ResponseEntity.ok(Map.of("success", true, "message", "Заявка удалена"));
}

    // ==================== ВСПОМОГАТЕЛЬНЫЙ МЕТОД: Сохранение файла ====================
    private String saveFile(MultipartFile file, String category) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Пустой файл");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            throw new IllegalArgumentException("Неизвестное имя файла");
        }

        // Безопасное извлечение расширения
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalName.length() - 1) {
            extension = originalName.substring(dotIndex).toLowerCase();
        }

        // Уникальное имя
        String uniqueName = UUID.randomUUID().toString() + extension;
        Path path = Paths.get(uploadDir + category + "/" + uniqueName);

        // Создаём папку, если нет
        Files.createDirectories(path.getParent());

        // Сохраняем файл
        Files.write(path, file.getBytes());

        // ✅ Возвращаем ПОЛНЫЙ URL для доступа из Android/веба
        return baseUrl + "/api/files/" + category + "/" + uniqueName;
    }
    // === GET: Доступные опции для выпадающих списков ===
    @GetMapping("/options")
    public ResponseEntity<?> getAvailableOptions() {
        try {
            List<String> careerTracks = appRepo.findDistinctCareerTracks();
            List<String> departments = appRepo.findDistinctDepartments();

            return ResponseEntity.ok(Map.of(
                    "careerTracks", careerTracks != null ? careerTracks : List.of(),
                    "departments", departments != null ? departments : List.of()
            ));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Ошибка при получении опций", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Не удалось загрузить опции"));
        }
    }
}
