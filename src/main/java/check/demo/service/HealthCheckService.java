package check.demo.service;

import check.demo.dto.HealthMetricEventDto;
import check.demo.model.HealthMetric;
import check.demo.repository.HealthMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    @Value("${RTSP_USERNAME}")
    private String username;

    @Value("${RTSP_PASSWORD}")
    private String password;

    @Value("${RTSP_PORT}")
    private String port;

    @Value("${RTSP_PATH}")
    private String path;

    private final HealthMetricRepository repository;
    private final KafkaEventProducer producer;
    private final IcmpChecker icmpChecker; // ← 의존성 주입
    private final FfprobeChecker ffprobeChecker;

    public void check(Long cctvId, String ip) {
        String rtspUrl = String.format("rtsp://%s:%s@%s:%s%s", username, password, ip, port, path);
        log.info("rtsp://{}:*****@{}:{}{}", username, ip, port, path);

        IcmpChecker.IcmpResult icmp = icmpChecker.check(ip);
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
