package org.example.service;

import org.example.dto.EmployeeDto;
import org.example.fileFabrica.FileGenerator;
import org.example.fileFabrica.FileGeneratorFactory;
import org.example.request.FileFormat;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class FileService {
    public ByteArrayOutputStream createFile(EmployeeDto employeeDto, FileFormat fileFormat) throws IOException {
        FileGenerator generator = FileGeneratorFactory.getFileGenerator(fileFormat);
        return generator.generateFile(employeeDto);
    }
}