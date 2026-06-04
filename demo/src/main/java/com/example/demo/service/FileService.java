package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {

    private static final String UPLOAD_DIR = "/tmp/uploads/";

    public String saveFile(MultipartFile file, String type) throws IOException {
        Path categoryPath = Paths.get(UPLOAD_DIR, type);
        if (!Files.exists(categoryPath)) {
            Files.createDirectories(categoryPath);
            System.out.println("✅ Создана папка: " + categoryPath.toAbsolutePath());
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        String fileName = UUID.randomUUID().toString() + extension;

        Path filePath = categoryPath.resolve(fileName);
        file.transferTo(filePath.toFile());

        System.out.println("✅ Файл сохранён: " + filePath.toAbsolutePath());

        return "/api/files/" + type + "/" + fileName;
    }
}
