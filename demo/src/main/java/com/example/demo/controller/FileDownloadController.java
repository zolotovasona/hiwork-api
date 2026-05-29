package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ✅ ДОБАВЛЕНЫ НЕОБХОДИМЫЕ ИМПОРТЫ
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileDownloadController {
    @Value("${app.upload-dir:uploads/}")
    private String uploadDir;

    @GetMapping("/{category}/{filename}")
    public ResponseEntity<?> downloadFile(@PathVariable String category, @PathVariable String filename) {
        try {
            // Валидация категории
            if (!category.matches("^(resume|portfolio|photo)$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Недопустимая категория"));
            }
            // Валидация имени файла
            if (filename == null || filename.isEmpty() || filename.contains("..") || filename.contains("/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Недопустимое имя файла"));
            }

            // Безопасная сборка пути
            Path basePath = Paths.get(uploadDir).normalize();
            Path filePath = basePath.resolve(category).resolve(filename).normalize();

            // Защита от path traversal
            if (!filePath.startsWith(basePath)) {
                return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещён"));
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) contentType = "application/octet-stream";

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Ошибка чтения файла: " + e.getMessage()));
        }
    }
}