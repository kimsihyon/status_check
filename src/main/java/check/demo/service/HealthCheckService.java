package check.demo.service;

import check.demo.model.FFProbeResult;
import check.demo.model.IcmpResult;
import check.demo.model.metrics.HealthMetric;
import check.demo.repository.metrics.HealthMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static check.demo.service.FFProbeUtil.runFFProbe;

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
    private final IcmpChecker icmpChecker;

    @Async
    @Transactional("metricsTx") // 메트릭 DB 트랜잭션
    public void check(Long cctvId, String ip) {
        String rtspUrl = String.format("rtsp://%s:%s@%s:%s%s", username, password, ip, port, path);
        log.info("rtsp://{}:*****@{}:{}{}", username, ip, port, path);

        IcmpResult icmp = icmpChecker.check(ip);
        FFProbeResult result = runFFProbe(rtspUrl);

        HealthMetric metric = new HealthMetric();
        metric.setCctvId(cctvId);
        metric.setEventTimestamp(LocalDateTime.now());

        // ICMP 상태 처리
        switch (icmp.getStatus()) {
            case FAILED -> {
                metric.setIcmpStatus(false);
                metric.setEventCode("ICMP_FAILED");
                metric.setIcmpAvgRttMs(null);
                metric.setIcmpPacketLossPct(null);
            }
            case TIMEOUT -> {
                metric.setIcmpStatus(false);
                metric.setEventCode("ICMP_TIMEOUT");
                metric.setIcmpAvgRttMs(icmp.getAvgRttMs());
                metric.setIcmpPacketLossPct(icmp.getPacketLossPct());
            }
            case OK -> {
                metric.setIcmpStatus(true);
                if (icmp.getPacketLossPct() != null && icmp.getPacketLossPct() > 0) {
                    metric.setEventCode("ICMP_LOSS");
                } else {
                    metric.setEventCode("ICMP_OK");
                }
                metric.setIcmpAvgRttMs(icmp.getAvgRttMs());
                metric.setIcmpPacketLossPct(icmp.getPacketLossPct());
            }
        }

        // RTSP(HLS) 상태 처리
        switch (result.getStatus()) {
            case OK -> {
                metric.setHlsStatus(true);
                metric.setEventCode(icmp.isSuccess() ? "HLS_OK" : "ICMP_FAIL");
            }
            case TIMEOUT -> {
                metric.setHlsStatus(false);
                metric.setEventCode("HLS_TIMEOUT");
            }
            case ERROR -> {
                metric.setHlsStatus(false);
                metric.setEventCode("HLS_ERROR");
            }
            case PORT_UNREACHABLE -> {
                metric.setHlsStatus(false);
                metric.setEventCode("RTSP_PORT_FAIL");
            }
        }

        // 메트릭 DB 저장
        repository.save(metric);
    }
}
