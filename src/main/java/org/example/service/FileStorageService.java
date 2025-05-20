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

/**
 * Сервис для управления хранением и удалением файлов (например, фотографий сотрудников).
 */
@Slf4j
@Service
public class FileStorageService {
    @Value("${upload.dir:uploads}")
    private String uploadDir;

    private Path uploadPath;
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * Инициализация директории для загрузки файлов.
     *
     * @throws IllegalStateException если не удалось создать директорию
     */
    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            log.info("Upload directory initialized: {}", uploadPath);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", uploadDir, e);
            throw new IllegalStateException("Cannot initialize upload directory: " + uploadDir, e);
        }
    }

    /**
     * Сохранение файла в директории загрузки.
     *
     * @param file файл для сохранения (не null и не пустой)
     * @return абсолютный путь к сохраненному файлу или null, если файл не передан
     * @throws IllegalArgumentException если файл имеет недопустимый формат или слишком большой
     * @throws IOException             если произошла ошибка при сохранении файла
     */
    public String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            log.debug("No file provided for storage");
            return null;
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            log.error("Invalid file format: {}", contentType);
            throw new IllegalArgumentException("Недопустимый формат файла. Используйте JPG, PNG или WEBP.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            log.error("File too large: {} bytes", file.getSize());
            throw new IllegalArgumentException("Файл слишком большой. Максимальный размер: 5MB.");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
        String fileName = System.currentTimeMillis() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        Path filePath = uploadPath.resolve(fileName).toAbsolutePath().normalize();

        // Проверка, что путь находится внутри uploadPath
        if (!filePath.startsWith(uploadPath)) {
            log.error("Attempted to store file outside upload directory: {}", filePath);
            throw new IllegalArgumentException("Недопустимый путь файла");
        }

        Files.write(filePath, file.getBytes());
        log.info("Stored file: {}", filePath);
        return filePath.toString();
    }

    /**
     * Удаление файла по указанному пути.
     *
     * @param filePath путь к файлу (может быть null)
     * @throws IOException если произошла ошибка при удалении файла
     */
    public void deleteFile(String filePath) throws IOException {
        if (filePath == null) {
            log.debug("No file path provided for deletion");
            return;
        }

        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        // Проверка, что путь находится внутри uploadPath
        if (!path.startsWith(uploadPath)) {
            log.error("Attempted to delete file outside upload directory: {}", path);
            throw new IllegalArgumentException("Недопустимый путь файла");
        }

        Files.deleteIfExists(path);
        log.info("Deleted file: {}", path);
    }
}