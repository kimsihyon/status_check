package check.demo.service;

import check.demo.dto.HealthMetricEventDto;
import check.demo.model.HealthMetric;
import check.demo.repository.HealthMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
//import check.demo.service.FfprobeChecker;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final HealthMetricRepository repository;
    private final KafkaEventProducer producer;
    private final IcmpChecker icmpChecker; // ← 의존성 주입
    private final FfprobeChecker ffprobeChecker;

    public void check(Long cctvId, String ip) {
        IcmpChecker.IcmpResult icmp = icmpChecker.check(ip);
        // TODO : 하드 코딩 지워야 함
        String rtspUrl = "rtsp://nouu30133:password@" + ip + ":554/stream1";

        FfprobeChecker.StreamStatus streamStatus = ffprobeChecker.check(rtspUrl);

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

        switch (streamStatus) {
            case OK -> {
                metric.setHlsStatus(true);
                metric.setEventCode(icmp.isSuccess() ? "HLS_OK" : "ICMP_FAIL");
            }
            case TIMEOUT -> {
                metric.setHlsStatus(false);
                metric.setEventCode("HLS_TIMEOUT");
            }
            case NOT_FOUND -> {
                metric.setHlsStatus(false);
                metric.setEventCode("HLS_NOT_FOUND");
            }
            case ERROR -> {
                metric.setHlsStatus(false);
                metric.setEventCode("HLS_ERROR");
            }
            case DOWN -> {
                metric.setHlsStatus(false);
                metric.setEventCode("RTSP_DOWN");
            }
        }


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
