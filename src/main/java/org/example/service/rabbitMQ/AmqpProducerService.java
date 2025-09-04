package org.example.service.rabbitMQ;

import lombok.AllArgsConstructor;
import org.example.config.RabbitConfig;
import org.example.dto.EmployeeDto;
import org.example.fileFabrica.FileFormat;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AmqpProducerService {
    private final RabbitTemplate template;

    public void sendToMailQueue(EmployeeDto employee, FileFormat fileFormat) {
        template.convertAndSend(RabbitConfig.MAIL_QUEUE, employee);
    }

    public void sendToBAGiSQueue(EmployeeDto employee, FileFormat fileFormat){
        template.convertAndSend(RabbitConfig.BAGiS_QUEUE, employee);
    }

    public void sendToQueue(EmployeeDto employee, String queueName) {
        template.convertAndSend(queueName, employee);
    }
}