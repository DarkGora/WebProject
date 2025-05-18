package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
public class FileStorageService {
    @Value("${upload.dir:uploads}")
    private String uploadDir;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            log.info("Upload directory initialized: {}", uploadPath);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", uploadDir, e);
            throw new IllegalStateException("Cannot initialize upload directory: " + uploadDir, e);
        }
    }

    public String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            log.error("Invalid file format: {}", file.getContentType());
            throw new IllegalArgumentException("Недопустимый формат файла. Используйте JPG, PNG или WEBP.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            log.error("File too large: {} bytes", file.getSize());
            throw new IllegalArgumentException("Файл слишком большой. Максимальный размер: 5MB.");
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.-]", "_");
        Path filePath = Paths.get(uploadDir).resolve(fileName).toAbsolutePath().normalize();
        Files.write(filePath, file.getBytes());
        log.info("Stored file: {}", filePath);
        return filePath.toString();
    }

    public void deleteFile(String filePath) {
        if (filePath != null) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
                log.info("Deleted file: {}", filePath);
            } catch (IOException e) {
                log.warn("Failed to delete file: {}", filePath, e);
            }
        }
    }
}