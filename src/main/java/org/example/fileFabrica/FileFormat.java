package org.example.fileFabrica;

public enum FileFormat {
    DOCX(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    EXEL(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final String extension;
    private final String contentType;

    FileFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String getExtension() {
        return extension;
    }

    public String getContentType() {
        return contentType;
    }
}