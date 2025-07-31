package check.demo.service;

import check.demo.dto.HealthMetricEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendEvent(HealthMetricEventDto dto) {
        try {
            String message = objectMapper.writeValueAsString(dto);
            System.out.println("KafkaEventProducer.sendEvent: " + message);
            kafkaTemplate.send("health-events", message); // topic명은 상황에 따라 변경
            log.info("Sent message: {}", message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
