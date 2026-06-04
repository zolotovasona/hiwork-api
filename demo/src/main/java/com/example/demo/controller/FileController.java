package com.example.demo.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLConnection;

@CrossOrigin(origins = {"https://hiwork-hiring-site.onrender.com", "http://localhost:5500", "*"})
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final String UPLOAD_DIR = "/tmp/uploads/";

    @GetMapping("/{category}/{filename}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String category,
            @PathVariable String filename) {
        try {
            String filePath = UPLOAD_DIR + category + "/" + filename;
            File file = new File(filePath);

            if (!file.exists()) {
                System.out.println("⚠️ Файл не найден: " + filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = URLConnection.guessContentTypeFromName(filename);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            System.err.println("❌ Ошибка при отдаче файла: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
