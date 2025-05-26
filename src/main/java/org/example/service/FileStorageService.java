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
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Slf4j
@Service
public class FileStorageService {
    @Value("${upload.dir:uploads}")
    private String uploadDir;
    private Path uploadPath;
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    // Максимальный размер файла (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * Инициализация сервиса - создание директории для загрузок
     * @throws IllegalStateException если директорию невозможно создать
     */
    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            log.info("Директория для загрузок создана: {}", uploadPath);
        } catch (IOException e) {
            log.error("Ошибка создания директории: {}", uploadDir, e);
            throw new IllegalStateException("Невозможно создать директорию для загрузок: " + uploadDir, e);
        }
    }

    /**
     * Сохраняет переданный файл в системе
     * @param file файл для сохранения
     * @return уникальное имя сохраненного файла или null, если файл пустой
     * @throws IllegalArgumentException при недопустимом формате или размере файла
     * @throws IOException при ошибках записи файла
     */
    public String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            log.debug("Пустой файл - сохранение не требуется");
            return null;
        }

        validateFile(file);

        String fileName = generateUniqueFileName(Objects.requireNonNull(file.getOriginalFilename()));
        Path filePath = uploadPath.resolve(fileName).normalize();

        validateFilePath(filePath);

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Файл сохранен: {}", fileName);
        return fileName;
    }

    /**
     * Удаляет файл по имени
     * @param fileName имя файла для удаления
     * @throws IOException при ошибках удаления файла
     */
    public void deleteFile(String fileName) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            log.debug("Не указано имя файла для удаления");
            return;
        }

        Path path = uploadPath.resolve(fileName).normalize();
        validateFilePath(path);

        Files.deleteIfExists(path);
        log.info("Файл удален: {}", fileName);
    }

    /**
     * Возвращает полный путь к файлу по его имени
     * @param fileName имя файла
     * @return абсолютный путь к файлу
     * @throws IllegalArgumentException при недопустимом имени файла
     */
    public Path getFilePath(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Имя файла не может быть пустым");
        }

        Path path = uploadPath.resolve(fileName).normalize();
        validateFilePath(path);
        return path;
    }

    /**
     * Проверяет файл на соответствие требованиям
     * @param file проверяемый файл
     * @throws IllegalArgumentException при несоответствии требованиям
     */
    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            log.error("Недопустимый тип файла: {}", contentType);
            throw new IllegalArgumentException(
                    "Поддерживаются только файлы JPG, PNG и WEBP"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.error("Превышен размер файла: {} байт", file.getSize());
            throw new IllegalArgumentException(
                    "Максимальный размер файла - 5MB"
            );
        }
    }

    /**
     * Проверяет что путь находится в разрешенной директории
     * @param filePath проверяемый путь
     * @throws IllegalArgumentException при попытке выйти за пределы uploadDir
     */
    private void validateFilePath(Path filePath) {
        if (!filePath.startsWith(uploadPath)) {
            log.error("Попытка доступа к файлу вне рабочей директории: {}", filePath);
            throw new IllegalArgumentException("Недопустимый путь к файлу");
        }
    }

    /**
     * Генерирует уникальное имя файла на основе оригинального
     * @param originalFilename исходное имя файла
     * @return безопасное уникальное имя файла
     */
    private String generateUniqueFileName(String originalFilename) {
        // Заменяем опасные символы в имени файла
        String safeFileName = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        // Добавляем UUID для уникальности
        return UUID.randomUUID() + "_" + safeFileName;
    }
}