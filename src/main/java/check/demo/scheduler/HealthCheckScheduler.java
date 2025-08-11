// src/main/java/check/demo/scheduler/HealthCheckScheduler.java
package check.demo.scheduler;

import check.demo.model.Cctv;
import check.demo.repository.CctvRepository;
import check.demo.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HealthCheckScheduler {

    private final HealthCheckService service;
    private final CctvRepository cctvRepository;

    // 10초마다 DB의 읽기모델에서 대상 조회
    @Scheduled(cron = "*/10 * * * * *")
    public void run() {
        List<Cctv> targets = cctvRepository.findAll();
        for (Cctv t : targets) {
            if (t.getIpAddress() != null && !t.getIpAddress().isBlank()) {
                service.check(t.getId(), t.getIpAddress()); // ICMP + ffprobe
            }
        }
    }
}
