package check.demo.scheduler;

import check.demo.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HealthCheckScheduler {

    private final HealthCheckService service;

    @Scheduled(cron = "*/1 * * * * *") // 매 1초마다 실행
    public void run() {
        service.check(1L); // 예시로 cctvId = 1
    }
}
