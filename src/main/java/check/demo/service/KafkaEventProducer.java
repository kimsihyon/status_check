package check.demo.service;

import check.demo.dto.HealthMetricEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendEvent(HealthMetricEventDto dto) {
        try {
            String message = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("health-events", message); // topic명은 상황에 따라 변경
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
