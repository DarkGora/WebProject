package org.example.service.rabbitMQ;

import lombok.AllArgsConstructor;
import org.example.config.RabbitConfig;
import org.example.dto.EmployeeDto;
import org.example.fileFabrica.FileFormat;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AmqpProducerServic {
    public final RabbitTemplate template;

    public void sendMessage(EmployeeDto employee, FileFormat fileFormat) {
        template.convertAndSend(RabbitConfig.BAGiS_QUEUE,employee);
    }
}

