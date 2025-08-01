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
    private final IcmpChecker icmpChecker; // ← 의존성 주입

    public void check(Long cctvId, String ip) {
        IcmpChecker.IcmpResult icmp = icmpChecker.check(ip);

        HealthMetric metric = new HealthMetric();
        metric.setCctvId(cctvId);
        metric.setTimestamp(LocalDateTime.now());

        if (icmp.isSuccess()) {
            metric.setIcmpStatus(true);
            metric.setEventCode("ICMP_OK");
        } else {
            metric.setIcmpStatus(false);
            metric.setEventCode("ICMP_FAIL");
        }

        // HLS는 아직 미사용 → 임시값
        metric.setHlsStatus(true);

        repository.save(metric);

        HealthMetricEventDto dto = new HealthMetricEventDto();
        dto.setCctvId(cctvId);
        dto.setTimestamp(metric.getTimestamp());
        dto.setIcmpStatus(metric.isIcmpStatus());
        dto.setHlsStatus(metric.isHlsStatus());
        dto.setEventCode(metric.getEventCode());

        producer.sendEvent(dto);
    }
}
