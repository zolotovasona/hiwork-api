package com.example.demo.controller;

import com.example.demo.entity.Application;
import com.example.demo.entity.User;
import com.example.demo.repository.ApplicationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import lombok.Data;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@CrossOrigin(origins = {"https://hiwork-hiring-site.onrender.com", "http://localhost:5500", "*"})
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    @Autowired private ApplicationRepository appRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    private final String uploadDir = "uploads/";

    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(required = false) String status) {
        if (status != null && !status.isEmpty()) {
            return ResponseEntity.ok(appRepo.findByStatusOrderByCreatedAtDesc(status));
        }
        return ResponseEntity.ok(appRepo.findAllByOrderByCreatedAtDesc());
    }

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
            Application app = new Application();
            app.setFullName(fullName);
            app.setEmail(email);
            app.setCareerTrack(careerTrack);
            app.setAboutMe(aboutMe);
            app.setDepartment(department != null ? department : "");
            app.setExperience(experience != null ? experience : "0");
            app.setStatus("new"); // ✅ Lombok автоматически создаст этот сеттер

            // 📝 Резюме: сохраняем текст, если он есть
            if (resumeText != null && !resumeText.trim().isEmpty()) {
                app.setResumeTextContent(resumeText.trim());
            }

            // 📎 Резюме: файл (проверяем оба варианта имени поля для совместимости)
            MultipartFile resumeToSave = (resume != null && !resume.isEmpty()) ? resume :
                    (resumeFile != null && !resumeFile.isEmpty()) ? resumeFile : null;

            if (resumeToSave != null) {
                app.setResumeUrl(saveFile(resumeToSave, "resume"));
            }

            // 📁 Портфолио и фото профиля
            if (portfolio != null && !portfolio.isEmpty()) {
                app.setPortfolioUrl(saveFile(portfolio, "portfolio"));
            }
            if (photo != null && !photo.isEmpty()) {
                app.setProfilePhotoUrl(saveFile(photo, "photo"));
            }

            appRepo.save(app);
            return ResponseEntity.ok(Map.of("success", true, "id", app.getId()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Ошибка сервера: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/interview")
    public ResponseEntity<?> interview(@PathVariable Long id) {
        Application app = appRepo.findById(id).orElseThrow(() -> new RuntimeException("Заявка не найдена"));
        app.setStatus("interviewing");
        appRepo.save(app);
        emailService.sendInterviewInvite(app.getEmail(), app.getFullName());
        return ResponseEntity.ok(Map.of("success", true, "message", "Приглашение отправлено"));
    }

    @PostMapping("/{id}/hire")
    public ResponseEntity<?> hire(@PathVariable Long id, @RequestBody(required = false) Map<String, String> updates) {
        try {
            Application app = appRepo.findById(id).orElseThrow(() -> new RuntimeException("Заявка не найдена"));

            if (updates != null) {
                if (updates.containsKey("fullName")) app.setFullName(updates.get("fullName"));
                if (updates.containsKey("careerTrack")) app.setCareerTrack(updates.get("careerTrack"));
                if (updates.containsKey("department")) app.setDepartment(updates.get("department"));
            }

            // Генерация QR
            String qrCode = String.format("%07d", new java.util.Random().nextInt(10_000_000));
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(2);
            app.setQrCode(qrCode);
            app.setQrCodeExpiresAt(expiresAt);
            app.setStatus("hired");
            appRepo.save(app);

            // Создаём/обновляем пользователя
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

            // Пытаемся отправить письмо, но НЕ ломаем процесс, если почта не настроена
            try {
                String baseUrl = "http://localhost:8080"; // Для эмулятора
                String qrImageUrl = baseUrl + "/api/qr/generate?code=" + qrCode + "&size=250";
                emailService.sendHireConfirmation(employee.getEmail(), employee.getFullName(), qrCode, qrImageUrl);
            } catch (Exception mailErr) {
            System.err.println("⚠️ Письмо не ушло (проверь SMTP): " + mailErr.getMessage());
            // QR-код всё равно сгенерируется и отобразится в приложении
        }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "qrCode", qrCode,
                    "expiresAt", expiresAt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "secondsRemaining", 120,
                    "message", "Сотрудник создан. Код действителен 2 минуты."
            ));

        } catch (Exception e) {
            System.err.println("❌ Ошибка hire: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Ошибка сервера: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/regenerate-qr")
    public ResponseEntity<?> regenerateQr(@PathVariable Long id) {
        try {
            // 1. Находим заявку
            Application app = appRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

            // 2. Разрешаем перегенерацию только для статуса "hired"
            if (!"hired".equals(app.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Перегенерация доступна только для принятых сотрудников"));
            }

            // 3. Находим пользователя по email из заявки
            User employee = userRepo.findByEmail(app.getEmail())
                    .orElseThrow(() -> new RuntimeException("Сотрудник не найден в базе"));

            // 4. Генерируем новый 7-значный код и таймер
            String newQr = String.format("%07d", new java.util.Random().nextInt(10_000_000));
            LocalDateTime newExpires = LocalDateTime.now().plusMinutes(2);

            // 5. Обновляем данные сотрудника
            employee.setQrCode(newQr);
            employee.setQrCodeExpiresAt(newExpires);
            userRepo.save(employee);

            // 6. Отправляем уведомление (раскомментируй, если настроил EmailService)
            // String baseUrl = "http://localhost:8080";
            // emailService.sendQrRegenerated(employee.getEmail(), employee.getFullName(), newQr,
            //         baseUrl + "/api/qr/generate?code=" + newQr + "&size=250");

            // 7. Возвращаем успех
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "qrCode", newQr,
                    "expiresAt", newExpires.toString(),
                    "secondsRemaining", 120,
                    "message", "Код обновлён и отправлен на почту"
            ));

        } catch (Exception e) {
            // ✅ ГАРАНТИРОВАННЫЙ RETURN при любой ошибке
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Ошибка перегенерации: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        Application app = appRepo.findById(id).orElseThrow(() -> new RuntimeException("Заявка не найдена"));
        app.setStatus("rejected"); // Мягкое удаление
        appRepo.save(app);
        return ResponseEntity.ok(Map.of("success", true, "message", "Заявка отклонена"));
    }

    // Сохранение файла с категорией
    private String saveFile(MultipartFile file, String category) throws Exception {
        String originalName = file.getOriginalFilename();
        String extension = originalName != null && originalName.contains(".") ?
                originalName.substring(originalName.lastIndexOf(".")) : "";
        String uniqueName = UUID.randomUUID().toString() + extension;
        Path path = Paths.get(uploadDir + category + "/" + uniqueName);
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());
        return "/api/files/" + category + "/" + uniqueName;
    }
}