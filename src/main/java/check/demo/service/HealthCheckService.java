package check.demo.service;

import check.demo.dto.HealthMetricEventDto;
import check.demo.model.HealthMetric;
import check.demo.repository.HealthMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final HealthMetricRepository repository;
    private final KafkaEventProducer producer;

    public void check(Long cctvId) {
        HealthMetric metric = new HealthMetric();
        metric.setCctvId(cctvId);
        metric.setTimestamp(LocalDateTime.now());
        metric.setIcmpStatus(false); // 예시로 오류 상태
        metric.setHlsStatus(true);
        metric.setTrafficInKbps(1.5f);
        metric.setTrafficOutKbps(3.2f);

        repository.save(metric);

        // Kafka 전송 DTO 구성
        HealthMetricEventDto dto = new HealthMetricEventDto();
        dto.setCctvId(cctvId);
        dto.setTimestamp(metric.getTimestamp());
        dto.setIcmpStatus(metric.isIcmpStatus());
        dto.setHlsStatus(metric.isHlsStatus());

        producer.sendEvent(dto);
    }
}
