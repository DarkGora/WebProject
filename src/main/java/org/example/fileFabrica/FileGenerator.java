package org.example.fileFabrica;

import org.example.model.dto.EmployeeDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface FileGenerator {
    ByteArrayOutputStream generateFile(EmployeeDto employee) throws IOException;
}