package org.example.mappers;

import org.example.dto.EmployeeDto;
import org.example.model.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {
    EmployeeMapper INSTANCE = Mappers.getMapper(EmployeeMapper.class);

    EmployeeDto toDto(Employee employee);
    List<EmployeeDto> toDto(List<Employee> employees);
}