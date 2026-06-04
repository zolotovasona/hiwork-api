package com.example.demo.controller;

import com.example.demo.entity.Application;
import com.example.demo.entity.User;
import com.example.demo.repository.ApplicationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.FileService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

@CrossOrigin(origins = {"https://hiwork-hiring-site.onrender.com", "http://localhost:5500", "*"})
@RestController
@RequestMapping("/api/applications")
@Log
public class ApplicationController {

    @Autowired
    private ApplicationRepository appRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private FileService fileService;

    @Value("${app.base-url:https://hiwork-api.onrender.com}")
    private String baseUrl;

    private final String uploadDir = "uploads/";

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

    @PostMapping
    public ResponseEntity<?> createApplication(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String careerTrack,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) String aboutMe,
            @RequestParam(required = false) String resumeTextContent,
            @RequestParam(required = false) MultipartFile resumeFile,
            @RequestParam(required = false) MultipartFile portfolio,
            @RequestParam(required = false) MultipartFile photo
    ) {
        try {
            Application app = new Application();
            app.setFullName(fullName);
            app.setEmail(email);
            app.setCareerTrack(careerTrack);
            app.setDepartment(department);
            app.setExperience(experience);
            app.setAboutMe(aboutMe);
            app.setResumeTextContent(resumeTextContent);
            app.setStatus("new");

            // ✅ Сохраняем файлы
            if (photo != null && !photo.isEmpty()) {
                String photoUrl = fileService.saveFile(photo, "photo");
                app.setProfilePhotoUrl(photoUrl);
            }

            if (resumeFile != null && !resumeFile.isEmpty()) {
                String resumeUrl = fileService.saveFile(resumeFile, "resume");
                app.setResumeUrl(resumeUrl);
            }

            if (portfolio != null && !portfolio.isEmpty()) {
                String portfolioUrl = fileService.saveFile(portfolio, "portfolio");
                app.setPortfolioUrl(portfolioUrl);
            }

            applicationRepository.save(app);
            return ResponseEntity.ok(Map.of("success", true, "id", app.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

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

    @PostMapping("/{id}/hire")
    public ResponseEntity<?> hire(@PathVariable Long id, @RequestBody(required = false) Map<String, String> updates) {
        try {
            Application app = appRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

            if (!"interviewing".equalsIgnoreCase(app.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Можно принять только кандидата с собеседования"));
            }

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

            String qrCode = String.format("%07d", new java.util.Random().nextInt(10_000_000));
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(2);

            app.setQrCode(qrCode);
            app.setQrCodeExpiresAt(expiresAt);
            app.setStatus("hired");
            appRepo.save(app);

            User employee = userRepo.findByEmail(app.getEmail()).orElse(new User());
            employee.setEmail(app.getEmail());
            employee.setPasswordHash(passwordEncoder.encode("temp123"));
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

            try {
                String qrImageUrl = baseUrl + "/api/qr/generate?code=" + qrCode + "&size=250";
                emailService.sendHireConfirmation(employee.getEmail(), employee.getFullName(), qrCode, qrImageUrl);
            } catch (Exception mailErr) {
                log.warning("⚠️ Письмо о приёме не отправлено: " + mailErr.getMessage());
            }

            log.info("✅ Сотрудник принят: " + employee.getEmail() + ", QR: " + qrCode);

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

    @PostMapping("/{id}/regenerate-qr")
    public ResponseEntity<?> regenerateQr(@PathVariable Long id) {
        try {
            Application app = appRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

            if (!"hired".equals(app.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Перегенерация доступна только для принятых"));
            }

            String newQr = String.format("%07d", new java.util.Random().nextInt(10_000_000));
            LocalDateTime newExpires = LocalDateTime.now().plusMinutes(2);

            app.setQrCode(newQr);
            app.setQrCodeExpiresAt(newExpires);
            appRepo.save(app);

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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        if (!appRepo.existsById(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Заявка не найдена"));
        }
        appRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Заявка удалена"));
    }

    private String saveFile(MultipartFile file, String category) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Пустой файл");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            throw new IllegalArgumentException("Неизвестное имя файла");
        }

        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalName.length() - 1) {
            extension = originalName.substring(dotIndex).toLowerCase();
        }

        String uniqueName = UUID.randomUUID().toString() + extension;
        Path path = Paths.get(uploadDir + category + "/" + uniqueName);

        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());

        return baseUrl + "/api/files/" + category + "/" + uniqueName;
    }

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

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String email = credentials.get("email");
            String password = credentials.get("password");

            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId", user.getId(),
                    "role", user.getRole(),
                    "fullName", user.getFullName(),
                    "email", user.getEmail()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Login error", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Ошибка сервера"));
        }
    }
}
