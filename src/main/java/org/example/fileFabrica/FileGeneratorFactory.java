package org.example.fileFabrica;

public class FileGeneratorFactory {
    public static FileGenerator getFileGenerator(FileFormat fileFormat) {
        return switch (fileFormat) {
            case DOCX -> new DocxFileGenerator();
            case EXEL -> new ExelFileGenerator();
            default -> throw new IllegalArgumentException("Неизвестный формат файла: " + fileFormat);
        };
    }
}