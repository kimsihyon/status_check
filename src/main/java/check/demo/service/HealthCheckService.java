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
    private final IcmpChecker icmpChecker;

    public void check(Long cctvId, String ip) {
        IcmpChecker.IcmpResult icmp = icmpChecker.check(ip);

        HealthMetric metric = new HealthMetric();
        metric.setCctvId(cctvId);
        metric.setTimestamp(LocalDateTime.now());

        if (icmp == null) {
            // ❌ ping 자체 실패
            metric.setIcmpStatus(false);
            metric.setEventCode("ICMP_FAILED");
            metric.setIcmpAvgRttMs(null);
            metric.setIcmpPacketLossPct(null);
        } else if (!icmp.isSuccess()) {
            // ❗ 응답 없음
            metric.setIcmpStatus(false);
            metric.setEventCode("ICMP_TIMEOUT");
            metric.setIcmpAvgRttMs(icmp.getAvgRtt());
            metric.setIcmpPacketLossPct(icmp.getPacketLossPct());
        } else if (icmp.getPacketLossPct() > 0) {
            // ⚠ 패킷 손실 있음
            metric.setIcmpStatus(true); // 응답은 있으므로 true
            metric.setEventCode("ICMP_LOSS");
            metric.setIcmpAvgRttMs(icmp.getAvgRtt());
            metric.setIcmpPacketLossPct(icmp.getPacketLossPct());
        } else {
            // ✅ 정상 응답
            metric.setIcmpStatus(true);
            metric.setEventCode("ICMP_OK");
            metric.setIcmpAvgRttMs(icmp.getAvgRtt());
            metric.setIcmpPacketLossPct(icmp.getPacketLossPct());
        }

        // HLS는 아직 미사용 → true
        metric.setHlsStatus(true);

        repository.save(metric);

        HealthMetricEventDto dto = new HealthMetricEventDto();
        dto.setCctvId(metric.getCctvId());
        dto.setTimestamp(metric.getTimestamp());
        dto.setIcmpStatus(metric.isIcmpStatus());
        dto.setHlsStatus(metric.isHlsStatus());
        dto.setEventCode(metric.getEventCode());
        dto.setIcmpAvgRttMs(metric.getIcmpAvgRttMs());
        dto.setIcmpPacketLossPct(metric.getIcmpPacketLossPct());

        producer.sendEvent(dto);
    }
}