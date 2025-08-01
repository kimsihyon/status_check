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
        String cctvIp = "192.168.32.32";
        // 건강 상태 체크 로직 (예시로 단순히 상태를 저장)
        // 실제로는 ICMP, HLS 체크 로직이 들어가야 함
        //1. ICMP 체크
        IcmpChecker.IcmpResult icmp = icmpChecker.check(ip);
        HealthMetric metric = new HealthMetric();
        metric.setCctvId(cctvId);
        metric.setTimestamp(LocalDateTime.now());
        if (icmp.isSuccess()) {
            metric.setIcmpStatus(true);
            metric.setEventCode("CHECK_EVENT"); // 예시로 이벤트 코드 설정
            // ICMP_OK, ICMP_LOSS
            // rtt
        } else {
            metric.setIcmpStatus(false);
            metric.setEventCode("CHECK_ERROR"); // 예시로 오류 이벤트 코드 설정
            // ICMP_TIMEOUT, ICMP_FAILED
        }
        //2. HLS 체크
        metric.setHlsStatus(true); // 예시로 HLS 상태도 true로 설정
        //3. 트래픽 체크 (예시로 단순히 상태를 저장)

        // HealthMetric metric = new HealthMetric();
        // metric.setCctvId(cctvId);
        // metric.setTimestamp(LocalDateTime.now());
        // metric.setIcmpStatus(false); // 예시로 오류 상태
        // metric.setHlsStatus(true);
        // metric.setEventCode("CHECK_EVENT"); // 예시로 이벤트 코드 설정
        // metric.setTrafficInKbps(1.5f);
        // metric.setTrafficOutKbps(3.2f);

        repository.save(metric);

        // Kafka 전송 DTO 구성
        HealthMetricEventDto dto = new HealthMetricEventDto();
        dto.setCctvId(cctvId);
        dto.setTimestamp(metric.getTimestamp());
        dto.setIcmpStatus(metric.isIcmpStatus());
        dto.setHlsStatus(metric.isHlsStatus());
        dto.setEventCode(metric.getEventCode());

        producer.sendEvent(dto);
    }
}
