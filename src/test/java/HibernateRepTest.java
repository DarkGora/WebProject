import org.example.model.Employee;
import org.example.repository.EmployeeRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class HibernateRepTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    public void testSaveEmployee() {
        Employee employee = Employee.builder()
                .name("Test Employee")
                .email("test@example.com")
                .phoneNumber("+1234567890")
                .build();
        Employee saved = employeeRepository.save(employee);
        Assertions.assertNotNull(saved.getId());
    }
}